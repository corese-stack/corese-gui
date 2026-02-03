package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.shacl.Shacl;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationReportItem;
import fr.inria.corese.gui.core.model.ValidationResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton service for SHACL validation operations.
 *
 * <p>This service:
 * <ul>
 *   <li>Validates RDF data against SHACL shapes using Corese's SHACL engine
 *   <li>Caches validation reports internally with unique identifiers
 *   <li>Extracts structured validation items from reports
 *   <li>Formats cached reports in various serialization formats
 * </ul>
 *
 * <p>Thread-safe implementation with concurrent cache management.
 *
 * @see ValidationResult
 * @see ValidationReportItem
 */
public class ShaclService {

  private static final Logger logger = LoggerFactory.getLogger(ShaclService.class);

  private static ShaclService instance;

  private final Map<String, Graph> reportCache;

  private ShaclService() {
    this.reportCache = new ConcurrentHashMap<>();
  }

  /**
   * Returns the singleton instance of ShaclService.
   *
   * @return the singleton ShaclService instance
   */
  public static synchronized ShaclService getInstance() {
    if (instance == null) {
      instance = new ShaclService();
    }
    return instance;
  }

  // ============================================================================================
  // Validation
  // ============================================================================================

  /**
   * Validates the current data graph against the provided SHACL shapes.
   *
   * <p>The shapes content is parsed as Turtle format and validated against the data graph
   * from {@link GraphStore}. The validation report is cached and can be retrieved later
   * for formatting or item extraction.
   *
   * @param shapesContent the SHACL shapes in Turtle format
   * @return a {@link ValidationResult} containing conformance status, report ID, and any errors
   */
  public ValidationResult validateShapes(String shapesContent) {
    if (shapesContent == null || shapesContent.isBlank()) {
      return new ValidationResult(false, null, "Shapes content cannot be null or empty");
    }

    try {
      Graph dataGraph = GraphStore.getInstance().getGraph();
      Graph shapeGraph = parseShapesGraph(shapesContent);

      Shacl validator = new Shacl(dataGraph, shapeGraph);
      Graph reportGraph = validator.eval();
      boolean conforms = validator.conform(reportGraph);

      String reportId = cacheReport(reportGraph);
      logger.info("SHACL validation completed (conforms: {}, report ID: {})", conforms, reportId);

      return new ValidationResult(conforms, reportId, null);

    } catch (Exception e) {
      logger.error("SHACL validation failed", e);
      return new ValidationResult(false, null, "Validation error: " + e.getMessage());
    }
  }

  // ============================================================================================
  // Report Formatting
  // ============================================================================================

  /**
   * Formats a cached validation report into the specified serialization format.
   *
   * <p>Supported formats: Turtle, RDF/XML, JSON-LD, N-Triples, N-Quads, TriG.
   *
   * @param reportId the report identifier from {@link #validateShapes(String)}
   * @param format the target serialization format
   * @return the serialized report string, or an error message if not found or formatting fails
   */
  public String formatReport(String reportId, SerializationFormat format) {
    Graph reportGraph = getReportGraph(reportId);
    if (reportGraph == null) {
      return "Error: Report not found for ID: " + reportId;
    }
    if (format == null) {
      return "Error: Format cannot be null";
    }

    return ResultFormatter.getInstance().formatGraph(reportGraph, format);
  }

  // ============================================================================================
  // Report Item Extraction
  // ============================================================================================

