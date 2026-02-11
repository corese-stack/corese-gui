package fr.inria.corese.gui.core.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ModalService.UnsavedChangesResult;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Standard dialog component for the application.
 *
 * <p>
 * Provides a consistent visual style for all dialogs with title, content area,
 * and action buttons. Integrates with AtlantaFX theme system for automatic
 * styling.
 *
 * <p>
 * Styling is defined in {@code app-dialog.css} for maintainability and
 * consistency.
 *
 * <p>
 * Factory methods are provided for common dialog types:
 * <ul>
 * <li>{@link #createError(String, String)} - Error notification dialogs</li>
 * <li>{@link #createInfo(String, String)} - Information dialogs</li>
 * <li>{@link #createUnsavedChanges(String, Consumer)} - Unsaved changes
 * confirmation</li>
 * <li>{@link #createConfirmation(String, String, String, boolean, Runnable)} -
 * generic confirmation dialog</li>
 * </ul>
 */
public class DialogLayout extends VBox {

	/**
	 * Visual tone used by reusable dialog impact blocks.
	 */
	public enum ImpactTone {
		WARNING("warning"), DANGER("danger");

		private final String styleClass;

		ImpactTone(String styleClass) {
			this.styleClass = styleClass;
		}

		String styleClass() {
			return styleClass;
		}
	}

	private static final String STYLESHEET;

	static {
		java.net.URL resource = DialogLayout.class.getResource("/css/components/app-dialog.css");
		STYLESHEET = (resource != null) ? resource.toExternalForm() : null;
	}

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	/**
	 * Creates a new dialog with the specified components.
	 *
	 * @param title
	 *            The dialog title.
	 * @param content
	 *            The main content area.
	 * @param actions
	 *            Optional action buttons to display at the bottom.
	 */
	public DialogLayout(String title, Node content, Node... actions) {
		getStyleClass().addAll(Styles.ELEVATED_1, "app-dialog");
		if (STYLESHEET != null) {
			getStylesheets().add(STYLESHEET);
		}
		setMaxHeight(Region.USE_PREF_SIZE);

		// Header with title and close button
		Label titleLabel = new Label(title);
		titleLabel.getStyleClass().add(Styles.TITLE_4);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		IconButtonWidget closeBtn = new IconButtonWidget(
				ButtonFactory.custom(ButtonIcon.CLOSE_WINDOW, "Close", ModalService.getInstance()::hide));

		HBox header = new HBox(titleLabel, spacer, closeBtn);
		header.getStyleClass().add("dialog-header");

		// Content area
		if (content instanceof Region region) {
			region.setMaxWidth(Double.MAX_VALUE);
		}
		if (content instanceof Label label) {
			label.getStyleClass().add("dialog-message");
		}

		getChildren().addAll(header, content);

		// Action buttons
		if (actions != null && actions.length > 0) {
			HBox actionBox = new HBox();
			actionBox.getStyleClass().add("dialog-actions");
			actionBox.getChildren().addAll(actions);
			getChildren().add(actionBox);
		}
	}

	// ==============================================================================================
	// Factory Methods
	// ==============================================================================================

	/**
	 * Creates an error dialog.
	 *
	 * @param title
	 *            The dialog title.
	 * @param message
	 *            The error message to display.
	 * @return A new error dialog instance.
	 */
	public static DialogLayout createError(String title, String message) {
		return createSimpleDialog(title, message, Styles.DANGER);
	}

	/**
	 * Creates an information dialog.
	 *
	 * @param title
	 *            The dialog title.
	 * @param message
	 *            The information message to display.
	 * @return A new information dialog instance.
	 */
	public static DialogLayout createInfo(String title, String message) {
		return createSimpleDialog(title, message, Styles.ACCENT);
	}

	/**
	 * Creates an unsaved changes confirmation dialog.
	 *
	 * @param message
	 *            The confirmation message.
	 * @param callback
	 *            The callback to receive the user's choice.
	 * @return A new unsaved changes dialog instance.
	 */
	public static DialogLayout createUnsavedChanges(String message, Consumer<UnsavedChangesResult> callback) {
		Label msgLabel = new Label(message);

		Button saveBtn = new Button("Save");
		saveBtn.getStyleClass().add(Styles.ACCENT);
		saveBtn.setOnAction(e -> {
			ModalService.getInstance().hide();
			callback.accept(UnsavedChangesResult.SAVE);
		});

		Button dontSaveBtn = new Button("Don't Save");
		dontSaveBtn.setOnAction(e -> {
			ModalService.getInstance().hide();
			callback.accept(UnsavedChangesResult.DONT_SAVE);
		});

		Button cancelBtn = new Button("Cancel");
		cancelBtn.setOnAction(e -> {
			ModalService.getInstance().hide();
			callback.accept(UnsavedChangesResult.CANCEL);
		});

		return new DialogLayout("Unsaved Changes", msgLabel, cancelBtn, dontSaveBtn, saveBtn);
	}

	/**
	 * Creates a generic confirmation dialog.
	 *
	 * @param title
	 *            dialog title
	 * @param message
	 *            dialog message
	 * @param confirmLabel
	 *            label for confirmation button
	 * @param dangerous
	 *            whether confirmation is destructive
	 * @param onConfirm
	 *            callback executed on confirmation
	 * @return a new confirmation dialog instance
	 */
	public static DialogLayout createConfirmation(String title, String message, String confirmLabel, boolean dangerous,
			Runnable onConfirm) {
		String safeTitle = (title == null || title.isBlank()) ? "Confirmation" : title;
		String safeMessage = (message == null || message.isBlank()) ? "Are you sure?" : message;
		String safeConfirmLabel = (confirmLabel == null || confirmLabel.isBlank()) ? "Confirm" : confirmLabel;

		Label msgLabel = new Label(safeMessage);

		Button cancelBtn = new Button("Cancel");
		cancelBtn.setOnAction(e -> ModalService.getInstance().hide());

		Button confirmBtn = new Button(safeConfirmLabel);
		confirmBtn.getStyleClass().add(dangerous ? Styles.DANGER : Styles.ACCENT);
		confirmBtn.setOnAction(e -> {
			ModalService.getInstance().hide();
			if (onConfirm != null) {
				onConfirm.run();
			}
		});

		return new DialogLayout(safeTitle, msgLabel, cancelBtn, confirmBtn);
	}

	/**
	 * Creates a reusable impact block for warning/danger dialog content.
	 *
	 * @param title
	 *            block title
	 * @param items
	 *            bullet-pointed impact lines
	 * @param tone
	 *            visual severity tone
	 * @return a styled impact block node
	 */
	public static VBox createImpactBlock(String title, List<String> items, ImpactTone tone) {
		ImpactTone safeTone = tone == null ? ImpactTone.WARNING : tone;
		String safeTitle = (title == null || title.isBlank()) ? "Notice" : title.trim();
		List<String> safeItems = sanitizeImpactItems(items);

		Label titleLabel = new Label(safeTitle);
		titleLabel.getStyleClass().add("dialog-impact-title");
		titleLabel.setWrapText(true);

		VBox itemList = new VBox(3);
		itemList.getStyleClass().add("dialog-impact-list");
		for (String item : safeItems) {
			Label itemLabel = new Label("• " + item);
			itemLabel.getStyleClass().add("dialog-impact-item");
			itemLabel.setWrapText(true);
			itemList.getChildren().add(itemLabel);
		}

		VBox impactBox = new VBox(6, titleLabel, itemList);
		impactBox.getStyleClass().addAll("dialog-impact", safeTone.styleClass());
		return impactBox;
	}

	// ==============================================================================================
	// Private Helpers
	// ==============================================================================================

	/**
	 * Creates a simple dialog with a message and an OK button.
	 *
	 * @param title
	 *            The dialog title.
	 * @param message
	 *            The message to display.
	 * @param buttonStyle
	 *            The style to apply to the OK button.
	 * @return A new dialog instance.
	 */
	private static DialogLayout createSimpleDialog(String title, String message, String buttonStyle) {
		Label msgLabel = new Label(message);

		Button okBtn = new Button("OK");
		okBtn.getStyleClass().add(buttonStyle);
		okBtn.setOnAction(e -> ModalService.getInstance().hide());

		return new DialogLayout(title, msgLabel, okBtn);
	}

	private static List<String> sanitizeImpactItems(List<String> items) {
		if (items == null || items.isEmpty()) {
			return List.of("No additional details.");
		}
		List<String> safeItems = new ArrayList<>();
		for (String item : items) {
			if (item == null) {
				continue;
			}
			String normalized = item.trim();
			if (!normalized.isBlank()) {
				safeItems.add(normalized);
			}
		}
		return safeItems.isEmpty() ? List.of("No additional details.") : List.copyOf(safeItems);
	}
}
