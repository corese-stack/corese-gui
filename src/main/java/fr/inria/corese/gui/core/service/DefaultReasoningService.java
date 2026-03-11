package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.logic.Entailment;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.gui.core.service.activity.GraphActivityLogEntry;
import fr.inria.corese.gui.core.service.activity.GraphActivityLogService;
import fr.inria.corese.gui.core.service.mutation.GraphMutationBus;
import fr.inria.corese.gui.core.service.mutation.GraphMutationEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ReasoningService}.
 *
 * <p>
 * Rule-based inferred triples are isolated in one named graph per reasoning
 * profile, while native RDFS subset entailment is managed directly on the
 * shared graph.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for shared graph reasoning state
public final class DefaultReasoningService implements ReasoningService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReasoningService.class);
	private static final DefaultReasoningService INSTANCE = new DefaultReasoningService();
	private static final String RULE_FILE_GRAPH_PREFIX = "urn:corese:inference:custom:";
	private static final EnumMap<ReasoningProfile, String> BUILT_IN_PROFILE_SOURCE_PATHS = createProfileSourcePaths();

	private final EnumMap<ReasoningProfile, Boolean> profileStates = new EnumMap<>(ReasoningProfile.class);
	private final LinkedHashMap<String, RuleFileDefinition> ruleFiles = new LinkedHashMap<>();
	private final GraphMutationBus mutationBus = GraphMutationBus.getInstance();
	private final GraphActivityLogService activityLogService = GraphActivityLogService.getInstance();
	private boolean rdfsSubsetEnabled = false;

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
		int tripleCountBefore = graphSizeSnapshot();
		profileStates.put(profile, enabled);
		try {
			recomputeEnabledProfilesInternal(false);
			int tripleCountAfter = graphSizeSnapshot();
			String action = enabled ? "Enabled reasoning profile" : "Disabled reasoning profile";
			logActivityDelta(GraphActivityLogEntry.Source.REASONING_SERVICE, action, profile.label(), tripleCountBefore,
					tripleCountAfter);
		} catch (RuntimeException e) {
			profileStates.put(profile, previous);
			throw e;
		}
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
	public synchronized BuiltInProfileSource getBuiltInProfileSource(ReasoningProfile profile) {
		validateProfile(profile);
		String sourcePath = BUILT_IN_PROFILE_SOURCE_PATHS.get(profile);
		if (sourcePath == null || sourcePath.isBlank()) {
			throw new IllegalArgumentException("No built-in source declared for profile: " + profile);
		}
		String sourceContent = readBuiltInSource(sourcePath);
		return new BuiltInProfileSource(profile.label(), sourcePath, sourceContent);
	}

	@Override
	public synchronized void setRdfsSubsetEnabled(boolean enabled) {
		if (rdfsSubsetEnabled == enabled) {
			return;
		}
		Graph mainGraph = GraphStoreService.getInstance().getGraph();
		int tripleCountBefore = Math.max(0, mainGraph.size());
		boolean previous = rdfsSubsetEnabled;
		rdfsSubsetEnabled = enabled;
		try {
			if (enabled || previous) {
				mainGraph.setRDFSEntailment(enabled);
			}
			mainGraph.clean();
		} catch (RuntimeException e) {
			rdfsSubsetEnabled = previous;
			try {
				if (enabled || previous) {
					mainGraph.setRDFSEntailment(previous);
				}
				mainGraph.clean();
			} catch (RuntimeException rollbackError) {
				e.addSuppressed(rollbackError);
			}
			throw new ReasoningException("Failed to update native RDFS subset entailment.", e);
		}
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.REASONING));
		int tripleCountAfter = Math.max(0, mainGraph.size());
		String action = enabled ? "Enabled native RDFS subset" : "Disabled native RDFS subset";
		logActivityDelta(GraphActivityLogEntry.Source.REASONING_SERVICE, action,
				"Native Corese entailment on the shared graph.", tripleCountBefore, tripleCountAfter);
	}

	@Override
	public synchronized boolean isRdfsSubsetEnabled() {
		return rdfsSubsetEnabled;
	}

	@Override
	public synchronized boolean hasAnyEnabledProfile() {
		for (boolean enabled : profileStates.values()) {
			if (enabled) {
				return true;
			}
		}
		for (RuleFileDefinition rule : ruleFiles.values()) {
			if (rule.enabled()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized void addRuleFile(File ruleFile) {
		String sourcePath = normalizeRuleFile(ruleFile);
		if (findRuleBySourcePath(sourcePath) != null) {
			throw new IllegalArgumentException("Rule file is already loaded: " + sourcePath);
		}

		String ruleId = UUID.randomUUID().toString();
		String label = ruleFile.getName();
		String namedGraphUri = createRuleFileGraphUri(sourcePath, label);
		int tripleCountBefore = graphSizeSnapshot();
		ruleFiles.put(ruleId, new RuleFileDefinition(ruleId, label, sourcePath, namedGraphUri, true));
		try {
			recomputeEnabledProfilesInternal(false);
			int tripleCountAfter = graphSizeSnapshot();
			logActivityDelta(GraphActivityLogEntry.Source.RULE_FILE_SERVICE, "Added rule file",
					label + " -> " + namedGraphUri, tripleCountBefore, tripleCountAfter);
		} catch (RuntimeException e) {
			ruleFiles.remove(ruleId);
			throw e;
		}
	}

	@Override
	public synchronized void removeRuleFile(String ruleId) {
		validateRuleId(ruleId);
		RuleFileDefinition removed = ruleFiles.remove(ruleId);
		if (removed == null) {
			return;
		}
		int tripleCountBefore = graphSizeSnapshot();
		try {
			recomputeEnabledProfilesInternal(false);
			int tripleCountAfter = graphSizeSnapshot();
			logActivityDelta(GraphActivityLogEntry.Source.RULE_FILE_SERVICE, "Removed rule file", removed.label(),
					tripleCountBefore, tripleCountAfter);
		} catch (RuntimeException e) {
			ruleFiles.put(removed.id(), removed);
			throw e;
		}
	}

	@Override
	public synchronized void removeAllRuleFiles() {
		if (ruleFiles.isEmpty()) {
			return;
		}
		int removedCount = ruleFiles.size();
		int tripleCountBefore = graphSizeSnapshot();
		LinkedHashMap<String, RuleFileDefinition> previousRules = new LinkedHashMap<>(ruleFiles);
		ruleFiles.clear();
		try {
			recomputeEnabledProfilesInternal(false);
			int tripleCountAfter = graphSizeSnapshot();
			logActivityDelta(GraphActivityLogEntry.Source.RULE_FILE_SERVICE, "Removed all rule files",
					removedCount + " file(s)", tripleCountBefore, tripleCountAfter);
		} catch (RuntimeException e) {
			ruleFiles.putAll(previousRules);
			throw e;
		}
	}

	@Override
	public synchronized void setRuleFileEnabled(String ruleId, boolean enabled) {
		validateRuleId(ruleId);
		RuleFileDefinition current = ruleFiles.get(ruleId);
		if (current == null) {
			throw new IllegalArgumentException("Unknown rule file id: " + ruleId);
		}
		if (current.enabled() == enabled) {
			return;
		}
		int tripleCountBefore = graphSizeSnapshot();
		ruleFiles.put(ruleId, current.withEnabled(enabled));
		try {
			recomputeEnabledProfilesInternal(false);
			int tripleCountAfter = graphSizeSnapshot();
			String action = enabled ? "Enabled rule file" : "Disabled rule file";
			logActivityDelta(GraphActivityLogEntry.Source.RULE_FILE_SERVICE, action, current.label(), tripleCountBefore,
					tripleCountAfter);
		} catch (RuntimeException e) {
			ruleFiles.put(ruleId, current);
			throw e;
		}
	}

	@Override
	public synchronized List<RuleFileState> snapshotRuleFiles() {
		return ruleFiles.values().stream().map(DefaultReasoningService::toRuleFileState).toList();
	}

	@Override
	public synchronized void applyRuleFileSelection(Collection<String> enabledRuleIds) {
		if (ruleFiles.isEmpty()) {
			return;
		}
		int tripleCountBefore = graphSizeSnapshot();

		LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
		if (enabledRuleIds != null) {
			for (String ruleId : enabledRuleIds) {
				if (ruleId != null && !ruleId.isBlank()) {
					selectedIds.add(ruleId);
				}
			}
		}

		for (String selectedId : selectedIds) {
			if (!ruleFiles.containsKey(selectedId)) {
				throw new IllegalArgumentException("Unknown rule file id: " + selectedId);
			}
		}

		LinkedHashMap<String, RuleFileDefinition> previousRules = new LinkedHashMap<>(ruleFiles);
		for (Map.Entry<String, RuleFileDefinition> entry : ruleFiles.entrySet()) {
			RuleFileDefinition current = entry.getValue();
			boolean enabled = selectedIds.contains(entry.getKey());
			if (current.enabled() != enabled) {
				entry.setValue(current.withEnabled(enabled));
			}
		}

		try {
			recomputeEnabledProfilesInternal(false);
			int tripleCountAfter = graphSizeSnapshot();
			logActivityDelta(GraphActivityLogEntry.Source.RULE_FILE_SERVICE, "Updated rule file selection",
					selectedIds.size() + " file(s) enabled", tripleCountBefore, tripleCountAfter);
		} catch (RuntimeException e) {
			ruleFiles.clear();
			ruleFiles.putAll(previousRules);
			throw e;
		}
	}

	@Override
	public synchronized void recomputeEnabledProfiles() {
		recomputeEnabledProfilesInternal(true);
	}

	private void recomputeEnabledProfilesInternal(boolean logActivity) {
		Graph mainGraph = GraphStoreService.getInstance().getGraph();
		int tripleCountBefore = Math.max(0, mainGraph.size());
		try (var _ = mutationBus.suspendPublishing()) {
			Graph assertedSnapshot = createAssertedSnapshot(mainGraph);
			replaceGraphContent(mainGraph, assertedSnapshot);

			if (!hasAnyEnabledProfile()) {
				mainGraph.clean();
				mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.REASONING));
				if (logActivity) {
					int tripleCountAfter = Math.max(0, mainGraph.size());
					logActivityDelta(GraphActivityLogEntry.Source.REASONING_SERVICE, "Recomputed reasoning inferences",
							"No reasoning profile or rule file is enabled.", tripleCountBefore, tripleCountAfter);
				}
				return;
			}

			for (ReasoningProfile profile : ReasoningProfile.values()) {
				if (isEnabled(profile)) {
					applyProfileInference(assertedSnapshot, mainGraph, profile);
				}
			}
			for (RuleFileDefinition ruleFile : ruleFiles.values()) {
				if (ruleFile.enabled()) {
					applyRuleFileInference(assertedSnapshot, mainGraph, ruleFile);
				}
			}

			mainGraph.clean();
		} catch (RuntimeException e) {
			throw new ReasoningException("Failed to recompute reasoning profiles.", e);
		}
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.REASONING));
		if (logActivity) {
			int tripleCountAfter = Math.max(0, mainGraph.size());
			String details = countEnabledProfiles() + " profile(s), " + countEnabledRuleFiles()
					+ " rule file(s) enabled";
			logActivityDelta(GraphActivityLogEntry.Source.REASONING_SERVICE, "Recomputed reasoning inferences", details,
					tripleCountBefore, tripleCountAfter);
		}
	}

	@Override
	public synchronized void resetAllProfiles() {
		int tripleCountBefore = graphSizeSnapshot();
		boolean previousRdfsSubsetState = rdfsSubsetEnabled;
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			profileStates.put(profile, false);
		}
		for (Map.Entry<String, RuleFileDefinition> entry : ruleFiles.entrySet()) {
			RuleFileDefinition rule = entry.getValue();
			if (rule.enabled()) {
				entry.setValue(rule.withEnabled(false));
			}
		}

		Graph mainGraph = GraphStoreService.getInstance().getGraph();
		try (var _ = mutationBus.suspendPublishing()) {
			if (previousRdfsSubsetState) {
				mainGraph.setRDFSEntailment(false);
			}
			rdfsSubsetEnabled = false;
			Graph assertedSnapshot = createAssertedSnapshot(mainGraph);
			replaceGraphContent(mainGraph, assertedSnapshot);
			mainGraph.clean();
		} catch (RuntimeException e) {
			throw new ReasoningException("Failed to reset reasoning profiles.", e);
		}
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.REASONING));
		int tripleCountAfter = graphSizeSnapshot();
		logActivityDelta(GraphActivityLogEntry.Source.REASONING_SERVICE, "Reset reasoning profiles",
				"All built-in profiles, native RDFS subset, and rule files were disabled.", tripleCountBefore,
				tripleCountAfter);
	}

	private void applyProfileInference(Graph assertedSnapshot, Graph targetGraph, ReasoningProfile profile) {
		try {
			Graph workingGraph = assertedSnapshot.copy();
			RuleEngine engine = RuleEngine.create(workingGraph);
			configureRuleEngine(engine);
			engine.setProfile(toCoreseProfile(profile));
			engine.processWithoutWorkflow();

			int insertedCount = insertInferredEdges(workingGraph, targetGraph, profile.namedGraphUri());

			LOGGER.debug("Applied reasoning profile {} with {} inferred triple(s).", profile, insertedCount);
		} catch (LoadException | EngineException e) {
			throw new ReasoningException("Failed to apply reasoning profile " + profile + ".", e);
		}
	}

	private void applyRuleFileInference(Graph assertedSnapshot, Graph targetGraph, RuleFileDefinition ruleFile) {
		try {
			Graph workingGraph = assertedSnapshot.copy();
			RuleEngine engine = RuleEngine.create(workingGraph);
			configureRuleEngine(engine);

			Load ruleLoad = Load.create(workingGraph);
			ruleLoad.setEngine(engine);
			ruleLoad.setQueryProcess(engine.getQueryProcess());
			ruleLoad.parse(resolveRuleFileLoadSource(ruleFile.sourcePath()), Loader.format.RULE_FORMAT);
			engine.processWithoutWorkflow();

			int insertedCount = insertInferredEdges(workingGraph, targetGraph, ruleFile.namedGraphUri());
			LOGGER.debug("Applied rule file {} with {} inferred triple(s).", ruleFile.sourcePath(), insertedCount);
		} catch (LoadException | EngineException e) {
			throw new ReasoningException("Failed to apply rule file " + ruleFile.label() + ".", e);
		}
	}

	private void configureRuleEngine(RuleEngine engine) {
		if (engine == null) {
			return;
		}
		// Native RDFS subset entailment changes predicate reachability semantics, so
		// keep the rule engine on the safe path when both modes coexist.
		engine.setSpeedUp(!rdfsSubsetEnabled);
	}

	private String resolveRuleFileLoadSource(String sourcePath) {
		if (sourcePath == null || sourcePath.isBlank()) {
			return "";
		}
		File sourceFile = new File(sourcePath);
		if (sourceFile.isFile()) {
			// Corese rule loader expects a URL-compatible source string; file URIs
			// keep behavior stable for local paths with spaces or non-ASCII chars.
			return sourceFile.toURI().toString();
		}
		return sourcePath;
	}

	private int insertInferredEdges(Graph sourceGraph, Graph targetGraph, String namedGraphUri) {
		IDatatype inferenceGraph = DatatypeMap.newResource(namedGraphUri);
		int insertedCount = 0;
		for (Edge edge : sourceGraph.getEdges()) {
			if (edge.getGraph() == null || !Entailment.RULE.equals(edge.getGraph().getLabel())) {
				continue;
			}
			IDatatype subject = edge.getNode(0).getDatatypeValue();
			IDatatype predicate = edge.getEdgeNode().getDatatypeValue();
			IDatatype object = edge.getNode(1).getDatatypeValue();
			if (targetGraph.insert(inferenceGraph, subject, predicate, object) != null) {
				insertedCount++;
			}
		}
		return insertedCount;
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
		return graphLabel.startsWith(RULE_FILE_GRAPH_PREFIX);
	}

	private RuleEngine.Profile toCoreseProfile(ReasoningProfile profile) {
		return switch (profile) {
			case RDFS -> RuleEngine.Profile.RDFS;
			case OWL_RL -> RuleEngine.Profile.OWLRL;
			case OWL_RL_LITE -> RuleEngine.Profile.OWLRL_LITE;
			case OWL_RL_EXT -> RuleEngine.Profile.OWLRL_EXT;
		};
	}

	private int graphSizeSnapshot() {
		return Math.max(0, GraphStoreService.getInstance().size());
	}

	private int countEnabledProfiles() {
		int enabledProfileCount = 0;
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			if (Boolean.TRUE.equals(profileStates.get(profile))) {
				enabledProfileCount++;
			}
		}
		return enabledProfileCount;
	}

	private int countEnabledRuleFiles() {
		int enabledRuleFileCount = 0;
		for (RuleFileDefinition ruleFile : ruleFiles.values()) {
			if (ruleFile.enabled()) {
				enabledRuleFileCount++;
			}
		}
		return enabledRuleFileCount;
	}

	private void logActivityDelta(GraphActivityLogEntry.Source source, String action, String details,
			int tripleCountBefore, int tripleCountAfter) {
		int insertedCount = Math.max(0, tripleCountAfter - tripleCountBefore);
		int deletedCount = Math.max(0, tripleCountBefore - tripleCountAfter);
		activityLogService.log(source, action, details, insertedCount, deletedCount);
	}

	private static EnumMap<ReasoningProfile, String> createProfileSourcePaths() {
		EnumMap<ReasoningProfile, String> paths = new EnumMap<>(ReasoningProfile.class);
		paths.put(ReasoningProfile.RDFS, "/rule/rdfs.rul");
		paths.put(ReasoningProfile.OWL_RL, "/rule/owlrl.rul");
		paths.put(ReasoningProfile.OWL_RL_LITE, "/rule/owlrllite.rul");
		paths.put(ReasoningProfile.OWL_RL_EXT, "/rule/owlrlext.rul");
		return paths;
	}

	private String readBuiltInSource(String sourcePath) {
		try (InputStream stream = DefaultReasoningService.class.getResourceAsStream(sourcePath)) {
			if (stream == null) {
				throw new IllegalStateException("Built-in rule resource not found: " + sourcePath);
			}
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new ReasoningException("Failed to read built-in rule resource: " + sourcePath, e);
		}
	}

	private void validateProfile(ReasoningProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("profile must not be null");
		}
	}

	private static String createRuleFileGraphUri(String sourcePath, String ruleLabel) {
		String readableSegment = toGraphReadableSegment(ruleLabel, sourcePath);
		String stableSuffix = toStablePathSuffix(sourcePath);
		return RULE_FILE_GRAPH_PREFIX + readableSegment + ":" + stableSuffix;
	}

	private static String toGraphReadableSegment(String ruleLabel, String sourcePath) {
		String candidate = ruleLabel;
		if (candidate == null || candidate.isBlank()) {
			candidate = new File(sourcePath).getName();
		}
		String normalized = candidate.trim().toLowerCase(Locale.ROOT);
		if (normalized.endsWith(".rul")) {
			normalized = normalized.substring(0, normalized.length() - 4);
		}
		normalized = normalized.replaceAll("[^a-z0-9]+", "-");
		normalized = normalized.replaceAll("(^-+)|(-+$)", "");
		return normalized.isBlank() ? "rule-file" : normalized;
	}

	private static String toStablePathSuffix(String sourcePath) {
		String uuid = UUID.nameUUIDFromBytes(sourcePath.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
		return uuid.substring(0, 12);
	}

	private String normalizeRuleFile(File ruleFile) {
		if (ruleFile == null) {
			throw new IllegalArgumentException("ruleFile must not be null");
		}
		if (!ruleFile.isFile()) {
			throw new IllegalArgumentException("Rule file does not exist: " + ruleFile);
		}
		String normalizedPath = ruleFile.getAbsoluteFile().toPath().normalize().toString();
		if (!normalizedPath.toLowerCase(Locale.ROOT).endsWith(".rul")) {
			throw new IllegalArgumentException("Unsupported rule format: " + ruleFile.getName());
		}
		return normalizedPath;
	}

	private RuleFileDefinition findRuleBySourcePath(String sourcePath) {
		for (RuleFileDefinition rule : ruleFiles.values()) {
			if (rule.sourcePath().equals(sourcePath)) {
				return rule;
			}
		}
		return null;
	}

	private void validateRuleId(String ruleId) {
		if (ruleId == null || ruleId.isBlank()) {
			throw new IllegalArgumentException("ruleId must not be blank");
		}
	}

	private static RuleFileState toRuleFileState(RuleFileDefinition rule) {
		return new RuleFileState(rule.id(), rule.label(), rule.sourcePath(), rule.namedGraphUri(), rule.enabled());
	}

	private record RuleFileDefinition(String id, String label, String sourcePath, String namedGraphUri,
			boolean enabled) {
		private RuleFileDefinition {
			Objects.requireNonNull(id, "id must not be null");
			Objects.requireNonNull(label, "label must not be null");
			Objects.requireNonNull(sourcePath, "sourcePath must not be null");
			Objects.requireNonNull(namedGraphUri, "namedGraphUri must not be null");
		}

		private RuleFileDefinition withEnabled(boolean value) {
			return new RuleFileDefinition(id, label, sourcePath, namedGraphUri, value);
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
