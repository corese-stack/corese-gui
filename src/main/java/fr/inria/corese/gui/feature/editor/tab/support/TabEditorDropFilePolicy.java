package fr.inria.corese.gui.feature.editor.tab.support;

import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.feature.editor.tab.TabEditorConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates drag-and-drop file acceptance rules for tab editors.
 *
 * <p>
 * This policy is immutable and derived from the editor allowed extensions
 * declared in {@link TabEditorConfig}. It keeps controller logic focused on
 * orchestration while centralizing extension normalization and matching rules.
 */
public final class TabEditorDropFilePolicy {

	private final List<String> normalizedAllowedExtensions;

	public TabEditorDropFilePolicy(List<String> allowedExtensions) {
		this.normalizedAllowedExtensions = FileTypeSupport.normalizeExtensions(allowedExtensions);
	}

	public DropEvaluation evaluate(List<File> droppedFiles) {
		if (droppedFiles == null || droppedFiles.isEmpty()) {
			return new DropEvaluation(List.of(), 0);
		}

		List<File> acceptedFiles = new ArrayList<>();
		int unsupportedFiles = 0;
		for (File file : droppedFiles) {
			if (file != null && file.isFile()) {
				if (isAllowed(file)) {
					acceptedFiles.add(file);
				} else {
					unsupportedFiles++;
				}
			}
		}

		return new DropEvaluation(List.copyOf(acceptedFiles), unsupportedFiles);
	}

	public boolean hasRestrictions() {
		return !normalizedAllowedExtensions.isEmpty();
	}

	public String describeAllowedExtensions() {
		if (normalizedAllowedExtensions.isEmpty()) {
			return "Expected extensions are configured for this editor.";
		}
		return "Expected extensions: " + String.join(", ", normalizedAllowedExtensions);
	}

	private boolean isAllowed(File file) {
		return FileTypeSupport.matchesAllowedExtensions(file, normalizedAllowedExtensions);
	}

	public record DropEvaluation(List<File> acceptedFiles, int unsupportedFiles) {
	}
}
