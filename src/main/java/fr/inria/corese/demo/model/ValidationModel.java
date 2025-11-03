package fr.inria.corese.demo.model;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.shacl.Shacl;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ValidationModel {
  public static final class ValidationResult {
    private final boolean conforms;
    private final Graph reportGraph;
    private final String errorMessage;

    public ValidationResult(boolean conforms, Graph reportGraph, String errorMessage) {
      this.conforms = conforms;
      this.reportGraph = reportGraph;
      this.errorMessage = errorMessage;
    }

    public boolean isConforms() {
      return conforms;
    }

    public Graph getReportGraph() {
      return reportGraph;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

  public ValidationResult validate(Graph dataGraph, String shapesContent) {
    try {

      Graph shapeGraph = Graph.create();
      Load.create(shapeGraph)
          .parse(
              new ByteArrayInputStream(shapesContent.getBytes(StandardCharsets.UTF_8)),
              Load.format.TURTLE_FORMAT);

      Shacl validator = new Shacl(dataGraph, shapeGraph);

      Graph reportGraph = validator.eval();

      boolean conforms = validator.conform(reportGraph);

      return new ValidationResult(conforms, reportGraph, null);

    } catch (Exception e) {
      e.printStackTrace();
      return new ValidationResult(false, null, "Validation Error: " + e.getMessage());
    }
  }
}
