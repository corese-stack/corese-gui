package fr.inria.corese.gui.feature.data.support;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for strict file filtering and warning messages in Data page
 * file inputs (selection and drag-and-drop).
 */
public final class DataFileSelectionSupport {

	private static final String WARNING_NONE_ACCEPTED_TEMPLATE = "No compatible files were %s. %s";
	private static final String WARNING_IGNORED_TEMPLATE = "Ignored %s. %s";

	/**
	 * Origin of files provided by the user.
	 */
	public enum InputOrigin {
		DROPPED("dropped", "dropped file"), SELECTED("selected", "selected file");

		private final String noneAcceptedVerb;
		private final String ignoredNoun;

		InputOrigin(String noneAcceptedVerb, String ignoredNoun) {
			this.noneAcceptedVerb = noneAcceptedVerb;
			this.ignoredNoun = ignoredNoun;
		}

		String noneAcceptedVerb() {
			return noneAcceptedVerb;
		}

		String ignoredNoun() {
			return ignoredNoun;
		}
	}

	/**
	 * Result of strict file filtering.
	 *
	 * @param acceptedFiles
	 *            accepted compatible files
	 * @param ignoredFiles
	 *            incompatible files count
	 */
	public record FileSelectionEvaluation(List<File> acceptedFiles, int ignoredFiles) {
		public FileSelectionEvaluation {
			acceptedFiles = acceptedFiles == null ? List.of() : List.copyOf(acceptedFiles);
			ignoredFiles = Math.max(ignoredFiles, 0);
		}

		public boolean hasAcceptedFiles() {
			return !acceptedFiles.isEmpty();
		}
	}

	private DataFileSelectionSupport() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Evaluates user-provided files against allowed extensions using strict
	 * extension matching.
	 *
	 * @param inputFiles
	 *            files selected or dropped by user
	 * @param allowedExtensions
	 *            allowed extensions
	 * @return evaluation with accepted files and ignored count
	 */
	public static FileSelectionEvaluation evaluateStrict(List<File> inputFiles, List<String> allowedExtensions) {
		List<File> safeFiles = inputFiles == null ? List.of() : List.copyOf(inputFiles);
		if (safeFiles.isEmpty()) {
			return new FileSelectionEvaluation(List.of(), 0);
		}

		List<File> compatibleFiles = new ArrayList<>();
		int ignoredCount = 0;
		for (File file : safeFiles) {
			if (file != null && file.isFile()
					&& FileTypeSupport.matchesAllowedExtensionsStrict(file, allowedExtensions)) {
				compatibleFiles.add(file);
			} else {
				ignoredCount++;
			}
		}
		return new FileSelectionEvaluation(compatibleFiles, ignoredCount);
	}

	/**
	 * Displays warnings based on filtering evaluation.
	 *
	 * @param evaluation
	 *            file evaluation result
	 * @param expectedExtensionsHint
	 *            expected extensions message
	 * @param origin
	 *            whether files were dropped or selected
	 */
	public static void notifyWarnings(FileSelectionEvaluation evaluation, String expectedExtensionsHint,
			InputOrigin origin) {
		if (evaluation == null || origin == null) {
			return;
		}
		// No file provided (e.g., file chooser cancelled): do not warn.
		if (!evaluation.hasAcceptedFiles() && evaluation.ignoredFiles() == 0) {
			return;
		}
		if (!evaluation.hasAcceptedFiles()) {
			NotificationWidget.getInstance().showWarning(
					String.format(WARNING_NONE_ACCEPTED_TEMPLATE, origin.noneAcceptedVerb(), expectedExtensionsHint));
			return;
		}
		if (evaluation.ignoredFiles() > 0) {
			NotificationWidget.getInstance()
					.showWarning(String.format(WARNING_IGNORED_TEMPLATE,
							DataUiMessageUtils.countLabel(evaluation.ignoredFiles(), origin.ignoredNoun()),
							expectedExtensionsHint));
		}
	}
}
