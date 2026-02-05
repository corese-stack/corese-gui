package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Service for loading RDF data into the shared graph.
 *
 * <p>
 * This service provides a clean API for RDF data operations, handling I/O and format detection
 * while delegating actual graph operations to the {@link GraphStoreService}.
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 *   <li>Only one data loading service should coordinate access to the shared graph</li>
 *   <li>Prevents concurrent loading issues and resource conflicts</li>
 *   <li>Maintains consistent error handling and logging across the application</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * RdfDataService service = RdfDataService.getInstance();
 * try {
 *     service.loadFile(new File("data.ttl"));
 * } catch (RdfLoadException e) {
 *     // Handle loading error
 * }
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for coordinated graph access
public class RdfDataService {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final Logger logger = LoggerFactory.getLogger(RdfDataService.class);
    private static final RdfDataService INSTANCE = new RdfDataService();

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    private RdfDataService() {}

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Returns the singleton instance of the RDF data service.
     *
     * @return The RdfDataService instance.
     */
    public static RdfDataService getInstance() {
        return INSTANCE;
    }

    /**
     * Loads an RDF file into the shared graph.
     *
     * <p>
     * Automatically detects the RDF format based on file extension.
     * Supported formats include Turtle, RDF/XML, N-Triples, JSON-LD, and others.
     *
     * @param file The RDF file to load.
     * @throws IllegalArgumentException if the file is null or doesn't exist.
     * @throws RdfLoadException if the file cannot be read or parsed.
     */
    @SuppressWarnings("java:S2139")
    public void loadFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }

        logger.info("Loading RDF file: {}", file.getAbsolutePath());
        
        fr.inria.corese.core.api.Loader.format format = LoadFormat.getFormat(file.getName());
        if (format == fr.inria.corese.core.api.Loader.format.UNDEF_FORMAT) {
            logger.warn("Could not detect format from extension for {}, Corese will attempt auto-detection.", file.getName());
        }

        try (InputStream stream = new FileInputStream(file)) {
            Load loader = Load.create(GraphStoreService.getInstance().getGraph());
            loader.parse(stream, format);
            logger.info("Successfully loaded {} triples from file.", GraphStoreService.getInstance().size());
        } catch (Exception e) { // Generic catch is justified: Corese can throw various exception types
            String errorMsg = String.format("Failed to load RDF file '%s': %s", file.getName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new RdfLoadException(errorMsg, e);
        }
    }

    /**
     * Clears all RDF data from the graph.
     *
     * <p>
     * This operation removes all triples from the graph but keeps the graph instance intact.
     */
    public void clearData() {
        logger.info("Clearing all RDF data from graph.");
        GraphStoreService.getInstance().clear();
    }

    /**
     * Checks if the graph contains any RDF data.
     *
     * @return true if the graph has at least one triple, false otherwise.
     */
    public boolean hasData() {
        return GraphStoreService.getInstance().hasData();
    }

    /**
     * Returns the number of triples currently in the graph.
     *
     * @return The count of RDF triples.
     */
    public int getTripleCount() {
        return GraphStoreService.getInstance().size();
    }

    // ==============================================================================================
    // Exception Classes
    // ==============================================================================================

    /**
     * Exception thrown when RDF file loading fails.
     */
    public static class RdfLoadException extends RuntimeException {
        public RdfLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}