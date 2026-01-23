package fr.inria.corese.gui.enums;

/**
 * Enumeration of RDF and SPARQL result serialization formats.
 *
 * <p>This enum centralizes all supported output formats for RDF graphs and SPARQL query results,
 * providing consistent format labels and file extensions across the application.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * SerializationFormat format = SerializationFormat.TURTLE;
 * String label = format.getLabel();        // "Turtle"
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

  // SPARQL Result Formats
  CSV("CSV", ".csv"),
  TSV("TSV", ".tsv"),
  JSON("JSON", ".json"),
  XML("XML", ".xml"),

  // Other
  MARKDOWN("Markdown", ".md");

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
      case TURTLE, TRIG, N_TRIPLES, N_QUADS -> "turtle";
      case RDF_XML, XML -> "xml";
      case JSON_LD, JSON -> "json";
      case CSV, TSV, MARKDOWN -> "text/plain";
    };
  }

  /**
   * Parses a format string to the corresponding enum value.
   *
   * <p>Supports both label matching ("Turtle") and uppercase name matching ("TURTLE").
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
    return new SerializationFormat[] {TURTLE, RDF_XML, JSON_LD, N_TRIPLES, N_QUADS, TRIG};
  }

  /**
   * Returns all SPARQL result formats.
   *
   * @return Array of SPARQL result formats
   */
  public static SerializationFormat[] sparqlResultFormats() {
    return new SerializationFormat[] {CSV, TSV, JSON, XML, MARKDOWN};
  }
}
