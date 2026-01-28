package fr.inria.corese.gui.feature.textResult;

import fr.inria.corese.gui.core.factory.ButtonFactory;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.utils.ExportHelper;






import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for text-based result display with format selection and export capabilities.
 */
public class TextResultController {

  /** The view component managed by this controller. */
  private final TextResultView view;

  /** Callback invoked when format selection changes. */
  private Consumer<SerializationFormat> onFormatChanged;

  /**
   * Constructs a new TextResultController.
   */
  public TextResultController() {
    this.view = new TextResultView();
    initialize();
  }

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
    // Configure the toolbar widget directly via the view
    view.getToolbarView().setButtons(List.of(
        ButtonFactory.copy(this::copyContent), 
        ButtonFactory.export(this::exportContent)
    ));
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