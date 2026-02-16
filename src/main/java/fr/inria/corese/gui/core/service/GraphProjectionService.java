package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides serialized projections of the shared graph for UI consumers.
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for shared stateless projection logic
public final class GraphProjectionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphProjectionService.class);
	private static final GraphProjectionService INSTANCE = new GraphProjectionService();
	private static final List<SerializationFormat> RDF_EXPORT_FORMATS = List
			.copyOf(Arrays.asList(SerializationFormat.rdfFormats()));
	/**
	 * Corese JSON-LD can occasionally emit malformed string values such as
	 * {@code ""@type""} (double-quoted twice). This pattern sanitizes those values
	 * into valid JSON strings ({@code "\"@type\""}).
	 */
	private static final Pattern MALFORMED_DOUBLE_QUOTED_VALUE_PATTERN = Pattern
			.compile("(^|[:\\[,\\s])\"\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"\"(?=\\s*[,}\\]])", Pattern.MULTILINE);
	/**
	 * Matches the typical top-level JSON-LD shape produced by Corese:
	 * {"@context": {...}, "@graph": [...]}
	 */
	private static final Pattern TOP_LEVEL_CONTEXT_GRAPH_PATTERN = Pattern
			.compile("(?s)^\\s*\\{\\s*\"@context\"\\s*:\\s*(\\{.*?\\})\\s*,\\s*\"@graph\"\\s*:\\s*(\\[.*\\])\\s*}\\s*$");

	private GraphProjectionService() {
	}

	/**
	 * Returns the singleton projection service.
	 *
	 * @return projection service instance
	 */
	public static GraphProjectionService getInstance() {
		return INSTANCE;
	}

	/**
	 * Serializes the current shared graph as JSON-LD.
	 *
	 * @return JSON-LD snapshot string, or empty string when graph is empty
	 */
	public String snapshotJsonLd() {
		return serializeGraph(SerializationFormat.JSON_LD);
	}

	/**
	 * Serializes the current shared graph with the requested RDF format.
	 *
	 * @param format
	 *            RDF format
	 * @return serialized graph string, or empty string when graph is empty
	 */
	public String serializeGraph(SerializationFormat format) {
		if (format == null) {
			throw new IllegalArgumentException("format must not be null");
		}
		Graph graph = GraphStoreService.getInstance().getGraph();
		if (graph.size() == 0) {
			return "";
		}
		Graph serializationGraph = graph;
		if (format == SerializationFormat.JSON_LD) {
			/*
			 * Corese JSON-LD rendering can keep stale internal state when serializing the
			 * same mutable graph instance repeatedly. Serializing a fresh copy guarantees
			 * that incremental loads are reflected in the preview/export payload.
			 */
			serializationGraph = graph.copy();
		}
		String serialized = ResultFormatter.getInstance().formatGraph(serializationGraph, format);
		if (serialized == null) {
			throw new IllegalStateException("Graph serialization returned null.");
		}
		if (serialized.startsWith("Error:")) {
			throw new IllegalStateException(serialized);
		}
		if (format == SerializationFormat.JSON_LD) {
			String sanitized = sanitizeMalformedJsonLd(serialized);
			return ensureSingleNamedGraphContainer(sanitized, graph);
		}
		return serialized;
	}

	private String sanitizeMalformedJsonLd(String jsonLd) {
		if (jsonLd == null || jsonLd.isBlank()) {
			return jsonLd;
		}

		Matcher matcher = MALFORMED_DOUBLE_QUOTED_VALUE_PATTERN.matcher(jsonLd);
		StringBuffer sanitized = new StringBuffer(jsonLd.length());
		int replacementCount = 0;
		while (matcher.find()) {
			String prefix = matcher.group(1);
			String content = matcher.group(2);
			String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"");
			String replacement = prefix + "\"\\\"" + escapedContent + "\\\"\"";
			matcher.appendReplacement(sanitized, Matcher.quoteReplacement(replacement));
			replacementCount++;
		}
		if (replacementCount == 0) {
			return jsonLd;
		}
		matcher.appendTail(sanitized);
		LOGGER.warn("Sanitized {} malformed JSON-LD quoted value(s) in graph snapshot.", replacementCount);
		return sanitized.toString();
	}

	private String ensureSingleNamedGraphContainer(String jsonLd, Graph graph) {
		if (jsonLd == null || jsonLd.isBlank() || graph == null || graph.size() == 0) {
			return jsonLd;
		}

		DataWorkspaceStatusSupport.GraphCountSnapshot snapshot = DataWorkspaceStatusSupport.computeGraphCountSnapshot(
				graph, Math.max(0, graph.size()), LOGGER);
		Map<String, Integer> namedGraphCounts = snapshot.namedGraphCounts();
		if (snapshot.defaultGraphTripleCount() > 0 || namedGraphCounts.size() != 1) {
			return jsonLd;
		}

		String namedGraphId = namedGraphCounts.keySet().iterator().next();
		if (namedGraphId == null || namedGraphId.isBlank()) {
			return jsonLd;
		}
		if (hasNamedGraphContainer(jsonLd, namedGraphId)) {
			return jsonLd;
		}

		Matcher matcher = TOP_LEVEL_CONTEXT_GRAPH_PATTERN.matcher(jsonLd);
		if (!matcher.matches()) {
			return jsonLd;
		}

		String contextJson = matcher.group(1);
		String graphArrayJson = matcher.group(2);
		String escapedGraphId = escapeJsonString(namedGraphId);

		LOGGER.info("Wrapped flattened JSON-LD @graph payload into named graph container: {}", namedGraphId);
		return "{\n\t\"@context\": " + contextJson + ",\n\t\"@graph\": [\n\t\t{\n\t\t\t\"@id\": \"" + escapedGraphId
				+ "\",\n\t\t\t\"@graph\": " + graphArrayJson + "\n\t\t}\n\t]\n}";
	}

	private boolean hasNamedGraphContainer(String jsonLd, String namedGraphId) {
		if (jsonLd == null || jsonLd.isBlank() || namedGraphId == null || namedGraphId.isBlank()) {
			return false;
		}
		Pattern containerPattern = Pattern
				.compile("\"@id\"\\s*:\\s*\"" + Pattern.quote(namedGraphId) + "\"\\s*,\\s*\"@graph\"");
		return containerPattern.matcher(jsonLd).find();
	}

	private String escapeJsonString(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	/**
	 * Returns all supported RDF export formats.
	 *
	 * @return immutable list of RDF serialization formats
	 */
	public List<SerializationFormat> supportedRdfExportFormats() {
		return RDF_EXPORT_FORMATS;
	}
}
