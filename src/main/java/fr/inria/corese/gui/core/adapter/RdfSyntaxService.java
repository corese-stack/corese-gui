package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for validating RDF syntax without loading data into the main graph.
 *
 * <p>
 * This service provides lightweight syntax checking for RDF content, useful for
 * editor features, pre-validation, and syntax highlighting support.
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 *   <li>Syntax checking is stateless and doesn't require multiple instances</li>
 *   <li>Provides a consistent validation interface across the application</li>
 *   <li>Optimizes resource usage with a single service instance</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * RdfSyntaxService service = RdfSyntaxService.getInstance();
 * CheckResult result = service.checkTurtle(turtleContent);
 * if (!result.valid()) {
 *     System.out.println("Syntax error: " + result.message());
 * }
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for stateless service
public class RdfSyntaxService {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final RdfSyntaxService INSTANCE = new RdfSyntaxService();

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    private RdfSyntaxService() {}

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Returns the singleton instance of the RDF syntax service.
     *
     * @return The RdfSyntaxService instance.
     */
    public static RdfSyntaxService getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if the provided Turtle content is syntactically valid.
     *
     * @param content The Turtle RDF content to validate.
     * @return A {@link CheckResult} indicating validity and any error message.
     */
    public CheckResult checkTurtle(String content) {
        return checkSyntax(content, Loader.format.TURTLE_FORMAT, "Turtle");
    }

    // ==============================================================================================
    // Private Methods
    // ==============================================================================================

    /**
     * Generic syntax check for a supported Corese RDF format.
     *
     * @param content The RDF content to validate.
     * @param format The Corese format to use for parsing.
     * @param formatName The human-readable format name for error messages.
     * @return A {@link CheckResult} with validation outcome.
     */
    private CheckResult checkSyntax(String content, Loader.format format, String formatName) {
        if (content == null || content.isBlank()) {
            return new CheckResult(false, "Content is empty.");
        }

        try {
            // Parse into a temporary disposable graph
            Graph temp = Graph.create();
            Load loader = Load.create(temp);
            loader.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), format);
            return new CheckResult(true, null);
        } catch (Exception e) {
            return new CheckResult(false, formatName + " syntax error: " + e.getMessage());
        }
    }

    // ==============================================================================================
    // Inner Classes
    // ==============================================================================================

    /**
     * Result object for syntax validation checks.
     *
     * @param valid true if the content is syntactically valid, false otherwise.
     * @param message Error message if invalid, null if valid.
     */
    public record CheckResult(boolean valid, String message) {}
}