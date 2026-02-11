package fr.inria.corese.gui.feature.data.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.dialog.DialogLayout.ImpactTone;
import fr.inria.corese.gui.core.service.ModalService;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * Modal dialog for confirming graph clearing with a standardized danger impact
 * block.
 */
public final class DataClearGraphDialog {

	private static final String DIALOG_TITLE = "Clear Graph";
	private static final String DANGER_TITLE = "Danger";
	private static final List<String> DANGER_ITEMS = List.of("All graph data and tracked sources will be removed.",
			"Reasoning profiles will be reset to OFF.", "This action cannot be undone.");

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
		Runnable safeOnConfirm = onConfirmClear == null ? () -> {
		} : onConfirmClear;

		VBox impactBox = DialogLayout.createImpactBlock(DANGER_TITLE, DANGER_ITEMS, ImpactTone.DANGER);
		VBox content = new VBox(impactBox);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button clearButton = new Button("Clear Graph");
		clearButton.getStyleClass().add(Styles.DANGER);
		clearButton.setOnAction(event -> {
			ModalService.getInstance().hide();
			safeOnConfirm.run();
		});

		ModalService.getInstance().show(new DialogLayout(DIALOG_TITLE, content, cancelButton, clearButton));
	}
}
