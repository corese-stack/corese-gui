package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

	private final RdfDataService rdfDataService = RdfDataService.getInstance();
	private final DataSourceRegistryService sourceRegistryService = DataSourceRegistryService.getInstance();
	private final GraphProjectionService graphProjectionService = GraphProjectionService.getInstance();
	private final ReasoningService reasoningService = DefaultReasoningService.getInstance();
	private final GraphMutationBus mutationBus = GraphMutationBus.getInstance();
	private final GraphActivityLogService activityLogService = GraphActivityLogService.getInstance();

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
		int beforeCount = rdfDataService.getTripleCount();
		rdfDataService.loadFile(file);
		sourceRegistryService.registerFile(file);
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.DATA_WORKSPACE));
		int insertedCount = Math.max(0, rdfDataService.getTripleCount() - beforeCount);
		String filePath = file == null ? "" : file.getAbsolutePath();
		activityLogService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "Loaded RDF file",
				filePath.isBlank() ? "Source: local file" : filePath, insertedCount, 0);
	}

	@Override
	public void loadUri(String uri) {
		int beforeCount = rdfDataService.getTripleCount();
		rdfDataService.loadUri(uri);
		sourceRegistryService.registerUri(uri);
		mutationBus.publish(GraphMutationEvent.bulkRefreshRequired(GraphMutationEvent.Source.DATA_WORKSPACE));
		int insertedCount = Math.max(0, rdfDataService.getTripleCount() - beforeCount);
		activityLogService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "Loaded RDF URI", safeString(uri),
				insertedCount, 0);
	}

	@Override
	public int reloadSources() {
		return reloadSources(sourceRegistryService.snapshot());
	}

	@Override
	public int reloadSources(List<DataSource> selectedSources) {
		int beforeCount = rdfDataService.getTripleCount();
		List<DataSource> normalizedSelection = selectedSources == null ? List.of() : List.copyOf(selectedSources);
		sourceRegistryService.replaceAll(normalizedSelection);
		rdfDataService.clearData();

		if (normalizedSelection.isEmpty()) {
			mutationBus.publish(GraphMutationEvent.clearAll(GraphMutationEvent.Source.DATA_WORKSPACE));
			activityLogService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE,
					"Reloaded data sources (empty selection)", "No source selected. Graph cleared.", 0, beforeCount);
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
		int afterCount = rdfDataService.getTripleCount();
		activityLogService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "Reloaded data sources",
				"Reloaded " + loadedCount + " source(s).", afterCount, beforeCount);
		return loadedCount;
	}

	@Override
	public void clearGraph() {
		int removedTriples = rdfDataService.getTripleCount();
		rdfDataService.clearData();
		sourceRegistryService.clear();
		mutationBus.publish(GraphMutationEvent.clearAll(GraphMutationEvent.Source.DATA_WORKSPACE));
		activityLogService.log(GraphActivityLogEntry.Source.DATA_WORKSPACE, "Cleared data graph",
				"Graph and tracked sources were cleared.", 0, removedTriples);
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
		DataWorkspaceStatusSupport.SourceStats sourceStats = DataWorkspaceStatusSupport.computeSourceStats(sources);

		Graph graph = GraphStoreService.getInstance().getGraph();
		int totalTripleCount = Math.max(0, graph.size());
		DataWorkspaceStatusSupport.GraphCountSnapshot graphCountSnapshot = DataWorkspaceStatusSupport
				.computeGraphCountSnapshot(graph, totalTripleCount, LOGGER);
		Map<String, Integer> graphTripleCounts = graphCountSnapshot.namedGraphCounts();
		int defaultGraphTripleCount = graphCountSnapshot.defaultGraphTripleCount();

		List<DataWorkspaceStatus.NamedGraphStat> namedGraphStats = DataWorkspaceStatusSupport
				.toSortedNamedGraphStats(graphTripleCounts);
		ReasoningStats reasoningStats = computeReasoningStats(graphTripleCounts);
		int explicitTripleCount = Math.max(0, totalTripleCount - reasoningStats.inferredTripleCount());

		return new DataWorkspaceStatus(totalTripleCount, explicitTripleCount, reasoningStats.inferredTripleCount(),
				defaultGraphTripleCount, sourceStats.total(), sourceStats.fileCount(), sourceStats.uriCount(),
				namedGraphStats.size(), namedGraphStats, reasoningStats.details());
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

	private record ReasoningStats(List<DataWorkspaceStatus.ReasoningStat> details, int inferredTripleCount) {
	}

	private static String safeString(String value) {
		return value == null ? "" : value.trim();
	}
}
