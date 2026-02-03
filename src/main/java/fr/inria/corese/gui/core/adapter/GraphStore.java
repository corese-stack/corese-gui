package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal package-private holder for the Corese {@link Graph} instance.
 *
 * <p>This class acts as the single source of truth for the RDF data within the GUI adapter layer.
 * It is intentionally package-private to prevent direct access to the {@code Graph} object
 * from outside the {@code adapter} package, enforcing strict encapsulation.
 *
 * <p>All read/write operations on the graph should be coordinated through the public services:
 * <ul>
 *   <li>{@link RdfDataService} for loading and clearing data</li>
 *   <li>{@link QueryService} for executing SPARQL queries</li>
 *   <li>{@link ShaclService} for validation</li>
 * </ul>
 */
class GraphStore {

    private static final Logger logger = LoggerFactory.getLogger(GraphStore.class);
    private static final GraphStore INSTANCE = new GraphStore();

    private final Graph graph;

    /**
     * Private constructor to enforce singleton usage within the package.
     */
    private GraphStore() {
        this.graph = Graph.create();
        logger.debug("Corese Graph initialized.");
    }

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
        // Create a new graph or clear the existing one?
        // Graph.create() is safer to ensure a clean slate, but we need to see if we can just clear.
        // Corese Graph usually supports clearing.
        // For safety and to reset indices/listeners, we'll assume emptying it is enough.
        // Actually, creating a new graph might break references held by other components if they cached it.
        // Since we control access via getGraph(), strictly speaking, if we replaced the instance,
        // we'd need to ensure everyone calls getGraph() every time.
        // To be safe, we will just empty the current graph instance.
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
     * Checks if the graph is not empty.
     *
     * @return true if the graph has data, false otherwise
     */
    boolean hasData() {
        return graph.size() > 0;
    }
}