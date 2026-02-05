package fr.inria.corese.gui.feature.result.graph;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.CssUtils;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

/**
 * View for displaying graph visualization results.
 * <p>
 * This view integrates a {@link GraphDisplayWidget} for the main visualization and a
 * {@link ToolbarWidget} for actions. It also includes a floating legend that explains the
 * node types (Resource, Literal, Blank Node).
 * </p>
 */
public class GraphResultView extends AbstractView {

      private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";
      private static final String VIEW_STYLESHEET_PATH = "/css/features/graph-result.css";
  // Animation constants
  private static final double OPACITY_IDLE = 0.7;
  private static final double OPACITY_HOVER = 1.0;
  private static final int FADE_DURATION_MS = 200;

  // Legend Colors (matching D3 visualization)
  private static final Color COLOR_RESOURCE = Color.web("#1f77b4");
  private static final Color COLOR_LITERAL = Color.web("#ff7f0e");
  private static final Color COLOR_BLANK_NODE = Color.web("#2ca02c");

  private final GraphDisplayWidget graphWidget;
  private final ToolbarWidget toolbarWidget;

  /**
   * Constructs a new GraphResultView.
   * Initializes the layout, graph widget, toolbar, and the floating legend.
   */
  public GraphResultView() {
    super(new BorderPane(), null);

    // Load common styles and view-specific styles
    CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
    CssUtils.applyViewStyles(getRoot(), VIEW_STYLESHEET_PATH);

    this.graphWidget = new GraphDisplayWidget();
    this.toolbarWidget = new ToolbarWidget();

    // Setup Toolbar Buttons
    setupToolbar();

    // Create Legend
    VBox legendBox = createLegend();

    // Animation for legend
    legendBox.setOpacity(OPACITY_IDLE);
    setupHoverAnimation(legendBox);

    // Stack graph and legend
    StackPane centerStack = new StackPane(graphWidget, legendBox);
    centerStack.getStyleClass().add("result-center-stack");
    StackPane.setAlignment(legendBox, Pos.TOP_RIGHT);
    StackPane.setMargin(legendBox, new Insets(15, 20, 0, 0));

    BorderPane root = (BorderPane) getRoot();
    root.setCenter(centerStack);
    root.setRight(toolbarWidget);
  }

  /**
   * Creates the floating legend panel explaining the graph node types.
   *
   * @return A VBox containing the legend items.
   */
  private VBox createLegend() {
    VBox legend = new VBox(6);
    legend.getStyleClass().add("legend-panel");
    legend.getStyleClass().add("floating-panel"); // Inherit common floating panel styles
    legend.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    legend.setMinWidth(140);

    Label title = new Label("Legend");
    title.getStyleClass().add("legend-title");

    legend.getChildren().add(title);
    legend.getChildren().add(createLegendItem(new Circle(5, COLOR_RESOURCE), "Resource"));
    legend.getChildren().add(createLegendItem(new Rectangle(10, 8, COLOR_LITERAL), "Literal"));
    legend.getChildren().add(createLegendItem(new Circle(5, COLOR_BLANK_NODE), "Blank Node"));

    return legend;
  }

  /**
   * Helper to create a single row in the legend.
   *
   * @param shape The shape representing the node type.
   * @param text  The description of the node type.
   * @return An HBox containing the shape and label.
   */
  private HBox createLegendItem(Shape shape, String text) {
    HBox item = new HBox();
    item.getStyleClass().add("legend-item");

    Label label = new Label(text);
    label.getStyleClass().add("legend-item-label");

    item.getChildren().addAll(shape, label);
    return item;
  }

  /**
   * Sets up a fade animation for the given node on mouse hover.
   *
   * @param node The node to animate.
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

  /**
   * Configures the default toolbar buttons (Reset, Zoom In/Out).
   */
  private void setupToolbar() {
    List<ButtonConfig> buttons =
        List.of(
            ButtonFactory.resetLayout(graphWidget::resetLayout),
            ButtonFactory.zoomIn(graphWidget::zoomIn),
            ButtonFactory.zoomOut(graphWidget::zoomOut));

    toolbarWidget.setButtons(buttons);
  }

  /**
   * Updates the toolbar actions, allowing external controllers to add specific buttons.
   *
   * @param buttons The list of button configurations to display in the toolbar.
   */
  public void setToolbarActions(List<ButtonConfig> buttons) {
    toolbarWidget.setButtons(buttons);
  }

  /**
   * Returns the underlying graph widget.
   *
   * @return The GraphDisplayWidget instance.
   */
  public GraphDisplayWidget getGraphWidget() {
    return graphWidget;
  }
}