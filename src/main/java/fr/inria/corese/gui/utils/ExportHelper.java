package fr.inria.corese.gui.utils;

import fr.inria.corese.gui.enums.SerializationFormat;
import java.io.File;
import java.nio.file.Files;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Utility class to handle common file export operations.
 *
 * <p>Standardizes the behavior of "Export" features across the application:
 *
 * <ul>
 *   <li>Consistent FileChooser configuration
 *   <li>Automatic extension handling
 *   <li>Asynchronous writing to prevent UI freezing
 *   <li>Standardized success/error feedback
 * </ul>
 */
public final class ExportHelper {

  private ExportHelper() {
    // Utility class
  }

  /**
   * Prompts the user to save text content to a file with the specified format.
   *
   * @param window The parent window for the dialog
   * @param content The text content to save
   * @param format The target serialization format
   */
  public static void exportText(Window window, String content, SerializationFormat format) {
    // Treat null as empty content and allow exporting empty files (valid use case)
    String contentToExport = (content != null) ? content : "";

    SerializationFormat safeFormat = (format != null) ? format : SerializationFormat.TURTLE;

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Result As");

    String extension = safeFormat.getExtension();
    fileChooser.setInitialFileName("export" + extension);

    FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter(
            safeFormat.getLabel() + " file (*" + extension + ")", "*" + extension);

    fileChooser.getExtensionFilters().add(extFilter);
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

    File file = fileChooser.showSaveDialog(window);

    if (file != null) {
      // Enforce extension
      if (!file.getName().toLowerCase().endsWith(extension)) {
        file = new File(file.getAbsolutePath() + extension);
      }

      writeFileAsync(file, contentToExport);
    }
  }

  private static void writeFileAsync(File file, String content) {
    Task<Void> task =
        new Task<>() {
          @Override
          protected Void call() throws Exception {
            Files.writeString(file.toPath(), content);
            return null;
          }

          @Override
          protected void succeeded() {
            Platform.runLater(
                () -> {
                  Alert alert = new Alert(Alert.AlertType.INFORMATION);
                  alert.setTitle("Export Successful");
                  alert.setHeaderText(null);
                  alert.setContentText("File saved successfully to:\n" + file.getAbsolutePath());
                  alert.showAndWait();
                });
          }

          @Override
          protected void failed() {
            Platform.runLater(
                () ->
                    showError(
                        "Export Failed",
                        "An error occurred while saving:\n" + getException().getMessage()));
          }
        };

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private static void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
