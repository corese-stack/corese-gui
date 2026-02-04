package fr.inria.corese.gui.core.dialog;

import atlantafx.base.controls.ModalPane;
import javafx.application.Platform;
import javafx.scene.Node;
import java.util.function.Consumer;

/**
 * Centralized service for application dialogs.
 *
 * <p>
 * Manages the global {@link ModalPane} and provides methods to display standard dialogs
 * (error, information, confirmation) as well as custom content.
 *
 * <p>
 * This singleton service must be initialized with {@link #setModalPane(ModalPane)} during
 * application startup before any dialogs can be shown.
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * // Show error dialog
 * DialogService.getInstance().showError("Error", "Something went wrong");
 * 
 * // Show confirmation dialog
 * DialogService.getInstance().showUnsavedChangesDialog("file.txt", result -> {
 *     if (result == UnsavedChangesResult.SAVE) {
 *         // Save file
 *     }
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
        SAVE,
        DONT_SAVE,
        CANCEL
    }

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final ModalService INSTANCE = new ModalService();
    private ModalPane modalPane;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    private ModalService() {}

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
     * Must be called once during application initialization before showing any dialogs.
     *
     * @param modalPane The AtlantaFX ModalPane instance.
     */
    public void setModalPane(ModalPane modalPane) {
        this.modalPane = modalPane;
    }

    /**
     * Displays a custom node as a modal dialog.
     *
     * @param content The content to display.
     */
    public void show(Node content) {
        if (modalPane != null) {
            modalPane.show(content);
            content.requestFocus();
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
     * @param title   The dialog title.
     * @param message The error message.
     */
    public void showError(String title, String message) {
        showError(title, message, null);
    }

    /**
     * Shows an error dialog with detailed information.
     *
     * @param title   The dialog title.
     * @param message The error message.
     * @param details Additional error details (e.g., stack trace).
     */
    public void showError(String title, String message, String details) {
        Platform.runLater(() -> {
            String fullMessage = message;
            if (details != null && !details.isBlank()) {
                fullMessage += "\n\nDetails:\n" + details;
            }
            show(DialogLayout.createError(title, fullMessage));
        });
    }

    /**
     * Shows an information dialog.
     *
     * @param title   The dialog title.
     * @param message The information message.
     */
    public void showInformation(String title, String message) {
        Platform.runLater(() -> show(DialogLayout.createInfo(title, message)));
    }

    /**
     * Shows a confirmation dialog for unsaved changes.
     *
     * @param fileName The name of the file with unsaved changes (optional).
     * @param callback The callback to receive the user's choice.
     */
    public void showUnsavedChangesDialog(String fileName, Consumer<UnsavedChangesResult> callback) {
        Platform.runLater(() -> {
            String message = fileName != null && !fileName.isBlank()
                    ? "Do you want to save the changes you made to \"" + fileName + "\"?"
                    : "Do you want to save the changes you made?";

            show(DialogLayout.createUnsavedChanges(message, callback));
        });
    }
}
