package fr.inria.corese.gui.core.service.data;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.File;
import java.util.List;

/**
 * GUI-facing facade for all Data page graph operations.
 *
 * <p>
 * This contract isolates the GUI from low-level graph/corese APIs.
 */
public interface DataWorkspaceService {

	/**
	 * Loads one RDF file into the shared graph and tracks it for reload.
	 *
	 * @param file
	 *            RDF file to load
	 */
	void loadFile(File file);

	/**
	 * Loads RDF data from a URI and tracks it for reload.
	 *
	 * @param uri
	 *            remote URI
	 */
	void loadUri(String uri);

	/**
	 * Reloads all tracked sources (file + URI).
	 *
	 * @return number of reloaded sources
	 */
	int reloadSources();

	/**
	 * Reloads only selected tracked sources.
	 *
	 * <p>
	 * This operation rebuilds the graph from scratch based on selected sources.
	 *
	 * @param selectedSources
	 *            sources to keep and reload
	 * @return number of reloaded sources
	 */
	int reloadSources(List<DataSource> selectedSources);

	/**
	 * Clears the shared graph and forgets tracked sources.
	 */
	void clearGraph();

	/**
	 * Returns the current graph as JSON-LD.
	 *
	 * @return JSON-LD snapshot or empty string
	 */
	String getGraphSnapshotJsonLd();

	/**
	 * Serializes the current graph in the requested RDF format.
	 *
	 * @param format
	 *            RDF format
	 * @return serialized graph string
	 */
	String serializeGraph(SerializationFormat format);

	/**
	 * Returns supported RDF export formats.
	 *
	 * @return immutable list of formats
	 */
	List<SerializationFormat> getRdfExportFormats();

	/**
	 * Returns whether the graph currently contains data.
	 *
	 * @return true when graph is non-empty
	 */
	boolean hasData();

	/**
	 * Returns graph triple count.
	 *
	 * @return triple count
	 */
	int getTripleCount();

	/**
	 * Returns number of tracked load sources.
	 *
	 * @return source count
	 */
	int getSourceCount();

	/**
	 * Returns tracked data sources used by the reload workflow.
	 *
	 * @return immutable source list
	 */
	List<DataSource> getTrackedSources();

	/**
	 * Returns a complete status snapshot for Data page counters and tooltips.
	 *
	 * @return workspace status snapshot
	 */
	DataWorkspaceStatus getStatus();
}
