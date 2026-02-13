package fr.inria.corese.gui.feature.editor.code.support;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.DefaultFileNameResolver;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.feature.editor.code.CodeEditorModeDetector;
import java.util.ArrayList;
import java.util.List;
import javafx.stage.FileChooser;

/**
 * Shared helpers for file extension rules and chooser setup in code editors.
 */
public final class CodeEditorFileSupport {

	private CodeEditorFileSupport() {
		throw new AssertionError("Utility class");
	}

	public static void addOpenFilters(FileChooser chooser, List<String> allowedExtensions) {
		List<String> safeAllowedExtensions = normalizeAllowedExtensions(allowedExtensions);
		if (safeAllowedExtensions.isEmpty()) {
			FileChooser.ExtensionFilter defaultFilter = FileTypeSupport.createExtensionFilter("RDF and SPARQL",
					FileTypeSupport.defaultEditorOpenExtensions(), true);
			chooser.getExtensionFilters().add(defaultFilter);
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
			chooser.setSelectedExtensionFilter(defaultFilter);
			return;
		}

		FileChooser.ExtensionFilter allowedFilter = FileTypeSupport.createExtensionFilter("Allowed Files",
				safeAllowedExtensions, true);
		chooser.getExtensionFilters().add(allowedFilter);
		chooser.setSelectedExtensionFilter(allowedFilter);
	}

	public static void addSaveFilters(FileChooser chooser, List<String> allowedExtensions, String preferredExtension,
			boolean restrictToCurrentFormat) {
		List<String> safeAllowedExtensions = normalizeAllowedExtensions(allowedExtensions);
		String safePreferredExtension = preferredExtension == null ? null : preferredExtension.trim().toLowerCase();

		if (restrictToCurrentFormat && safePreferredExtension != null && !safePreferredExtension.equals(".txt")
				&& (safeAllowedExtensions.isEmpty() || safeAllowedExtensions.contains(safePreferredExtension))) {
			String label = formatLabelForExtension(safePreferredExtension);
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
					label + " (*" + safePreferredExtension + ")", "*" + safePreferredExtension));
			return;
		}

		if (safeAllowedExtensions.isEmpty()) {
			addDefaultSaveFilters(chooser);
			return;
		}

		for (String extension : safeAllowedExtensions) {
			String label = formatLabelForExtension(extension);
			chooser.getExtensionFilters()
					.add(new FileChooser.ExtensionFilter(label + " (*" + extension + ")", "*" + extension));
		}
	}

	public static void selectDefaultSaveFilter(FileChooser chooser, String preferredExtension) {
		if (chooser.getExtensionFilters().isEmpty()) {
			return;
		}
		if (preferredExtension != null) {
			String expectedPattern = "*" + preferredExtension;
			for (FileChooser.ExtensionFilter filter : chooser.getExtensionFilters()) {
				if (filter.getExtensions().contains(expectedPattern)) {
					chooser.setSelectedExtensionFilter(filter);
					return;
				}
			}
		}
		chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().get(0));
	}

	public static String resolvePreferredSaveExtension(String filePath, String content, List<String> allowedExtensions,
			CodeEditorModeDetector modeDetector) {
		String detectedExtension = extractExtension(filePath);
		if (detectedExtension == null || detectedExtension.isBlank()) {
			SerializationFormat detectedFormat = modeDetector.resolveFromContent(content);
			if (detectedFormat != null) {
				detectedExtension = detectedFormat.getExtension();
			}
		}

		List<String> safeAllowedExtensions = normalizeAllowedExtensions(allowedExtensions);
		if (!safeAllowedExtensions.isEmpty()
				&& (detectedExtension == null || !safeAllowedExtensions.contains(detectedExtension))) {
			return safeAllowedExtensions.get(0);
		}

		return detectedExtension != null ? detectedExtension : ".txt";
	}

	public static String resolveDefaultBaseName(String filePath, List<String> allowedExtensions, boolean forExport) {
		return DefaultFileNameResolver.editorBaseName(filePath, normalizeAllowedExtensions(allowedExtensions),
				forExport);
	}

	public static SerializationFormat resolveSourceGraphFormat(String filePath, String content,
			CodeEditorModeDetector detector) {
		SerializationFormat fromPath = SerializationFormat.forExtension(extractExtension(filePath));
		if (fromPath != null) {
			return fromPath;
		}
		SerializationFormat fromContent = detector.resolveFromContent(content);
		if (fromContent != null && fromContent != SerializationFormat.TEXT) {
			return fromContent;
		}
		return SerializationFormat.TURTLE;
	}

	public static List<SerializationFormat> graphExportFormats() {
		return new ArrayList<>(List.of(SerializationFormat.rdfFormats()));
	}

	public static boolean isGraphEditor(List<String> allowedExtensions) {
		List<String> safeAllowedExtensions = normalizeAllowedExtensions(allowedExtensions);
		if (safeAllowedExtensions.isEmpty()) {
			return false;
		}
		for (String extension : safeAllowedExtensions) {
			SerializationFormat format = SerializationFormat.forExtension(extension);
			if (format != null && format != SerializationFormat.SPARQL_QUERY && format != SerializationFormat.TEXT) {
				return true;
			}
		}
		return false;
	}

	public static String extractExtension(String path) {
		return FileTypeSupport.extractExtension(path);
	}

	public static String formatLabelForExtension(String extension) {
		SerializationFormat format = SerializationFormat.forExtension(extension);
		if (format != null) {
			return format.getLabel();
		}
		String value = extension == null ? "" : extension;
		String withoutDot = value.startsWith(".") ? value.substring(1) : value;
		return withoutDot.toUpperCase();
	}

	private static void addDefaultSaveFilters(FileChooser chooser) {
		addSaveFilter(chooser, SerializationFormat.TURTLE.getLabel(), SerializationFormat.TURTLE.getExtension());
		for (String extension : FileTypeSupport.queryExtensions()) {
			addSaveFilter(chooser, SerializationFormat.SPARQL_QUERY.getLabel(), extension);
		}
		addSaveFilter(chooser, SerializationFormat.RDF_XML.getLabel(), SerializationFormat.RDF_XML.getExtension());
		addSaveFilter(chooser, SerializationFormat.JSON_LD.getLabel(), SerializationFormat.JSON_LD.getExtension());
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
	}

	private static void addSaveFilter(FileChooser chooser, String label, String extension) {
		String normalizedExtension = FileTypeSupport.normalizeExtension(extension);
		if (normalizedExtension == null) {
			return;
		}
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(label + " (*" + normalizedExtension + ")", "*" + normalizedExtension));
	}

	private static List<String> normalizeAllowedExtensions(List<String> allowedExtensions) {
		return allowedExtensions == null ? List.of() : allowedExtensions;
	}
}
