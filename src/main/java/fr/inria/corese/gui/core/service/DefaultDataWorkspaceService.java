package fr.inria.corese.gui.core.service;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import java.io.File;
import java.util.List;

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
}
