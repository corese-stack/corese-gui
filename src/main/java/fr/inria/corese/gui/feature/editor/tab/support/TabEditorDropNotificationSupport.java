package fr.inria.corese.gui.feature.editor.tab.support;

import fr.inria.corese.gui.component.notification.NotificationWidget;

/**
 * User-facing feedback helpers for drag-and-drop in tab editors.
 */
public final class TabEditorDropNotificationSupport {

	private static final String DROP_WARNING_NONE_ACCEPTED_TEMPLATE = "No compatible files were dropped. %s";
	private static final String DROP_WARNING_IGNORED_TEMPLATE = "Ignored %s. %s";

	private TabEditorDropNotificationSupport() {
		throw new AssertionError("Utility class");
	}

	public static void notifyWarnings(TabEditorDropFilePolicy policy,
			TabEditorDropFilePolicy.DropEvaluation evaluation) {
		if (policy == null || evaluation == null) {
			return;
		}
		if (evaluation.unsupportedFiles() <= 0 || !policy.hasRestrictions()) {
			return;
		}

		String hint = policy.describeAllowedExtensions();
		if (evaluation.acceptedFiles().isEmpty()) {
			NotificationWidget.getInstance().showWarning(String.format(DROP_WARNING_NONE_ACCEPTED_TEMPLATE, hint));
			return;
		}

		NotificationWidget.getInstance().showWarning(String.format(DROP_WARNING_IGNORED_TEMPLATE,
				countLabel(evaluation.unsupportedFiles(), "dropped file"), hint));
	}

	private static String countLabel(int count, String noun) {
		if (count == 1) {
			return "1 " + noun;
		}
		return count + " " + noun + "s";
	}
}
