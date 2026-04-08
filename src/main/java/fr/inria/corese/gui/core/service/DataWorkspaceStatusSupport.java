package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.gui.core.service.data.DataSource;
import fr.inria.corese.gui.core.service.data.DataWorkspaceStatus;
import fr.inria.corese.gui.core.service.data.SourceType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * Shared status aggregation helpers for the Data workspace.
 */
public final class DataWorkspaceStatusSupport {

	private static final String CORESE_DEFAULT_GRAPH_URI = "http://ns.inria.fr/corese.core.kgram/default";
	private static final String CORESE_DEFAULT_GRAPH_URI_LEGACY = "http://ns.inria.fr/edelweiss/2010/kgram/default";
	private static final String CORESE_DEFAULT_GRAPH_URI_ALT = "http://ns.inria.fr/corese/kgram/default";
	private static final Set<String> CORESE_DEFAULT_GRAPH_ALIASES = Set.of(CORESE_DEFAULT_GRAPH_URI,
			CORESE_DEFAULT_GRAPH_URI_LEGACY, CORESE_DEFAULT_GRAPH_URI_ALT);

	private DataWorkspaceStatusSupport() {
		throw new AssertionError("Utility class");
	}

	public static SourceStats computeSourceStats(List<DataSource> sources) {
		List<DataSource> safeSources = sources == null ? List.of() : sources;
		int fileCount = (int) safeSources.stream().filter(source -> source.type() == SourceType.FILE).count();
		int uriCount = (int) safeSources.stream().filter(source -> source.type() == SourceType.URI).count();
		return new SourceStats(safeSources.size(), fileCount, uriCount);
	}

	public static GraphCountSnapshot computeGraphCountSnapshot(Graph graph, int totalTripleCount, Logger logger) {
		GraphMetricsSnapshot metrics = collectGraphMetrics(graph, Set.of(), totalTripleCount, logger);
		return new GraphCountSnapshot(metrics.namedGraphCounts(), metrics.defaultGraphTripleCount());
	}

	public static List<DataWorkspaceStatus.NamedGraphStat> toSortedNamedGraphStats(
			Map<String, Integer> graphTripleCounts) {
		if (graphTripleCounts == null || graphTripleCounts.isEmpty()) {
			return List.of();
		}
		return graphTripleCounts.entrySet().stream().filter(entry -> entry.getValue() > 0)
				.map(entry -> new DataWorkspaceStatus.NamedGraphStat(entry.getKey(), entry.getValue()))
				.sorted((left, right) -> {
					int byCount = Integer.compare(right.tripleCount(), left.tripleCount());
					return byCount != 0 ? byCount : left.graphName().compareTo(right.graphName());
				}).toList();
	}

	public static DistinctTripleSnapshot computeDistinctTripleSnapshot(Graph graph,
			Set<String> managedInferenceGraphNames) {
		int totalTripleCount = graph == null ? 0 : Math.max(0, graph.size());
		GraphMetricsSnapshot metrics = collectGraphMetrics(graph, managedInferenceGraphNames, totalTripleCount, null);
		return new DistinctTripleSnapshot(metrics.totalTripleCount(), metrics.assertedTripleCount(),
				metrics.inferredTripleCount());
	}

	private static GraphMetricsSnapshot collectGraphMetrics(Graph graph, Set<String> managedInferenceGraphNames,
			int totalTripleCountFallback, Logger logger) {
		int safeFallback = Math.max(0, totalTripleCountFallback);
		if (graph == null || graph.size() == 0) {
			return new GraphMetricsSnapshot(Map.of(), safeFallback, 0, 0, 0);
		}
		try {
			Set<String> normalizedManagedInferenceGraphNames = normalizeGraphNameSet(managedInferenceGraphNames);
			Map<String, Set<TripleKey>> namedGraphTriples = new HashMap<>();
			Set<TripleKey> defaultGraphTriples = new HashSet<>();
			Set<TripleKey> visibleTriples = new HashSet<>();
			Set<TripleKey> assertedTriples = new HashSet<>();
			Set<TripleKey> inferredTriples = new HashSet<>();

			for (Edge edge : graph.getEdges()) {
				EdgeSnapshot snapshot = snapshotEdge(edge);
				if (snapshot == null) {
					continue;
				}
				visibleTriples.add(snapshot.triple());
				if (isDefaultGraph(snapshot.graphName())) {
					defaultGraphTriples.add(snapshot.triple());
				} else {
					namedGraphTriples.computeIfAbsent(snapshot.graphName(), ignored -> new HashSet<>())
							.add(snapshot.triple());
				}
				if (normalizedManagedInferenceGraphNames.contains(snapshot.graphName())) {
					inferredTriples.add(snapshot.triple());
				} else {
					assertedTriples.add(snapshot.triple());
				}
			}

			Set<TripleKey> inferredOnlyTriples = new HashSet<>(inferredTriples);
			inferredOnlyTriples.removeAll(assertedTriples);
			return new GraphMetricsSnapshot(toCountMap(namedGraphTriples), defaultGraphTriples.size(), visibleTriples.size(),
					assertedTriples.size(), inferredOnlyTriples.size());
		} catch (Exception e) {
			if (logger != null) {
				logger.warn("Unable to compute graph metrics safely, status will fallback to global counters", e);
			}
			return new GraphMetricsSnapshot(Map.of(), safeFallback, safeFallback, safeFallback, 0);
		}
	}

