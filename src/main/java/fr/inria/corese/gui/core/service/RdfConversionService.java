package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for converting RDF content between serialization formats.
 *
 * <p>All direct Corese API usage for conversion should be isolated here to keep
 * the rest of the app decoupled from Corese implementation details.
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for stateless service
public final class RdfConversionService {

  private static final RdfConversionService INSTANCE = new RdfConversionService();

  private RdfConversionService() {}

  public static RdfConversionService getInstance() {
    return INSTANCE;
  }

  /**
   * Converts RDF content from a source format to a target format using Corese.
   *
   * @param content The RDF content to convert
   * @param sourceFormat The source serialization format
   * @param targetFormat The target serialization format
   * @return The converted content as a string
   */
  public String convertGraphContent(
      String content, SerializationFormat sourceFormat, SerializationFormat targetFormat) {
    if (content == null) {
      throw new IllegalArgumentException("Content cannot be null.");
    }
    Graph graph = parseGraphContent(content, sourceFormat);
    String formatted = ResultFormatter.getInstance().formatGraph(graph, targetFormat);
    if (formatted == null || formatted.startsWith("Error")) {
      throw new IllegalStateException("Failed to format graph to " + targetFormat);
    }
    return formatted;
  }

  private Graph parseGraphContent(String content, SerializationFormat format) {
    Loader.format loadFormat = toLoaderFormat(format);
    if (loadFormat == null) {
      throw new IllegalArgumentException("Unsupported RDF format: " + format);
    }
    try {
      Graph graph = Graph.create();
      Load loader = Load.create(graph);
      loader.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), loadFormat);
      return graph;
    } catch (Exception e) {
      throw new RdfConversionException("Failed to parse RDF content: " + e.getMessage(), e);
    }
  }

  private Loader.format toLoaderFormat(SerializationFormat format) {
    if (format == null) {
      return null;
    }
    return switch (format) {
      case TURTLE -> Loader.format.TURTLE_FORMAT;
      case TRIG -> Loader.format.TRIG_FORMAT;
      case N_TRIPLES -> Loader.format.NT_FORMAT;
      case N_QUADS, RDFC10, RDFC10_SHA384 -> Loader.format.NQUADS_FORMAT;
      case RDF_XML -> Loader.format.RDFXML_FORMAT;
      case JSON_LD -> Loader.format.JSONLD_FORMAT;
      default -> null;
    };
  }
}
