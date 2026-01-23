package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.AppButtons;
import fr.inria.corese.gui.enums.SerializationFormat;
import fr.inria.corese.gui.utils.ExportHelper;
import fr.inria.corese.gui.view.TextResultView;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for text-based result display with format selection and export capabilities.
 *
 * <p>This controller manages:
 *
 * <ul>
 *   <li>CodeMirror view for displaying results with syntax highlighting
 *   <li>Format selector (Turtle, RDF/XML, JSON-LD, etc.)
 *   <li>Copy to clipboard functionality
 *   <li>Export to file functionality (via {@link ExportHelper})
 * </ul>
 *
 * <p><b>Architecture:</b> Follows clean MVC separation with {@link TextResultView}.
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
   * Constructs a new TextResultController.
   *
   * <p>Initializes the view with default Copy and Export capabilities.
   */
  public TextResultController() {
    this.view = new TextResultView();
    initialize();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /** Initializes UI components and event handlers. */
  private void initialize() {
    initializeSidebar();

    // Configure format selector
    view.configureFormatSelector(SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);

    // Listen to format changes
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
    updateSyntaxHighlighting(view.getFormat());
  }

  private void initializeSidebar() {
    view.setToolbarButtons(
        List.of(AppButtons.copy(this::copyContent), AppButtons.export(this::exportContent)));
  }

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
  // Event Handlers
  // ==============================================================================================

  /** Handles copy to clipboard action. */
  public void copyContent() {
    ClipboardContent content = new ClipboardContent();
    content.putString(view.getContent());
    Clipboard.getSystemClipboard().setContent(content);
  }

  /** Handles export to file action. */
  public void exportContent() {
    ExportHelper.exportText(
        view.getRoot().getScene().getWindow(), view.getContent(), view.getFormat());
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
