package fr.inria.corese.gui.component.tabstrip;

import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

/** View for a reusable custom tab strip. */
public class TabStripView extends HBox {

  private static final String STYLESHEET = "/css/components/tab-strip.css";
  private static final String STYLE_CLASS_TAB_STRIP = "editor-tab-strip";
  private static final String STYLE_CLASS_OVERFLOW_SHADOW = "editor-tab-overflow-shadow";
  private static final String STYLE_CLASS_OVERFLOW_SHADOW_LEFT = "left";
  private static final String STYLE_CLASS_OVERFLOW_SHADOW_RIGHT = "right";
  private static final String STYLE_CLASS_TAB_SCROLL = "editor-tab-scroll";
  private static final String STYLE_CLASS_TAB_TRACK = "editor-tab-track";
  private static final String STYLE_CLASS_TAB_ITEM = "editor-tab-item";
  private static final String STYLE_CLASS_TAB_ITEM_SELECTED = "selected";
  private static final String STYLE_CLASS_TAB_TITLE = "editor-tab-title";
  private static final String STYLE_CLASS_TAB_CLOSE = "editor-tab-close";
  private static final String STYLE_CLASS_TAB_DIRTY = "editor-tab-dirty";
  private static final String STYLE_CLASS_TAB_ICON = "editor-tab-icon";
  private static final double HEADER_HEIGHT = 44.0;
  private static final double LEADING_SLOT_WIDTH = 14.0;
  private static final double MODIFIED_CIRCLE_RADIUS = 4.0;
  private static final double CLOSE_AREA_WIDTH = 30.0;
  private static final double NO_ACTIONS_AREA_WIDTH = 0.0;
  private static final double SCROLL_STEP_PIXELS = 32.0;
  private static final double MIN_TAB_WIDTH = 180.0;
  private static final double SHADOW_WIDTH = 16.0;
  private static final double SCROLL_EPSILON = 0.001;

  private final ScrollPane scrollPane;
  private final HBox tabTrack;
  private final Region leftOverflowShadow;
  private final Region rightOverflowShadow;
  private int fullWildBaseWidth = 0;
  private int fullWildRemainder = 0;
  private List<Tab> currentTabs = List.of();
  private Tab currentSelectedTab;
  private Color currentAccentColor = Color.TRANSPARENT;
  private boolean currentShowCloseButton = true;
  private Consumer<Tab> currentOnSelect = tab -> {};
  private Consumer<Tab> currentOnCloseRequest = tab -> {};

