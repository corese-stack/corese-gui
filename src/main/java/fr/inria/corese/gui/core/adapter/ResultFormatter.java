package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.kgram.tool.NodeImpl;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Singleton service for formatting and serializing RDF graphs and SPARQL results.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Graph serialization to various RDF formats (Turtle, RDF/XML, JSON-LD, etc.)
 *   <li>Mappings (SPARQL results) serialization to various formats (XML, JSON, CSV, TSV, Markdown)
 *   <li>Raw table data formatting when Mappings objects are not available
 * </ul>
 *
 * <p>All formatting is delegated to Corese's native {@link ResultFormat} for consistency.
 *
 * @see ResultFormat
 * @see SerializationFormat
 */
public class ResultFormatter {

  private static ResultFormatter instance;

  private ResultFormatter() {
    // Singleton
  }

  /**
   * Returns the singleton instance of ResultFormatter.
   *
   * @return the singleton ResultFormatter instance
   */
  public static synchronized ResultFormatter getInstance() {
    if (instance == null) {
      instance = new ResultFormatter();
    }
    return instance;
  }

  // ============================================================================================
  // Graph Formatting
  // ============================================================================================

  /**
   * Formats a Corese Graph into the specified RDF serialization format.
   *
   * <p>Supported formats: Turtle, RDF/XML, JSON-LD, N-Triples, N-Quads, TriG.
   *
   * @param graph the graph to serialize
   * @param format the target serialization format
   * @return the serialized graph as a string, or an error message if formatting fails
   */
  String formatGraph(Graph graph, SerializationFormat format) {
    if (graph == null) {
      return "Error: Graph is null";
    }

    if (format == SerializationFormat.MARKDOWN) {
      return "Error: Markdown format is not supported for Graph serialization";
    }

    ResultFormat.format coreseFormat = toCoreseFormat(format);
    if (coreseFormat == null) {
      return "Error: Unsupported format for Graph serialization: " + format;
    }

    try {
      return ResultFormat.create(graph, coreseFormat).toString();
    } catch (Exception e) {
      return "Error formatting Graph: " + e.getMessage();
    }
  }

  // ============================================================================================
  // Mappings Formatting
  // ============================================================================================

  /**
   * Formats Corese Mappings (SPARQL query results) into the specified format.
   *
   * <p>Supported formats: XML, JSON, CSV, TSV, Markdown.
   *
   * @param mappings the SPARQL query results to serialize
   * @param format the target serialization format
   * @return the serialized results as a string, or an error message if formatting fails
   */
  String formatMappings(Mappings mappings, SerializationFormat format) {
    if (mappings == null) {
      return "Error: Mappings is null";
    }

    ResultFormat.format coreseFormat = toCoreseFormat(format);
    if (coreseFormat == null) {
      return "Error: Unsupported format for Mappings serialization: " + format;
    }

    try {
      return ResultFormat.create(mappings, coreseFormat).toString();
    } catch (Exception e) {
      return "Error formatting Mappings: " + e.getMessage();
    }
  }

  // ============================================================================================
  // Table Formatting
  // ============================================================================================

  /**
   * Formats raw table data (headers and rows) into the specified format.
   *
   * <p>This method is used when the original Mappings object is not available,
   * such as for UI-generated table data. It converts the raw data into a synthetic
   * Mappings object and then formats it using Corese's ResultFormat.
   *
   * <p>Supported formats: CSV, Markdown.
   *
   * @param rows the data rows
   * @param headers the column headers
   * @param format the target serialization format
   * @return the formatted table as a string, or an error message if formatting fails
   */
  public String formatTable(List<String[]> rows, String[] headers, SerializationFormat format) {
    if (format == null) {
      return "Error: Format cannot be null";
    }

    ResultFormat.format coreseFormat = toCoreseFormat(format);
    if (coreseFormat == null) {
      return "Error: Unsupported format for table export: " + format;
    }

    try {
      Mappings mappings = convertToMappings(headers, rows);
      return ResultFormat.create(mappings, coreseFormat).toString();
    } catch (Exception e) {
      return "Error formatting table: " + e.getMessage();
    }
  }

  // ============================================================================================
  // Private Helpers
  // ============================================================================================

  /**
   * Converts raw table data into a Corese Mappings object.
   *
   * <p>Creates synthetic variable nodes from headers and populates mappings with row data.
   */
  private Mappings convertToMappings(String[] headers, List<String[]> rows) {
    Query query = Query.create(0);
    List<Node> selectNodes = buildSelectNodes(headers);
    query.setSelect(selectNodes);

    Mappings mappings = Mappings.create(query);
    Node[] variables = selectNodes.toArray(new Node[0]);

    if (rows != null) {
      for (String[] row : rows) {
        Node[] values = new Node[variables.length];
        for (int i = 0; i < variables.length; i++) {
          String cellValue = (row != null && i < row.length && row[i] != null) ? row[i] : "";
          values[i] = NodeImpl.createConstant(cellValue);
        }
        Mapping mapping = Mapping.create(variables, values);
        mapping.setSelect(variables);
        mappings.add(mapping);
      }
    }

    return mappings;
  }

  /**
   * Builds SPARQL variable nodes from table headers.
   *
   * <p>Normalizes header names to valid SPARQL variable names:
   * <ul>
   *   <li>Removes leading '?' if present
   *   <li>Replaces invalid characters with underscores
   *   <li>Ensures variables don't start with digits
   *   <li>Handles duplicates by appending suffixes
   *   <li>Provides default names (col1, col2, ...) for empty headers
   * </ul>
   */
  private List<Node> buildSelectNodes(String[] headers) {
    List<Node> selectNodes = new ArrayList<>();
    Set<String> usedNames = new HashSet<>();

    if (headers == null || headers.length == 0) {
      selectNodes.add(NodeImpl.createVariable("col1"));
      return selectNodes;
    }

    for (int i = 0; i < headers.length; i++) {
      String normalized = normalizeHeaderName(headers[i], i);
      String uniqueName = ensureUniqueName(normalized, usedNames);
      usedNames.add(uniqueName);
      selectNodes.add(NodeImpl.createVariable(uniqueName));
    }

    return selectNodes;
  }

  /**
   * Normalizes a header name to a valid SPARQL variable name.
   */
  private String normalizeHeaderName(String header, int index) {
    String raw = (header != null) ? header.trim() : "";

    // Remove leading '?' if present
    if (raw.startsWith("?")) {
      raw = raw.substring(1);
    }

    // Replace invalid characters with underscores
    String normalized = raw.replaceAll("[^A-Za-z0-9_]", "_");

    // Provide default name if empty
    if (normalized.isEmpty()) {
      return "col" + (index + 1);
    }

    // Ensure doesn't start with a digit
    if (Character.isDigit(normalized.charAt(0))) {
      normalized = "col_" + normalized;
    }

    return normalized;
  }

  /**
   * Ensures a variable name is unique by appending a suffix if necessary.
   */
  private String ensureUniqueName(String baseName, Set<String> usedNames) {
    String uniqueName = baseName;
    int suffix = 2;
    while (usedNames.contains(uniqueName)) {
      uniqueName = baseName + "_" + suffix;
      suffix++;
    }
    return uniqueName;
  }

  /**
   * Converts a SerializationFormat enum to a Corese ResultFormat.format.
   *
   * @return the corresponding Corese format, or null if unsupported
   */
  private ResultFormat.format toCoreseFormat(SerializationFormat format) {
    if (format == null) {
      return null;
    }

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
      default -> null;
    };
  }
}
