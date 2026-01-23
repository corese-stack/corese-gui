package fr.inria.corese.gui.view;

import fr.inria.corese.gui.enums.SerializationFormat;
import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.codeEditor.CodeMirrorView;
import fr.inria.corese.gui.view.icon.IconButtonBarView;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
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

  private static final String STYLESHEET_PATH = "/styles/text-result.css";

  // Layout Constants
  private static final double SELECTOR_TOP_MARGIN = 15.0;
  private static final double SELECTOR_RIGHT_MARGIN = 20.0;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  public TextResultView() {
    super(new BorderPane(), STYLESHEET_PATH);

    // Initialize components
    this.formatLabel = new Label("Format:");
    this.formatChoiceBox = new ChoiceBox<>();
    this.codeMirrorView = new CodeMirrorView(true); // Read-only mode
    this.iconButtonBarView = new IconButtonBarView();

    // Format selector container (Floating)
    HBox formatBox = new HBox(formatLabel, formatChoiceBox);
    formatBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    formatBox.getStyleClass().add("format-selector-box");

    // Center area: Editor + Floating Selector
    StackPane centerStack = new StackPane(codeMirrorView, formatBox);
    centerStack.getStyleClass().add("result-center-stack");

    // Layout Constraints (Margins are instructions to the StackPane layout engine)
    StackPane.setMargin(formatBox, new Insets(SELECTOR_TOP_MARGIN, SELECTOR_RIGHT_MARGIN, 0, 0));

    // Main Layout
    BorderPane root = (BorderPane) getRoot();
    root.setCenter(centerStack);
    root.setRight(iconButtonBarView);
  }

  // ==============================================================================================
  // Accessors & Encapsulation
  // ==============================================================================================

  /**
   * Retrieves the currently selected serialization format.
   *
   * @return The selected format, or null
   */
  public SerializationFormat getFormat() {
    return formatChoiceBox.getValue();
  }

  /**
   * Retrieves the current content of the text editor.
   *
   * @return The text content
   */
  public String getContent() {
    return codeMirrorView.getContent();
  }

  /**
   * Sets the content of the text editor.
   *
   * @param content The text to display
   */
  public void setContent(String content) {
    codeMirrorView.setContent(content);
  }

  /**
   * Configures the toolbar buttons.
   *
   * @param buttons The list of button configurations (icon, tooltip, action)
   */
  public void setToolbarButtons(java.util.List<fr.inria.corese.gui.core.ButtonConfig> buttons) {
    iconButtonBarView.setButtons(buttons);
  }

  /**
   * Sets the syntax highlighting mode using a strongly-typed format.
   *
   * @param format The serialization format
   */
  public void setMode(SerializationFormat format) {
    codeMirrorView.setMode(format);
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
   */
  public void configureFormatSelector(
      SerializationFormat[] formats, SerializationFormat defaultFormat) {
    formatChoiceBox.getItems().setAll(formats);
    formatChoiceBox.setValue(defaultFormat);
  }

  /**
   * Sets the format change listener.
   *
   * @param listener The listener to invoke when format changes
   */
  public void setOnFormatChanged(ChangeListener<SerializationFormat> listener) {
    formatChoiceBox.getSelectionModel().selectedItemProperty().addListener(listener);
  }
}
