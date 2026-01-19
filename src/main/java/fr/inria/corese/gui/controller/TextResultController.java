package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.view.codeEditor.CodeMirrorView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
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
 *   <li>Export to file functionality
 * </ul>
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

  /** CodeMirror view for displaying result content with syntax highlighting. */
  private final CodeMirrorView codeMirrorView;

  /** Choice box for selecting output format. */
  private final ChoiceBox<String> formatChoiceBox;

  /** Button for copying content to clipboard. */
  private final Button copyButton;

  /** Button for exporting content to file. */
  private final Button exportButton;

  /** Root view node containing all UI components. */
  private final BorderPane rootView;

  /** Callback invoked when format selection changes. */
  private Consumer<String> onFormatChanged;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new TextResultController with the specified toolbar buttons.
   *
   * @param buttons List of button configurations for the toolbar
   * @throws NullPointerException if buttons is null
   */
  public TextResultController(List<ButtonConfig> buttons) {
    this.codeMirrorView = new CodeMirrorView(true); // Read-only mode for results
    this.formatChoiceBox = new ChoiceBox<>();
    this.copyButton = createButton(buttons, IconButtonType.COPY);
    this.exportButton = createButton(buttons, IconButtonType.EXPORT);
    this.rootView = new BorderPane();

    initialize();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /** Initializes UI components and event handlers. */
  private void initialize() {
    // Configure format selector
    formatChoiceBox.getItems().setAll("TURTLE", "RDF/XML", "JSON-LD", "N-TRIPLES", "N-QUADS", "TRIG");
    formatChoiceBox.getSelectionModel().select("TURTLE");
    formatChoiceBox
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null && onFormatChanged != null) {
                onFormatChanged.accept(newVal);
              }
            });

    // Configure buttons
    copyButton.setOnAction(event -> handleCopy());
    exportButton.setOnAction(event -> handleExport());

    // Build layout
    StackPane textContent = new StackPane();
    textContent.getChildren().add(codeMirrorView);

    StackPane.setAlignment(formatChoiceBox, Pos.TOP_RIGHT);
    StackPane.setMargin(formatChoiceBox, new Insets(10));
    textContent.getChildren().add(formatChoiceBox);

    rootView.setCenter(textContent);
  }

  /**
   * Creates a button from button configurations.
   *
   * @param buttons List of button configurations
   * @param type The icon type to search for
   * @return A configured button, or a default button if not found
   */
  private Button createButton(List<ButtonConfig> buttons, IconButtonType type) {
    return buttons.stream()
        .filter(b -> b.getIcon() == type)
        .findFirst()
        .map(
            config -> {
              Button btn = new Button();
              if (config.getTooltip() != null) {
                btn.setTooltip(new Tooltip(config.getTooltip()));
              }
              return btn;
            })
        .orElseGet(Button::new);
  }

  // ==============================================================================================
  // Event Handlers
  // ==============================================================================================

  /** Handles copy to clipboard action. */
  private void handleCopy() {
    ClipboardContent content = new ClipboardContent();
    content.putString(codeMirrorView.getContent());
    Clipboard.getSystemClipboard().setContent(content);

    Tooltip tooltip = copyButton.getTooltip();
    if (tooltip != null) {
      String originalText = tooltip.getText();
      tooltip.setText("Copied!");
      PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
      pause.setOnFinished(e -> tooltip.setText(originalText));
      pause.play();
    }
  }

  /** Handles export to file action. */
  private void handleExport() {
    String contentToExport = codeMirrorView.getContent();
    if (contentToExport == null || contentToExport.isBlank()) {
      showError("Export Error", "There is no text content to export.");
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Result As");

    String selectedFormat = formatChoiceBox.getValue();
    if (selectedFormat == null) {
      selectedFormat = "Text";
    }
    String extension = getExtensionForFormat(selectedFormat);

    fileChooser.setInitialFileName("output" + extension);
    FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter(
            selectedFormat + " file (*" + extension + ")", "*" + extension);
    fileChooser.getExtensionFilters().add(extFilter);
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

    Window window = rootView.getScene() != null ? rootView.getScene().getWindow() : null;
    File file = fileChooser.showSaveDialog(window);

    if (file != null) {
      File fileToSave = file;
      if (!file.getName().toLowerCase().endsWith(extension)) {
        fileToSave = new File(file.getAbsolutePath() + extension);
      }

      try {
        Files.writeString(fileToSave.toPath(), contentToExport);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText(null);
        alert.setContentText(
            "The result has been successfully exported to:\n" + fileToSave.getAbsolutePath());
        alert.showAndWait();
      } catch (IOException e) {
        showError("Export Failed", "An error occurred while saving the file:\n" + e.getMessage());
      }
    }
  }

  /**
   * Maps format names to file extensions.
   *
   * @param format The format name
   * @return The corresponding file extension
   */
  private String getExtensionForFormat(String format) {
    return switch (format.toUpperCase()) {
      case "RDF/XML" -> ".rdf";
      case "JSON-LD" -> ".jsonld";
      case "N-TRIPLES" -> ".nt";
      case "N-QUADS" -> ".nq";
      case "TRIG" -> ".trig";
      case "TURTLE" -> ".ttl";
      case "JSON" -> ".json";
      case "CSV" -> ".csv";
      case "TSV" -> ".tsv";
      case "XML" -> ".xml";
      case "MARKDOWN" -> ".md";
      default -> ".ttl";
    };
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
    Platform.runLater(() -> codeMirrorView.setContent(content != null ? content : ""));
  }

  /**
   * Clears all text content.
   */
  public void clear() {
    Platform.runLater(() -> codeMirrorView.setContent(""));
  }

  /**
   * Sets the callback for format change events.
   *
   * @param listener Consumer that receives the newly selected format
   */
  public void setOnFormatChanged(Consumer<String> listener) {
    this.onFormatChanged = listener;
  }

  /**
   * Returns the root view node.
   *
   * @return The BorderPane containing all UI components
   */
  public Node getView() {
    return rootView;
  }

  /**
   * Returns the currently selected format.
   *
   * @return The selected format string (e.g., "TURTLE", "JSON-LD")
   */
  public String getSelectedFormat() {
    return formatChoiceBox.getValue();
  }

  /**
   * Sets the selected format programmatically.
   *
   * @param format The format to select
   */
  public void setSelectedFormat(String format) {
    Platform.runLater(() -> formatChoiceBox.getSelectionModel().select(format));
  }
}
