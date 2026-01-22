package fr.inria.corese.gui.view;

import fr.inria.corese.gui.enums.SerializationFormat;
import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.codeEditor.CodeMirrorView;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * View for displaying text-based results with format selection and action buttons.
 *
 * <p>This view provides:
 *
 * <ul>
 *   <li>A toolbar with format selector, copy and export buttons
 *   <li>A CodeMirror editor for displaying formatted text results
 *   <li>Proper layout that doesn't overlap UI elements
 * </ul>
 *
 * <p><b>Design:</b> Clean separation of UI structure from controller logic.
 */
public class TextResultView extends AbstractView {

  // ==============================================================================================
  // UI Components
  // ==============================================================================================

  private final ToolBar toolbar;
  private final Label formatLabel;
  private final ChoiceBox<SerializationFormat> formatChoiceBox;
  private final Button copyButton;
  private final Button exportButton;
  private final CodeMirrorView codeMirrorView;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  public TextResultView() {
    super(new BorderPane(), null);

    // Initialize components
    this.formatLabel = new Label("Format:");
    this.formatChoiceBox = new ChoiceBox<>();
    this.formatChoiceBox.setPrefWidth(120); // Ensure a consistent width on first render
    this.copyButton = new Button("Copy");
    this.exportButton = new Button("Export");
    this.codeMirrorView = new CodeMirrorView(true); // Read-only mode

    // Initialize toolbar
    this.toolbar = createToolbar();

    // Build layout
    BorderPane root = (BorderPane) getRoot();
    root.setTop(toolbar);
    root.setCenter(codeMirrorView);
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /**
   * Creates the toolbar with format selector and action buttons.
   *
   * @return Configured toolbar
   */
  private ToolBar createToolbar() {
    // Spacer to push buttons to the right
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    ToolBar bar = new ToolBar();
    bar.getItems().addAll(formatLabel, formatChoiceBox, spacer, copyButton, exportButton);
    bar.setStyle("-fx-padding: 5px;");

    return bar;
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
   * Returns the copy button.
   *
   * @return The copy button
   */
  public Button getCopyButton() {
    return copyButton;
  }

  /**
   * Returns the export button.
   *
   * @return The export button
   */
  public Button getExportButton() {
    return exportButton;
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
