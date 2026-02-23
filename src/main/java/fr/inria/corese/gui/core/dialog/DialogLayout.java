package fr.inria.corese.gui.core.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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

	private static final String STYLE_CLASS_COMPACT = "compact";
	private static final double DIALOG_RADIUS = 10;

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
		this(title, null, content, actions);
	}

	/**
	 * Creates a new dialog with title, optional subtitle, content, and actions.
	 *
	 * @param title
	 *            dialog title
	 * @param subtitle
	 *            optional subtitle shown under the title
	 * @param content
	 *            main content node
	 * @param actions
	 *            optional action buttons
	 */
	public DialogLayout(String title, String subtitle, Node content, Node... actions) {
		getStyleClass().addAll(Styles.ELEVATED_1, "app-dialog");
		if (STYLESHEET != null) {
			getStylesheets().add(STYLESHEET);
		}
		setMaxHeight(Region.USE_PREF_SIZE);
		RoundedClipSupport.applyRoundedClip(this, DIALOG_RADIUS);

		String safeTitle = (title == null || title.isBlank()) ? "Dialog" : title.trim();
		String safeSubtitle = subtitle == null ? "" : subtitle.trim();

		// Header with title, optional subtitle, and close button
		Label titleLabel = new Label(safeTitle);
		titleLabel.getStyleClass().addAll(Styles.TITLE_4, "dialog-title");

		VBox titleBox = new VBox(4);
		titleBox.getStyleClass().add("dialog-title-box");
		titleBox.getChildren().add(titleLabel);
		if (!safeSubtitle.isBlank()) {
			Label subtitleLabel = new Label(safeSubtitle);
			subtitleLabel.getStyleClass().add("dialog-subtitle");
			subtitleLabel.setWrapText(true);
			titleBox.getChildren().add(subtitleLabel);
		}

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		IconButtonWidget closeBtn = new IconButtonWidget(
				ButtonFactory.custom(ButtonIcon.CLOSE_WINDOW, "Close", ModalService.getInstance()::hide));

		HBox header = new HBox(titleBox, spacer, closeBtn);
		header.getStyleClass().add("dialog-header");

		// Content area
		if (content instanceof Region region) {
			region.setMaxWidth(Double.MAX_VALUE);
			VBox.setVgrow(region, Priority.ALWAYS);
		}
		if (content instanceof Label label) {
			label.getStyleClass().add("dialog-message");
			label.setWrapText(true);
		}
		VBox body = new VBox(content);
		body.getStyleClass().add("dialog-body");
		body.setFillWidth(true);
		VBox.setVgrow(body, Priority.ALWAYS);
		if (actions == null || actions.length == 0) {
			body.getStyleClass().add("no-actions");
		}

		getChildren().addAll(header, body);

		// Action buttons
		if (actions != null && actions.length > 0) {
			HBox actionBox = new HBox();
			actionBox.getStyleClass().add("dialog-actions");
			actionBox.getChildren().addAll(actions);
			markDefaultActionButton(actionBox);
			getChildren().add(actionBox);
		}
	}

	private static void markDefaultActionButton(HBox actionBox) {
		if (actionBox == null || actionBox.getChildren().isEmpty()) {
			return;
		}
		List<Node> children = actionBox.getChildren();
		for (int i = children.size() - 1; i >= 0; i--) {
			Node child = children.get(i);
			if (child instanceof Button button) {
				button.setDefaultButton(true);
				return;
			}
		}
	}

	/**
	 * Applies compact spacing intended for short message dialogs.
	 *
	 * @return current dialog instance
	 */
	public DialogLayout compact() {
		if (!getStyleClass().contains(STYLE_CLASS_COMPACT)) {
			getStyleClass().add(STYLE_CLASS_COMPACT);
		}
		return this;
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
		return createError(title, message, null);
	}

	/**
	 * Creates an error dialog with optional detailed technical information.
	 *
	 * @param title
	 *            dialog title
	 * @param message
	 *            short user-facing message
	 * @param details
	 *            optional technical details (stack trace, diagnostics, etc.)
	 * @return a new error dialog instance
	 */
	public static DialogLayout createError(String title, String message, String details) {
		String safeTitle = (title == null || title.isBlank()) ? "Error" : title.trim();
		String safeMessage = (message == null || message.isBlank()) ? "An unexpected error occurred." : message.trim();
		String safeDetails = details == null ? "" : details.trim();

		Label messageLabel = new Label(safeMessage);
		messageLabel.getStyleClass().add("dialog-message");
		messageLabel.setWrapText(true);

		VBox content = new VBox(8, messageLabel);
		content.setFillWidth(true);

		Button closeButton = new Button("Close");
		closeButton.getStyleClass().add(Styles.DANGER);
		closeButton.setOnAction(e -> ModalService.getInstance().hide());

		if (safeDetails.isBlank()) {
			return new DialogLayout(safeTitle, content, closeButton).compact();
		}

		Label detailsTitle = new Label("Details");
		detailsTitle.getStyleClass().addAll("dialog-error-details-title", Styles.TEXT_BOLD);

		TextArea detailsArea = new TextArea(safeDetails);
		detailsArea.getStyleClass().add("dialog-error-details");
		detailsArea.setEditable(false);
		detailsArea.setWrapText(false);
		detailsArea.setPrefRowCount(12);
		VBox.setVgrow(detailsArea, Priority.ALWAYS);

		content.getChildren().addAll(detailsTitle, detailsArea);

		Button copyDetailsButton = new Button("Copy Details");
		copyDetailsButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
		copyDetailsButton.setOnAction(e -> {
			ClipboardContent clipboardContent = new ClipboardContent();
			clipboardContent.putString(safeDetails);
			Clipboard.getSystemClipboard().setContent(clipboardContent);
		});

		return new DialogLayout(safeTitle, content, copyDetailsButton, closeButton);
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
	public static DialogLayout createUnsavedChanges(String message,
			Consumer<ModalService.UnsavedChangesResult> callback) {
		Label msgLabel = new Label(message);

		Button saveBtn = new Button("Save");
		saveBtn.getStyleClass().add(Styles.ACCENT);
		saveBtn.setOnAction(e -> {
			ModalService.getInstance().hide();
			callback.accept(ModalService.UnsavedChangesResult.SAVE);
		});

		Button dontSaveBtn = new Button("Don't Save");
		dontSaveBtn.setOnAction(e -> {
			ModalService.getInstance().hide();
			callback.accept(ModalService.UnsavedChangesResult.DONT_SAVE);
		});

		Button cancelBtn = new Button("Cancel");
		cancelBtn.setOnAction(e -> {
			ModalService.getInstance().hide();
			callback.accept(ModalService.UnsavedChangesResult.CANCEL);
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

		return new DialogLayout(safeTitle, msgLabel, cancelBtn, confirmBtn).compact();
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

		return new DialogLayout(title, msgLabel, okBtn).compact();
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
