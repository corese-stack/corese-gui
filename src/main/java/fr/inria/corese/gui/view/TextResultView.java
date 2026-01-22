package fr.inria.corese.gui.view;

import fr.inria.corese.gui.enums.SerializationFormat;
import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.codeEditor.CodeMirrorView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * View for displaying text-based results with format selection.
 *
 * <p>This view provides:
 *
 * <ul>
 *   <li>A CodeMirror editor for displaying formatted text results
 *   <li>A floating format selector in the top-right corner
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

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  public TextResultView() {
    super(new StackPane(), null);

    // Initialize components
    this.formatLabel = new Label("Format:");
    this.formatChoiceBox = new ChoiceBox<>();
    this.formatChoiceBox.setPrefWidth(120);
    this.codeMirrorView = new CodeMirrorView(true); // Read-only mode

    // Build layout
    StackPane root = (StackPane) getRoot();
    
    // Format selector container
    HBox formatBox = new HBox(10, formatLabel, formatChoiceBox);
    formatBox.setAlignment(Pos.CENTER_RIGHT);
    formatBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    formatBox.setPadding(new Insets(10));
    formatBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); -fx-background-radius: 5;"); // Semi-transparent background

    // Stack components
    root.getChildren().addAll(codeMirrorView, formatBox);
    StackPane.setAlignment(formatBox, Pos.TOP_RIGHT);
  }

  // ==============================================================================================
  // Accessors
  // ==============================================================================================

  /**
   * Returns the format choice box.
   *
   * @return The ChoiceBox for format selection
   */
  public ChoiceBox<SerializationFormat> getFormatChoiceBox() {
    return formatChoiceBox;
  }

  /**
   * Returns the CodeMirror view component.
   *
   * @return The CodeMirror editor view
   */
  public CodeMirrorView getCodeMirrorView() {
    return codeMirrorView;
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
