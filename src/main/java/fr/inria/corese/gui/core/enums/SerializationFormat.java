package fr.inria.corese.gui.core.enums;

/**
 * Enumeration of RDF and SPARQL result serialization formats.
 *
 * <p>
 * This enum centralizes all supported output formats for RDF graphs and SPARQL
 * query results,
 * providing consistent format labels and file extensions across the
 * application.
 *
 * <p>
 * <b>Usage example:</b>
 *
 * <pre>{@code
 * SerializationFormat format = SerializationFormat.TURTLE;
 * String label = format.getLabel(); // "Turtle"
 * String extension = format.getExtension(); // ".ttl"
 * }</pre>
 */
public enum SerializationFormat {
  // RDF Serialization Formats
  TURTLE("Turtle", ".ttl"),
  RDF_XML("RDF/XML", ".rdf"),
  JSON_LD("JSON-LD", ".jsonld"),
  N_TRIPLES("N-Triples", ".nt"),
  N_QUADS("N-Quads", ".nq"),
  TRIG("TriG", ".trig"),
  RDFC10("RDFC-1.0", ".nq"),
  RDFC10_SHA384("RDFC-1.0 (SHA-384)", ".nq"),

  // SPARQL Result Formats
  CSV("CSV", ".csv"),
  TSV("TSV", ".tsv"),
  JSON("JSON", ".json"),
  XML("XML", ".xml"),

  // SPARQL Query/Update
  SPARQL_QUERY("SPARQL Query", ".rq"),
  SPARQL_UPDATE("SPARQL Update", ".ru"),

  // Scripts
  JAVASCRIPT("JavaScript", ".js"),

  // Other
  MARKDOWN("Markdown", ".md"),
  TEXT("Text", ".txt");

  private final String label;
  private final String extension;

  SerializationFormat(String label, String extension) {
    this.label = label;
    this.extension = extension;
  }

  /**
   * Returns the human-readable label for this format.
   *
   * @return The format label (e.g., "Turtle", "JSON-LD")
   */
  @Override
  public String toString() {
    return label;
  }

  /**
   * Returns the human-readable label for this format.
   *
   * @return The format label (e.g., "Turtle", "JSON-LD")
   */
  public String getLabel() {
    return label;
  }

  /**
   * Returns the file extension for this format (including the dot).
   *
   * @return The file extension (e.g., ".ttl", ".jsonld")
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Returns the CodeMirror mode string associated with this format.
   *
   * @return The CodeMirror mode (e.g., "turtle", "xml", "json")
   */
  public String getCodeMirrorMode() {
    return switch (this) {
      case TURTLE, TRIG, N_TRIPLES, N_QUADS, RDFC10, RDFC10_SHA384 -> "turtle";
      case RDF_XML, XML -> "xml";
      case JSON_LD, JSON -> "json";
      case CSV, TSV, MARKDOWN, TEXT -> "text/plain";
      case SPARQL_QUERY, SPARQL_UPDATE -> "sparql";
      case JAVASCRIPT -> "javascript";
    };
  }

  /**
   * Finds a serialization format by its file extension.
   *
   * @param extension The file extension (e.g., ".ttl", "rq") - case insensitive,
   *                  with or without dot
   * @return The matching SerializationFormat, or null if not found
   */
  public static SerializationFormat forExtension(String extension) {
    if (extension == null || extension.isBlank()) {
      return null;
    }

    String ext = extension.toLowerCase().startsWith(".") ? extension.toLowerCase() : "." + extension.toLowerCase();

    // Direct match
    for (SerializationFormat format : values()) {
      if (format.extension.equals(ext)) {
        return format;
      }
    }

    // Aliases / Related formats logic
    return switch (ext) {
      case ".n3" -> TURTLE;
      case ".sparql" -> SPARQL_QUERY;
      case ".owl" -> RDF_XML;
      default -> null;
    };
  }

  /**
   * Parses a format string to the corresponding enum value.
   *
   * <p>
   * Supports both label matching ("Turtle") and uppercase name matching
   * ("TURTLE").
   *
   * @param formatString The format string to parse
   * @return The corresponding SerializationFormat, or TURTLE as default
   */
  public static SerializationFormat fromString(String formatString) {
    if (formatString == null || formatString.isBlank()) {
      return TURTLE;
    }

    // Try label matching first (case-insensitive)
    for (SerializationFormat format : values()) {
      if (format.label.equalsIgnoreCase(formatString)) {
        return format;
      }
    }

    // Try enum name matching with normalization
    String normalized = formatString.toUpperCase().replace("/", "_").replace("-", "_");
    try {
      return valueOf(normalized);
    } catch (IllegalArgumentException _) {
      return TURTLE; // Default fallback
    }
  }

  /**
   * Returns all RDF serialization formats.
   *
   * @return Array of RDF formats
   */
  public static SerializationFormat[] rdfFormats() {
    return new SerializationFormat[] { TURTLE, RDF_XML, JSON_LD, N_TRIPLES, N_QUADS, TRIG, RDFC10, RDFC10_SHA384 };
  }

  /**
   * Returns all SPARQL result formats.
   *
   * @return Array of SPARQL result formats
   */
  public static SerializationFormat[] sparqlResultFormats() {
    return new SerializationFormat[] { CSV, TSV, JSON, XML, MARKDOWN };
  }
}
