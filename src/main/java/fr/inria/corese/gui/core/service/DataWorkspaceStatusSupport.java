package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.gui.core.service.data.DataSource;
import fr.inria.corese.gui.core.service.data.DataWorkspaceStatus;
import fr.inria.corese.gui.core.service.data.SourceType;
import java.util.HashMap;
import java.util.List;
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
	private static final String NAMED_GRAPH_COUNT_QUERY = """
			SELECT ?g (COUNT(*) AS ?count)
			WHERE { GRAPH ?g { ?s ?p ?o } }
			GROUP BY ?g
			""";

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
		Map<String, Integer> normalizedCounts = normalizeGraphTripleCounts(computeGraphTripleCounts(graph, logger));
		int reportedTripleTotal = normalizedCounts.values().stream().mapToInt(Integer::intValue).sum();
		int unassignedTripleCount = Math.max(0, totalTripleCount - reportedTripleTotal);
		int defaultGraphTripleCount = saturatingAdd(normalizedCounts.getOrDefault(CORESE_DEFAULT_GRAPH_URI, 0),
				unassignedTripleCount);

		// The Corese default graph is treated as the default graph and is excluded from
		// named graph stats.
		Map<String, Integer> namedGraphCounts = new HashMap<>(normalizedCounts);
		namedGraphCounts.remove(CORESE_DEFAULT_GRAPH_URI);
		return new GraphCountSnapshot(namedGraphCounts, defaultGraphTripleCount);
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

	private static Map<String, Integer> computeGraphTripleCounts(Graph graph, Logger logger) {
		Map<String, Integer> counts = new HashMap<>();
		if (graph == null || graph.size() == 0) {
			return counts;
		}
		try {
			QueryProcess queryProcess = QueryProcess.create(graph);
			Mappings mappings = queryProcess.query(NAMED_GRAPH_COUNT_QUERY);
			for (Mapping mapping : mappings) {
				IDatatype graphValue = mapping.getValue("?g");
				if (graphValue == null || graphValue.getLabel() == null || graphValue.getLabel().isBlank()) {
					continue;
				}
				IDatatype countValue = mapping.getValue("?count");
				counts.put(graphValue.getLabel(), toNonNegativeInt(countValue));
			}
		} catch (Exception e) {
			if (logger != null) {
				logger.warn("Unable to compute named graph counts safely, status will fallback to global counters", e);
			}
		}
		return counts;
	}

	private static int toNonNegativeInt(IDatatype value) {
		if (value == null || value.getLabel() == null || value.getLabel().isBlank()) {
			return 0;
		}
		try {
			long parsed = Long.parseLong(value.getLabel());
			if (parsed <= 0) {
				return 0;
			}
			return parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;
		} catch (NumberFormatException _) {
			return 0;
		}
	}

	private static int saturatingAdd(int left, int right) {
		long sum = (long) left + right;
		return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
	}

	private static Map<String, Integer> normalizeGraphTripleCounts(Map<String, Integer> rawCounts) {
		Map<String, Integer> normalized = new HashMap<>();
		if (rawCounts == null || rawCounts.isEmpty()) {
			return normalized;
		}
		for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
			String graphName = normalizeGraphName(entry.getKey());
			if (graphName == null || graphName.isBlank()) {
				continue;
			}
			int count = Math.max(0, entry.getValue());
			normalized.put(graphName, saturatingAdd(normalized.getOrDefault(graphName, 0), count));
		}
		return normalized;
	}

	private static String normalizeGraphName(String graphName) {
		if (graphName == null || graphName.isBlank()) {
			return graphName;
		}
		return isCoreseDefaultGraphAlias(graphName) ? CORESE_DEFAULT_GRAPH_URI : graphName;
	}

	private static boolean isCoreseDefaultGraphAlias(String graphName) {
		return CORESE_DEFAULT_GRAPH_ALIASES.contains(graphName);
	}

	public record SourceStats(int total, int fileCount, int uriCount) {
	}

	public record GraphCountSnapshot(Map<String, Integer> namedGraphCounts, int defaultGraphTripleCount) {
	}
}
