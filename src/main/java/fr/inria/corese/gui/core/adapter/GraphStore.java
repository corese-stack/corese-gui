package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Corese graph store.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Maintain the singleton Corese Graph instance.</li>
 *   <li>Load RDF data from files.</li>
 * </ul>
 */
public class GraphStore {

  private static final Logger logger = LoggerFactory.getLogger(GraphStore.class);

  private static GraphStore instance;
  private Graph graph;

  private GraphStore() {
    this.graph = Graph.create();
  }

  public static synchronized GraphStore getInstance() {
    if (instance == null) {
      instance = new GraphStore();
    }
    return instance;
  }

  // ============================================================================================
  // Graph Lifecycle
  // ============================================================================================

  synchronized Graph getGraph() {
    return graph;
  }

  private synchronized int getTripletCount() {
    return (graph != null) ? graph.size() : 0;
  }

  public synchronized boolean hasData() {
    return getTripletCount() > 0;
  }

  // ============================================================================================
  // Loading
  // ============================================================================================

  public synchronized void loadFile(File file) throws Exception {
    if (file == null || !file.exists()) {
      throw new IllegalArgumentException("File does not exist: " + file);
    }
    try {
      Load loader = Load.create(graph);
      Load.format format = detectFormatFromFileName(file.getName());
      loader.parse(file.getAbsolutePath(), format);
      logger.info("Loaded file: {}", file.getAbsolutePath());
    } catch (Exception e) {
      logger.error("Error loading file {}: {}", file.getName(), e.getMessage());
      throw e;
    }
  }

  // ============================================================================================
  // Utils
  // ============================================================================================

  private Load.format detectFormatFromFileName(String fileName) {
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

  // Logging is handled directly by this store.
}
