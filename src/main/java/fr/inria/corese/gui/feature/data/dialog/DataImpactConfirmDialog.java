package fr.inria.corese.gui.feature.data.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.dialog.DialogLayout.ImpactTone;
import fr.inria.corese.gui.core.dialog.ModalService;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * Shared confirmation dialog helper using an impact block and two actions.
 */
final class DataImpactConfirmDialog {

	private DataImpactConfirmDialog() {
		throw new AssertionError("Utility class");
	}

	static void show(String dialogTitle, String impactTitle, List<String> impactItems, ImpactTone impactTone,
			String confirmLabel, boolean dangerConfirmButton, Runnable onConfirm) {
		Runnable safeOnConfirm = onConfirm == null ? () -> {
		} : onConfirm;
		List<String> safeImpactItems = impactItems == null ? List.of() : impactItems;
		ImpactTone safeImpactTone = impactTone == null ? ImpactTone.WARNING : impactTone;
		String safeDialogTitle = (dialogTitle == null || dialogTitle.isBlank()) ? "Confirm Action" : dialogTitle;
		String safeImpactTitle = (impactTitle == null || impactTitle.isBlank()) ? "Impact" : impactTitle;
		String safeConfirmLabel = (confirmLabel == null || confirmLabel.isBlank()) ? "Confirm" : confirmLabel;

		VBox impactBox = DialogLayout.createImpactBlock(safeImpactTitle, safeImpactItems, safeImpactTone);
		VBox content = new VBox(impactBox);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button confirmButton = new Button(safeConfirmLabel);
		if (dangerConfirmButton) {
			confirmButton.getStyleClass().add(Styles.DANGER);
		}
		confirmButton.setOnAction(event -> {
			ModalService.getInstance().hide();
			safeOnConfirm.run();
		});

		ModalService.getInstance().show(new DialogLayout(safeDialogTitle, content, cancelButton, confirmButton));
	}
}
