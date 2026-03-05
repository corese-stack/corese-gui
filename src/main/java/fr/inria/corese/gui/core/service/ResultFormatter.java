package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.core.sparql.api.ResultFormatDef.format;
import fr.inria.corese.gui.core.enums.SerializationFormat;

/**
 * Utility service for formatting Corese results into various serialization
 * formats.
 *
 * <p>
 * This service translates between Corese's internal {@link ResultFormat} and
 * the GUI's {@link SerializationFormat}, handling both graph serialization and
 * SPARQL result formatting.
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
	// Public API (Corese Types)
	// ==============================================================================================

	/**
	 * Formats a Corese graph into the specified serialization format.
	 *
	 * @param graph
	 *            The Corese graph to format.
	 * @param format
	 *            The desired output format.
	 * @return The formatted graph as a string, or an error message if formatting
	 *         fails.
	 */
	public String formatGraph(Graph graph, SerializationFormat format) {
		try {
			return formatGraphOrThrow(graph, format);
		} catch (ResultFormattingException e) {
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * Formats a graph and throws on invalid input/formatting failures.
	 *
	 * <p>
	 * Services should prefer this method to avoid string-based error protocols.
	 *
	 * @param graph
	 *            graph to serialize
	 * @param format
	 *            output format
	 * @return serialized graph
	 * @throws ResultFormattingException
	 *             when serialization cannot be performed
	 */
	public String formatGraphOrThrow(Graph graph, SerializationFormat format) {
		if (graph == null) {
			throw new ResultFormattingException("No graph data available.");
		}
		format coreseFormat = toCoreseFormat(format);
		if (coreseFormat == null) {
			throw new ResultFormattingException("Unsupported format for Graph serialization: " + format);
		}
		try {
			return ResultFormat.create(graph, coreseFormat).toString();
		} catch (Exception e) {
			throw new ResultFormattingException("Failed to format graph: " + safeMessage(e), e);
		}
	}

	/**
	 * Formats Corese SPARQL query results into the specified serialization format.
	 *
	 * @param mappings
	 *            The Corese mappings to format.
	 * @param format
	 *            The desired output format.
	 * @return The formatted results as a string, or an error message if formatting
	 *         fails.
	 */
	public String formatMappings(Mappings mappings, SerializationFormat format) {
		try {
			return formatMappingsOrThrow(mappings, format);
		} catch (ResultFormattingException e) {
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * Formats mappings and throws on invalid input/formatting failures.
	 *
	 * @param mappings
	 *            mappings to serialize
	 * @param format
	 *            output format
	 * @return serialized mappings
	 * @throws ResultFormattingException
	 *             when serialization cannot be performed
	 */
	public String formatMappingsOrThrow(Mappings mappings, SerializationFormat format) {
		if (mappings == null) {
			throw new ResultFormattingException("No query results available.");
		}
		format coreseFormat = toCoreseFormat(format);
		if (coreseFormat == null) {
			throw new ResultFormattingException("Unsupported format for Mappings serialization: " + format);
		}
		try {
			return ResultFormat.create(mappings, coreseFormat).toString();
		} catch (Exception e) {
			throw new ResultFormattingException("Failed to format results: " + safeMessage(e), e);
		}
	}

	// ==============================================================================================
	// Private Methods
	// ==============================================================================================

	/**
	 * Converts GUI SerializationFormat to Corese ResultFormat.
	 *
	 * @param format
	 *            The GUI format enum.
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

	private static String safeMessage(Throwable throwable) {
		if (throwable == null) {
			return "Unknown error.";
		}
		String message = throwable.getMessage();
		if (message == null || message.isBlank()) {
			return throwable.getClass().getSimpleName();
		}
		return message;
	}

	/**
	 * Raised when graph/result formatting cannot be completed.
	 */
	public static class ResultFormattingException extends RuntimeException {
		public ResultFormattingException(String message) {
			super(message);
		}

		public ResultFormattingException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
