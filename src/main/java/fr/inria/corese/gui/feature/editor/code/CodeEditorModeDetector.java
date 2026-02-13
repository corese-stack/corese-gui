package fr.inria.corese.gui.feature.editor.code;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import java.util.List;

/**
 * Resolves the editor syntax mode from a file path or raw content.
 */
final class CodeEditorModeDetector {

	private final List<String> allowedExtensions;

	CodeEditorModeDetector(List<String> allowedExtensions) {
		this.allowedExtensions = allowedExtensions == null ? List.of() : List.copyOf(allowedExtensions);
	}

	SerializationFormat resolve(String filePath, String content) {
		if (filePath != null) {
			return resolveFromExtension(filePath);
		}
		if (content != null) {
			return resolveFromContent(content);
		}
		return SerializationFormat.TEXT;
	}

	SerializationFormat resolveFromExtension(String path) {
		if (path == null) {
			return SerializationFormat.TEXT;
		}
		String extension = FileTypeSupport.extractExtension(path);
		if (extension == null) {
			return SerializationFormat.TEXT;
		}
		SerializationFormat format = SerializationFormat.forExtension(extension);
		if (format == null || !isModeAllowed(format)) {
			return SerializationFormat.TEXT;
		}
		return format;
	}

	SerializationFormat resolveFromContent(String content) {
		if (content == null || content.isBlank()) {
			return SerializationFormat.TEXT;
		}

		String lower = content.toLowerCase();
		String trimmed = content.trim();

		SerializationFormat format = detectXmlFormat(trimmed);
		if (format != null && looksLikeXmlRuleDocument(lower, trimmed)) {
			return format;
		}

		format = detectSparqlFormat(lower);
		if (format != null) {
			return format;
		}

		format = detectTurtleOrTrigFormat(trimmed, lower);
		if (format != null) {
			return format;
		}

		format = detectNTriplesOrQuadsFormat(trimmed);
		if (format != null) {
			return format;
		}

		format = detectJsonFormat(trimmed);
		if (format != null) {
			return format;
		}

		format = detectXmlFormat(trimmed);
		return format != null ? format : SerializationFormat.TEXT;
	}

	private SerializationFormat detectSparqlFormat(String lower) {
		if (!isModeAllowed(SerializationFormat.SPARQL_QUERY)) {
			return null;
		}

		String normalized = " " + lower.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ') + " ";
		boolean looksLikeSparql = normalized.contains(" select ") || normalized.contains(" construct ")
				|| normalized.contains(" ask ") || normalized.contains(" describe ") || normalized.contains(" prefix ")
				|| normalized.contains(" base ") || normalized.contains(" insert ") || normalized.contains(" delete ")
				|| normalized.contains(" load ") || normalized.contains(" clear ") || normalized.contains(" create ")
				|| normalized.contains(" drop ") || normalized.contains(" move ") || normalized.contains(" copy ")
				|| normalized.contains(" add ") || normalized.contains(" with ") || normalized.contains(" using ");
		return looksLikeSparql ? SerializationFormat.SPARQL_QUERY : null;
	}

	private SerializationFormat detectTurtleOrTrigFormat(String trimmed, String lower) {
		if (!(isModeAllowed(SerializationFormat.TURTLE) || isModeAllowed(SerializationFormat.TRIG))) {
			return null;
		}
		boolean looksLikeTurtle = lower.contains("@prefix") || lower.contains("@base") || lower.contains(" a ")
				|| trimmed.endsWith(".");
		if (!looksLikeTurtle) {
			return null;
		}
		boolean trigLike = isModeAllowed(SerializationFormat.TRIG) && looksLikeTrig(trimmed, lower);
		if (trigLike) {
			return SerializationFormat.TRIG;
		}
		return isModeAllowed(SerializationFormat.TURTLE) ? SerializationFormat.TURTLE : SerializationFormat.TRIG;
	}

	private SerializationFormat detectNTriplesOrQuadsFormat(String trimmed) {
		if (!(isModeAllowed(SerializationFormat.N_TRIPLES) || isModeAllowed(SerializationFormat.N_QUADS))) {
			return null;
		}
		if (!looksLikeNTriplesOrQuads(trimmed)) {
			return null;
		}
		return isModeAllowed(SerializationFormat.N_QUADS) ? SerializationFormat.N_QUADS : SerializationFormat.N_TRIPLES;
	}

	private SerializationFormat detectJsonFormat(String trimmed) {
		if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
			return null;
		}
		if (!(isModeAllowed(SerializationFormat.JSON_LD) || isModeAllowed(SerializationFormat.JSON))) {
			return null;
		}
		if (isModeAllowed(SerializationFormat.JSON_LD) && looksLikeJsonLd(trimmed)) {
			return SerializationFormat.JSON_LD;
		}
		if (isModeAllowed(SerializationFormat.JSON)) {
			return SerializationFormat.JSON;
		}
		return SerializationFormat.JSON_LD;
	}

	private SerializationFormat detectXmlFormat(String trimmed) {
		if (!trimmed.startsWith("<")) {
			return null;
		}
		if (isModeAllowed(SerializationFormat.RDF_XML)) {
			return SerializationFormat.RDF_XML;
		}
		if (isModeAllowed(SerializationFormat.XML)) {
			return SerializationFormat.XML;
		}
		return null;
	}

	private static boolean looksLikeXmlRuleDocument(String lower, String trimmed) {
		if (trimmed.startsWith("<?xml")) {
			return true;
		}
		return lower.startsWith("<rdf:rdf") || lower.startsWith("<rule") || lower.contains("<![cdata[")
				|| (lower.contains("<rdf:rdf") && lower.contains("</rdf:rdf>"));
	}

	private static boolean looksLikeJsonLd(String trimmed) {
		String lower = trimmed.toLowerCase();
		return lower.contains("\"@context\"") || lower.contains("\"@id\"") || lower.contains("\"@graph\"");
	}

	private static boolean looksLikeTrig(String trimmed, String lower) {
		if (trimmed.isEmpty()) {
			return false;
		}
		return lower.contains("graph ") || (trimmed.contains("{") && trimmed.contains("}"));
	}

	private static boolean looksLikeNTriplesOrQuads(String trimmed) {
		if (trimmed.isEmpty()) {
			return false;
		}
		String firstLine = trimmed.split("\n", 2)[0].trim();
		if (firstLine.isEmpty()) {
			return false;
		}
		boolean startsLikeTriple = firstLine.startsWith("<") || firstLine.startsWith("_:");
		boolean endsWithDot = firstLine.endsWith(".");
		return startsLikeTriple && endsWithDot && !firstLine.contains("{") && !firstLine.contains("}");
	}

	private boolean isModeAllowed(SerializationFormat format) {
		if (allowedExtensions.isEmpty()) {
			return true;
		}
		if (format == null) {
			return false;
		}
		if (allowedExtensions.contains(format.getExtension())) {
			return true;
		}

		for (String ext : allowedExtensions) {
			SerializationFormat mapped = SerializationFormat.forExtension(ext);
			if (mapped == format) {
				return true;
			}
		}
		return false;
	}
}
