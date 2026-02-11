package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link DataWorkspaceService}.
 *
 * <p>
 * Coordinates data loading, source tracking, graph projection, and centralized
 * mutation notifications for the Data page.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for shared graph workspace management
public final class DefaultDataWorkspaceService implements DataWorkspaceService {

	private static final DefaultDataWorkspaceService INSTANCE = new DefaultDataWorkspaceService();

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final DataSourceRegistryService sourceRegistryService = DataSourceRegistryService.getInstance();
	private final GraphProjectionService graphProjectionService = GraphProjectionService.getInstance();
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
		int fileSourceCount = (int) sources.stream().filter(source -> source.type() == SourceType.FILE).count();
		int uriSourceCount = (int) sources.stream().filter(source -> source.type() == SourceType.URI).count();

		Graph graph = GraphStoreService.getInstance().getGraph();
		int totalTripleCount = Math.max(0, graph.size());
		String defaultGraphName = resolveDefaultGraphName(graph);
		Map<String, Integer> graphTripleCounts = computeGraphTripleCounts(graph);
		int defaultGraphTripleCount = defaultGraphName == null
				? 0
				: graphTripleCounts.getOrDefault(defaultGraphName, 0);

		List<DataWorkspaceStatus.NamedGraphStat> namedGraphStats = graphTripleCounts.entrySet().stream()
				.filter(entry -> entry.getValue() > 0)
				.filter(entry -> defaultGraphName == null || !defaultGraphName.equals(entry.getKey()))
				.map(entry -> new DataWorkspaceStatus.NamedGraphStat(entry.getKey(), entry.getValue()))
				.sorted((left, right) -> {
					int byCount = Integer.compare(right.tripleCount(), left.tripleCount());
					return byCount != 0 ? byCount : left.graphName().compareTo(right.graphName());
				}).toList();

		List<DataWorkspaceStatus.ReasoningStat> reasoningStats = new ArrayList<>();
		int inferredTripleCount = 0;
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			int profileCount = graphTripleCounts.getOrDefault(profile.namedGraphUri(), 0);
			inferredTripleCount += profileCount;
			reasoningStats
					.add(new DataWorkspaceStatus.ReasoningStat(profile.label(), profile.namedGraphUri(), profileCount));
		}
		int explicitTripleCount = Math.max(0, totalTripleCount - inferredTripleCount);

		return new DataWorkspaceStatus(totalTripleCount, explicitTripleCount, inferredTripleCount,
				defaultGraphTripleCount, sources.size(), fileSourceCount, uriSourceCount, namedGraphStats.size(),
				namedGraphStats, reasoningStats);
	}

	private static Map<String, Integer> computeGraphTripleCounts(Graph graph) {
		Map<String, Integer> counts = new HashMap<>();
		for (Node graphNode : graph.getGraphNodes()) {
			if (graphNode == null || graphNode.getLabel() == null || graphNode.getLabel().isBlank()) {
				continue;
			}
			counts.put(graphNode.getLabel(), countEdges(graph.getEdges(graphNode, Graph.IGRAPH)));
		}
		return counts;
	}

	private static String resolveDefaultGraphName(Graph graph) {
		Node defaultGraphNode = graph.getDefaultGraphNode();
		if (defaultGraphNode == null) {
			return null;
		}
		String label = defaultGraphNode.getLabel();
		return (label == null || label.isBlank()) ? null : label;
	}

	private static int countEdges(Iterable<Edge> edges) {
		int count = 0;
		if (edges == null) {
			return count;
		}
		for (@SuppressWarnings("unused")
		Edge ignored : edges) {
			count++;
		}
		return count;
	}
}
