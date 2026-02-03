package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for validating RDF syntax without loading the data into the main graph.
 *
 * <p>Useful for editor features like syntax checking or pre-validation.
 */
public class RdfSyntaxService {

    private static final RdfSyntaxService INSTANCE = new RdfSyntaxService();

    private RdfSyntaxService() {}

    public static RdfSyntaxService getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if the provided Turtle content is valid.
     *
     * @param content The Turtle string.
     * @return A {@link CheckResult} indicating validity.
     */
    public CheckResult checkTurtle(String content) {
        return checkSyntax(content, Load.format.TURTLE_FORMAT, "Turtle");
    }

    /**
     * Generic syntax check for a supported Corese format.
     */
    private CheckResult checkSyntax(String content, Load.format format, String formatName) {
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

    /**
     * Simple result DTO for syntax checks.
     */
    public record CheckResult(boolean valid, String message) {}
}