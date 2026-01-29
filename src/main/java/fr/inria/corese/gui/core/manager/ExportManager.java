package fr.inria.corese.gui.core.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.util.List;

/**
 * Centralized manager for handling data export and serialization.
 *
 * <p>This class consolidates export logic, utilizing Corese's native {@link ResultFormat} where
 * possible (for Graphs and standard SPARQL results) and providing robust fallbacks or manual
 * implementations for formats not natively supported (e.g., Markdown) or for data structures
 * decoupled from the core model (e.g., List of Strings).
 */
public class ExportManager {

  private static ExportManager instance;

  private ExportManager() {
    // Singleton
  }

  public static synchronized ExportManager getInstance() {
    if (instance == null) {
      instance = new ExportManager();
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
  public String formatGraph(Graph graph, SerializationFormat format) {
    if (graph == null) return "";

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
  public String formatMappings(Mappings mappings, SerializationFormat format) {
    if (mappings == null) return "";

    // Handle formats that might require manual processing or specific handling
    if (format == SerializationFormat.CSV) {
      // Corese ResultFormat might support CSV, but we ensure consistent output here
      // or delegate if reliable.
      // If we want to use Corese's serialization:
      // return ResultFormat.create(mappings, ResultFormat.format.CSV_FORMAT).toString();
      // However, strictly following the request to use Corese serialization:
      try {
        return ResultFormat.create(mappings, ResultFormat.format.CSV_FORMAT).toString();
      } catch (Exception e) {
        // Fallback if native CSV fails or is not available
        return generateCsvFromMappings(mappings);
      }
    }

    if (format == SerializationFormat.MARKDOWN) {
      return generateMarkdownFromMappings(mappings);
    }

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
  public String formatTableData(List<String[]> rows, String[] headers, SerializationFormat format) {
    if (format == SerializationFormat.MARKDOWN) {
      return generateMarkdown(rows, headers);
    } else {
      // Default to CSV
      return generateCsv(rows, headers);
    }
  }

  // ==============================================================================================
  // Manual Implementations (Fallbacks & Non-Corese Formats)
  // ==============================================================================================

  private String generateCsvFromMappings(Mappings mappings) {
    if (mappings == null || mappings.getAST() == null) {
      return "";
    }
    StringBuilder csv = new StringBuilder();
    List<Variable> variables = mappings.getAST().getSelect();

    // Header
    for (int i = 0; i < variables.size(); i++) {
      csv.append(variables.get(i).getName().substring(1)); // remove leading '?'
      if (i < variables.size() - 1) {
        csv.append(',');
      }
    }
    csv.append("\n");

    // Rows
    mappings.forEach(
        mapping -> {
          for (int i = 0; i < variables.size(); i++) {
            Variable var = variables.get(i);
            Node node = mapping.getNode(var);
            if (node != null) {
              csv.append(node.getLabel());
            }
            if (i < variables.size() - 1) {
              csv.append(',');
            }
          }
          csv.append("\n");
        });

    return csv.toString();
  }

  private String generateMarkdownFromMappings(Mappings mappings) {
    if (mappings == null || mappings.getAST() == null) {
      return "";
    }
    List<Variable> variables = mappings.getAST().getSelect();
    String[] headers =
        variables.stream()
            .map(v -> v.getName().substring(1)) // remove leading '?'
            .toArray(String[]::new);

    // Convert mappings to List<String[]> for reuse of generateMarkdown
    // This is a bit expensive but keeps logic DRY.
    // Alternatively, implement direct loop.
    StringBuilder sb = new StringBuilder();

    // 1. Headers
    if (headers.length > 0) {
      sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
      sb.append("|");
      for (int i = 0; i < headers.length; i++) {
        sb.append("---").append(i < headers.length - 1 ? "|" : "");
      }
      sb.append("|\n");
    }

    // 2. Data
    mappings.forEach(
        mapping -> {
          sb.append("| ");
          for (int i = 0; i < variables.size(); i++) {
            Variable var = variables.get(i);
            Node node = mapping.getNode(var);
            sb.append(node != null ? node.getLabel() : "");
            if (i < variables.size() - 1) {
              sb.append(" | ");
            }
          }
          sb.append(" |\n");
        });

    return sb.toString();
  }

  private String generateCsv(List<String[]> rows, String[] headers) {
    StringBuilder sb = new StringBuilder();
    if (headers != null) {
      sb.append(String.join(",", headers)).append("\n");
    }
    for (String[] row : rows) {
      sb.append(String.join(",", row)).append("\n");
    }
    return sb.toString();
  }

  private String generateMarkdown(List<String[]> rows, String[] headers) {
    StringBuilder sb = new StringBuilder();

    // 1. Headers
    if (headers != null) {
      sb.append("| ").append(String.join(" | ", headers)).append(" |\n");

      // Separator row: |---|---|...            sb.append("|");
      for (int i = 0; i < headers.length; i++) {
        sb.append("---").append(i < headers.length - 1 ? "|" : "");
      }
      sb.append("|\n");
    }

    // 2. Data
    for (String[] row : rows) {
      sb.append("| ").append(String.join(" | ", row)).append(" |\n");
    }

    return sb.toString();
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
      default -> null; // Markdown, etc.
    };
  }
}
