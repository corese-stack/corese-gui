package fr.inria.corese.gui.feature.data.dialog;

import fr.inria.corese.gui.core.dialog.DialogLayout.ImpactTone;
import java.util.List;

/**
 * Modal dialog for confirming graph clearing with a standardized danger impact
 * block.
 */
public final class DataClearGraphDialog {

	private static final String DIALOG_TITLE = "Clear Graph";
	private static final String DANGER_TITLE = "Danger";
	private static final List<String> DANGER_ITEMS = List.of("All graph data and tracked sources will be removed.",
			"Reasoning profiles and native entailment modes will be reset to OFF.", "This action cannot be undone.");

	private DataClearGraphDialog() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Shows the clear-graph confirmation modal.
	 *
	 * @param onConfirmClear
	 *            action to execute when user confirms clear operation
	 */
	public static void show(Runnable onConfirmClear) {
		DataImpactConfirmDialog.show(DIALOG_TITLE, DANGER_TITLE, DANGER_ITEMS, ImpactTone.DANGER, "Clear Graph", true,
				onConfirmClear);
	}
}
