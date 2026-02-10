package fr.inria.corese.gui.core.io;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.File;
import java.util.List;

/**
 * Centralizes default file name generation used by save/export dialogs.
 *
 * <p>
 * Keeping this policy in one place ensures coherent naming across editors,
 * result exports, and graph exports.
 */
public final class DefaultFileNameResolver {

	private static final String BASE_FILE = "file";
	private static final String BASE_EXPORT = "export";
	private static final String BASE_QUERY = "query";
	private static final String BASE_QUERY_RESULTS = "query-results";
	private static final String BASE_SHAPES = "shapes";
	private static final String BASE_GRAPH = "graph";
	private static final String EXPORT_SUFFIX = "-export";

	private DefaultFileNameResolver() {
		// Utility class
	}

	/**
	 * Returns the default base name for editor Save/Export actions.
	 *
	 * @param filePath
	 *            current editor file path (if any)
	 * @param allowedExtensions
	 *            editor allowed extensions
	 * @param forExport
	 *            true when preparing an export filename
	 * @return the default base name without extension
	 */
	public static String editorBaseName(String filePath, List<String> allowedExtensions, boolean forExport) {
		String fileBase = extractBaseName(filePath);
		if (fileBase != null && !fileBase.isBlank()) {
			return forExport ? withExportSuffix(fileBase) : fileBase;
		}

		if (isSparqlEditor(allowedExtensions)) {
			return forExport ? withExportSuffix(BASE_QUERY) : BASE_QUERY;
		}

		if (isRdfEditor(allowedExtensions)) {
			return forExport ? withExportSuffix(BASE_SHAPES) : BASE_SHAPES;
		}

		return forExport ? BASE_EXPORT : BASE_FILE;
	}

	/** Returns the default base name for exporting a single formatted result. */
	public static String resultBaseName(SerializationFormat format) {
		if (format == null) {
			return BASE_EXPORT;
		}
		if (isSparqlResultFormat(format)) {
			return BASE_QUERY_RESULTS;
		}
		if (isRdfGraphFormat(format)) {
			return BASE_GRAPH;
		}
		return BASE_EXPORT;
	}

	/** Returns the default base name for exporting results in multiple formats. */
	public static String resultBaseName(List<SerializationFormat> formats) {
		if (formats == null || formats.isEmpty()) {
			return BASE_EXPORT;
		}
		boolean hasSparql = false;
		boolean hasRdf = false;
		for (SerializationFormat format : formats) {
			if (isSparqlResultFormat(format)) {
				hasSparql = true;
			} else if (isRdfGraphFormat(format)) {
				hasRdf = true;
			}
		}
		if (hasSparql && !hasRdf) {
			return BASE_QUERY_RESULTS;
		}
		if (hasRdf && !hasSparql) {
			return BASE_GRAPH;
		}
		return BASE_EXPORT;
	}

	/** Returns the canonical base name for graph exports. */
	public static String graphBaseName() {
		return BASE_GRAPH;
	}

	private static String withExportSuffix(String baseName) {
		if (baseName == null || baseName.isBlank()) {
			return BASE_EXPORT;
		}
		if (baseName.endsWith(EXPORT_SUFFIX)) {
			return baseName;
		}
		return baseName + EXPORT_SUFFIX;
	}

	private static String extractBaseName(String path) {
		if (path == null || path.isBlank()) {
			return null;
		}
		String fileName = new File(path).getName();
		int dot = fileName.lastIndexOf('.');
		if (dot <= 0) {
			return fileName;
		}
		return fileName.substring(0, dot);
	}

	private static boolean isSparqlEditor(List<String> allowedExtensions) {
		if (allowedExtensions == null || allowedExtensions.isEmpty()) {
			return false;
		}
		for (String extension : allowedExtensions) {
			if (SerializationFormat.forExtension(extension) != SerializationFormat.SPARQL_QUERY) {
				return false;
			}
		}
		return true;
	}

	private static boolean isRdfEditor(List<String> allowedExtensions) {
		if (allowedExtensions == null || allowedExtensions.isEmpty()) {
			return false;
		}
		for (String ext : allowedExtensions) {
			SerializationFormat format = SerializationFormat.forExtension(ext);
			if (format != null && format != SerializationFormat.SPARQL_QUERY && format != SerializationFormat.TEXT) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSparqlResultFormat(SerializationFormat format) {
		return switch (format) {
			case CSV, TSV, JSON, XML, MARKDOWN -> true;
			default -> false;
		};
	}

	private static boolean isRdfGraphFormat(SerializationFormat format) {
		return switch (format) {
			case TURTLE, RDF_XML, JSON_LD, N_TRIPLES, N_QUADS, TRIG, RDFC10, RDFC10_SHA384 -> true;
			default -> false;
		};
	}
}
