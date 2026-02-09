package fr.inria.corese.gui.feature.editor.tab;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates drag-and-drop file acceptance rules for tab editors.
 *
 * <p>
 * This policy is immutable and derived from the editor allowed extensions
 * declared in {@link TabEditorConfig}. It keeps controller logic focused on
 * orchestration while centralizing extension normalization and matching rules.
 */
final class TabEditorDropFilePolicy {

	private final List<String> normalizedAllowedExtensions;

	TabEditorDropFilePolicy(List<String> allowedExtensions) {
		this.normalizedAllowedExtensions = normalizeAllowedExtensions(allowedExtensions);
	}

	DropEvaluation evaluate(List<File> droppedFiles) {
		if (droppedFiles == null || droppedFiles.isEmpty()) {
			return new DropEvaluation(List.of(), 0);
		}

		List<File> acceptedFiles = new ArrayList<>();
		int unsupportedFiles = 0;
		for (File file : droppedFiles) {
			if (file == null || !file.isFile()) {
				continue;
			}
			if (!isAllowed(file)) {
				unsupportedFiles++;
				continue;
			}
			acceptedFiles.add(file);
		}

		return new DropEvaluation(List.copyOf(acceptedFiles), unsupportedFiles);
	}

	boolean hasRestrictions() {
		return !normalizedAllowedExtensions.isEmpty();
	}

	String describeAllowedExtensions() {
		if (normalizedAllowedExtensions.isEmpty()) {
			return "Allowed extensions are restricted for this editor.";
		}
		return "Allowed: " + String.join(", ", normalizedAllowedExtensions);
	}

	private boolean isAllowed(File file) {
		if (!hasRestrictions()) {
			return true;
		}

		String extension = extractExtension(file.getName());
		if (extension == null) {
			return false;
		}

		String normalizedExtension = normalizeExtension(extension);
		SerializationFormat droppedFormat = SerializationFormat.forExtension(normalizedExtension);

		for (String normalizedAllowed : normalizedAllowedExtensions) {
			if (normalizedAllowed.equals(normalizedExtension)) {
				return true;
			}
			SerializationFormat allowedFormat = SerializationFormat.forExtension(normalizedAllowed);
			if (droppedFormat != null && droppedFormat == allowedFormat) {
				return true;
			}
		}
		return false;
	}

	private static List<String> normalizeAllowedExtensions(List<String> allowedExtensions) {
		if (allowedExtensions == null || allowedExtensions.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String extension : allowedExtensions) {
			String normalizedExtension = normalizeExtension(extension);
			if (normalizedExtension != null) {
				normalized.add(normalizedExtension);
			}
		}
		return List.copyOf(normalized);
	}

	private static String extractExtension(String fileName) {
		if (fileName == null) {
			return null;
		}
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
			return null;
		}
		return fileName.substring(dotIndex);
	}

	private static String normalizeExtension(String extension) {
		if (extension == null) {
			return null;
		}
		String normalized = extension.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			return null;
		}
		return normalized.startsWith(".") ? normalized : "." + normalized;
	}

	record DropEvaluation(List<File> acceptedFiles, int unsupportedFiles) {
	}
}
