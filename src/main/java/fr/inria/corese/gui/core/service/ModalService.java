package fr.inria.corese.gui.core.service;

import atlantafx.base.controls.ModalPane;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.function.Consumer;

/**
 * Centralized service for application dialogs.
 *
 * <p>
 * Manages the global {@link ModalPane} and provides methods to display standard
 * dialogs (error, information, confirmation) as well as custom content.
 *
 * <p>
 * This singleton service must be initialized with
 * {@link #setModalPane(ModalPane)} during application startup before any
 * dialogs can be shown.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * // Show error dialog
 * DialogService.getInstance().showError("Error", "Something went wrong");
 *
 * // Show confirmation dialog
 * DialogService.getInstance().showUnsavedChangesDialog("file.txt", result -> {
 * 	if (result == UnsavedChangesResult.SAVE) {
 * 		// Save file
 * 	}
 * });
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton is intentional for global dialog management
public class ModalService {

	// ==============================================================================================
	// Inner Types
	// ==============================================================================================

	/** Result of the unsaved changes dialog. */
	public enum UnsavedChangesResult {
		SAVE, DONT_SAVE, CANCEL
	}

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private static final ModalService INSTANCE = new ModalService();
	private static final String MODAL_CTRL_ENTER_FILTER_KEY = "corese.modal.ctrlEnterFilter";
	private static final String MODAL_KEYBOARD_NAV_FILTER_KEY = "corese.modal.keyboardNavFilter";
	private ModalPane modalPane;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	private ModalService() {
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Returns the singleton instance.
	 *
	 * @return The DialogService instance.
	 */
	public static ModalService getInstance() {
		return INSTANCE;
	}

	/**
	 * Registers the root ModalPane.
	 *
	 * <p>
	 * Must be called once during application initialization before showing any
	 * dialogs.
	 *
	 * @param modalPane
	 *            The AtlantaFX ModalPane instance.
	 */
	public void setModalPane(ModalPane modalPane) {
		this.modalPane = modalPane;
	}

	/**
	 * Displays a custom node as a modal dialog.
	 *
	 * @param content
	 *            The content to display.
	 */
	public void show(Node content) {
		if (modalPane != null) {
			modalPane.show(content);
			installCtrlEnterSubmitShortcut(content);
			installKeyboardNavigationShortcut(content);
			Platform.runLater(() -> {
				if (!focusFirstFocusableNode(content)) {
					content.requestFocus();
				}
			});
		}
	}

	/**
	 * Hides the currently active dialog.
	 */
	public void hide() {
		if (modalPane != null) {
			modalPane.hide();
		}
	}

	/**
	 * Shows an error dialog.
	 *
	 * @param title
	 *            The dialog title.
	 * @param message
	 *            The error message.
	 */
	public void showError(String title, String message) {
		showError(title, message, null);
	}

	/**
	 * Shows an error dialog with detailed information.
	 *
	 * @param title
	 *            The dialog title.
	 * @param message
	 *            The error message.
	 * @param details
	 *            Additional error details (e.g., stack trace).
	 */
	public void showError(String title, String message, String details) {
		Platform.runLater(() -> {
			show(DialogLayout.createError(title, message, details));
		});
	}

	/**
	 * Shows an error dialog with details generated from an exception chain.
	 *
	 * @param title
	 *            dialog title
	 * @param message
	 *            short user-facing message
	 * @param throwable
	 *            source exception (optional)
	 */
	public void showException(String title, String message, Throwable throwable) {
		showError(title, message, formatThrowableDetails(throwable));
	}

	/**
	 * Formats a throwable with full stack trace and causes for detailed
	 * diagnostics.
	 *
	 * @param throwable
	 *            source throwable (optional)
	 * @return formatted details string, or empty string when throwable is null
	 */
	public static String formatThrowableDetails(Throwable throwable) {
		if (throwable == null) {
			return "";
		}
		StringWriter writer = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(writer)) {
			throwable.printStackTrace(printWriter);
		}
		return writer.toString().trim();
	}

	/**
	 * Shows an information dialog.
	 *
	 * @param title
	 *            The dialog title.
	 * @param message
	 *            The information message.
	 */
	public void showInformation(String title, String message) {
		Platform.runLater(() -> show(DialogLayout.createInfo(title, message)));
	}

	/**
	 * Shows a generic confirmation dialog.
	 *
	 * @param title
	 *            The dialog title.
	 * @param message
	 *            The confirmation message.
	 * @param confirmLabel
	 *            Label for the confirmation button.
	 * @param dangerous
	 *            Whether the confirm action is destructive.
	 * @param onConfirm
	 *            Callback executed when the user confirms.
	 */
	public void showConfirmation(String title, String message, String confirmLabel, boolean dangerous,
			Runnable onConfirm) {
		Platform.runLater(
				() -> show(DialogLayout.createConfirmation(title, message, confirmLabel, dangerous, onConfirm)));
	}

	/**
	 * Shows a confirmation dialog for unsaved changes.
	 *
	 * @param fileName
	 *            The name of the file with unsaved changes (optional).
	 * @param callback
	 *            The callback to receive the user's choice.
	 */
	public void showUnsavedChangesDialog(String fileName, Consumer<UnsavedChangesResult> callback) {
		Platform.runLater(() -> {
			String message = fileName != null && !fileName.isBlank()
					? "Do you want to save the changes you made to \"" + fileName + "\"?"
					: "Do you want to save the changes you made?";

			show(DialogLayout.createUnsavedChanges(message, callback));
		});
	}

	@SuppressWarnings("unchecked")
	private void installCtrlEnterSubmitShortcut(Node content) {
		if (content == null) {
			return;
		}
		Object existingHandler = content.getProperties().get(MODAL_CTRL_ENTER_FILTER_KEY);
		if (existingHandler instanceof EventHandler<?> handler) {
			content.removeEventFilter(KeyEvent.KEY_PRESSED, (EventHandler<KeyEvent>) handler);
		}

		EventHandler<KeyEvent> handler = event -> {
			if (event == null || event.isConsumed()) {
				return;
			}
			if (!event.isShortcutDown() || event.getCode() != KeyCode.ENTER) {
				return;
			}
			Button submitButton = resolveSubmitButton(content);
			if (submitButton == null || submitButton.isDisabled() || !submitButton.isVisible()) {
				return;
			}
			submitButton.fire();
			event.consume();
		};

		content.addEventFilter(KeyEvent.KEY_PRESSED, handler);
		content.getProperties().put(MODAL_CTRL_ENTER_FILTER_KEY, handler);
	}

	@SuppressWarnings("unchecked")
	private void installKeyboardNavigationShortcut(Node content) {
		if (content == null) {
			return;
		}
		Object existingHandler = content.getProperties().get(MODAL_KEYBOARD_NAV_FILTER_KEY);
		if (existingHandler instanceof EventHandler<?> handler) {
			content.removeEventFilter(KeyEvent.KEY_PRESSED, (EventHandler<KeyEvent>) handler);
		}

		EventHandler<KeyEvent> handler = event -> {
			if (event == null || event.isConsumed()) {
				return;
			}

			KeyCode code = event.getCode();
			if (code == null) {
				return;
			}

			if (code == KeyCode.LEFT || code == KeyCode.UP || code == KeyCode.RIGHT || code == KeyCode.DOWN) {
				if (isArrowNavigationHandledByControl(event.getTarget())) {
					return;
				}
				int direction = (code == KeyCode.RIGHT || code == KeyCode.DOWN) ? 1 : -1;
				if (moveFocus(content, direction)) {
					event.consume();
				}
				return;
			}

			if (code != KeyCode.ENTER || event.isShortcutDown()) {
				return;
			}
			if (isInsideTextArea(event.getTarget())) {
				return;
			}
			Node focusOwner = resolveFocusOwner(content);
			if (fireFocusableAction(focusOwner) || fireSubmitButton(content)) {
				event.consume();
			}
		};

		content.addEventFilter(KeyEvent.KEY_PRESSED, handler);
		content.getProperties().put(MODAL_KEYBOARD_NAV_FILTER_KEY, handler);
	}

	private boolean fireSubmitButton(Node root) {
		Button submitButton = resolveSubmitButton(root);
		if (submitButton == null || submitButton.isDisabled() || !submitButton.isVisible()) {
			return false;
		}
		submitButton.fire();
		return true;
	}

	private boolean fireFocusableAction(Node focusOwner) {
		if (!(focusOwner instanceof ButtonBase buttonBase)) {
			return false;
		}
		if (buttonBase.isDisabled() || !buttonBase.isVisible()) {
			return false;
		}
		buttonBase.fire();
		return true;
	}

	private boolean moveFocus(Node root, int direction) {
		List<Node> focusableNodes = collectFocusableNodes(root);
		if (focusableNodes.isEmpty()) {
			return false;
		}

		Node currentFocusOwner = resolveFocusOwner(root);
		int currentIndex = focusableNodes.indexOf(currentFocusOwner);
		if (currentIndex < 0) {
			Node firstTarget = direction >= 0 ? focusableNodes.get(0) : focusableNodes.get(focusableNodes.size() - 1);
			firstTarget.requestFocus();
			return true;
		}

		int safeDirection = direction >= 0 ? 1 : -1;
		int nextIndex = Math.floorMod(currentIndex + safeDirection, focusableNodes.size());
		focusableNodes.get(nextIndex).requestFocus();
		return true;
	}

	private boolean focusFirstFocusableNode(Node root) {
		List<Node> focusableNodes = collectFocusableNodes(root);
		if (focusableNodes.isEmpty()) {
			return false;
		}
		focusableNodes.get(0).requestFocus();
		return true;
	}

	private List<Node> collectFocusableNodes(Node root) {
		List<Node> focusableNodes = new ArrayList<>();
		collectFocusableNodesDepthFirst(root, focusableNodes);
		return focusableNodes;
	}

	private void collectFocusableNodesDepthFirst(Node node, List<Node> output) {
		if (node == null || output == null) {
			return;
		}
		if (isFocusableNode(node)) {
			output.add(node);
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				collectFocusableNodesDepthFirst(child, output);
			}
		}
	}

	private boolean isFocusableNode(Node node) {
		return node.isVisible() && node.isManaged() && !node.isDisabled() && node.isFocusTraversable();
	}

	private Node resolveFocusOwner(Node root) {
		if (root == null || root.getScene() == null) {
			return null;
		}
		Node focusOwner = root.getScene().getFocusOwner();
		if (focusOwner == null) {
			return null;
		}
		return isDescendantOf(root, focusOwner) ? focusOwner : null;
	}

	private boolean isDescendantOf(Node ancestor, Node candidate) {
		if (ancestor == null || candidate == null) {
			return false;
		}
		for (Node current = candidate; current != null; current = current.getParent()) {
			if (current == ancestor) {
				return true;
			}
		}
		return false;
	}

	private boolean isInsideTextArea(Object target) {
		return hasControlAncestor(target, TextArea.class);
	}

	private boolean isArrowNavigationHandledByControl(Object target) {
		return hasControlAncestor(target, TextInputControl.class) || hasControlAncestor(target, ComboBoxBase.class)
				|| hasControlAncestor(target, ChoiceBox.class) || hasControlAncestor(target, Spinner.class)
				|| hasControlAncestor(target, ListView.class) || hasControlAncestor(target, TableView.class)
				|| hasControlAncestor(target, TreeView.class) || hasControlAncestor(target, TreeTableView.class)
				|| hasControlAncestor(target, Slider.class);
	}

	private boolean hasControlAncestor(Object target, Class<?> controlType) {
		if (controlType == null || !(target instanceof Node node)) {
			return false;
		}
		for (Node current = node; current != null; current = current.getParent()) {
			if (controlType.isInstance(current)) {
				return true;
			}
		}
		return false;
	}

	private Button resolveSubmitButton(Node root) {
		List<Button> buttons = new ArrayList<>();
		collectButtonsDepthFirst(root, buttons);
		if (buttons.isEmpty()) {
			return null;
		}
		for (Button button : buttons) {
			if (button.isDefaultButton()) {
				return button;
			}
		}
		return buttons.get(buttons.size() - 1);
	}

	private void collectButtonsDepthFirst(Node node, List<Button> output) {
		if (node == null || output == null) {
			return;
		}
		if (node instanceof Button button) {
			output.add(button);
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				collectButtonsDepthFirst(child, output);
			}
		}
	}
}
