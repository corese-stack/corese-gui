package fr.inria.corese.gui.core.dialog;

import atlantafx.base.controls.ModalPane;
import javafx.application.Platform;
import javafx.scene.Node;
import java.util.function.Consumer;

/**
 * Central service for managing and displaying dialogs in the application.
 *
 * <p>This service replaces the old ModalManager and DialogHelper. It manages the
 * global {@link ModalPane} and provides high-level methods to show standard dialogs
 * (Error, Information, Confirmation) as well as custom content.
 */
public class DialogService {

    private static final DialogService INSTANCE = new DialogService();
    private ModalPane modalPane;

    private DialogService() {}

    /**
     * @return The singleton instance of the DialogService.
     */
    public static DialogService getInstance() {
        return INSTANCE;
    }

    // ============================================================================================
    // Configuration
    // ============================================================================================

    /**
     * Registers the root ModalPane of the application.
     * This must be called once during application initialization.
     *
     * @param modalPane The AtlantaFX ModalPane instance.
     */
    public void setModalPane(ModalPane modalPane) {
        this.modalPane = modalPane;
    }

    public ModalPane getModalPane() {
        return modalPane;
    }

    // ============================================================================================
    // Core Actions
    // ============================================================================================

    /**
     * Displays a node as a modal dialog.
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
     * Hides the currently active modal.
     */
    public void hide() {
        if (modalPane != null) {
            modalPane.hide();
        }
    }

    // ============================================================================================
    // High-Level Dialogs
    // ============================================================================================

    /** Result of the unsaved changes dialog. */
    public enum UnsavedChangesResult {
        SAVE,
        DONT_SAVE,
        CANCEL
    }

    /**
     * Shows an error dialog.
     *
     * @param title   The dialog title
     * @param message The error message to display
     */
    public void showError(String title, String message) {
        showError(title, message, null);
    }

    /**
     * Shows an error dialog with detailed information.
     *
     * @param title   The dialog title
     * @param message The error message to display
     * @param details The detailed error message (e.g., stack trace)
     */
    public void showError(String title, String message, String details) {
        Platform.runLater(() -> {
            String fullMessage = message;
            if (details != null && !details.isBlank()) {
                fullMessage += "\n\nDetails:\n" + details;
            }
            show(AppDialog.createError(title, fullMessage));
        });
    }

    /**
     * Shows an information dialog.
     *
     * @param title   The dialog title
     * @param message The message to display
     */
    public void showInformation(String title, String message) {
        Platform.runLater(() -> {
            show(AppDialog.createInfo(title, message));
        });
    }

    /**
     * Shows a confirmation dialog for unsaved changes.
     *
     * @param fileName The name of the file with unsaved changes
     * @param callback The callback to receive the user's choice
     */
    public void showUnsavedChangesDialog(String fileName, Consumer<UnsavedChangesResult> callback) {
        Platform.runLater(() -> {
            String message = fileName != null && !fileName.isBlank()
                    ? "Do you want to save the changes you made to \"" + fileName + "\"?"
                    : "Do you want to save the changes you made?";

            show(AppDialog.createUnsavedChanges(message, callback));
        });
    }
}
