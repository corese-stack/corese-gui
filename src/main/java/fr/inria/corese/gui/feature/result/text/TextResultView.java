package fr.inria.corese.gui.feature.result.text;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
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
 * View for displaying text-based results (Turtle, JSON-LD, XML, etc.).
 *
 * <p>Features:
 * <ul>
 *   <li><b>Code Editor:</b> Uses {@link CodeMirrorWidget} for syntax highlighting and read-only display.
 *   <li><b>Format Selector:</b> A floating dropdown to switch between serialization formats.
 *   <li><b>Toolbar:</b> Sidebar for common actions (Copy, Export, Zoom).
 * </ul>
 *
 * <p>Design Note: The format selector overlays the editor content in the top-right corner to save space,
 * fading out when not in use.
 */
public class TextResultView extends AbstractView {

    // ==============================================================================================
    // Constants
    // ==============================================================================================

    private static final String STYLESHEET_PATH = "/css/features/text-result.css";
    private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";
    
    private static final String STYLE_CLASS_FLOATING_PANEL = "floating-panel";
    private static final String STYLE_CLASS_FORMAT_BOX = "format-selector-box";
    private static final String STYLE_CLASS_CENTER_STACK = "result-center-stack";

    private static final double SELECTOR_TOP_MARGIN = 15.0;
    private static final double SELECTOR_RIGHT_MARGIN = 20.0;
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

        // Apply common styles (required for floating panel look)
        CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);

        this.formatLabel = new Label("Format:");
        this.formatChoiceBox = new ChoiceBox<>();
        this.editorWidget = new CodeMirrorWidget(true); // true = Read-only
        this.toolbarWidget = new ToolbarWidget();

        setupLayout();
    }

    // ==============================================================================================
    // Layout
    // ==============================================================================================

    private void setupLayout() {
        // 1. Format Selector (Floating Box)
        HBox formatBox = new HBox(formatLabel, formatChoiceBox);
        formatBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        formatBox.getStyleClass().addAll(STYLE_CLASS_FLOATING_PANEL, STYLE_CLASS_FORMAT_BOX);
        
        // Setup fade animation for the floating box
        formatBox.setOpacity(OPACITY_IDLE);
        setupHoverAnimation(formatBox);

        // 2. Center Stack (Editor + Floating Selector)
        StackPane centerStack = new StackPane(editorWidget, formatBox);
        centerStack.getStyleClass().add(STYLE_CLASS_CENTER_STACK);
        
        // Position the floating box
        StackPane.setMargin(formatBox, new Insets(SELECTOR_TOP_MARGIN, SELECTOR_RIGHT_MARGIN, 0, 0));

        // 3. Main Layout
        BorderPane root = (BorderPane) getRoot();
        root.setCenter(centerStack);
        root.setRight(toolbarWidget);
    }

    private void setupHoverAnimation(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(FADE_DURATION_MS), node);

        node.setOnMouseEntered(e -> {
            fade.stop();
            fade.setFromValue(node.getOpacity());
            fade.setToValue(OPACITY_HOVER);
            fade.play();
        });

        node.setOnMouseExited(e -> {
            fade.stop();
            fade.setFromValue(node.getOpacity());
            fade.setToValue(OPACITY_IDLE);
            fade.play();
        });
    }

    // ==============================================================================================
    // Public API - Data
    // ==============================================================================================

    /**
     * Gets the currently selected serialization format.
     */
    public SerializationFormat getFormat() {
        return formatChoiceBox.getValue();
    }

    /**
     * Sets the editor mode based on the given serialization format.
     */
    public void setMode(SerializationFormat format) {
        editorWidget.setMode(format);
    }

    /**
     * Gets the editor text content.
     */
    public String getContent() {
        return editorWidget.getContent();
    }

    /**
     * Sets the editor text content.
     */
    public void setContent(String content) {
        editorWidget.setContent(content);
    }


    public void zoomIn() {
        editorWidget.zoomIn();
    }

    public void zoomOut() {
        editorWidget.zoomOut();
    }

    // ==============================================================================================
    // Public API - Configuration
    // ==============================================================================================

    public void setToolbarActions(List<ButtonConfig> buttons) {
        toolbarWidget.setButtons(buttons);
    }

    public void configureFormatSelector(SerializationFormat[] formats, SerializationFormat defaultFormat) {
        formatChoiceBox.getItems().setAll(formats);
        formatChoiceBox.setValue(defaultFormat);
    }

    public void setOnFormatChanged(ChangeListener<SerializationFormat> listener) {
        formatChoiceBox.getSelectionModel().selectedItemProperty().addListener(listener);
    }
}