  public TabStripView() {
    CssUtils.applyViewStyles(this, STYLESHEET);
    getStyleClass().add(STYLE_CLASS_TAB_STRIP);
    setMinWidth(0);
    HBox.setHgrow(this, Priority.ALWAYS);

    tabTrack = new HBox();
    tabTrack.getStyleClass().add(STYLE_CLASS_TAB_TRACK);
    tabTrack.setMinHeight(HEADER_HEIGHT);
    tabTrack.setPrefHeight(HEADER_HEIGHT);
    tabTrack.setMaxHeight(HEADER_HEIGHT);

    scrollPane = new ScrollPane(tabTrack);
    scrollPane.getStyleClass().add(STYLE_CLASS_TAB_SCROLL);
    scrollPane.setMinWidth(0);
    scrollPane.setFitToHeight(true);
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setPannable(true);
    scrollPane.addEventFilter(ScrollEvent.SCROLL, this::handleHorizontalScroll);
    scrollPane
        .viewportBoundsProperty()
        .addListener(
            (obs, oldBounds, newBounds) -> {
              renderInternal();
              updateOverflowState();
            });
    scrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> updateOverflowState());
    tabTrack.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> updateOverflowState());

    leftOverflowShadow = new Region();
    leftOverflowShadow.getStyleClass().addAll(STYLE_CLASS_OVERFLOW_SHADOW, STYLE_CLASS_OVERFLOW_SHADOW_LEFT);
    leftOverflowShadow.prefHeightProperty().bind(heightProperty());
    leftOverflowShadow.setMouseTransparent(true);
    leftOverflowShadow.setMinWidth(SHADOW_WIDTH);
    leftOverflowShadow.setPrefWidth(SHADOW_WIDTH);
    leftOverflowShadow.setMaxWidth(SHADOW_WIDTH);
    leftOverflowShadow.setVisible(false);

    rightOverflowShadow = new Region();
    rightOverflowShadow.getStyleClass().addAll(STYLE_CLASS_OVERFLOW_SHADOW, STYLE_CLASS_OVERFLOW_SHADOW_RIGHT);
    rightOverflowShadow.prefHeightProperty().bind(heightProperty());
    rightOverflowShadow.setMouseTransparent(true);
    rightOverflowShadow.setMinWidth(SHADOW_WIDTH);
    rightOverflowShadow.setPrefWidth(SHADOW_WIDTH);
    rightOverflowShadow.setMaxWidth(SHADOW_WIDTH);
    rightOverflowShadow.setVisible(false);

    StackPane scrollContainer = new StackPane(scrollPane, leftOverflowShadow, rightOverflowShadow);
    StackPane.setAlignment(leftOverflowShadow, Pos.CENTER_LEFT);
    StackPane.setAlignment(rightOverflowShadow, Pos.CENTER_RIGHT);
    getChildren().add(scrollContainer);
    HBox.setHgrow(scrollContainer, Priority.ALWAYS);
  }

  public void render(
      List<Tab> tabs,
      Tab selectedTab,
      Color accentColor,
      boolean showCloseButton,
      Consumer<Tab> onSelect,
      Consumer<Tab> onCloseRequest) {
    currentTabs = tabs;
    currentSelectedTab = selectedTab;
    currentAccentColor = accentColor;
    currentShowCloseButton = showCloseButton;
    currentOnSelect = onSelect;
    currentOnCloseRequest = onCloseRequest;
    renderInternal();
  }

  private void renderInternal() {
    tabTrack.getChildren().clear();
    double tabWidth = computeTabWidth(currentTabs.size());
    boolean fullWild = !Double.isNaN(tabWidth);
    if (fullWild) {
      scrollPane.setHvalue(0);
    }

    for (int i = 0; i < currentTabs.size(); i++) {
      Tab tab = currentTabs.get(i);
      HBox tabNode =
          createTabNode(
              tab,
              tab == currentSelectedTab,
              currentAccentColor,
              currentShowCloseButton,
              currentOnSelect,
              currentOnCloseRequest);
      double effectiveWidth = tabWidth;
      if (fullWild) {
        effectiveWidth = fullWildBaseWidth + (i < fullWildRemainder ? 1.0 : 0.0);
      }
      applyTabSizing(tabNode, effectiveWidth, fullWild);
      tabTrack.getChildren().add(tabNode);
    }
    updateOverflowState();
  }

  private HBox createTabNode(
      Tab tab,
      boolean selected,
      Color accentColor,
      boolean showCloseButton,
      Consumer<Tab> onSelect,
      Consumer<Tab> onCloseRequest) {
    HBox item = new HBox();
    item.getStyleClass().add(STYLE_CLASS_TAB_ITEM);
    if (selected) {
      item.getStyleClass().add(STYLE_CLASS_TAB_ITEM_SELECTED);
    }
    item.setDisable(tab.isDisable());
    item.setAlignment(Pos.CENTER);
    item.setMaxWidth(Region.USE_COMPUTED_SIZE);

    Label title = new Label();
    title.textProperty().bind(tab.textProperty());
    title.getStyleClass().add(STYLE_CLASS_TAB_TITLE);

    HBox titleWithGraphic = new HBox(6);
    titleWithGraphic.setAlignment(Pos.CENTER);
    Circle dirtyMarker = createDirtyMarker(tab, accentColor);
    FontIcon icon = createTabIcon(tab);
    StackPane leadingSlot = new StackPane();
    leadingSlot.setMinWidth(LEADING_SLOT_WIDTH);
    leadingSlot.setPrefWidth(LEADING_SLOT_WIDTH);
    leadingSlot.setMaxWidth(LEADING_SLOT_WIDTH);
    if (dirtyMarker != null) {
      leadingSlot.getChildren().add(dirtyMarker);
    } else if (icon != null) {
      leadingSlot.getChildren().add(icon);
    }
    titleWithGraphic.getChildren().add(leadingSlot);
    titleWithGraphic.getChildren().add(title);

    StackPane centerLayer = new StackPane(titleWithGraphic);
    centerLayer.setMinWidth(0);
    HBox.setHgrow(centerLayer, Priority.ALWAYS);

    HBox rightControls = new HBox(6);
    if (showCloseButton) {
      Button close = new Button("×");
      close.getStyleClass().add(STYLE_CLASS_TAB_CLOSE);
      close.setDisable(tab.isDisable());
      close.setOnAction(
          e -> {
            e.consume();
            onCloseRequest.accept(tab);
          });
      rightControls.getChildren().add(close);
    }
    rightControls.setAlignment(Pos.CENTER_RIGHT);
    double sideAreaWidth = showCloseButton ? CLOSE_AREA_WIDTH : NO_ACTIONS_AREA_WIDTH;
    rightControls.setMinWidth(sideAreaWidth);
    rightControls.setPrefWidth(sideAreaWidth);

    Region leftPad = new Region();
    leftPad.setMinWidth(sideAreaWidth);
    leftPad.setPrefWidth(sideAreaWidth);

    StackPane controlsLayer = new StackPane(rightControls);
    controlsLayer.setMinWidth(sideAreaWidth);
    controlsLayer.setPrefWidth(sideAreaWidth);

    item.setOnMouseClicked(
        event -> {
          if (tab.isDisable()) {
            return;
          }
          if (event.getButton() == MouseButton.PRIMARY) {
            onSelect.accept(tab);
            return;
          }
          if (event.getButton() == MouseButton.MIDDLE && showCloseButton && tab.isClosable()) {
            onCloseRequest.accept(tab);
            event.consume();
          }
        });

    item.getChildren().addAll(leftPad, centerLayer, controlsLayer);
    return item;
  }

  private void applyTabSizing(HBox tabNode, double tabWidth, boolean fullWild) {
    if (fullWild) {
      tabNode.setMinWidth(tabWidth);
      tabNode.setPrefWidth(tabWidth);
      tabNode.setMaxWidth(tabWidth);
      HBox.setHgrow(tabNode, Priority.NEVER);
      return;
    }

    tabNode.setMinWidth(MIN_TAB_WIDTH);
    tabNode.setPrefWidth(MIN_TAB_WIDTH);
    tabNode.setMaxWidth(Region.USE_PREF_SIZE);
    HBox.setHgrow(tabNode, Priority.NEVER);
  }

  private double computeTabWidth(int tabCount) {
    fullWildBaseWidth = 0;
    fullWildRemainder = 0;
    if (tabCount <= 0) {
      return Double.NaN;
    }

    double viewportWidth = scrollPane.getViewportBounds().getWidth();
    if (viewportWidth <= 0) {
      viewportWidth = scrollPane.getWidth();
    }
    if (viewportWidth <= 0) {
      viewportWidth = getWidth();
    }
    if (viewportWidth <= 0) {
      return Double.NaN;
    }

    // In JavaFX Region sizing, border is included in the node width (not added on top),
    // so we must use the full viewport width here.
    int availablePixels = Math.max(0, (int) Math.round(viewportWidth));
    fullWildBaseWidth = availablePixels / tabCount;
    fullWildRemainder = availablePixels % tabCount;
    double candidate = fullWildBaseWidth;
    return candidate >= MIN_TAB_WIDTH ? candidate : Double.NaN;
  }

  private void handleHorizontalScroll(ScrollEvent event) {
    double viewportWidth = scrollPane.getViewportBounds().getWidth();
    double contentWidth = tabTrack.getLayoutBounds().getWidth();
    double scrollableWidth = contentWidth - viewportWidth;
    if (scrollableWidth <= 0) {
      scrollPane.setHvalue(0);
      scrollPane.setVvalue(0);
      event.consume();
      return;
    }

    double delta = Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY()) ? -event.getDeltaX() : -event.getDeltaY();
    if (delta == 0) {
      return;
    }

    double currentPixels = scrollPane.getHvalue() * scrollableWidth;
    double nextPixels = clamp(currentPixels + (Math.signum(delta) * SCROLL_STEP_PIXELS), 0, scrollableWidth);
    scrollPane.setHvalue(nextPixels / scrollableWidth);
    scrollPane.setVvalue(0);
    event.consume();
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private void updateOverflowState() {
    double scrollableWidth = getScrollableWidth();
    if (scrollableWidth <= 0.5) {
      leftOverflowShadow.setVisible(false);
      rightOverflowShadow.setVisible(false);
      return;
    }

    boolean canScrollLeft = scrollPane.getHvalue() > SCROLL_EPSILON;
    boolean canScrollRight = scrollPane.getHvalue() < (1.0 - SCROLL_EPSILON);
    leftOverflowShadow.setVisible(canScrollLeft);
    rightOverflowShadow.setVisible(canScrollRight);
  }

  private double getScrollableWidth() {
    return tabTrack.getLayoutBounds().getWidth() - scrollPane.getViewportBounds().getWidth();
  }

  private Circle createDirtyMarker(Tab tab, Color accentColor) {
    if (!(tab.getGraphic() instanceof Circle)) {
      return null;
    }
    Circle dirty = new Circle(MODIFIED_CIRCLE_RADIUS);
    dirty.getStyleClass().add(STYLE_CLASS_TAB_DIRTY);
    dirty.setFill(accentColor);
    return dirty;
  }

  private FontIcon createTabIcon(Tab tab) {
    if (!(tab.getGraphic() instanceof FontIcon sourceIcon)) {
      return null;
    }
    FontIcon icon = new FontIcon(sourceIcon.getIconCode());
    icon.setIconSize(sourceIcon.getIconSize());
    icon.setOpacity(tab.isDisable() ? 0.45 : 1.0);
    icon.getStyleClass().add(STYLE_CLASS_TAB_ICON);
    icon.getStyleClass().addAll(sourceIcon.getStyleClass());
    return icon;
  }
}
