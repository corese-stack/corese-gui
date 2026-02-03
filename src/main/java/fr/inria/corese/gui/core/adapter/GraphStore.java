package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton manager for the Corese RDF graph store.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Access to the singleton Corese {@link Graph} instance
 *   <li>RDF file loading with automatic format detection
 *   <li>Graph state queries (data presence checks)
 * </ul>
 *
 * <p>Thread-safe implementation using synchronized methods.
 *
 * @see Graph
 * @see Load
 */
public class GraphStore {

  private static final Logger logger = LoggerFactory.getLogger(GraphStore.class);

  private static GraphStore instance;
  private final Graph graph;

  private GraphStore() {
    this.graph = Graph.create();
  }

  /**
   * Returns the singleton instance of GraphStore.
   *
   * @return the singleton GraphStore instance
   */
  public static synchronized GraphStore getInstance() {
    if (instance == null) {
      instance = new GraphStore();
    }
    return instance;
  }

  // ============================================================================================
  // Graph Access
  // ============================================================================================

  /**
   * Returns the underlying Corese Graph instance.
   *
   * <p>This method is used by other adapter services (QueryService, ShaclService)
   * to access the graph for operations.
   *
   * @return the Corese Graph
   */
  public synchronized Graph getGraph() {
    return graph;
  }

  /**
   * Checks whether the graph contains any RDF triples.
   *
   * @return {@code true} if the graph contains at least one triple, {@code false} otherwise
   */
  public synchronized boolean hasData() {
    return graph != null && graph.size() > 0;
  }

  // ============================================================================================
  // RDF Loading
  // ============================================================================================

  /**
   * Loads RDF data from a file into the graph.
   *
   * <p>The RDF format is automatically detected based on the file extension:
   * <ul>
   *   <li>.ttl → Turtle</li>
   *   <li>.rdf, .xml → RDF/XML</li>
   *   <li>.jsonld → JSON-LD</li>
   *   <li>.nt → N-Triples</li>
   *   <li>.nq → N-Quads</li>
   *   <li>.trig → TriG</li>
   *   <li>default → Turtle</li>
   * </ul>
   *
   * @param file the RDF file to load
   * @throws IllegalArgumentException if the file is null or does not exist
   * @throws Exception if an error occurs during parsing
   */
  public synchronized void loadFile(File file) throws Exception {
    if (file == null || !file.exists()) {
      throw new IllegalArgumentException("File does not exist: " + file);
    }

    Load loader = Load.create(graph);
    Load.format format = detectFormatFromFileName(file.getName());
    loader.parse(file.getAbsolutePath(), format);
    logger.info("Successfully loaded RDF file: {} (format: {})", file.getAbsolutePath(), format);
  }

  // ============================================================================================
  // Private Helpers
  // ============================================================================================

  /**
   * Detects the RDF format from a file name based on its extension.
   *
   * @param fileName the name of the file
   * @return the detected RDF format, defaulting to Turtle if unknown
   */
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

    return Load.format.TURTLE_FORMAT;
  }
}
