package fr.inria.corese.gui.core.io;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import javafx.stage.FileChooser;

/**
 * Central utilities and extension profiles for file type handling.
 *
 * <p>
 * This class provides:
 *
 * <ul>
 * <li>Canonical extension lists by use case (query editors, RDF files, default
 * editor open)
 * <li>Normalization helpers (leading dot + lowercase)
 * <li>File matching logic that supports aliases via {@link SerializationFormat}
 * <li>FileChooser filter/pattern generation with optional case-insensitive
 * variants
 * </ul>
 */
public final class FileTypeSupport {

	private static final List<String> QUERY_EXTENSIONS = List.of(".rq", ".sparql");
	private static final List<String> RDF_EXTENSIONS = buildRdfExtensions();
	private static final List<String> RULE_EXTENSIONS = List.of(".rul");
	private static final List<String> DEFAULT_EDITOR_OPEN_EXTENSIONS = mergeExtensions(RDF_EXTENSIONS,
			QUERY_EXTENSIONS);

	private FileTypeSupport() {
		// Utility class
	}

	public static List<String> queryExtensions() {
		return QUERY_EXTENSIONS;
	}

	/**
	 * Returns RDF-related extensions accepted by data/validation/editor workflows.
	 *
	 * <p>
	 * Includes primary serialization extensions and common aliases.
	 */
	public static List<String> rdfExtensions() {
		return RDF_EXTENSIONS;
	}

	/**
	 * Returns custom reasoning rule extensions accepted by the Data page.
	 */
	public static List<String> ruleExtensions() {
		return RULE_EXTENSIONS;
	}

	/** Returns the default extension set for generic editor "Open file" dialogs. */
	public static List<String> defaultEditorOpenExtensions() {
		return DEFAULT_EDITOR_OPEN_EXTENSIONS;
	}

