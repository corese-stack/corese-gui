package fr.inria.corese.gui.core;

import fr.inria.corese.gui.component.modal.GenericDialog;
import fr.inria.corese.gui.component.modal.ModalManager;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Utility class for creating dialog UI components using the global ModalManager.
 *
 * <p>This class provides reusable methods to create consistent dialog UI across the application.
 */
public class DialogHelper {

  // ===============================================================================
  // Enums
  // ===============================================================================

  /** Result of the unsaved changes dialog. */
  public enum UnsavedChangesResult {
    SAVE,
    DONT_SAVE,
    CANCEL
  }

  // ===============================================================================
  // Public Methods - Error Dialogs
  // ===============================================================================

  /**
   * Shows an error dialog.
   *
   * @param title The dialog title
   * @param message The error message to display
   */
  public static void showError(String title, String message) {
    showError(title, message, null);
  }

  /**
   * Shows an error dialog with detailed information.
   *
   * @param title The dialog title
   * @param message The error message to display
   * @param details The detailed error message (e.g., stack trace)
   */
  public static void showError(String title, String message, String details) {
    Platform.runLater(() -> {
      String fullMessage = message;
      if (details != null && !details.isBlank()) {
        fullMessage += "\n\nDetails:\n" + details;
      }
      ModalManager.getInstance().show(GenericDialog.createError(title, fullMessage));
    });
  }

  // ===============================================================================
  // Public Methods - Unsaved Changes Dialog
  // ===============================================================================

  /**
   * Shows a confirmation dialog for unsaved changes.
   *
   * @param fileName The name of the file with unsaved changes
   * @param callback The callback to receive the user's choice
   */
  public static void showUnsavedChangesDialog(
      String fileName, Consumer<UnsavedChangesResult> callback) {
    
    String message = fileName != null && !fileName.isBlank()
        ? "Do you want to save the changes you made to \"" + fileName + "\"?"
        : "Do you want to save the changes you made?";

    Platform.runLater(() -> {
        // We use a custom GenericDialog construction or a specialized method if needed.
        // For now, let's map it to a simple confirm/cancel or a custom layout if needed.
        // Since GenericDialog.createConfirm supports only OK/Cancel, we might need a custom one for Save/Don't Save/Cancel.
        // Or we assume "Confirm" = Save, "Cancel" = Cancel... but "Don't Save" is missing.
        
        // Let's create a custom dialog here since GenericDialog is basic.
        // Actually, we can extend GenericDialog or just build it here since it's a specific use case.
        
        // For simplicity and reusing the "Clean" directive, I'll use GenericDialog with custom buttons. 
        
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("Save");
        saveBtn.getStyleClass().add(atlantafx.base.theme.Styles.ACCENT);
        saveBtn.setOnAction(e -> {
            ModalManager.getInstance().hide();
            callback.accept(UnsavedChangesResult.SAVE);
        });

        javafx.scene.control.Button dontSaveBtn = new javafx.scene.control.Button("Don't Save");
        dontSaveBtn.setOnAction(e -> {
            ModalManager.getInstance().hide();
            callback.accept(UnsavedChangesResult.DONT_SAVE);
        });

        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Cancel");
        cancelBtn.setOnAction(e -> {
            ModalManager.getInstance().hide();
            callback.accept(UnsavedChangesResult.CANCEL);
        });

        javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(message);
        msgLabel.setWrapText(true);

        GenericDialog dialog = new GenericDialog("Unsaved Changes", msgLabel, cancelBtn, dontSaveBtn, saveBtn);
        ModalManager.getInstance().show(dialog);
    });
  }

  // ===============================================================================
  // Private Constructor
  // ===============================================================================

  private DialogHelper() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }
}
