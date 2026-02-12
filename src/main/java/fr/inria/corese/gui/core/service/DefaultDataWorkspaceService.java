package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link DataWorkspaceService}.
 *
 * <p>
 * Coordinates data loading, source tracking, graph projection, and centralized
 * mutation notifications for the Data page.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for shared graph workspace management
public final class DefaultDataWorkspaceService implements DataWorkspaceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataWorkspaceService.class);
	private static final DefaultDataWorkspaceService INSTANCE = new DefaultDataWorkspaceService();
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

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final DataSourceRegistryService sourceRegistryService = DataSourceRegistryService.getInstance();
	private final GraphProjectionService graphProjectionService = GraphProjectionService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();
	private final GraphMutationBus mutationBus = GraphMutationBus.getInstance();

	private DefaultDataWorkspaceService() {
	}

	/**
	 * Returns the singleton workspace service.
	 *
	 * @return workspace service instance
	 */
	public static DataWorkspaceService getInstance() {
		return INSTANCE;
	}

	@Override
	public void loadFile(File file) {
		rdfDataService.loadFile(file);
		sourceRegistryService.registerFile(file);
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.DATA_WORKSPACE));
	}

	@Override
	public void loadUri(String uri) {
		rdfDataService.loadUri(uri);
		sourceRegistryService.registerUri(uri);
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.DATA_WORKSPACE));
	}

	@Override
	public int reloadSources() {
		return reloadSources(sourceRegistryService.snapshot());
	}

	@Override
	public int reloadSources(List<DataSource> selectedSources) {
		List<DataSource> normalizedSelection = selectedSources == null ? List.of() : List.copyOf(selectedSources);
		sourceRegistryService.replaceAll(normalizedSelection);
		rdfDataService.clearData();

		if (normalizedSelection.isEmpty()) {
			mutationBus.publish(GraphMutationEvent.clearAll(GraphMutationEvent.Source.DATA_WORKSPACE));
			return 0;
		}

		int loadedCount = 0;
		for (DataSource source : normalizedSelection) {
			if (source.type() == SourceType.FILE) {
				rdfDataService.loadFile(new File(source.location()));
				loadedCount++;
			} else if (source.type() == SourceType.URI) {
				rdfDataService.loadUri(source.location());
				loadedCount++;
			}
		}
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.DATA_WORKSPACE));
		return loadedCount;
	}

	@Override
	public void clearGraph() {
		rdfDataService.clearData();
		sourceRegistryService.clear();
		mutationBus.publish(GraphMutationEvent.clearAll(GraphMutationEvent.Source.DATA_WORKSPACE));
	}

	@Override
	public String getGraphSnapshotJsonLd() {
		return graphProjectionService.snapshotJsonLd();
	}

	@Override
	public String serializeGraph(SerializationFormat format) {
		return graphProjectionService.serializeGraph(format);
	}

	@Override
	public List<SerializationFormat> getRdfExportFormats() {
		return graphProjectionService.supportedRdfExportFormats();
	}

	@Override
	public boolean hasData() {
		return rdfDataService.hasData();
	}

	@Override
	public int getTripleCount() {
		return rdfDataService.getTripleCount();
	}

	@Override
	public int getSourceCount() {
		return sourceRegistryService.size();
	}

	@Override
	public List<DataSource> getTrackedSources() {
		return sourceRegistryService.snapshot();
	}

	@Override
	public DataWorkspaceStatus getStatus() {
		List<DataSource> sources = sourceRegistryService.snapshot();
		SourceStats sourceStats = computeSourceStats(sources);

		Graph graph = GraphStoreService.getInstance().getGraph();
		int totalTripleCount = Math.max(0, graph.size());
		GraphCountSnapshot graphCountSnapshot = computeGraphCountSnapshot(graph, totalTripleCount);
		Map<String, Integer> graphTripleCounts = graphCountSnapshot.namedGraphCounts();
		int defaultGraphTripleCount = graphCountSnapshot.defaultGraphTripleCount();

		List<DataWorkspaceStatus.NamedGraphStat> namedGraphStats = toSortedNamedGraphStats(graphTripleCounts);
		ReasoningStats reasoningStats = computeReasoningStats(graphTripleCounts);
		int explicitTripleCount = Math.max(0, totalTripleCount - reasoningStats.inferredTripleCount());

		return new DataWorkspaceStatus(totalTripleCount, explicitTripleCount, reasoningStats.inferredTripleCount(),
				defaultGraphTripleCount, sourceStats.total(), sourceStats.fileCount(), sourceStats.uriCount(),
				namedGraphStats.size(), namedGraphStats, reasoningStats.details());
	}

	private static Map<String, Integer> computeGraphTripleCounts(Graph graph) {
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
			LOGGER.warn("Unable to compute named graph counts safely, status will fallback to global counters", e);
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
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int saturatingAdd(int left, int right) {
		long sum = (long) left + right;
		return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
	}

	private static GraphCountSnapshot computeGraphCountSnapshot(Graph graph, int totalTripleCount) {
		Map<String, Integer> normalizedCounts = normalizeGraphTripleCounts(computeGraphTripleCounts(graph));
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

	private static SourceStats computeSourceStats(List<DataSource> sources) {
		int fileCount = (int) sources.stream().filter(source -> source.type() == SourceType.FILE).count();
		int uriCount = (int) sources.stream().filter(source -> source.type() == SourceType.URI).count();
		return new SourceStats(sources.size(), fileCount, uriCount);
	}

	private static List<DataWorkspaceStatus.NamedGraphStat> toSortedNamedGraphStats(
			Map<String, Integer> graphTripleCounts) {
		return graphTripleCounts.entrySet().stream().filter(entry -> entry.getValue() > 0)
				.map(entry -> new DataWorkspaceStatus.NamedGraphStat(entry.getKey(), entry.getValue()))
				.sorted((left, right) -> {
					int byCount = Integer.compare(right.tripleCount(), left.tripleCount());
					return byCount != 0 ? byCount : left.graphName().compareTo(right.graphName());
				}).toList();
	}

	private ReasoningStats computeReasoningStats(Map<String, Integer> graphTripleCounts) {
		List<DataWorkspaceStatus.ReasoningStat> details = new ArrayList<>();
		int inferredTripleCount = 0;
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			int profileCount = graphTripleCounts.getOrDefault(profile.namedGraphUri(), 0);
			inferredTripleCount += profileCount;
			details.add(new DataWorkspaceStatus.ReasoningStat(profile.label(), profile.namedGraphUri(), profileCount));
		}
		for (RuleFileState ruleFile : reasoningService.snapshotRuleFiles()) {
			int ruleCount = graphTripleCounts.getOrDefault(ruleFile.namedGraphUri(), 0);
			inferredTripleCount += ruleCount;
			details.add(new DataWorkspaceStatus.ReasoningStat(ruleFile.label(), ruleFile.namedGraphUri(), ruleCount));
		}
		return new ReasoningStats(details, inferredTripleCount);
	}

	private record SourceStats(int total, int fileCount, int uriCount) {
	}

	private record ReasoningStats(List<DataWorkspaceStatus.ReasoningStat> details, int inferredTripleCount) {
	}

	private record GraphCountSnapshot(Map<String, Integer> namedGraphCounts, int defaultGraphTripleCount) {
	}
}
