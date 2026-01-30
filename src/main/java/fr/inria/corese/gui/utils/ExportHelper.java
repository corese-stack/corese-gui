package fr.inria.corese.gui.utils;

import fr.inria.corese.gui.component.notification.NotificationManager;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.concurrent.Task;
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

  /**
   * Prompts the user to save content, allowing selection from multiple supported formats.
   *
   * @param window The parent window for the dialog
   * @param formats List of supported serialization formats
   * @param contentProvider Function that generates the content string for a selected format
   */
  public static void exportResult(
      Window window,
      List<SerializationFormat> formats,
      Function<SerializationFormat, String> contentProvider) {

    if (formats == null || formats.isEmpty()) {
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Result");
    fileChooser.setInitialFileName("export"); // No extension initially

    // Add filters for each format
    for (SerializationFormat format : formats) {
      String ext = format.getExtension();
      FileChooser.ExtensionFilter filter =
          new FileChooser.ExtensionFilter(format.getLabel() + " (*" + ext + ")", "*" + ext);
      fileChooser.getExtensionFilters().add(filter);
    }

    File file = fileChooser.showSaveDialog(window);

    if (file != null) {
      // Determine format from the selected filter or file extension
      SerializationFormat selectedFormat = formats.get(0); // Default
      FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();

      if (selectedFilter != null) {
        for (SerializationFormat fmt : formats) {
          if (selectedFilter.getDescription().contains(fmt.getLabel())) {
            selectedFormat = fmt;
            break;
          }
        }
      } else {
        // Fallback: check extension
        String name = file.getName().toLowerCase();
        for (SerializationFormat fmt : formats) {
          if (name.endsWith(fmt.getExtension())) {
            selectedFormat = fmt;
            break;
          }
        }
      }

      // Enforce extension
      if (!file.getName().toLowerCase().endsWith(selectedFormat.getExtension())) {
        file = new File(file.getAbsolutePath() + selectedFormat.getExtension());
      }

      // Generate content using the provider
      String content = contentProvider.apply(selectedFormat);
      writeFileAsync(file, content);
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
            Platform.runLater(() -> NotificationManager.getInstance().showSuccess("File saved successfully"));
          }

          @Override
          protected void failed() {
            Platform.runLater(
                () ->
                    NotificationManager.getInstance().showError(
                        "Export Failed: " + getException().getMessage()));
          }
        };

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }
}
