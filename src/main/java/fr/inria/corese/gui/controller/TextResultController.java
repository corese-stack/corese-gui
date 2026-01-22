package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.SerializationFormat;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.view.TextResultView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for text-based result display with format selection and export capabilities.
 *
 * <p>This controller manages:
 *
 * <ul>
 *   <li>CodeMirror view for displaying results with syntax highlighting
 *   <li>Format selector (Turtle, RDF/XML, JSON-LD, etc.)
 *   <li>Copy to clipboard functionality
 *   <li>Export to file functionality (asynchronous to avoid UI freezing)
 * </ul>
 *
 * <p><b>Architecture:</b> Follows clean MVC separation with {@link TextResultView}.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * TextResultController controller = new TextResultController(buttons);
 * controller.setOnFormatChanged(format -> reserializeResults(format));
 * controller.updateText(results);
 * Node view = controller.getView();
 * }</pre>
 */
public class TextResultController {

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  /** The view component managed by this controller. */
  private final TextResultView view;

  /** Callback invoked when format selection changes. */
  private Consumer<SerializationFormat> onFormatChanged;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new TextResultController with the specified toolbar buttons.
   *
   * @param buttons List of button configurations for customizing button appearance and tooltips
   */
  public TextResultController(List<ButtonConfig> buttons) {
    this.view = new TextResultView();
    applyButtonConfigs(buttons);
    initialize();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /**
   * Applies button configurations from the provided list.
   *
   * @param buttons List of button configurations
   */
  private void applyButtonConfigs(List<ButtonConfig> buttons) {
    // Apply custom configuration to Copy button if provided
    buttons.stream()
        .filter(b -> b.getIcon() == IconButtonType.COPY)
        .findFirst()
        .ifPresent(
            config -> {
              if (config.getText() != null) {
                view.getCopyButton().setText(config.getText());
              }
              if (config.getTooltip() != null) {
                view.getCopyButton().setTooltip(new Tooltip(config.getTooltip()));
              }
            });

    // Apply custom configuration to Export button if provided
    buttons.stream()
        .filter(b -> b.getIcon() == IconButtonType.EXPORT)
        .findFirst()
        .ifPresent(
            config -> {
              if (config.getText() != null) {
                view.getExportButton().setText(config.getText());
              }
              if (config.getTooltip() != null) {
                view.getExportButton().setTooltip(new Tooltip(config.getTooltip()));
              }
            });
  }

  /** Initializes UI components and event handlers. */
  private void initialize() {
    // Configure format selector using high-level method
    view.configureFormatSelector(
        SerializationFormat.rdfFormats(),
        SerializationFormat.TURTLE,
        new StringConverter<SerializationFormat>() {
          @Override
          public String toString(SerializationFormat format) {
            return format != null ? format.getLabel() : "";
          }

          @Override
          public SerializationFormat fromString(String string) {
            return SerializationFormat.fromString(string);
          }
        });

    // Listen to format changes using high-level method
    view.setOnFormatChanged(
        (obs, oldVal, newVal) -> {
          if (newVal != null) {
            updateSyntaxHighlighting(newVal);
            if (onFormatChanged != null) {
              onFormatChanged.accept(newVal);
            }
          }
        });

    // Initial highlighting setup
    updateSyntaxHighlighting(view.getFormatChoiceBox().getValue());

    // Set default tooltips if not already configured
    if (view.getCopyButton().getTooltip() == null) {
      view.getCopyButton().setTooltip(new Tooltip("Copy to clipboard"));
    }
    if (view.getExportButton().getTooltip() == null) {
      view.getExportButton().setTooltip(new Tooltip("Export to file"));
    }

    // Configure button actions
    view.getCopyButton().setOnAction(event -> handleCopy());
    view.getExportButton().setOnAction(event -> handleExport());
  }

  /**
   * Updates the CodeMirror syntax highlighting mode based on the selected format.
   *
   * @param format The current serialization format
   */
  private void updateSyntaxHighlighting(SerializationFormat format) {
    if (format == null) return;

    String mode = switch (format) {
      case TURTLE, TRIG, N_TRIPLES, N_QUADS -> "turtle";
      case RDF_XML, XML -> "xml";
      case JSON_LD, JSON -> "json";
      default -> "text/plain";
    };

    Platform.runLater(() -> view.getCodeMirrorView().setMode(mode));
  }

  // ==============================================================================================
  // Event Handlers
  // ==============================================================================================

  /** Handles copy to clipboard action. */
  private void handleCopy() {
    ClipboardContent content = new ClipboardContent();
    content.putString(view.getCodeMirrorView().getContent());
    Clipboard.getSystemClipboard().setContent(content);

    // Temporary tooltip feedback
    Tooltip tooltip = view.getCopyButton().getTooltip();
    if (tooltip != null) {
      String originalText = tooltip.getText();
      tooltip.setText("Copied!");
      PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
      pause.setOnFinished(e -> tooltip.setText(originalText));
      pause.play();
    }
  }

  /** Handles export to file action (asynchronous to avoid blocking UI). */
  private void handleExport() {
    String contentToExport = view.getCodeMirrorView().getContent();
    if (contentToExport == null || contentToExport.isBlank()) {
      showError("Export Error", "There is no text content to export.");
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Result As");

    SerializationFormat selectedFormat = view.getFormatChoiceBox().getValue();
    if (selectedFormat == null) {
      selectedFormat = SerializationFormat.TURTLE;
    }

    String extension = selectedFormat.getExtension();
    fileChooser.setInitialFileName("output" + extension);

    FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter(
            selectedFormat.getLabel() + " file (*" + extension + ")", "*" + extension);
    fileChooser.getExtensionFilters().add(extFilter);
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

    Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
    File file = fileChooser.showSaveDialog(window);

    if (file != null) {
      File fileToSave = file;
      if (!file.getName().toLowerCase().endsWith(extension)) {
        fileToSave = new File(file.getAbsolutePath() + extension);
      }

      // Export asynchronously to avoid blocking UI for large files
      exportFileAsync(fileToSave, contentToExport);
    }
  }

  /**
   * Exports content to file asynchronously.
   *
   * @param file The target file
   * @param content The content to write
   */
  private void exportFileAsync(File file, String content) {
    Task<Void> exportTask =
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
                  alert.setContentText(
                      "The result has been successfully exported to:\n" + file.getAbsolutePath());
                  alert.showAndWait();
                });
          }

