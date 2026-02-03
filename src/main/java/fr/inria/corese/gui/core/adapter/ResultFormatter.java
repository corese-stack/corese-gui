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
 * Centralized formatter for handling data export and serialization.
 *
 * <p>This class consolidates export logic using Corese's native {@link ResultFormat} for graphs
 * and SPARQL results. It only provides manual formatting for UI table data that is not backed by
 * Corese objects.
 */
public class ResultFormatter {

  private static ResultFormatter instance;

  private ResultFormatter() {
    // Singleton
  }

  public static synchronized ResultFormatter getInstance() {
    if (instance == null) {
      instance = new ResultFormatter();
    }
    return instance;
  }

  /**
   * Formats a Corese Graph into the specified format.
   *
   * @param graph The graph to serialize.
   * @param format The target serialization format.
   * @return The serialized string.
   */
  String formatGraph(Graph graph, SerializationFormat format) {
    if (graph == null) return "";
    if (format == SerializationFormat.MARKDOWN) {
      return "Error: Unsupported format for Graph export: " + format;
    }

    ResultFormat.format coreseFormat = toCoreseFormat(format);
    if (coreseFormat == null) {
      return "Error: Unsupported format for Graph export: " + format;
    }

    try {
      return ResultFormat.create(graph, coreseFormat).toString();
    } catch (Exception e) {
      return "Error formatting Graph: " + e.getMessage();
    }
  }

  /**
   * Formats Corese Mappings (SPARQL results) into the specified format.
   *
   * @param mappings The mappings to serialize.
   * @param format The target serialization format.
   * @return The serialized string.
   */
  String formatMappings(Mappings mappings, SerializationFormat format) {
    if (mappings == null) return "";

    ResultFormat.format coreseFormat = toCoreseFormat(format);
    if (coreseFormat != null) {
      try {
        return ResultFormat.create(mappings, coreseFormat).toString();
      } catch (Exception e) {
        return "Error formatting Mappings: " + e.getMessage();
      }
    }

    return "Error: Unsupported format for Mappings export: " + format;
  }

  /**
   * Formats raw table data (List of String arrays) into the specified format. Used when the
   * original Mappings object is not available.
   *
   * @param rows The data rows.
   * @param headers The header row.
   * @param format The target serialization format (CSV or MARKDOWN).
   * @return The serialized string.
   */
  public String formatTable(List<String[]> rows, String[] headers, SerializationFormat format) {
    if (format == null) {
      return "Error: Unsupported format for table export.";
    }

    ResultFormat.format coreseFormat = toCoreseFormat(format);
    if (coreseFormat == null) {
      return "Error: Unsupported format for table export: " + format;
    }

    Mappings mappings = toMappings(headers, rows);
    try {
      return ResultFormat.create(mappings, coreseFormat).toString();
    } catch (Exception e) {
      return "Error formatting table: " + e.getMessage();
    }
  }

  private Mappings toMappings(String[] headers, List<String[]> rows) {
    Query query = Query.create(0);
    List<Node> select = buildSelectNodes(headers);
    query.setSelect(select);

    Mappings mappings = Mappings.create(query);

    Node[] vars = select.toArray(new Node[0]);
    if (rows != null) {
      for (String[] row : rows) {
        Node[] values = new Node[vars.length];
        for (int i = 0; i < vars.length; i++) {
          String cell = (row != null && i < row.length) ? row[i] : "";
          if (cell == null) {
            cell = "";
          }
          values[i] = NodeImpl.createConstant(cell);
        }
        Mapping mapping = Mapping.create(vars, values);
        mapping.setSelect(vars);
        mappings.add(mapping);
      }
    }

    return mappings;
  }

  private List<Node> buildSelectNodes(String[] headers) {
    List<Node> select = new ArrayList<>();
    Set<String> used = new HashSet<>();

    if (headers == null || headers.length == 0) {
      select.add(NodeImpl.createVariable("col1"));
      return select;
    }

    for (int i = 0; i < headers.length; i++) {
      String raw = headers[i] == null ? "" : headers[i].trim();
      if (raw.startsWith("?")) {
        raw = raw.substring(1);
      }
      String normalized = raw.replaceAll("[^A-Za-z0-9_]", "_");
      if (normalized.isEmpty()) {
        normalized = "col" + (i + 1);
      }
      if (Character.isDigit(normalized.charAt(0))) {
        normalized = "col_" + normalized;
      }
      String name = normalized;
      int suffix = 2;
      while (used.contains(name)) {
        name = normalized + "_" + suffix;
        suffix++;
      }
      used.add(name);
      select.add(NodeImpl.createVariable(name));
    }

    return select;
  }

  // ==============================================================================================
  // Utils
  // ==============================================================================================

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
      default -> null; // Unsupported format
    };
  }
}
