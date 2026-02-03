package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Singleton service for RDF syntax validation.
 *
 * <p>This service validates RDF content by attempting to parse it with Corese's parser.
 * It currently supports Turtle format validation, with extensibility for other RDF formats.
 *
 * <p>The GUI layer uses this service to validate user-entered RDF before loading it
 * into the graph store, ensuring syntax correctness without exposing corese-core types.
 *
 * @see SyntaxCheckResult
 */
public class RdfSyntaxService {

  private static RdfSyntaxService instance;

  private RdfSyntaxService() {
    // Singleton
  }

  /**
   * Returns the singleton instance of RdfSyntaxService.
   *
   * @return the singleton RdfSyntaxService instance
   */
  public static synchronized RdfSyntaxService getInstance() {
    if (instance == null) {
      instance = new RdfSyntaxService();
    }
    return instance;
  }

  // ============================================================================================
  // Syntax Validation
  // ============================================================================================

  /**
   * Validates Turtle (TTL) syntax by attempting to parse the content.
   *
   * <p>The content is parsed into a temporary graph using Corese's Turtle parser.
   * If parsing succeeds, the syntax is valid. If it fails, the error message
   * is captured and returned in the result.
   *
   * @param content the Turtle RDF content to validate
   * @return a {@link SyntaxCheckResult} indicating validity and any error details
   */
  public SyntaxCheckResult validateTurtle(String content) {
    if (content == null || content.isBlank()) {
      return SyntaxCheckResult.invalid("Content is empty");
    }

    try {
      Graph tempGraph = Graph.create();
      Load.create(tempGraph)
          .parse(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
              Load.format.TURTLE_FORMAT);
      return SyntaxCheckResult.valid();
    } catch (Exception e) {
      return SyntaxCheckResult.invalid(e.getMessage());
    }
  }

  /**
   * Validates RDF/XML syntax by attempting to parse the content.
   *
   * @param content the RDF/XML content to validate
   * @return a {@link SyntaxCheckResult} indicating validity and any error details
   */
  public SyntaxCheckResult validateRdfXml(String content) {
    return validateFormat(content, Load.format.RDFXML_FORMAT, "RDF/XML");
  }

  /**
   * Validates JSON-LD syntax by attempting to parse the content.
   *
   * @param content the JSON-LD content to validate
   * @return a {@link SyntaxCheckResult} indicating validity and any error details
   */
  public SyntaxCheckResult validateJsonLd(String content) {
    return validateFormat(content, Load.format.JSONLD_FORMAT, "JSON-LD");
  }

  /**
   * Validates N-Triples syntax by attempting to parse the content.
   *
   * @param content the N-Triples content to validate
   * @return a {@link SyntaxCheckResult} indicating validity and any error details
   */
  public SyntaxCheckResult validateNTriples(String content) {
    return validateFormat(content, Load.format.NT_FORMAT, "N-Triples");
  }

  /**
   * Validates TriG syntax by attempting to parse the content.
   *
   * @param content the TriG content to validate
   * @return a {@link SyntaxCheckResult} indicating validity and any error details
   */
  public SyntaxCheckResult validateTrig(String content) {
    return validateFormat(content, Load.format.TRIG_FORMAT, "TriG");
  }

  // ============================================================================================
  // Private Helpers
  // ============================================================================================

  /**
   * Generic validation method for any RDF format supported by Corese.
   */
  private SyntaxCheckResult validateFormat(String content, Load.format format, String formatName) {
    if (content == null || content.isBlank()) {
      return SyntaxCheckResult.invalid("Content is empty");
    }

    try {
      Graph tempGraph = Graph.create();
      Load.create(tempGraph)
          .parse(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
              format);
      return SyntaxCheckResult.valid();
    } catch (Exception e) {
      return SyntaxCheckResult.invalid(formatName + " parsing error: " + e.getMessage());
    }
  }

  // ============================================================================================
  // Result Class
  // ============================================================================================

  /**
   * Immutable result of an RDF syntax validation.
   *
   * <p>Contains:
   * <ul>
   *   <li>Validity status (true if syntax is correct)
   *   <li>Error message (null if valid, otherwise contains parser error details)
   * </ul>
   */
  public static final class SyntaxCheckResult {

    private final boolean valid;
    private final String errorMessage;

    private SyntaxCheckResult(boolean valid, String errorMessage) {
      this.valid = valid;
      this.errorMessage = errorMessage;
    }

    /**
     * Creates a valid syntax check result.
     *
     * @return a valid result with no error message
     */
    public static SyntaxCheckResult valid() {
      return new SyntaxCheckResult(true, null);
    }

    /**
     * Creates an invalid syntax check result with an error message.
     *
     * @param errorMessage the parser error message
     * @return an invalid result with the specified error
     */
    public static SyntaxCheckResult invalid(String errorMessage) {
      return new SyntaxCheckResult(false, errorMessage);
    }

    /**
     * Returns whether the syntax is valid.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean isValid() {
      return valid;
    }

    /**
     * Returns the error message if the syntax is invalid.
     *
     * @return the error message, or {@code null} if valid
     */
    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