	/** Normalizes extensions to lowercase and ensures a leading dot. */
	public static List<String> normalizeExtensions(Collection<String> extensions) {
		if (extensions == null || extensions.isEmpty()) {
			return List.of();
		}

		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String extension : extensions) {
			String value = normalizeExtension(extension);
			if (value != null) {
				normalized.add(value);
			}
		}
		return List.copyOf(normalized);
	}

	/** Normalizes one extension to lowercase and ensures a leading dot. */
	public static String normalizeExtension(String extension) {
		if (extension == null) {
			return null;
		}
		String normalized = extension.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return null;
		}
		return normalized.startsWith(".") ? normalized : "." + normalized;
	}

	/**
	 * Extracts extension (with leading dot, lowercase) from a file path or name.
	 */
	public static String extractExtension(String fileNameOrPath) {
		if (fileNameOrPath == null || fileNameOrPath.isBlank()) {
			return null;
		}
		int dot = fileNameOrPath.lastIndexOf('.');
		if (dot < 0 || dot == fileNameOrPath.length() - 1) {
			return null;
		}
		return normalizeExtension(fileNameOrPath.substring(dot));
	}

	/**
	 * Checks whether the given file is accepted by the provided extension
	 * restrictions.
	 */
	public static boolean matchesAllowedExtensions(File file, Collection<String> allowedExtensions) {
		if (file == null) {
			return false;
		}
		return matchesAllowedExtensions(file.getName(), allowedExtensions);
	}

	/**
	 * Checks whether the given file matches one of the allowed extensions exactly
	 * (no format alias fallback).
	 */
	public static boolean matchesAllowedExtensionsStrict(File file, Collection<String> allowedExtensions) {
		if (file == null) {
			return false;
		}
		return matchesAllowedExtensionsStrict(file.getName(), allowedExtensions);
	}

	/**
	 * Checks whether a file name/path is accepted by the provided extension
	 * restrictions.
	 *
	 * <p>
	 * If restrictions are empty, any file is accepted.
	 */
	public static boolean matchesAllowedExtensions(String fileNameOrPath, Collection<String> allowedExtensions) {
		List<String> normalizedAllowed = normalizeExtensions(allowedExtensions);
		if (normalizedAllowed.isEmpty()) {
			return true;
		}

		String normalizedFileExtension = extractExtension(fileNameOrPath);
		if (normalizedFileExtension == null) {
			return false;
		}

		if (normalizedAllowed.contains(normalizedFileExtension)) {
			return true;
		}

		SerializationFormat fileFormat = SerializationFormat.forExtension(normalizedFileExtension);
		if (fileFormat == null) {
			return false;
		}

		for (String allowed : normalizedAllowed) {
			SerializationFormat allowedFormat = SerializationFormat.forExtension(allowed);
			if (fileFormat == allowedFormat) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a file name/path matches one of the allowed extensions
	 * exactly, without comparing serialization-format aliases.
	 */
	public static boolean matchesAllowedExtensionsStrict(String fileNameOrPath, Collection<String> allowedExtensions) {
		List<String> normalizedAllowed = normalizeExtensions(allowedExtensions);
		if (normalizedAllowed.isEmpty()) {
			return true;
		}
		String normalizedFileExtension = extractExtension(fileNameOrPath);
		if (normalizedFileExtension == null) {
			return false;
		}
		return normalizedAllowed.contains(normalizedFileExtension);
	}

	/**
	 * Builds wildcard patterns such as {@code *.ttl}, optionally adding uppercase
	 * variants.
	 */
	public static List<String> wildcardPatterns(Collection<String> extensions, boolean includeUppercaseVariants) {
		List<String> normalized = normalizeExtensions(extensions);
		if (normalized.isEmpty()) {
			return List.of();
		}

		LinkedHashSet<String> patterns = new LinkedHashSet<>();
		for (String extension : normalized) {
			patterns.add("*" + extension);
			if (includeUppercaseVariants) {
				patterns.add("*" + extension.toUpperCase(Locale.ROOT));
			}
		}
		return List.copyOf(patterns);
	}

	/** Creates a JavaFX {@link FileChooser.ExtensionFilter} from extension list. */
	public static FileChooser.ExtensionFilter createExtensionFilter(String label, Collection<String> extensions,
			boolean includeUppercaseVariants) {
		List<String> patterns = wildcardPatterns(extensions, includeUppercaseVariants);
		if (patterns.isEmpty()) {
			return new FileChooser.ExtensionFilter(label, "*.*");
		}
		return new FileChooser.ExtensionFilter(label, patterns);
	}

	/**
	 * Builds a human-readable sentence listing accepted extensions.
	 *
	 * @param extensions
	 *            accepted extension collection
	 * @return formatted sentence, or empty string if none
	 */
	public static String acceptedExtensionsHint(Collection<String> extensions) {
		List<String> normalized = normalizeExtensions(extensions);
		if (normalized.isEmpty()) {
			return "";
		}
		return "Accepted extensions: " + String.join(", ", normalized) + ".";
	}

	/**
	 * Appends a normalized accepted-extensions hint to a base message.
	 *
	 * @param baseMessage
	 *            primary message
	 * @param extensions
	 *            accepted extension collection
	 * @return base message with extension hint when available
	 */
	public static String withAcceptedExtensions(String baseMessage, Collection<String> extensions) {
		String normalizedBase = baseMessage == null ? "" : baseMessage.trim();
		String hint = acceptedExtensionsHint(extensions);
		if (hint.isBlank()) {
			return normalizedBase;
		}
		if (normalizedBase.isBlank()) {
			return hint;
		}
		return normalizedBase + "\n" + hint;
	}

	private static List<String> buildRdfExtensions() {
		LinkedHashSet<String> extensions = new LinkedHashSet<>();
		for (SerializationFormat format : SerializationFormat.rdfFormats()) {
			String ext = normalizeExtension(format.getExtension());
			if (ext != null) {
				extensions.add(ext);
			}
		}
		// Common aliases that map to the same RDF serializations.
		extensions.add(".n3");
		extensions.add(".owl");
		extensions.add(".xml");
		// RDFa documents embedded in HTML/XHTML.
		extensions.add(".html");
		extensions.add(".xhtml");
		extensions.add(".htm");
		return List.copyOf(extensions);
	}

	private static List<String> mergeExtensions(Collection<String> first, Collection<String> second) {
		List<String> merged = new ArrayList<>();
		merged.addAll(normalizeExtensions(first));
		merged.addAll(normalizeExtensions(second));
		return normalizeExtensions(merged);
	}
}
