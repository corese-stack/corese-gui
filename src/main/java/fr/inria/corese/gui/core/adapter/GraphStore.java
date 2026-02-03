package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal package-private holder for the Corese {@link Graph} instance.
 *
 * <p>
 * This class acts as the single source of truth for the RDF data within the GUI adapter layer.
 * It is intentionally package-private to prevent direct access to the {@code Graph} object
 * from outside the {@code adapter} package, enforcing strict encapsulation.
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 *   <li>Only one RDF graph should exist for the entire application</li>
 *   <li>All adapter services must operate on the same graph instance</li>
 *   <li>Package-private visibility ensures controlled access only through service APIs</li>
 * </ul>
 *
 * <p>
 * All read/write operations on the graph should be coordinated through the public services:
 * <ul>
 *   <li>{@link RdfDataService} for loading and clearing data</li>
 *   <li>{@link QueryService} for executing SPARQL queries</li>
 *   <li>{@link ShaclService} for validation</li>
 * </ul>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for global graph management
class GraphStore {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final Logger logger = LoggerFactory.getLogger(GraphStore.class);
    private static final GraphStore INSTANCE = new GraphStore();

    private final Graph graph;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    /**
     * Private constructor to enforce singleton usage within the package.
     */
    private GraphStore() {
        this.graph = Graph.create();
        logger.debug("Corese Graph initialized.");
    }

    // ==============================================================================================
    // Package-Private API
    // ==============================================================================================

    /**
     * Returns the singleton instance of the GraphStore.
     *
     * @return the singleton instance
     */
    static GraphStore getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the underlying Corese Graph object.
     *
     * @return the {@link Graph} instance
     */
    Graph getGraph() {
        return graph;
    }

    /**
     * Clears all data from the graph.
     */
    void clear() {
        try {
            graph.empty();
            logger.info("Graph data cleared.");
        } catch (Exception e) {
            logger.error("Failed to clear graph", e);
        }
    }

    /**
     * Returns the number of triples in the graph.
     *
     * @return the size of the graph
     */
    int size() {
        return graph.size();
    }

    /**
     * Checks if the graph contains any data.
     *
     * @return true if the graph has data, false otherwise
     */
    boolean hasData() {
        return graph.size() > 0;
    }
}