	private static Set<String> normalizeGraphNameSet(Set<String> graphNames) {
		if (graphNames == null || graphNames.isEmpty()) {
			return Set.of();
		}
		Set<String> normalized = new HashSet<>();
		for (String graphName : graphNames) {
			String normalizedGraphName = normalizeGraphName(graphName);
			if (normalizedGraphName != null && !normalizedGraphName.isBlank()) {
				normalized.add(normalizedGraphName);
			}
		}
		return normalized;
	}

	private static Map<String, Integer> toCountMap(Map<String, Set<TripleKey>> namedGraphTriples) {
		if (namedGraphTriples.isEmpty()) {
			return Map.of();
		}
		Map<String, Integer> counts = new HashMap<>();
		for (Map.Entry<String, Set<TripleKey>> entry : namedGraphTriples.entrySet()) {
			counts.put(entry.getKey(), entry.getValue().size());
		}
		return counts;
	}

	private static String normalizeGraphName(String graphName) {
		if (graphName == null || graphName.isBlank()) {
			return graphName;
		}
		return isCoreseDefaultGraphAlias(graphName) ? CORESE_DEFAULT_GRAPH_URI : graphName;
	}

	private static boolean isDefaultGraph(String graphName) {
		return graphName == null || graphName.isBlank() || CORESE_DEFAULT_GRAPH_URI.equals(graphName);
	}

	private static boolean isCoreseDefaultGraphAlias(String graphName) {
		return CORESE_DEFAULT_GRAPH_ALIASES.contains(graphName);
	}

	private static EdgeSnapshot snapshotEdge(Edge edge) {
		if (edge == null || edge.getEdgeNode() == null || edge.getNode(0) == null || edge.getNode(1) == null) {
			return null;
		}
		IDatatype subject = edge.getNode(0).getDatatypeValue();
		IDatatype predicate = edge.getEdgeNode().getDatatypeValue();
		IDatatype object = edge.getNode(1).getDatatypeValue();
		if (subject == null || predicate == null || object == null) {
			return null;
		}
		String graphName = null;
		if (edge.getGraph() != null) {
			graphName = normalizeGraphName(edge.getGraph().getLabel());
			if ((graphName == null || graphName.isBlank()) && edge.getGraph().getDatatypeValue() != null) {
				graphName = normalizeGraphName(edge.getGraph().getDatatypeValue().getLabel());
			}
		}
		return new EdgeSnapshot(graphName, new TripleKey(toTermKey(subject), toTermKey(predicate), toTermKey(object)));
	}

	private static TermKey toTermKey(IDatatype datatype) {
		if (datatype == null) {
			return new TermKey("null", "", "", "");
		}
		if (datatype.isURI()) {
			return new TermKey("uri", datatype.getLabel(), "", "");
		}
		if (datatype.isBlank()) {
			return new TermKey("blank", datatype.getLabel(), "", "");
		}
		if (datatype.isLiteral()) {
			return new TermKey("literal", datatype.getLabel(), safe(datatype.getDatatypeURI()),
					safe(datatype.getLang()).toLowerCase(Locale.ROOT));
		}
		if (datatype.isTriple()) {
			return new TermKey("triple", datatype.toSparql(false, true), "", "");
		}
		return new TermKey("other", datatype.getLabel(), safe(datatype.getDatatypeURI()),
				safe(datatype.getLang()).toLowerCase(Locale.ROOT));
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	public record SourceStats(int total, int fileCount, int uriCount) {
	}

	public record GraphCountSnapshot(Map<String, Integer> namedGraphCounts, int defaultGraphTripleCount) {
	}

	public record DistinctTripleSnapshot(int totalTripleCount, int assertedTripleCount, int inferredTripleCount) {
	}

	private record GraphMetricsSnapshot(Map<String, Integer> namedGraphCounts, int defaultGraphTripleCount,
			int totalTripleCount, int assertedTripleCount, int inferredTripleCount) {
	}

	private record EdgeSnapshot(String graphName, TripleKey triple) {
	}

	private record TripleKey(TermKey subject, TermKey predicate, TermKey object) {
	}

	private record TermKey(String kind, String lexicalForm, String datatypeUri, String languageTag) {
	}
}
