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
 * Service for SHACL validation operations.
 * Handles the interaction with Corese SHACL validator and result formatting.
 */
public class ShaclService {
  private static final Logger logger = LoggerFactory.getLogger(ShaclService.class);

  private static ShaclService instance;

  private final Map<String, Graph> reportCache = new ConcurrentHashMap<>();

  private ShaclService() {}

  public static synchronized ShaclService getInstance() {
    if (instance == null) {
      instance = new ShaclService();
    }
    return instance;
  }

  /**
   * Validates the current data graph against the provided shapes content.
   *
   * @param shapesContent The SHACL shapes in Turtle format.
   * @return The result of the validation.
   */
  public ValidationResult validateShapes(String shapesContent) {
    try {
      if (shapesContent == null || shapesContent.isBlank()) {
        return new ValidationResult(false, null, "Validation Error: Shapes content is empty.");
      }

      // Get the data graph from the store
      Graph dataGraph = GraphStore.getInstance().getGraph();

      Graph shapeGraph = Graph.create();
      Load.create(shapeGraph)
          .parse(
              new ByteArrayInputStream(shapesContent.getBytes(StandardCharsets.UTF_8)),
              Load.format.TURTLE_FORMAT);

      Shacl validator = new Shacl(dataGraph, shapeGraph);

      Graph reportGraph = validator.eval();

      boolean conforms = validator.conform(reportGraph);
      String reportId = cacheReport(reportGraph);

      return new ValidationResult(conforms, reportId, null);

    } catch (Exception e) {
      logger.error("Validation Error", e);
      return new ValidationResult(false, null, "Validation Error: " + e.getMessage());
    }
  }

  /**
   * Formats a cached validation report into the specified format.
   *
   * @param reportId The report identifier.
   * @param format The target serialization format.
   * @return The serialized report string, or null if the report is not found.
   */
  public String formatReport(String reportId, SerializationFormat format) {
    Graph reportGraph = getReportGraph(reportId);
    if (reportGraph == null) {
      return null;
    }
    if (format == null) {
      return "Error: Unsupported format for report export.";
    }
    return ResultFormatter.getInstance().formatGraph(reportGraph, format);
  }

  /**
   * Extracts validation report items from a cached report.
   *
   * @param reportId The report identifier.
   * @return A list of ValidationReportItem objects.
   */
  public List<ValidationReportItem> extractReportItems(String reportId) {
    List<ValidationReportItem> items = new ArrayList<>();
    Graph reportGraph = getReportGraph(reportId);
    if (reportGraph == null) {
      return items;
    }

    try {
      QueryProcess exec = QueryProcess.create(reportGraph);
      String query =
          "SELECT ?severity ?focusNode ?resultPath ?value ?message WHERE { "
              + "?r a <http://www.w3.org/ns/shacl#ValidationResult> ; "
              + "<http://www.w3.org/ns/shacl#resultSeverity> ?severity ; "
              + "<http://www.w3.org/ns/shacl#focusNode> ?focusNode . "
              + "OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultPath> ?resultPath } "
              + "OPTIONAL { ?r <http://www.w3.org/ns/shacl#value> ?value } "
              + "OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultMessage> ?message } "
              + "}";
      Mappings map = exec.query(query);
      for (var mapping : map) {
        String severity =
            mapping.getValue("?severity") != null
                ? mapping.getValue("?severity").getLabel()
                : "";
        String focusNode =
            mapping.getValue("?focusNode") != null
                ? mapping.getValue("?focusNode").getLabel()
                : "";
        String resultPath =
            mapping.getValue("?resultPath") != null
                ? mapping.getValue("?resultPath").getLabel()
                : "";
        String value =
            mapping.getValue("?value") != null ? mapping.getValue("?value").getLabel() : "";
        String message =
            mapping.getValue("?message") != null
                ? mapping.getValue("?message").getLabel()
                : "";

        // Simplify severity URI
        if (severity.contains("#")) {
          severity = severity.substring(severity.lastIndexOf('#') + 1);
        }

        items.add(new ValidationReportItem(severity, focusNode, resultPath, value, message));
      }
    } catch (Exception e) {
      logger.error("Error extracting report items", e);
    }
    return items;
  }

  /**
   * Releases a cached report to avoid memory leaks.
   *
   * @param reportId The report identifier to release.
   */
  public void releaseReport(String reportId) {
    if (reportId != null) {
      reportCache.remove(reportId);
    }
  }

  private String cacheReport(Graph reportGraph) {
    String reportId = UUID.randomUUID().toString();
    reportCache.put(reportId, reportGraph);
    return reportId;
  }

  private Graph getReportGraph(String reportId) {
    if (reportId == null) {
      return null;
    }
    return reportCache.get(reportId);
  }
}
