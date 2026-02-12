package fr.inria.corese.gui.feature.data.dialog;

import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.service.ModalService;

/**
 * Modal dialog for confirming rule-file removal operations.
 */
public final class DataClearRuleFilesDialog {

	private static final String DIALOG_TITLE = "Clear Rule Files";
	private static final String MESSAGE = "Clear all loaded rule files from this session?";
	private static final String CONFIRM_LABEL = "Clear Rule Files";

	private DataClearRuleFilesDialog() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Shows the clear-rule-files confirmation modal.
	 *
	 * @param onConfirmClear
	 *            action to execute when user confirms clear operation
	 */
	public static void show(Runnable onConfirmClear) {
		ModalService.getInstance()
				.show(DialogLayout.createConfirmation(DIALOG_TITLE, MESSAGE, CONFIRM_LABEL, false, onConfirmClear));
	}
}
