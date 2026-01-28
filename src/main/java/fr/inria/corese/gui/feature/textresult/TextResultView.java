package fr.inria.corese.gui.feature.textresult;

import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.CssUtils;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

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
  // Constants
  // ==============================================================================================

  private static final String STYLESHEET_PATH = "/styles/text-result.css";
  private static final String COMMON_STYLESHEET_PATH = "/styles/common.css";
  
  // Layout
  private static final double SELECTOR_TOP_MARGIN = 15.0;
  private static final double SELECTOR_RIGHT_MARGIN = 20.0;

  // Animation
  private static final double OPACITY_IDLE = 0.4;
  private static final double OPACITY_HOVER = 1.0;
  private static final int FADE_DURATION_MS = 200;

  // ==============================================================================================
  // UI Components
  // ==============================================================================================

  private final Label formatLabel;
  private final ChoiceBox<SerializationFormat> formatChoiceBox;
  private final CodeMirrorWidget editorWidget;
  private final ToolbarWidget toolbarWidget;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  public TextResultView() {
    super(new BorderPane(), STYLESHEET_PATH);
    
    // Load common styles (for floating panels)
    CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);

    // Initialize components
    this.formatLabel = new Label("Format:");
    this.formatChoiceBox = new ChoiceBox<>();
    this.editorWidget = new CodeMirrorWidget(true); // Read-only mode
    this.toolbarWidget = new ToolbarWidget();

    // Setup Layout
    setupLayout();
  }

  // ==============================================================================================
  // Layout & Initialization
  // ==============================================================================================

  private void setupLayout() {
    // Format selector container (Floating)
    HBox formatBox = new HBox(formatLabel, formatChoiceBox);
    formatBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    
    // Apply common style for floating look + specific style for layout
    formatBox.getStyleClass().addAll("floating-panel", "format-selector-box");

    // Animation setup for the floating box
    formatBox.setOpacity(OPACITY_IDLE);
    setupHoverAnimation(formatBox);

    // Center area: Editor + Floating Selector stacked
    StackPane centerStack = new StackPane(editorWidget, formatBox);
    centerStack.getStyleClass().add("result-center-stack");

    // Constraints for floating box position
    StackPane.setMargin(formatBox, new Insets(SELECTOR_TOP_MARGIN, SELECTOR_RIGHT_MARGIN, 0, 0));

    // Main Layout
    BorderPane root = (BorderPane) getRoot();
    root.setCenter(centerStack);
    root.setRight(toolbarWidget);
  }

  /**
   * Configures the fade transition for the format selector box.
   *
   * @param node The node to animate
   */
  private void setupHoverAnimation(Node node) {
    FadeTransition fade = new FadeTransition(Duration.millis(FADE_DURATION_MS), node);

    node.setOnMouseEntered(
        e -> {
          fade.stop();
          fade.setFromValue(node.getOpacity());
          fade.setToValue(OPACITY_HOVER);
          fade.play();
        });

    node.setOnMouseExited(
        e -> {
          fade.stop();
          fade.setFromValue(node.getOpacity());
          fade.setToValue(OPACITY_IDLE);
          fade.play();
        });
  }

  // ==============================================================================================
  // Public API - Content & State
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
    return editorWidget.getContent();
  }

  /**
   * Sets the content of the text editor.
   *
   * @param content The text to display
   */
  public void setContent(String content) {
    editorWidget.setContent(content);
  }

  /**
   * Sets the syntax highlighting mode using a strongly-typed format.
   *
   * @param format The serialization format
   */
  public void setMode(SerializationFormat format) {
    editorWidget.setMode(format);
  }

  // ==============================================================================================
  // Public API - Configuration (Demeter's Law)
  // ==============================================================================================

  /**
   * Configures the toolbar buttons.
   * 
   * @param buttons List of button configurations
   */
  public void setToolbarActions(List<ButtonConfig> buttons) {
    toolbarWidget.setButtons(buttons);
  }

  /**
   * Configures the format choice box with the specified formats.
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