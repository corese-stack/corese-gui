package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for drag-and-drop file filtering and warnings in the Data
 * page.
 */
final class DataDroppedFilesSupport {

	private static final String DROP_WARNING_NONE_ACCEPTED_TEMPLATE = "No compatible files were dropped. %s";
	private static final String DROP_WARNING_IGNORED_TEMPLATE = "Ignored %s. %s";

	record DropEvaluation(List<File> acceptedFiles, int ignoredFiles) {
		DropEvaluation {
			acceptedFiles = acceptedFiles == null ? List.of() : List.copyOf(acceptedFiles);
			ignoredFiles = Math.max(ignoredFiles, 0);
		}

		boolean hasAcceptedFiles() {
			return !acceptedFiles.isEmpty();
		}
	}

	private DataDroppedFilesSupport() {
		throw new AssertionError("Utility class");
	}

	static DropEvaluation evaluate(List<File> droppedFiles, List<String> allowedExtensions) {
		List<File> safeDroppedFiles = droppedFiles == null ? List.of() : List.copyOf(droppedFiles);
		if (safeDroppedFiles.isEmpty()) {
			return new DropEvaluation(List.of(), 0);
		}

		List<File> compatibleFiles = new ArrayList<>();
		int ignoredCount = 0;
		for (File file : safeDroppedFiles) {
			if (file != null && file.isFile() && FileTypeSupport.matchesAllowedExtensions(file, allowedExtensions)) {
				compatibleFiles.add(file);
			} else {
				ignoredCount++;
			}
		}
		return new DropEvaluation(compatibleFiles, ignoredCount);
	}

	static void notifyWarnings(DropEvaluation evaluation, String expectedExtensionsHint) {
		if (evaluation == null) {
			return;
		}
		if (!evaluation.hasAcceptedFiles()) {
			NotificationWidget.getInstance()
					.showWarning(String.format(DROP_WARNING_NONE_ACCEPTED_TEMPLATE, expectedExtensionsHint));
			return;
		}
		if (evaluation.ignoredFiles() > 0) {
			NotificationWidget.getInstance().showWarning(String.format(DROP_WARNING_IGNORED_TEMPLATE,
					DataUiMessageUtils.countLabel(evaluation.ignoredFiles(), "dropped file"), expectedExtensionsHint));
		}
	}
}
