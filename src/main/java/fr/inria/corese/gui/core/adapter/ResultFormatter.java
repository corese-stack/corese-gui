package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.gui.core.enums.SerializationFormat;

/**
 * Utility service for formatting Corese results (Graphs and Mappings) into string representations.
 *
 * <p>This class encapsulates the translation between Corese's internal {@link ResultFormat}
 * and the GUI's {@link SerializationFormat}. It handles:
 * <ul>
 *   <li>Graph serialization (Turtle, RDF/XML, JSON-LD, etc.)</li>
 *   <li>SPARQL Result serialization (XML, JSON, CSV, TSV)</li>
 * </ul>
 */
public class ResultFormatter {

    private static final ResultFormatter INSTANCE = new ResultFormatter();

    private ResultFormatter() {}

    /**
     * Returns the singleton instance of ResultFormatter.
     *
     * @return the singleton instance
     */
    public static ResultFormatter getInstance() {
        return INSTANCE;
    }

    // ============================================================================================ 
    // Internal Package-Private API (Using Corese Types)
    // ============================================================================================ 

    /**
     * Formats a Corese Graph.
     */
    String formatGraph(Graph graph, SerializationFormat format) {
        if (graph == null) return "Error: No graph data available.";
        
        ResultFormat.format coreseFormat = toCoreseFormat(format);
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
     * Formats Corese Mappings (Query Results).
     */
    String formatMappings(Mappings mappings, SerializationFormat format) {
        if (mappings == null) return "Error: No query results available.";

        ResultFormat.format coreseFormat = toCoreseFormat(format);
        if (coreseFormat == null) {
            return "Error: Unsupported format for Mappings serialization: " + format;
        }

        try {
            return ResultFormat.create(mappings, coreseFormat).toString();
        } catch (Exception e) {
            return "Error formatting results: " + e.getMessage();
        }
    }

    // ============================================================================================ 
    // Helpers
    // ============================================================================================ 

    private ResultFormat.format toCoreseFormat(SerializationFormat format) {
        if (format == null) return null;
        return switch (format) {
            case XML -> ResultFormat.format.XML_FORMAT;
            case RDF_XML -> ResultFormat.format.RDF_XML_FORMAT;
            case JSON -> ResultFormat.format.JSON_FORMAT;
            case JSON_LD -> ResultFormat.format.JSONLD_FORMAT;
            case CSV -> ResultFormat.format.CSV_FORMAT;
            case TSV -> ResultFormat.format.TSV_FORMAT;
            case TURTLE -> ResultFormat.format.TURTLE_FORMAT;
            case N_TRIPLES -> ResultFormat.format.NTRIPLES_FORMAT;
            case N_QUADS -> ResultFormat.format.NQUADS_FORMAT;
            case TRIG -> ResultFormat.format.TRIG_FORMAT;
            case MARKDOWN -> ResultFormat.format.MARKDOWN_FORMAT;
        };
    }
}