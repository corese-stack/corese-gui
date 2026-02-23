package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal holder for the shared Corese {@link Graph} instance.
 *
 * <p>
 * This class acts as the single source of truth for the RDF data within the GUI
 * adapter layer.
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 * <li>Only one RDF graph should exist for the entire application</li>
 * <li>All adapter services must operate on the same graph instance</li>
 * <li>Centralized visibility ensures controlled access through service
 * APIs</li>
 * </ul>
 *
 * <p>
 * All read/write operations on the graph should be coordinated through the
 * public services:
 * <ul>
 * <li>{@link RdfDataService} for loading and clearing data</li>
 * <li>{@link QueryService} for executing SPARQL queries</li>
 * <li>{@link ShaclService} for validation</li>
 * </ul>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for global graph management
public class GraphStoreService {

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphStoreService.class);
	private static final GraphStoreService INSTANCE = new GraphStoreService();

	private final Graph graph;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	/**
	 * Private constructor to enforce singleton usage within the package.
	 */
	private GraphStoreService() {
		this.graph = Graph.create();
		LOGGER.debug("Corese Graph initialized.");
	}

	// ==============================================================================================
	// Public API (internal-use by core services)
	// ==============================================================================================

	/**
	 * Returns the singleton instance of the GraphStoreService.
	 *
	 * @return the singleton instance
	 */
	public static GraphStoreService getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the underlying Corese Graph object.
	 *
	 * @return the {@link Graph} instance
	 */
	public Graph getGraph() {
		return graph;
	}

	/**
	 * Clears all data from the graph.
	 */
	public void clear() {
		try {
			graph.clear();
			LOGGER.debug("Graph data cleared.");
		} catch (Exception e) {
			LOGGER.error("Failed to clear graph", e);
		}
	}

	/**
	 * Returns the number of triples in the graph.
	 *
	 * @return the size of the graph
	 */
	public int size() {
		return graph.size();
	}

	/**
	 * Checks if the graph contains any data.
	 *
	 * @return true if the graph has data, false otherwise
	 */
	public boolean hasData() {
		return graph.size() > 0;
	}
}