  /**
   * Extracts structured validation items from a cached report.
   *
   * <p>Queries the validation report graph to extract SHACL validation results with details:
   * severity, focus node, result path, invalid value, and error message.
   *
   * @param reportId the report identifier from {@link #validateShapes(String)}
   * @return a list of {@link ValidationReportItem} objects, or an empty list if not found
   */
  public List<ValidationReportItem> extractReportItems(String reportId) {
    List<ValidationReportItem> items = new ArrayList<>();
    Graph reportGraph = getReportGraph(reportId);

    if (reportGraph == null) {
      logger.warn("Cannot extract items: report not found for ID {}", reportId);
      return items;
    }

    try {
      QueryProcess queryProcess = QueryProcess.create(reportGraph);
      Mappings mappings = queryProcess.query(VALIDATION_RESULT_QUERY);

      for (var mapping : mappings) {
        String severity = extractNodeLabel(mapping, "?severity");
        String focusNode = extractNodeLabel(mapping, "?focusNode");
        String resultPath = extractNodeLabel(mapping, "?resultPath");
        String value = extractNodeLabel(mapping, "?value");
        String message = extractNodeLabel(mapping, "?message");

        // Simplify severity URI (e.g., "http://www.w3.org/ns/shacl#Violation" → "Violation")
        severity = simplifyUri(severity);

        items.add(new ValidationReportItem(severity, focusNode, resultPath, value, message));
      }

      logger.debug("Extracted {} validation items from report {}", items.size(), reportId);

    } catch (Exception e) {
      logger.error("Error extracting report items from report {}", reportId, e);
    }

    return items;
  }

  // ============================================================================================
  // Cache Management
  // ============================================================================================

  /**
   * Releases a cached validation report to free memory.
   *
   * <p>Should be called when the report is no longer needed by the GUI.
   *
   * @param reportId the report identifier to release
   */
  public void releaseReport(String reportId) {
    if (reportId != null) {
      reportCache.remove(reportId);
      logger.debug("Released cached report: {}", reportId);
    }
  }

  // ============================================================================================
  // Private Helpers
  // ============================================================================================

  /**
   * SPARQL query to extract validation results from a SHACL report graph.
   */
  private static final String VALIDATION_RESULT_QUERY =
      """
      SELECT ?severity ?focusNode ?resultPath ?value ?message WHERE {
        ?r a <http://www.w3.org/ns/shacl#ValidationResult> ;
           <http://www.w3.org/ns/shacl#resultSeverity> ?severity ;
           <http://www.w3.org/ns/shacl#focusNode> ?focusNode .
        OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultPath> ?resultPath }
        OPTIONAL { ?r <http://www.w3.org/ns/shacl#value> ?value }
        OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultMessage> ?message }
      }
      """;

  /**
   * Parses SHACL shapes content (Turtle format) into a Corese Graph.
   */
  private Graph parseShapesGraph(String shapesContent) throws Exception {
    Graph shapeGraph = Graph.create();
    Load.create(shapeGraph)
        .parse(
            new ByteArrayInputStream(shapesContent.getBytes(StandardCharsets.UTF_8)),
            Load.format.TURTLE_FORMAT);
    return shapeGraph;
  }

  /**
   * Caches a validation report graph and returns its unique identifier.
   */
  private String cacheReport(Graph reportGraph) {
    String reportId = UUID.randomUUID().toString();
    reportCache.put(reportId, reportGraph);
    return reportId;
  }

  /**
   * Retrieves a cached validation report graph by ID.
   */
  private Graph getReportGraph(String reportId) {
    if (reportId == null) {
      return null;
    }
    return reportCache.get(reportId);
  }

  /**
   * Extracts the label from a mapping variable, or returns an empty string if null.
   */
  private String extractNodeLabel(fr.inria.corese.core.kgram.core.Mapping mapping, String variable) {
    var node = mapping.getValue(variable);
    return (node != null) ? node.getLabel() : "";
  }

  /**
   * Simplifies a URI by extracting the local name after '#' or '/'.
   */
  private String simplifyUri(String uri) {
    if (uri == null || uri.isEmpty()) {
      return uri;
    }
    if (uri.contains("#")) {
      return uri.substring(uri.lastIndexOf('#') + 1);
    }
    if (uri.contains("/")) {
      return uri.substring(uri.lastIndexOf('/') + 1);
    }
    return uri;
  }
}
