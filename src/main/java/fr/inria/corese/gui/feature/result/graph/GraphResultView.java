package fr.inria.corese.gui.feature.result.graph;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import fr.inria.corese.gui.core.factory.ButtonFactory;
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
 * View for displaying graph visualization results with a dedicated toolbar and floating legend.
 */
public class GraphResultView extends AbstractView {

  private static final String COMMON_STYLESHEET_PATH = "/styles/common.css";
  
  // Animation constants
  private static final double OPACITY_IDLE = 0.7;
  private static final double OPACITY_HOVER = 1.0;
  private static final int FADE_DURATION_MS = 200;

  private final GraphDisplayWidget graphWidget;
  private final ToolbarWidget toolbarWidget;

  public GraphResultView() {
    super(new BorderPane(), null);

    // Load common styles for floating panels
    CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);

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
    centerStack.getStyleClass().add("result-center-stack"); // Reuse stack alignment
    StackPane.setAlignment(legendBox, Pos.TOP_RIGHT);
    StackPane.setMargin(legendBox, new Insets(15, 20, 0, 0));

    BorderPane root = (BorderPane) getRoot();
    root.setCenter(centerStack);
    root.setRight(toolbarWidget);
  }

  private VBox createLegend() {
    VBox legend = new VBox(6);
    legend.getStyleClass().add("floating-panel");
    legend.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    legend.setMinWidth(140); // Consistent with ChoiceBox width
    
    Label title = new Label("Legend");
    title.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-fg-muted; -fx-font-size: 10px; -fx-padding: 0 0 2 0;");
    
    legend.getChildren().add(title);
    legend.getChildren().add(createLegendItem(new Circle(5, Color.web("#1f77b4")), "Resource"));
    legend.getChildren().add(createLegendItem(new Rectangle(10, 8, Color.web("#ff7f0e")), "Literal"));
    legend.getChildren().add(createLegendItem(new Circle(5, Color.web("#2ca02c")), "Blank Node"));
    
    return legend;
  }

  private HBox createLegendItem(Shape shape, String text) {
    HBox item = new HBox(10);
    item.setAlignment(Pos.CENTER_LEFT);
    
    Label label = new Label(text);
    label.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-default;");
    
    item.getChildren().addAll(shape, label);
    return item;
  }

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

  private void setupToolbar() {
    List<ButtonConfig> buttons =
        List.of(
            new ButtonConfig(ButtonIcon.RELOAD, "Reset Layout", graphWidget::resetLayout),
            ButtonFactory.zoomIn(graphWidget::zoomIn),
            ButtonFactory.zoomOut(graphWidget::zoomOut));

    toolbarWidget.setButtons(buttons);
  }

  public GraphDisplayWidget getGraphWidget() {
    return graphWidget;
  }
}