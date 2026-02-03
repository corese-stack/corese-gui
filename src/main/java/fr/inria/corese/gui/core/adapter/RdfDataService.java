package fr.inria.corese.gui.core.adapter;

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
 * <p>Handles I/O operations for RDF files and updates the central {@link GraphStore}.
 */
public class RdfDataService {

    private static final Logger logger = LoggerFactory.getLogger(RdfDataService.class);
    private static final RdfDataService INSTANCE = new RdfDataService();

    private RdfDataService() {}

    public static RdfDataService getInstance() {
        return INSTANCE;
    }

    /**
     * Loads an RDF file into the shared graph.
     *
     * @param file The file to load.
     * @throws Exception If the file cannot be read or parsed.
     */
    public void loadFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist or is null.");
        }

        logger.info("Loading file: {}", file.getAbsolutePath());
        
        // Use LoadFormat to detect format from extension
        Load.format format = LoadFormat.getFormat(file.getName());
        if (format == Load.format.UNDEF_FORMAT) {
             // Fallback or warning? Corese might auto-detect better, but let's log.
             logger.warn("Could not detect format from extension for {}, attempting auto-detection.", file.getName());
        }

        try (InputStream stream = new FileInputStream(file)) {
            Load loader = Load.create(GraphStore.getInstance().getGraph());
            loader.parse(stream, format);
            logger.info("File loaded successfully.");
        } catch (Exception e) {
            logger.error("Failed to load file: " + file.getName(), e);
            throw e;
        }
    }

    /**
     * Clears all data from the graph.
     */
    public void clearData() {
        GraphStore.getInstance().clear();
    }

    /**
     * Checks if the graph contains any data.
     */
    public boolean hasData() {
        return GraphStore.getInstance().hasData();
    }
}