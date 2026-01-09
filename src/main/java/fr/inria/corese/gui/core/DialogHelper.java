package fr.inria.corese.gui.core;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.view.component.ErrorDialog;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Utility class for creating dialog UI components with AtlantaFX ModalPane.
 *
 * <p>This class provides reusable methods to create consistent dialog UI across the application.
 * Views should use these methods to display dialogs with their own ModalPane instances.
 *
 * <p><b>Usage (from a view class):</b>
 *
 * <pre>{@code
 * // Error dialog
 * DialogHelper.showError(modalPane, "File Error", "Could not read file: " + filePath);
 *
 * // Unsaved changes with callback
 * DialogHelper.showUnsavedChangesDialog(modalPane, "MyFile.txt", result -> {
 *     if (result == UnsavedChangesResult.SAVE) {
 *         saveFile();
 *     } else if (result == UnsavedChangesResult.DONT_SAVE) {
 *         closeFile();
 *     }
 * });
 * }</pre>
 */
public class DialogHelper {

  // ===============================================================================
  // Constants
  // ===============================================================================

  private static final String DIALOG_TITLE_UNSAVED_CHANGES = "Unsaved Changes";
  private static final String DIALOG_BUTTON_SAVE = "Save";
  private static final String DIALOG_BUTTON_DONT_SAVE = "Don't Save";
  private static final String DIALOG_BUTTON_CANCEL = "Cancel";

  // ===============================================================================
  // Enums
  // ===============================================================================

  /** Result of the unsaved changes dialog. */
  public enum UnsavedChangesResult {
    /** User chose to save the changes. */
    SAVE,
    /** User chose not to save the changes. */
    DONT_SAVE,
    /** User cancelled the operation. */
    CANCEL
  }

  // ===============================================================================
  // Public Methods - Error Dialogs
  // ===============================================================================

  /**
   * Shows an error dialog using AtlantaFX ModalPane.
   *
   * @param modalPane The ModalPane to display the error in
   * @param title The dialog title
   * @param message The error message to display
   */
  public static void showError(ModalPane modalPane, String title, String message) {
    showError(modalPane, title, message, null);
  }

  /**
   * Shows an error dialog with detailed information using AtlantaFX ModalPane.
   *
   * @param modalPane The ModalPane to display the error in
   * @param title The dialog title
   * @param message The error message to display
   * @param details The detailed error message (e.g., stack trace)
   */
  public static void showError(ModalPane modalPane, String title, String message, String details) {
    Platform.runLater(() -> {
      ErrorDialog errorDialog = new ErrorDialog(modalPane, title, message, details);
      modalPane.show(errorDialog.getRoot());
    });
  }

  // ===============================================================================
  // Public Methods - Unsaved Changes Dialog
  // ===============================================================================

  /**
   * Shows a confirmation dialog for unsaved changes using AtlantaFX ModalPane.
   *
   * <p>The result is provided via callback since ModalPane dialogs are non-blocking.
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * DialogHelper.showUnsavedChangesDialog(modalPane, "MyFile.txt", result -> {
   *     switch (result) {
   *         case SAVE:
   *             saveFile();
   *             closeTab();
   *             break;
   *         case DONT_SAVE:
   *             closeTab();
   *             break;
   *         case CANCEL:
   *             // Do nothing
   *             break;
   *     }
   * });
   * }</pre>
   *
   * @param modalPane The ModalPane to display the dialog in
   * @param fileName The name of the file with unsaved changes
   * @param callback The callback to receive the user's choice
   */
  public static void showUnsavedChangesDialog(
      ModalPane modalPane, String fileName, Consumer<UnsavedChangesResult> callback) {
    ModalBox modalBox = new ModalBox(modalPane);
    modalBox.setMaxSize(450, 180);

    VBox content = new VBox(15);
    content.getStyleClass().add("unsaved-changes-dialog");

    Label titleLabel = new Label(DIALOG_TITLE_UNSAVED_CHANGES);
    titleLabel.getStyleClass().add(Styles.TITLE_3);

    Label messageLabel = new Label("Save changes to " + fileName + "?");
    messageLabel.setWrapText(true);

    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);

    Button saveButton = new Button(DIALOG_BUTTON_SAVE);
    saveButton.getStyleClass().addAll(Styles.ACCENT);

    Button dontSaveButton = new Button(DIALOG_BUTTON_DONT_SAVE);

    Button cancelButton = new Button(DIALOG_BUTTON_CANCEL);
    cancelButton.getStyleClass().add(Styles.BUTTON_OUTLINED);

    buttonBox.getChildren().addAll(cancelButton, dontSaveButton, saveButton);

    content.getChildren().addAll(titleLabel, messageLabel, buttonBox);
    VBox.setVgrow(messageLabel, Priority.ALWAYS);

    modalBox.addContent(content);

    saveButton.setOnAction(
        e -> {
          modalPane.hide();
          callback.accept(UnsavedChangesResult.SAVE);
        });

    dontSaveButton.setOnAction(
        e -> {
          modalPane.hide();
          callback.accept(UnsavedChangesResult.DONT_SAVE);
        });

    cancelButton.setOnAction(
        e -> {
          modalPane.hide();
          callback.accept(UnsavedChangesResult.CANCEL);
        });

    modalPane.show(modalBox);
  }

  // ===============================================================================
  // Private Constructor
  // ===============================================================================

  /** Private constructor to prevent instantiation. */
  private DialogHelper() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }
}
