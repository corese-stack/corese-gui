package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for RDF syntax validation.
 *
 * <p>Encapsulates corese-core parsing to keep GUI code independent from corese-core types.
 */
public class RdfSyntaxService {

  private static RdfSyntaxService instance;

  private RdfSyntaxService() {}

  public static synchronized RdfSyntaxService getInstance() {
    if (instance == null) {
      instance = new RdfSyntaxService();
    }
    return instance;
  }

  /**
   * Validates Turtle syntax by attempting to parse the content.
   *
   * @param content Turtle content
   * @return a validation result with error details when invalid
   */
  public SyntaxCheckResult validateTurtle(String content) {
    if (content == null || content.isBlank()) {
      return SyntaxCheckResult.invalid("Content is empty.");
    }

    try {
      Graph graph = Graph.create();
      Load.create(graph)
          .parse(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
              Load.format.TURTLE_FORMAT);
      return SyntaxCheckResult.valid();
    } catch (Exception e) {
      return SyntaxCheckResult.invalid(e.getMessage());
    }
  }

  /** Result of a syntax validation. */
  public static final class SyntaxCheckResult {
    private final boolean valid;
    private final String errorMessage;

    private SyntaxCheckResult(boolean valid, String errorMessage) {
      this.valid = valid;
      this.errorMessage = errorMessage;
    }

    public static SyntaxCheckResult valid() {
      return new SyntaxCheckResult(true, null);
    }

    public static SyntaxCheckResult invalid(String errorMessage) {
      return new SyntaxCheckResult(false, errorMessage);
    }

    public boolean isValid() {
      return valid;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
