package fr.inria.corese.gui.view;

import fr.inria.corese.gui.enums.SerializationFormat;
import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.codeEditor.CodeMirrorView;
import fr.inria.corese.gui.view.icon.IconButtonBarView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * View for displaying text-based results with format selection.
 *
 * <p>This view provides:
 *
 * <ul>
 *   <li>A CodeMirror editor for displaying formatted text results
 *   <li>A floating format selector in the top-right corner
 *   <li>A dedicated sidebar for actions (Copy, Export)
 * </ul>
 *
 * <p><b>Design:</b> Clean separation of UI structure from controller logic.
 */
public class TextResultView extends AbstractView {

  // ==============================================================================================
  // UI Components
  // ==============================================================================================

  private final Label formatLabel;
  private final ChoiceBox<SerializationFormat> formatChoiceBox;
  private final CodeMirrorView codeMirrorView;
  private final IconButtonBarView iconButtonBarView;

  private static final String STYLESHEET_PATH = "/styles/text-result-view.css";

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  public TextResultView() {
    super(new BorderPane(), STYLESHEET_PATH);

    // Initialize components
    this.formatLabel = new Label("Format:");
    this.formatChoiceBox = new ChoiceBox<>();
    this.formatChoiceBox.setPrefWidth(120);
    this.codeMirrorView = new CodeMirrorView(true); // Read-only mode
    this.iconButtonBarView = new IconButtonBarView();

    // Format selector container (Floating)
    HBox formatBox = new HBox(10, formatLabel, formatChoiceBox);
    formatBox.setAlignment(Pos.CENTER_RIGHT);
    formatBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    formatBox.setPadding(new Insets(10));
    formatBox.getStyleClass().add("format-selector-box");

    // Center area: Editor + Floating Selector
    StackPane centerStack = new StackPane(codeMirrorView, formatBox);
    StackPane.setAlignment(formatBox, Pos.TOP_RIGHT);
    StackPane.setMargin(formatBox, new Insets(15, 20, 0, 0));

    // Main Layout
    BorderPane root = (BorderPane) getRoot();
    root.setCenter(centerStack);
    root.setRight(iconButtonBarView);
  }

  // ==============================================================================================
  // Accessors & Encapsulation
  // ==============================================================================================

  /**
   * Returns the icon button bar view.
   *
   * @return The sidebar view
   */
  public IconButtonBarView getIconButtonBarView() {
    return iconButtonBarView;
  }

  /**
   * Sets the content of the text editor.
   *
   * @param content The text to display
   */
  public void setEditorContent(String content) {
    codeMirrorView.setContent(content);
  }

  /**
   * Retrieves the current content of the text editor.
   *
   * @return The text content
   */
  public String getEditorContent() {
    return codeMirrorView.getContent();
  }

  /**
   * Sets the syntax highlighting mode of the editor.
   *
   * @param mode The mode string (e.g., "turtle", "xml", "json")
   */
  public void setEditorMode(String mode) {
    codeMirrorView.setMode(mode);
  }

  /**
   * Retrieves the currently selected serialization format.
   *
   * @return The selected format, or null
   */
  public SerializationFormat getSelectedFormat() {
    return formatChoiceBox.getValue();
  }

  /**
   * Sets the selected serialization format.
   *
   * @param format The format to select
   */
  public void setSelectedFormat(SerializationFormat format) {
    formatChoiceBox.setValue(format);
  }

  // ==============================================================================================
  // High-level Configuration Methods (Demeter's Law compliance)
  // ==============================================================================================

  /**
   * Configures the format choice box with the specified formats.
   *
   * <p>This method encapsulates the internal configuration of the choice box.
   *
   * @param formats Array of formats to display
   * @param defaultFormat The default selected format
   * @param converter String converter for displaying format labels
   */
  public void configureFormatSelector(
      SerializationFormat[] formats,
      SerializationFormat defaultFormat,
      javafx.util.StringConverter<SerializationFormat> converter) {
    formatChoiceBox.getItems().setAll(formats);
    formatChoiceBox.setValue(defaultFormat);
    formatChoiceBox.setConverter(converter);
  }

  /**
   * Sets the format change listener.
   *
   * @param listener The listener to invoke when format changes
   */
  public void setOnFormatChanged(
      javafx.beans.value.ChangeListener<SerializationFormat> listener) {
    formatChoiceBox.getSelectionModel().selectedItemProperty().addListener(listener);
  }
}
