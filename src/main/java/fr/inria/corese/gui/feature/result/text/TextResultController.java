package fr.inria.corese.gui.feature.result.text;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.factory.ButtonFactory;
import fr.inria.corese.gui.utils.ExportHelper;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/** 
 * Controller for text-based result display.
 * 
 * <p>Manages the {@link TextResultView}, handles user interactions (format change, copy, export),
 * and coordinates data updates.
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
  // Constructor & Initialization
  // ==============================================================================================

  /** Constructs a new TextResultController. */
  public TextResultController() {
    this.view = new TextResultView();
    initialize();
  }

  /** Initializes UI components and event handlers. */
  private void initialize() {
    // 1. Setup Toolbar
    view.setToolbarActions(List.of(
        ButtonFactory.copy(this::copyContent),
        ButtonFactory.export(this::exportContent)
    ));

    // 2. Configure format selector (default: Turtle)
    view.configureFormatSelector(SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);

    // 3. Setup Listeners
    view.setOnFormatChanged(
        (obs, oldVal, newVal) -> {
          if (newVal != null) {
            handleFormatChange(newVal);
          }
        });

    // 4. Initial State
    updateSyntaxHighlighting(view.getFormat());
  }

  // ==============================================================================================
  // Event Handlers
  // ==============================================================================================

  private void handleFormatChange(SerializationFormat newVal) {
    updateSyntaxHighlighting(newVal);
    if (onFormatChanged != null) {
      onFormatChanged.accept(newVal);
    }
  }

  /** Handles copy to clipboard action. */
  private void copyContent() {
    ClipboardContent content = new ClipboardContent();
    content.putString(view.getContent());
    Clipboard.getSystemClipboard().setContent(content);
  }

  /** Handles export to file action. */
  private void exportContent() {
    ExportHelper.exportText(
        view.getRoot().getScene().getWindow(), view.getContent(), view.getFormat());
  }

  // ==============================================================================================
  // Logic
  // ==============================================================================================

  /**
   * Updates the CodeMirror syntax highlighting mode based on the selected format.
   *
   * @param format The current serialization format
   */
  private void updateSyntaxHighlighting(SerializationFormat format) {
    if (format == null) return;
    view.setMode(format);
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Sets the displayed text content.
   *
   * @param content The text content to display
   */
  public void setContent(String content) {
    Platform.runLater(
        () -> {
          view.setContent(content != null ? content : "");
          updateSyntaxHighlighting(view.getFormat());
        });
  }

  /** Clears the displayed text content. */
  public void clearContent() {
    Platform.runLater(() -> view.setContent(""));
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
   * Updates the available formats in the format selector.
   *
   * @param formats The list of available serialization formats
   * @param defaultFormat The format to select by default
   */
  public void setAvailableFormats(
      SerializationFormat[] formats, SerializationFormat defaultFormat) {
    Platform.runLater(
        () -> {
          view.configureFormatSelector(formats, defaultFormat);
          // Ensure highlighting is updated for the new default
          updateSyntaxHighlighting(defaultFormat);
        });
  }
}