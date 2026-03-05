package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
	private static final String JSON_LD_VALUE_KEY = "\"@value\"";

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
		String serialized;
		try {
			serialized = ResultFormatter.getInstance().formatGraphOrThrow(serializationGraph, format);
		} catch (ResultFormatter.ResultFormattingException e) {
			throw new IllegalStateException(e.getMessage(), e);
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

		// Pass 1: legacy Corese bug where values are emitted as ""value"".
		SanitizationResult doubleQuotedSanitization = sanitizeMalformedDoubleQuotedValues(jsonLd);
		// Pass 2: malformed @value literals with raw quotes/control chars inside
		// string payloads.
		SanitizationResult literalQuoteSanitization = sanitizeMalformedLiteralQuotedValues(
				doubleQuotedSanitization.sanitized());
		int totalReplacements = doubleQuotedSanitization.replacementCount()
				+ literalQuoteSanitization.replacementCount();
		if (totalReplacements > 0) {
			LOGGER.warn(
					"Sanitized {} malformed JSON-LD quoted value(s) in graph snapshot (double-quoted: {}, literal-inner-quotes: {}).",
					totalReplacements, doubleQuotedSanitization.replacementCount(),
					literalQuoteSanitization.replacementCount());
		}
		return totalReplacements == 0 ? jsonLd : literalQuoteSanitization.sanitized();
	}

	private SanitizationResult sanitizeMalformedDoubleQuotedValues(String jsonLd) {
		StringBuilder sanitized = new StringBuilder(jsonLd.length());
		int cursor = 0;
		int replacementCount = 0;
		while (cursor < jsonLd.length()) {
			int start = findMalformedQuotedValueStart(jsonLd, cursor);
			if (start < 0) {
				sanitized.append(jsonLd, cursor, jsonLd.length());
				cursor = jsonLd.length();
			} else {
				ReplacementAttempt attempt = tryReplaceMalformedQuotedValue(jsonLd, cursor, start, sanitized);
				cursor = attempt.nextCursor();
				if (attempt.replaced()) {
					replacementCount++;
				}
			}
		}
		return replacementCount == 0
				? new SanitizationResult(jsonLd, 0)
				: new SanitizationResult(sanitized.toString(), replacementCount);
	}

	private SanitizationResult sanitizeMalformedLiteralQuotedValues(String jsonLd) {
		StringBuilder sanitized = new StringBuilder(jsonLd.length());
		int cursor = 0;
		int replacementCount = 0;

		while (cursor < jsonLd.length()) {
			LiteralQuotedValueSanitizationStep step = sanitizeLiteralQuotedValueAt(jsonLd, cursor, sanitized);
			replacementCount += step.replacementCount();
			cursor = step.nextCursor();
			if (step.stop()) {
				cursor = jsonLd.length();
			}
		}
		return replacementCount == 0
				? new SanitizationResult(jsonLd, 0)
				: new SanitizationResult(sanitized.toString(), replacementCount);
	}

	private static LiteralQuotedValueSanitizationStep sanitizeLiteralQuotedValueAt(String jsonLd, int cursor,
			StringBuilder sanitized) {
		int keyStart = jsonLd.indexOf(JSON_LD_VALUE_KEY, cursor);
		if (keyStart < 0) {
			sanitized.append(jsonLd, cursor, jsonLd.length());
			return new LiteralQuotedValueSanitizationStep(jsonLd.length(), 0, true);
		}

		sanitized.append(jsonLd, cursor, keyStart);
		int keyEnd = keyStart + JSON_LD_VALUE_KEY.length();
		int colonIndex = jsonLd.indexOf(':', keyEnd);
		if (colonIndex < 0) {
			sanitized.append(jsonLd, keyStart, jsonLd.length());
			return new LiteralQuotedValueSanitizationStep(jsonLd.length(), 0, true);
		}

		int valueStart = skipWhitespace(jsonLd, colonIndex + 1);
		if (valueStart >= jsonLd.length() || jsonLd.charAt(valueStart) != '"') {
			sanitized.append(jsonLd, keyStart, valueStart);
			return new LiteralQuotedValueSanitizationStep(valueStart, 0, false);
		}

		sanitized.append(jsonLd, keyStart, valueStart + 1);
		LiteralQuotedValueContentParseResult parseResult = parseLiteralQuotedValueContent(jsonLd, valueStart + 1,
				sanitized);
		return new LiteralQuotedValueSanitizationStep(parseResult.nextCursor(), parseResult.replacementCount(),
				!parseResult.closed());
	}

	private static LiteralQuotedValueContentParseResult parseLiteralQuotedValueContent(String jsonLd, int startIndex,
			StringBuilder sanitized) {
		int replacementCount = 0;
		boolean escaping = false;
		int index = startIndex;
		while (index < jsonLd.length()) {
			char current = jsonLd.charAt(index);
			if (escaping) {
				if (isValidJsonEscapeCharacter(current)) {
					sanitized.append(current);
				} else {
					/*
					 * Preserve the literal content while restoring valid JSON escaping. Example:
					 * malformed "\'" becomes valid "\\'".
					 */
					sanitized.append('\\').append(current);
					replacementCount++;
				}
				escaping = false;
			} else if (current <= 0x1F) {
				sanitized.append(escapeJsonControlCharacter(current));
				replacementCount++;
			} else if (current == '\\') {
				sanitized.append(current);
				escaping = true;
			} else if (current == '"') {
				if (isLikelyLiteralValueClosingQuote(jsonLd, index + 1)) {
					sanitized.append(current);
					return new LiteralQuotedValueContentParseResult(index + 1, replacementCount, true);
				}
				sanitized.append("\\\"");
				replacementCount++;
			} else {
				sanitized.append(current);
			}
			index++;
		}
		return new LiteralQuotedValueContentParseResult(index, replacementCount, false);
	}

	private static boolean isValidJsonEscapeCharacter(char value) {
		return value == '"' || value == '\\' || value == '/' || value == 'b' || value == 'f' || value == 'n'
				|| value == 'r' || value == 't' || value == 'u';
	}

	private static String escapeJsonControlCharacter(char value) {
		return switch (value) {
			case '\b' -> "\\b";
			case '\f' -> "\\f";
			case '\n' -> "\\n";
			case '\r' -> "\\r";
			case '\t' -> "\\t";
			default -> "\\u%04x".formatted((int) value);
		};
	}

	private static ReplacementAttempt tryReplaceMalformedQuotedValue(String jsonLd, int cursor, int start,
			StringBuilder output) {
		int end = findMalformedQuotedValueEnd(jsonLd, start + 2);
		if (end < 0 || !hasMalformedValueSuffix(jsonLd, end + 2)) {
			// No safe replacement from this position, keep scanning.
			output.append(jsonLd, cursor, start + 1);
			return new ReplacementAttempt(start + 1, false);
		}

		String content = jsonLd.substring(start + 2, end);
		String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"");
		output.append(jsonLd, cursor, start);
		output.append("\"\\\"").append(escapedContent).append("\\\"\"");
		return new ReplacementAttempt(end + 2, true);
	}

	private String ensureSingleNamedGraphContainer(String jsonLd, Graph graph) {
		if (jsonLd == null || jsonLd.isBlank() || graph == null || graph.size() == 0) {
			return jsonLd;
		}

		DataWorkspaceStatusSupport.GraphCountSnapshot snapshot = DataWorkspaceStatusSupport
				.computeGraphCountSnapshot(graph, Math.max(0, graph.size()), LOGGER);
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

		TopLevelJsonLdParts parts = extractTopLevelContextAndGraph(jsonLd);
		if (parts == null) {
			return jsonLd;
		}

		String escapedGraphId = escapeJsonString(namedGraphId);

		LOGGER.info("Wrapped flattened JSON-LD @graph payload into named graph container: {}", namedGraphId);
		return "{\n\t\"@context\": " + parts.contextJson() + ",\n\t\"@graph\": [\n\t\t{\n\t\t\t\"@id\": \""
				+ escapedGraphId + "\",\n\t\t\t\"@graph\": " + parts.graphArrayJson() + "\n\t\t}\n\t]\n}";
	}

	private boolean hasNamedGraphContainer(String jsonLd, String namedGraphId) {
		if (jsonLd == null || jsonLd.isBlank() || namedGraphId == null || namedGraphId.isBlank()) {
			return false;
		}
		String expectedId = "\"" + escapeJsonString(namedGraphId) + "\"";
		int idKeyIndex = jsonLd.indexOf("\"@id\"");
		while (idKeyIndex >= 0) {
			if (matchesNamedGraphContainerAt(jsonLd, idKeyIndex, expectedId)) {
				return true;
			}
			idKeyIndex = jsonLd.indexOf("\"@id\"", idKeyIndex + "\"@id\"".length());
		}
		return false;
	}

	private static boolean matchesNamedGraphContainerAt(String jsonLd, int idKeyIndex, String expectedId) {
		int colonIndex = jsonLd.indexOf(':', idKeyIndex);
		if (colonIndex < 0) {
			return false;
		}
		int valueStart = skipWhitespace(jsonLd, colonIndex + 1);
		if (valueStart >= jsonLd.length() || jsonLd.charAt(valueStart) != '"') {
			return false;
		}
		int valueEnd = findJsonValueEnd(jsonLd, valueStart);
		if (valueEnd <= valueStart) {
			return false;
		}
		String actualId = jsonLd.substring(valueStart, valueEnd + 1);
		return expectedId.equals(actualId) && jsonLd.indexOf("\"@graph\"", valueEnd) >= 0;
	}

	private static int findMalformedQuotedValueStart(String jsonLd, int fromIndex) {
		for (int i = Math.max(0, fromIndex); i < jsonLd.length() - 1; i++) {
			if (jsonLd.charAt(i) == '"' && jsonLd.charAt(i + 1) == '"' && hasMalformedValuePrefix(jsonLd, i - 1)
					&& !hasMalformedValueSuffix(jsonLd, i + 2)) {
				return i;
			}
		}
		return -1;
	}

	private static int findMalformedQuotedValueEnd(String jsonLd, int contentStart) {
		boolean escaping = false;
		for (int i = Math.max(0, contentStart); i < jsonLd.length() - 1; i++) {
			char current = jsonLd.charAt(i);
			if (escaping) {
				escaping = false;
			} else if (current == '\\') {
				escaping = true;
			} else if (current == '"' && jsonLd.charAt(i + 1) == '"') {
				return i;
			}
		}
		return -1;
	}

	private static boolean hasMalformedValuePrefix(String jsonLd, int index) {
		if (index < 0) {
			return true;
		}
		char prefix = jsonLd.charAt(index);
		return prefix == ':' || prefix == '[' || prefix == ',' || Character.isWhitespace(prefix);
	}

	private static boolean hasMalformedValueSuffix(String jsonLd, int index) {
		int cursor = skipWhitespace(jsonLd, index);
		if (cursor >= jsonLd.length()) {
			return false;
		}
		char suffix = jsonLd.charAt(cursor);
		return suffix == ',' || suffix == '}' || suffix == ']';
	}

	private static boolean isLikelyLiteralValueClosingQuote(String jsonLd, int fromIndex) {
		int nextToken = skipWhitespace(jsonLd, fromIndex);
		if (nextToken >= jsonLd.length()) {
			return true;
		}
		char candidate = jsonLd.charAt(nextToken);
		if (candidate == '}') {
			return true;
		}
		if (candidate != ',') {
			return false;
		}

		/*
		 * Consider comma as string terminator only when the following token looks like
		 * a JSON object field (`"key": ...`). This avoids false positives for literal
		 * text fragments like `"term", sometimes ...`.
		 */
		int afterComma = skipWhitespace(jsonLd, nextToken + 1);
		if (afterComma >= jsonLd.length() || jsonLd.charAt(afterComma) != '"') {
			return false;
		}
		int keyEnd = findStringEnd(jsonLd, afterComma + 1);
		if (keyEnd < 0) {
			return false;
		}
		int colon = skipWhitespace(jsonLd, keyEnd + 1);
		return colon < jsonLd.length() && jsonLd.charAt(colon) == ':';
	}

	private TopLevelJsonLdParts extractTopLevelContextAndGraph(String jsonLd) {
		String trimmed = jsonLd == null ? "" : jsonLd.trim();
		if (trimmed.isEmpty() || trimmed.charAt(0) != '{') {
			return null;
		}

		int contextValueStart = findTopLevelValueStart(trimmed, "@context", 0);
		if (contextValueStart < 0) {
			return null;
		}
		int contextValueEnd = findJsonValueEnd(trimmed, contextValueStart);
		if (contextValueEnd < 0) {
			return null;
		}

		int graphValueStart = findTopLevelValueStart(trimmed, "@graph", contextValueEnd + 1);
		if (graphValueStart < 0 || trimmed.charAt(graphValueStart) != '[') {
			return null;
		}
		int graphValueEnd = findJsonValueEnd(trimmed, graphValueStart);
		if (graphValueEnd < 0) {
			return null;
		}

		String contextJson = trimmed.substring(contextValueStart, contextValueEnd + 1);
		String graphArrayJson = trimmed.substring(graphValueStart, graphValueEnd + 1);
		return new TopLevelJsonLdParts(contextJson, graphArrayJson);
	}

	private static int findTopLevelValueStart(String json, String key, int fromIndex) {
		String keyToken = "\"" + key + "\"";
		boolean inString = false;
		boolean escaping = false;
		int depth = 0;
		for (int i = 0; i < json.length(); i++) {
			char current = json.charAt(i);
			if (inString) {
				QuotedStringState state = consumeQuotedStringChar(current, escaping);
				escaping = state.escaping();
				inString = state.inString();
			} else if (current == '"') {
				int valueStart = resolveTopLevelKeyValueStart(json, keyToken, fromIndex, depth, i);
				if (valueStart >= 0) {
					return valueStart;
				}
				inString = true;
			} else {
				depth = updateNestingDepth(depth, current);
			}
		}
		return -1;
	}

	private static int resolveTopLevelKeyValueStart(String json, String keyToken, int fromIndex, int depth, int index) {
		if (index < fromIndex || depth != 1 || !json.startsWith(keyToken, index)) {
			return -1;
		}
		int colonIndex = skipWhitespace(json, index + keyToken.length());
		if (colonIndex >= json.length() || json.charAt(colonIndex) != ':') {
			return -1;
		}
		return skipWhitespace(json, colonIndex + 1);
	}

	private static int updateNestingDepth(int depth, char current) {
		if (current == '{' || current == '[') {
			return depth + 1;
		}
		if (current == '}' || current == ']') {
			return Math.max(0, depth - 1);
		}
		return depth;
	}

	private static int findJsonValueEnd(String json, int valueStart) {
		if (valueStart < 0 || valueStart >= json.length()) {
			return -1;
		}
		char startChar = json.charAt(valueStart);
		if (startChar == '{' || startChar == '[') {
			return findMatchingBracket(json, valueStart, startChar, startChar == '{' ? '}' : ']');
		}
		if (startChar == '"') {
			return findStringEnd(json, valueStart + 1);
		}
		return findPrimitiveValueEnd(json, valueStart);
	}

	private static int findStringEnd(String json, int fromIndex) {
		boolean escaping = false;
		for (int i = fromIndex; i < json.length(); i++) {
			char current = json.charAt(i);
			if (escaping) {
				escaping = false;
			} else if (current == '\\') {
				escaping = true;
			} else if (current == '"') {
				return i;
			}
		}
		return -1;
	}

	private static int findPrimitiveValueEnd(String json, int valueStart) {
		for (int i = valueStart; i < json.length(); i++) {
			char current = json.charAt(i);
			if (current == ',' || current == '}') {
				return i - 1;
			}
		}
		return json.length() - 1;
	}

	private static int findMatchingBracket(String json, int start, char open, char close) {
		int depth = 0;
		boolean inString = false;
		boolean escaping = false;
		for (int i = start; i < json.length(); i++) {
			char current = json.charAt(i);
			if (inString) {
				QuotedStringState state = consumeQuotedStringChar(current, escaping);
				escaping = state.escaping();
				inString = state.inString();
			} else if (current == '"') {
				inString = true;
			} else {
				depth = updateBracketDepth(depth, current, open, close);
				if (depth == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	private static int updateBracketDepth(int depth, char current, char open, char close) {
		if (current == open) {
			return depth + 1;
		}
		if (current == close) {
			return depth - 1;
		}
		return depth;
	}

	private static QuotedStringState consumeQuotedStringChar(char current, boolean escaping) {
		if (escaping) {
			return new QuotedStringState(true, false);
		}
		if (current == '\\') {
			return new QuotedStringState(true, true);
		}
		if (current == '"') {
			return new QuotedStringState(false, false);
		}
		return new QuotedStringState(true, false);
	}

	private static int skipWhitespace(String value, int index) {
		int cursor = Math.max(0, index);
		while (cursor < value.length() && Character.isWhitespace(value.charAt(cursor))) {
			cursor++;
		}
		return cursor;
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

	private record TopLevelJsonLdParts(String contextJson, String graphArrayJson) {
	}

	private record ReplacementAttempt(int nextCursor, boolean replaced) {
	}

	private record SanitizationResult(String sanitized, int replacementCount) {
	}

	private record QuotedStringState(boolean inString, boolean escaping) {
	}

	private record LiteralQuotedValueSanitizationStep(int nextCursor, int replacementCount, boolean stop) {
	}

	private record LiteralQuotedValueContentParseResult(int nextCursor, int replacementCount, boolean closed) {
	}
}
