package fr.inria.corese.gui.model;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.shacl.Shacl;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.gui.manager.GraphManager;
import fr.inria.corese.gui.manager.QueryManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Model for the Validation feature.
 * Handles the SHACL validation logic and stores the result.
 */
public class ValidationModel {

  private ValidationResult lastResult;

  /**
   * Checks if there is data loaded in the graph to validate.
   *
   * @return true if data is loaded, false otherwise.
   */
  public boolean isDataLoaded() {
    return GraphManager.getInstance().getGraph().size() > 0;
  }

  /**
   * Validates the current data graph against the provided shapes content.
   *
   * @param shapesContent The SHACL shapes in Turtle format.
   * @return The result of the validation.
   */
  public ValidationResult validate(String shapesContent) {
    try {
      // Get the data graph from the manager
      Graph dataGraph = GraphManager.getInstance().getGraph();

      Graph shapeGraph = Graph.create();
      Load.create(shapeGraph)
          .parse(
              new ByteArrayInputStream(shapesContent.getBytes(StandardCharsets.UTF_8)),
              Load.format.TURTLE_FORMAT);

      Shacl validator = new Shacl(dataGraph, shapeGraph);

      Graph reportGraph = validator.eval();

      boolean conforms = validator.conform(reportGraph);

      this.lastResult = new ValidationResult(conforms, reportGraph, null);
      return this.lastResult;

    } catch (Throwable e) {
      e.printStackTrace();
      this.lastResult = new ValidationResult(false, null, "Validation Error: " + e.getMessage());
      return this.lastResult;
    }
  }

  /**
   * Formats the last report graph into the specified format.
   *
   * @param format The desired format (e.g., "TURTLE", "JSON-LD").
   * @return The formatted string, or null if no report exists.
   */
  public String formatLastReport(String format) {
    if (lastResult == null || lastResult.getReportGraph() == null || format == null) {
      return null;
    }

    ResultFormatDef.format coreseFormat;
    switch (format.toUpperCase()) {
      case "RDF/XML":
        coreseFormat = ResultFormatDef.format.RDF_XML_FORMAT;
        break;
      case "JSON-LD":
        coreseFormat = ResultFormatDef.format.JSONLD_FORMAT;
        break;
      case "N-TRIPLES":
        coreseFormat = ResultFormatDef.format.NTRIPLES_FORMAT;
        break;
      case "N-QUADS":
        coreseFormat = ResultFormatDef.format.NQUADS_FORMAT;
        break;
      case "TRIG":
        coreseFormat = ResultFormatDef.format.TRIG_FORMAT;
        break;
      default:
        coreseFormat = ResultFormatDef.format.TURTLE_FORMAT;
        break;
    }
    return QueryManager.getInstance().formatGraph(lastResult.getReportGraph(), coreseFormat);
  }

  public ValidationResult getLastResult() {
    return lastResult;
  }
}