          @Override
          protected void failed() {
            Platform.runLater(
                () ->
                    showError(
                        "Export Failed",
                        "An error occurred while saving the file:\n"
                            + getException().getMessage()));
          }
        };

    // Run in background thread (daemon to avoid keeping JVM alive on app close)
    Thread exportThread = new Thread(exportTask);
    exportThread.setDaemon(true);
    exportThread.start();
  }

  /**
   * Shows an error dialog.
   *
   * @param title The dialog title
   * @param message The error message
   */
  private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Updates the displayed text content.
   *
   * @param content The text content to display
   */
  public void updateText(String content) {
    Platform.runLater(() -> {
        view.getCodeMirrorView().setContent(content != null ? content : "");
        updateSyntaxHighlighting(view.getFormatChoiceBox().getValue());
    });
  }

  /**
   * Clears all text content.
   */
  public void clear() {
    Platform.runLater(() -> view.getCodeMirrorView().setContent(""));
  }

  /**
   * Sets the callback for format change events.
   *
   * @param listener Consumer that receives the newly selected format
   */
  public void setOnFormatChanged(Consumer<SerializationFormat> listener) {
    this.onFormatChanged = listener;
  }

  /**
   * Returns the root view node.
   *
   * @return The view's root node
   */
  public Node getView() {
    return view.getRoot();
  }

  /**
   * Returns the currently selected format.
   *
   * @return The selected SerializationFormat
   */
  public SerializationFormat getSelectedFormat() {
    return view.getFormatChoiceBox().getValue();
  }

  /**
   * Sets the selected format programmatically.
   *
   * @param format The format to select
   */
  public void setSelectedFormat(SerializationFormat format) {
    Platform.runLater(() -> view.getFormatChoiceBox().setValue(format));
  }
}
