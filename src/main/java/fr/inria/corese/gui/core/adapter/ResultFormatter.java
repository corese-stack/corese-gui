package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.core.sparql.api.ResultFormatDef.format;

/**
 * Utility service for formatting Corese results into various serialization
 * formats.
 *
 * <p>
 * This service translates between Corese's internal {@link ResultFormat} and
 * the GUI's
 * {@link SerializationFormat}, handling both graph serialization and SPARQL
 * result formatting.
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 * <li>Formatting operations are stateless and don't require multiple
 * instances</li>
 * <li>Provides consistent format conversion across the application</li>
 * <li>Optimizes resource usage with a single formatter instance</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * ResultFormatter formatter = ResultFormatter.getInstance();
 * String turtle = formatter.formatGraph(graph, SerializationFormat.TURTLE);
 * String jsonResults = formatter.formatMappings(mappings, SerializationFormat.JSON);
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for stateless service
public class ResultFormatter {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final ResultFormatter INSTANCE = new ResultFormatter();

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    private ResultFormatter() {
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Returns the singleton instance of the result formatter.
     *
     * @return The ResultFormatter instance.
     */
    public static ResultFormatter getInstance() {
        return INSTANCE;
    }

    // ==============================================================================================
    // Package-Private API (Corese Types)
    // ==============================================================================================

    /**
     * Formats a Corese graph into the specified serialization format.
     *
     * @param graph  The Corese graph to format.
     * @param format The desired output format.
     * @return The formatted graph as a string, or an error message if formatting
     *         fails.
     */
    String formatGraph(Graph graph, SerializationFormat format) {
        if (graph == null) {
            return "Error: No graph data available.";
        }

        format coreseFormat = toCoreseFormat(format);
        if (coreseFormat == null) {
            return "Error: Unsupported format for Graph serialization: " + format;
        }

        try {
            return ResultFormat.create(graph, coreseFormat).toString();
        } catch (Exception e) {
            return "Error formatting graph: " + e.getMessage();
        }
    }

    /**
     * Formats Corese SPARQL query results into the specified serialization format.
     *
     * @param mappings The Corese mappings to format.
     * @param format   The desired output format.
     * @return The formatted results as a string, or an error message if formatting
     *         fails.
     */
    String formatMappings(Mappings mappings, SerializationFormat format) {
        if (mappings == null) {
            return "Error: No query results available.";
        }

        format coreseFormat = toCoreseFormat(format);
        if (coreseFormat == null) {
            return "Error: Unsupported format for Mappings serialization: " + format;
        }

        try {
            return ResultFormat.create(mappings, coreseFormat).toString();
        } catch (Exception e) {
            return "Error formatting results: " + e.getMessage();
        }
    }

    // ==============================================================================================
    // Private Methods
    // ==============================================================================================

    /**
     * Converts GUI SerializationFormat to Corese ResultFormat.
     *
     * @param format The GUI format enum.
     * @return The corresponding Corese format, or null if not applicable.
     */
    private format toCoreseFormat(SerializationFormat format) {
        if (format == null)
            return null;
        return switch (format) {
            case XML -> ResultFormatDef.format.XML_FORMAT;
            case RDF_XML -> ResultFormatDef.format.RDF_XML_FORMAT;
            case JSON -> ResultFormatDef.format.JSON_FORMAT;
            case JSON_LD -> ResultFormatDef.format.JSONLD_FORMAT;
            case CSV -> ResultFormatDef.format.CSV_FORMAT;
            case TSV -> ResultFormatDef.format.TSV_FORMAT;
            case TURTLE -> ResultFormatDef.format.TURTLE_FORMAT;
            case N_TRIPLES -> ResultFormatDef.format.NTRIPLES_FORMAT;
            case N_QUADS -> ResultFormatDef.format.NQUADS_FORMAT;
            case TRIG -> ResultFormatDef.format.TRIG_FORMAT;
            case RDFC10 -> ResultFormatDef.format.RDFC10_FORMAT;
            case RDFC10_SHA384 -> ResultFormatDef.format.RDFC10_SHA384_FORMAT;
            case MARKDOWN -> ResultFormatDef.format.MARKDOWN_FORMAT;
            // These formats are for code editing, not result serialization
            case SPARQL_QUERY, TEXT -> null;
        };
    }
}