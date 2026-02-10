package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.logic.Entailment;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ReasoningService}.
 *
 * <p>
 * Inferred triples are isolated in one named graph per reasoning profile.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for shared graph reasoning state
public final class DefaultReasoningService implements ReasoningService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReasoningService.class);
	private static final DefaultReasoningService INSTANCE = new DefaultReasoningService();

	private final EnumMap<ReasoningProfile, Boolean> profileStates = new EnumMap<>(ReasoningProfile.class);
	private final GraphMutationBus mutationBus = GraphMutationBus.getInstance();

	private DefaultReasoningService() {
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			profileStates.put(profile, false);
		}
	}

	/**
	 * Returns the singleton reasoning service.
	 *
	 * @return reasoning service instance
	 */
	public static ReasoningService getInstance() {
		return INSTANCE;
	}

	@Override
	public synchronized void setEnabled(ReasoningProfile profile, boolean enabled) {
		validateProfile(profile);
		boolean previous = profileStates.getOrDefault(profile, false);
		if (previous == enabled) {
			return;
		}
		profileStates.put(profile, enabled);
		recomputeEnabledProfiles();
	}

	@Override
	public synchronized boolean isEnabled(ReasoningProfile profile) {
		validateProfile(profile);
		return profileStates.getOrDefault(profile, false);
	}

	@Override
	public synchronized Map<ReasoningProfile, Boolean> snapshotStates() {
		return Map.copyOf(profileStates);
	}

	@Override
	public synchronized boolean hasAnyEnabledProfile() {
		for (boolean enabled : profileStates.values()) {
			if (enabled) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized void recomputeEnabledProfiles() {
		Graph mainGraph = GraphStoreService.getInstance().getGraph();
		try (AutoCloseable ignored = GraphMutationCollectorService.getInstance().suspendPublishing()) {
			Graph assertedSnapshot = createAssertedSnapshot(mainGraph);
			replaceGraphContent(mainGraph, assertedSnapshot);

			if (!hasAnyEnabledProfile()) {
				mainGraph.clean();
				mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.REASONING));
				return;
			}

			for (ReasoningProfile profile : ReasoningProfile.values()) {
				if (isEnabled(profile)) {
					applyProfileInference(assertedSnapshot, mainGraph, profile);
				}
			}

			mainGraph.clean();
		} catch (Exception e) {
			throw new ReasoningException("Failed to recompute reasoning profiles.", e);
		}
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.REASONING));
	}

	@Override
	public synchronized void resetAllProfiles() {
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			profileStates.put(profile, false);
		}

		Graph mainGraph = GraphStoreService.getInstance().getGraph();
		try (AutoCloseable ignored = GraphMutationCollectorService.getInstance().suspendPublishing()) {
			Graph assertedSnapshot = createAssertedSnapshot(mainGraph);
			replaceGraphContent(mainGraph, assertedSnapshot);
			mainGraph.clean();
		} catch (Exception e) {
			throw new ReasoningException("Failed to reset reasoning profiles.", e);
		}
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.REASONING));
	}

	private void applyProfileInference(Graph assertedSnapshot, Graph targetGraph, ReasoningProfile profile)
			throws Exception {
		Graph workingGraph = assertedSnapshot.copy();
		RuleEngine engine = RuleEngine.create(workingGraph);
		engine.setSpeedUp(true);
		engine.setProfile(toCoreseProfile(profile));
		engine.processWithoutWorkflow();

		IDatatype profileGraph = DatatypeMap.newResource(profile.namedGraphUri());
		int insertedCount = 0;
		for (Edge edge : workingGraph.getEdges()) {
			if (edge.getGraph() == null || !Entailment.RULE.equals(edge.getGraph().getLabel())) {
				continue;
			}
			IDatatype subject = edge.getNode(0).getDatatypeValue();
			IDatatype predicate = edge.getEdgeNode().getDatatypeValue();
			IDatatype object = edge.getNode(1).getDatatypeValue();
			if (targetGraph.insert(profileGraph, subject, predicate, object) != null) {
				insertedCount++;
			}
		}

		LOGGER.debug("Applied reasoning profile {} with {} inferred triple(s).", profile, insertedCount);
	}

	private Graph createAssertedSnapshot(Graph sourceGraph) {
		Graph snapshot = Graph.create();
		for (Edge edge : sourceGraph.getEdges()) {
			if (isManagedInferenceEdge(edge)) {
				continue;
			}
			copyEdge(snapshot, edge);
		}
		snapshot.clean();
		return snapshot;
	}

	private void replaceGraphContent(Graph targetGraph, Graph sourceGraph) {
		targetGraph.clear();
		for (Edge edge : sourceGraph.getEdges()) {
			copyEdge(targetGraph, edge);
		}
	}

	private void copyEdge(Graph targetGraph, Edge edge) {
		if (edge == null || edge.getGraph() == null || edge.getEdgeNode() == null || edge.getNode(0) == null
				|| edge.getNode(1) == null) {
			return;
		}
		IDatatype graphName = edge.getGraph().getDatatypeValue();
		IDatatype subject = edge.getNode(0).getDatatypeValue();
		IDatatype predicate = edge.getEdgeNode().getDatatypeValue();
		IDatatype object = edge.getNode(1).getDatatypeValue();
		if (graphName == null || subject == null || predicate == null || object == null) {
			return;
		}
		targetGraph.insert(graphName, subject, predicate, object);
	}

	private boolean isManagedInferenceEdge(Edge edge) {
		return edge != null && edge.getGraph() != null && isManagedInferenceGraphLabel(edge.getGraph().getLabel());
	}

	private boolean isManagedInferenceGraphLabel(String graphLabel) {
		if (graphLabel == null || graphLabel.isBlank()) {
			return false;
		}
		if (Entailment.RULE.equals(graphLabel) || Entailment.CONSTRAINT.equals(graphLabel)) {
			return true;
		}
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			if (profile.namedGraphUri().equals(graphLabel)) {
				return true;
			}
		}
		return false;
	}

	private RuleEngine.Profile toCoreseProfile(ReasoningProfile profile) {
		return switch (profile) {
			case RDFS -> RuleEngine.Profile.RDFS;
			case OWL_RL -> RuleEngine.Profile.OWLRL;
			case OWL_RL_LITE -> RuleEngine.Profile.OWLRL_LITE;
			case OWL_RL_EXT -> RuleEngine.Profile.OWLRL_EXT;
		};
	}

	private void validateProfile(ReasoningProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("profile must not be null");
		}
	}

	/**
	 * Exception thrown when reasoning profile operations fail.
	 */
	public static class ReasoningException extends RuntimeException {
		public ReasoningException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
