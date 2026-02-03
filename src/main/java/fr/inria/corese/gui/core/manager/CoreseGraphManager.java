package fr.inria.corese.gui.core.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified manager for Corese Graph operations.
 * Consolidates functionality previously split between DataManager and GraphManager.
 * 
 * Capabilities:
 * - Manage the singleton Graph instance.
 * - Load data from Files and URLs.
 * - Clear the graph.
 * - Export the graph to files or strings.
 */
public class CoreseGraphManager {

    private static final Logger logger = LoggerFactory.getLogger(CoreseGraphManager.class);

    private static CoreseGraphManager instance;
    private Graph graph;

    private CoreseGraphManager() {
        this.graph = Graph.create();
    }

    public static synchronized CoreseGraphManager getInstance() {
        if (instance == null) {
            instance = new CoreseGraphManager();
        }
        return instance;
    }

    // ============================================================================================
    // Graph Lifecycle
    // ============================================================================================

    synchronized Graph getGraph() {
        return graph;
    }

    public synchronized void clearGraph() {
        if (graph != null) {
            graph.clear(); // Or re-instantiate if clear() is insufficient for deep reset
            // For a full reset in Corese, sometimes new Graph.create() is safer, 
            // but let's stick to clear() if supported or re-create.
            // GraphManager.java used to do this logic:
             // this.graph = Graph.create(); // if re-creation is preferred
        } else {
            graph = Graph.create();
        }
        logger.info("Graph cleared.");
    }

    public synchronized int getTripletCount() {
        return (graph != null) ? graph.size() : 0;
    }
    
    public synchronized boolean isDataLoaded() {
        return getTripletCount() > 0;
    }

    // ============================================================================================
    // Loading
    // ============================================================================================

    public synchronized void loadFromFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        try {
            Load loader = Load.create(graph);
            Load.format format = detectFormat(file.getName());
            loader.parse(file.getAbsolutePath(), format);
            logger.info("Loaded file: {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error loading file {}: {}", file.getName(), e.getMessage());
            throw e;
        }
    }

    public synchronized void loadFromUrl(String url) throws Exception {
        if (url == null || url.isBlank()) {
             throw new IllegalArgumentException("URL cannot be empty.");
        }
        try {
            Load loader = Load.create(graph);
            // URL loading might need auto-detection or specific format if known.
            // Corese Load.parse(url) typically handles content negotiation or extension detection.
            loader.parse(url); 
            logger.info("Loaded URL: {}", url);
        } catch (Exception e) {
             logger.error("Error loading URL {}: {}", url, e.getMessage());
             throw e;
        }
    }

    // ============================================================================================
    // Exporting
    // ============================================================================================

    public synchronized String exportToString(SerializationFormat format) {
        return ExportManager.getInstance().formatGraph(graph, format);
    }

    public synchronized void exportToFile(File file, SerializationFormat format) throws Exception {
         String content = exportToString(format);
         try (FileOutputStream out = new FileOutputStream(file)) {
             out.write(content.getBytes(StandardCharsets.UTF_8));
         }
         logger.info("Graph exported to: {}", file.getAbsolutePath());
    }

    // ============================================================================================
    // Utils
    // ============================================================================================

    private Load.format detectFormat(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".ttl")) {
            return Load.format.TURTLE_FORMAT;
        } else if (lowerName.endsWith(".rdf") || lowerName.endsWith(".xml")) {
            return Load.format.RDFXML_FORMAT;
        } else if (lowerName.endsWith(".jsonld")) {
            return Load.format.JSONLD_FORMAT;
        } else if (lowerName.endsWith(".nt")) {
             return Load.format.NT_FORMAT;
        } else if (lowerName.endsWith(".nq")) {
             return Load.format.NQUADS_FORMAT;
        } else if (lowerName.endsWith(".trig")) {
             return Load.format.TRIG_FORMAT;
        }
        return Load.format.TURTLE_FORMAT; // Default
    }

    // Logging is handled directly by this manager.
}
