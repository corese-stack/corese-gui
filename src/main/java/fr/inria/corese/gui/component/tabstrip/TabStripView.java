package fr.inria.corese.gui.component.tabstrip;

import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.util.Duration;
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
  private static final String STYLE_CLASS_TAB_BASELINE = "editor-tab-baseline";
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
  private static final double WIDTH_EPSILON = 0.5;
  private static final double REVEAL_RIGHT_BIAS = 1.0;

  private static final Duration WIDTH_ANIMATION_DURATION = Duration.millis(180);
  private static final Duration CLOSE_TAB_DURATION = Duration.millis(130);
  private static final Duration CLOSE_AFTER_PAUSE = Duration.millis(120);
  private static final Duration OVERFLOW_SHADOW_SHOW_DELAY = Duration.millis(28);
  private static final Duration OVERFLOW_SHADOW_FADE_IN_DURATION = Duration.millis(180);
  private static final Duration OVERFLOW_SHADOW_FADE_OUT_DURATION = Duration.millis(220);
  private static final Duration AUTO_SCROLL_DURATION = Duration.millis(220);

  private final boolean animationsEnabled;
  private final ScrollPane scrollPane;
  private final HBox tabTrack;
  private final Region baseline;
  private final Region leftOverflowShadow;
  private final Region rightOverflowShadow;
  private final PauseTransition leftOverflowShadowShowDelay;
  private final PauseTransition rightOverflowShadowShowDelay;
  private final FadeTransition leftOverflowShadowFadeIn;
  private final FadeTransition leftOverflowShadowFadeOut;
  private final FadeTransition rightOverflowShadowFadeIn;
  private final FadeTransition rightOverflowShadowFadeOut;

  private final Map<Tab, TabNode> tabNodes = new LinkedHashMap<>();
  private final Map<HBox, Timeline> widthAnimations = new HashMap<>();

  private boolean selectedTabEnsureScheduled = false;
  private boolean forceRightRevealPending = false;
  private boolean forceRightRevealScheduled = false;
  private int fullWildBaseWidth = 0;
  private int fullWildRemainder = 0;
  private int lastRenderedTabCount = 0;
  private boolean firstRenderDone = false;
  private boolean deferredRenderScheduled = false;
  private boolean closeAnimationInProgress = false;
  private boolean noOverflowExpected = false;
  private Timeline autoScrollAnimation;
  private double autoScrollTargetHValue = Double.NaN;

  private List<Tab> currentTabs = List.of();
  private Tab currentSelectedTab;
  private Color currentAccentColor = Color.TRANSPARENT;
  private boolean currentShowCloseButton = true;
  private boolean currentAnimateWidthChanges = true;
  private Consumer<Tab> currentOnSelect = tab -> {};
  private Consumer<Tab> currentOnCloseRequest = tab -> {};

  public TabStripView() {
    this(true);
  }

  public TabStripView(boolean animationsEnabled) {
    this.animationsEnabled = animationsEnabled;
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
    scrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> updateOverflowState());
    tabTrack.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> updateOverflowState());
    scrollPane
        .viewportBoundsProperty()
        .addListener(
            (obs, oldBounds, newBounds) -> {
              updateOverflowState();
              if (newBounds == null || currentTabs.isEmpty() || closeAnimationInProgress) {
                return;
              }
              double oldWidth = oldBounds == null ? -1.0 : oldBounds.getWidth();
              if (Math.abs(newBounds.getWidth() - oldWidth) > WIDTH_EPSILON && !deferredRenderScheduled) {
                scheduleDeferredRender();
              }
            });

    baseline = new Region();
    baseline.getStyleClass().add(STYLE_CLASS_TAB_BASELINE);
    baseline.setMouseTransparent(true);
    baseline.setMinHeight(1);
    baseline.setPrefHeight(1);
    baseline.setMaxHeight(1);

    leftOverflowShadow = new Region();
    leftOverflowShadow.getStyleClass().addAll(STYLE_CLASS_OVERFLOW_SHADOW, STYLE_CLASS_OVERFLOW_SHADOW_LEFT);
    leftOverflowShadow.prefHeightProperty().bind(heightProperty());
    leftOverflowShadow.setMouseTransparent(true);
    leftOverflowShadow.setMinWidth(SHADOW_WIDTH);
    leftOverflowShadow.setPrefWidth(SHADOW_WIDTH);
    leftOverflowShadow.setMaxWidth(SHADOW_WIDTH);
    leftOverflowShadow.setOpacity(0.0);
    leftOverflowShadow.setVisible(false);
    leftOverflowShadowFadeIn = createOverflowFade(leftOverflowShadow, OVERFLOW_SHADOW_FADE_IN_DURATION, 1.0);
    leftOverflowShadowFadeOut = createOverflowFade(leftOverflowShadow, OVERFLOW_SHADOW_FADE_OUT_DURATION, 0.0);
    leftOverflowShadowFadeOut.setOnFinished(
        e -> {
          if (leftOverflowShadow.getOpacity() <= SCROLL_EPSILON) {
            leftOverflowShadow.setVisible(false);
          }
        });
    leftOverflowShadowShowDelay = new PauseTransition(OVERFLOW_SHADOW_SHOW_DELAY);
    leftOverflowShadowShowDelay.setOnFinished(
        e -> showOverflowShadow(leftOverflowShadow, leftOverflowShadowFadeIn, leftOverflowShadowFadeOut));

    rightOverflowShadow = new Region();
    rightOverflowShadow.getStyleClass().addAll(STYLE_CLASS_OVERFLOW_SHADOW, STYLE_CLASS_OVERFLOW_SHADOW_RIGHT);
    rightOverflowShadow.prefHeightProperty().bind(heightProperty());
    rightOverflowShadow.setMouseTransparent(true);
    rightOverflowShadow.setMinWidth(SHADOW_WIDTH);
    rightOverflowShadow.setPrefWidth(SHADOW_WIDTH);
    rightOverflowShadow.setMaxWidth(SHADOW_WIDTH);
    rightOverflowShadow.setOpacity(0.0);
    rightOverflowShadow.setVisible(false);
    rightOverflowShadowFadeIn =
        createOverflowFade(rightOverflowShadow, OVERFLOW_SHADOW_FADE_IN_DURATION, 1.0);
    rightOverflowShadowFadeOut =
        createOverflowFade(rightOverflowShadow, OVERFLOW_SHADOW_FADE_OUT_DURATION, 0.0);
    rightOverflowShadowFadeOut.setOnFinished(
        e -> {
          if (rightOverflowShadow.getOpacity() <= SCROLL_EPSILON) {
            rightOverflowShadow.setVisible(false);
          }
        });
    rightOverflowShadowShowDelay = new PauseTransition(OVERFLOW_SHADOW_SHOW_DELAY);
    rightOverflowShadowShowDelay.setOnFinished(
        e -> showOverflowShadow(rightOverflowShadow, rightOverflowShadowFadeIn, rightOverflowShadowFadeOut));

    StackPane scrollContainer = new StackPane(scrollPane, baseline, leftOverflowShadow, rightOverflowShadow);
    StackPane.setAlignment(baseline, Pos.BOTTOM_CENTER);
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
      boolean animateWidthChanges,
      Consumer<Tab> onSelect,
      Consumer<Tab> onCloseRequest) {
    currentTabs = tabs;
    currentSelectedTab = selectedTab;
    currentAccentColor = accentColor;
    currentShowCloseButton = showCloseButton;
    currentAnimateWidthChanges = animateWidthChanges;
    currentOnSelect = onSelect;
    currentOnCloseRequest = onCloseRequest;
    renderInternal();
  }

  private void renderInternal() {
    if (closeAnimationInProgress) {
      return;
    }
    boolean tabCountIncreased = currentTabs.size() > lastRenderedTabCount;
    if (tabCountIncreased) {
      forceRightRevealPending = true;
    }

    double tabWidth = computeTabWidth(currentTabs.size());
    boolean fullWild = !Double.isNaN(tabWidth);
    noOverflowExpected = fullWild;

    if (!fullWild && !deferredRenderScheduled && !currentTabs.isEmpty()) {
      double viewportWidth = resolveViewportWidth();
      boolean firstTabShouldBeFullWild =
          currentTabs.size() == 1 && viewportWidth > (MIN_TAB_WIDTH + WIDTH_EPSILON);
      if (viewportWidth <= 0 || firstTabShouldBeFullWild) {
        // At startup, viewport metrics can arrive late. Keep retrying until the first tab can
        // expand to full width, without relying on a fixed retry budget.
        scheduleDeferredRender();
      }
    }

    if (!fullWild
        && currentTabs.size() == 1
        && resolveViewportWidth() > (MIN_TAB_WIDTH + WIDTH_EPSILON)
        && !deferredRenderScheduled) {
      scheduleDeferredRender();
    }

    List<Tab> staleTabs = collectStaleTabs();
    if (!staleTabs.isEmpty()) {
      startCloseAnimation(staleTabs);
      return;
    }

    double previousHValue = scrollPane.getHvalue();
    List<javafx.scene.Node> orderedChildren = new ArrayList<>();
    for (int i = 0; i < currentTabs.size(); i++) {
      Tab tab = currentTabs.get(i);
      TabNode tabNode = tabNodes.computeIfAbsent(tab, this::createTabNode);
      boolean isNew = tabNode.item.getParent() == null;
      updateTabNode(tabNode, tab, tab == currentSelectedTab, currentAccentColor, currentShowCloseButton);

      double targetWidth = fullWild ? fullWildBaseWidth + (i < fullWildRemainder ? 1.0 : 0.0) : MIN_TAB_WIDTH;
      animateTabWidth(
          tabNode.item,
          targetWidth,
          isNew,
          currentAnimateWidthChanges);
      orderedChildren.add(tabNode.item);
    }

    boolean trackChildrenReplaced = replaceTrackChildrenIfNeeded(orderedChildren);
    if (fullWild) {
      selectedTabEnsureScheduled = false;
      forceRightRevealPending = false;
      setScrollHValue(0.0, false);
    } else {
      if (trackChildrenReplaced) {
        maybeRestoreScrollFromHValue(previousHValue);
        Platform.runLater(() -> maybeRestoreScrollFromHValue(previousHValue));
      }
      if (forceRightRevealPending) {
        if (widthAnimations.isEmpty()) {
          scheduleForceRightReveal();
        }
      } else {
        scheduleEnsureSelectedTabVisible();
        if (!widthAnimations.isEmpty()) {
          Platform.runLater(this::scheduleEnsureSelectedTabVisible);
        }
      }
    }
    updateOverflowState();
    lastRenderedTabCount = currentTabs.size();
    firstRenderDone = true;
  }

  private void ensureSelectedTabVisible() {
    if (currentSelectedTab == null) {
      return;
    }
    TabNode selectedNode = tabNodes.get(currentSelectedTab);
    if (selectedNode == null || selectedNode.item.getParent() == null) {
      return;
    }

    double scrollableWidth = getScrollableWidth();
    if (scrollableWidth <= 0.5) {
      forceRightRevealPending = false;
      return;
    }

    if (forceRightRevealPending && isSelectedTabLast()) {
      setScrollHValue(1.0, true);
      forceRightRevealPending = false;
      return;
    }

    double viewportWidth = scrollPane.getViewportBounds().getWidth();
    if (viewportWidth <= 0) {
      return;
    }

    double currentLeft = scrollPane.getHvalue() * scrollableWidth;
    double effectiveLeft = currentLeft;
    double effectiveRight = currentLeft + viewportWidth;
    double selectedLeft = selectedNode.item.getBoundsInParent().getMinX();
    double selectedRight = selectedNode.item.getBoundsInParent().getMaxX();

    double targetLeft = currentLeft;
    if (selectedLeft < effectiveLeft) {
      targetLeft = selectedLeft;
    } else if (selectedRight > effectiveRight) {
      targetLeft = selectedRight - viewportWidth + REVEAL_RIGHT_BIAS;
    } else {
      return;
    }

    double clampedLeft = clamp(targetLeft, 0.0, scrollableWidth);
    setScrollHValue(clampedLeft / scrollableWidth, true);
  }

  private void scheduleEnsureSelectedTabVisible() {
    if (selectedTabEnsureScheduled) {
      return;
    }
    selectedTabEnsureScheduled = true;
    Platform.runLater(
        () -> {
          selectedTabEnsureScheduled = false;
          ensureSelectedTabVisible();
        });
  }

  private void setScrollHValue(double targetHValue, boolean animated) {
    double clamped = clamp(targetHValue, 0.0, 1.0);
    if (!firstRenderDone) {
      stopAutoScrollAnimation();
      scrollPane.setHvalue(clamped);
      return;
    }
    if (!animated || !animationsEnabled) {
      stopAutoScrollAnimation();
      if (Math.abs(scrollPane.getHvalue() - clamped) <= SCROLL_EPSILON) {
        return;
      }
      scrollPane.setHvalue(clamped);
      return;
    }

    if (autoScrollAnimation != null) {
      if (Math.abs(autoScrollTargetHValue - clamped) <= SCROLL_EPSILON
          && autoScrollAnimation.getStatus() == Animation.Status.RUNNING) {
        return;
      }
      stopAutoScrollAnimation();
    }

    if (Math.abs(scrollPane.getHvalue() - clamped) <= SCROLL_EPSILON) {
      return;
    }
    autoScrollTargetHValue = clamped;
    autoScrollAnimation =
        new Timeline(
            new KeyFrame(
                AUTO_SCROLL_DURATION,
                new KeyValue(scrollPane.hvalueProperty(), clamped, Interpolator.EASE_BOTH)));
    autoScrollAnimation.setOnFinished(
        e -> {
          autoScrollAnimation = null;
          autoScrollTargetHValue = Double.NaN;
          updateOverflowState();
        });
    autoScrollAnimation.play();
    updateOverflowState();
  }

  private void stopAutoScrollAnimation() {
    if (autoScrollAnimation == null) {
      return;
    }
    autoScrollAnimation.stop();
    autoScrollAnimation = null;
    autoScrollTargetHValue = Double.NaN;
  }

  private boolean replaceTrackChildrenIfNeeded(List<javafx.scene.Node> orderedChildren) {
    javafx.collections.ObservableList<javafx.scene.Node> currentChildren = tabTrack.getChildren();
    if (currentChildren.size() == orderedChildren.size()) {
      boolean identicalOrder = true;
      for (int i = 0; i < orderedChildren.size(); i++) {
        if (currentChildren.get(i) != orderedChildren.get(i)) {
          identicalOrder = false;
          break;
        }
      }
      if (identicalOrder) {
        return false;
      }
    }
    currentChildren.setAll(orderedChildren);
    return true;
  }

  private void maybeRestoreScrollFromHValue(double hValue) {
    if (hValue <= SCROLL_EPSILON || scrollPane.getHvalue() > SCROLL_EPSILON) {
      return;
    }
    scrollPane.setHvalue(clamp(hValue, 0.0, 1.0));
  }

  private List<Tab> collectStaleTabs() {
    List<Tab> stale = new ArrayList<>();
    for (Tab tab : tabNodes.keySet()) {
      if (!currentTabs.contains(tab)) {
        stale.add(tab);
      }
    }
    return stale;
  }

  private void startCloseAnimation(List<Tab> staleTabs) {
    if (!animationsEnabled || !firstRenderDone || tabTrack.getChildren().isEmpty()) {
      removeTabsImmediately(staleTabs);
      renderInternal();
      return;
    }

    closeAnimationInProgress = true;
    ParallelTransition parallelClose = new ParallelTransition();

    for (Tab tab : staleTabs) {
      TabNode node = tabNodes.get(tab);
      if (node == null) {
        continue;
      }

      Timeline existing = widthAnimations.remove(node.item);
      if (existing != null) {
        existing.stop();
      }

      node.item.setMouseTransparent(true);
      double fromWidth = getCurrentNodeWidth(node.item);
      Timeline collapse =
          new Timeline(
              new KeyFrame(
                  CLOSE_TAB_DURATION,
                  new KeyValue(node.item.minWidthProperty(), 0.0, Interpolator.EASE_BOTH),
                  new KeyValue(node.item.prefWidthProperty(), 0.0, Interpolator.EASE_BOTH),
                  new KeyValue(node.item.maxWidthProperty(), 0.0, Interpolator.EASE_BOTH)));
      if (fromWidth <= WIDTH_EPSILON) {
        setTabWidth(node.item, 0.0);
      } else {
        setTabWidth(node.item, fromWidth);
      }

      FadeTransition fade = new FadeTransition(CLOSE_TAB_DURATION, node.item);
      fade.setToValue(0.0);

      ParallelTransition perTab = new ParallelTransition(collapse, fade);
      parallelClose.getChildren().add(perTab);
    }

    SequentialTransition sequence = new SequentialTransition(parallelClose, new PauseTransition(CLOSE_AFTER_PAUSE));
    sequence.setOnFinished(
        e -> {
          removeTabsImmediately(staleTabs);
          closeAnimationInProgress = false;
          renderInternal();
        });
    sequence.play();
  }

  private double getCurrentNodeWidth(HBox node) {
    double width = node.getPrefWidth();
    if (width > 0) {
      return width;
    }
    width = node.getWidth();
    if (width > 0) {
      return width;
    }
    width = node.getLayoutBounds().getWidth();
    return Math.max(width, 0.0);
  }

  private void removeTabsImmediately(List<Tab> tabs) {
    for (Tab tab : tabs) {
      TabNode node = tabNodes.remove(tab);
      if (node == null) {
        continue;
      }
      Timeline timeline = widthAnimations.remove(node.item);
      if (timeline != null) {
        timeline.stop();
      }
      tabTrack.getChildren().remove(node.item);
    }
  }

  private TabNode createTabNode(Tab tab) {
    HBox item = new HBox();
    item.getStyleClass().add(STYLE_CLASS_TAB_ITEM);
    item.setAlignment(Pos.CENTER);
    item.setMaxWidth(Region.USE_COMPUTED_SIZE);

    Label title = new Label();
    title.textProperty().bind(tab.textProperty());
    title.getStyleClass().add(STYLE_CLASS_TAB_TITLE);

    StackPane leadingSlot = new StackPane();
    leadingSlot.setMinWidth(LEADING_SLOT_WIDTH);
    leadingSlot.setPrefWidth(LEADING_SLOT_WIDTH);
    leadingSlot.setMaxWidth(LEADING_SLOT_WIDTH);

    HBox titleWithGraphic = new HBox(6, leadingSlot, title);
    titleWithGraphic.setAlignment(Pos.CENTER);

    StackPane centerLayer = new StackPane(titleWithGraphic);
    centerLayer.setMinWidth(0);
    HBox.setHgrow(centerLayer, Priority.ALWAYS);

    HBox rightControls = new HBox(6);
    rightControls.setAlignment(Pos.CENTER_RIGHT);

    StackPane controlsLayer = new StackPane(rightControls);

    Region leftPad = new Region();

    item.getChildren().addAll(leftPad, centerLayer, controlsLayer);
    return new TabNode(item, leadingSlot, rightControls, controlsLayer, leftPad);
  }

  private void updateTabNode(
      TabNode tabNode, Tab tab, boolean selected, Color accentColor, boolean showCloseButton) {
    if (selected) {
      if (!tabNode.item.getStyleClass().contains(STYLE_CLASS_TAB_ITEM_SELECTED)) {
        tabNode.item.getStyleClass().add(STYLE_CLASS_TAB_ITEM_SELECTED);
      }
    } else {
      tabNode.item.getStyleClass().remove(STYLE_CLASS_TAB_ITEM_SELECTED);
    }

    tabNode.item.setDisable(tab.isDisable());
    tabNode.leadingSlot.getChildren().setAll(createLeadingGraphic(tab, accentColor));
    updateCloseControl(tabNode.rightControls, tab, showCloseButton);

    double sideAreaWidth = showCloseButton ? CLOSE_AREA_WIDTH : NO_ACTIONS_AREA_WIDTH;
    tabNode.rightControls.setMinWidth(sideAreaWidth);
    tabNode.rightControls.setPrefWidth(sideAreaWidth);
    tabNode.controlsLayer.setMinWidth(sideAreaWidth);
    tabNode.controlsLayer.setPrefWidth(sideAreaWidth);
    tabNode.leftPad.setMinWidth(sideAreaWidth);
    tabNode.leftPad.setPrefWidth(sideAreaWidth);

    tabNode.item.setOnMouseClicked(
        event -> {
          if (tab.isDisable()) {
            return;
          }
          if (event.getButton() == MouseButton.PRIMARY) {
            currentOnSelect.accept(tab);
            return;
          }
          if (event.getButton() == MouseButton.MIDDLE && showCloseButton && tab.isClosable()) {
            currentOnCloseRequest.accept(tab);
            event.consume();
          }
        });
  }

  private javafx.scene.Node createLeadingGraphic(Tab tab, Color accentColor) {
    if (tab.getGraphic() instanceof Circle) {
      Circle dirty = new Circle(MODIFIED_CIRCLE_RADIUS);
      dirty.getStyleClass().add(STYLE_CLASS_TAB_DIRTY);
      dirty.setFill(accentColor);
      return dirty;
    }

    if (tab.getGraphic() instanceof FontIcon sourceIcon) {
      FontIcon icon = new FontIcon(sourceIcon.getIconCode());
      icon.setIconSize(sourceIcon.getIconSize());
      icon.setOpacity(tab.isDisable() ? 0.45 : 1.0);
      icon.getStyleClass().add(STYLE_CLASS_TAB_ICON);
      icon.getStyleClass().addAll(sourceIcon.getStyleClass());
      return icon;
    }

    return new Region();
  }

  private void updateCloseControl(HBox rightControls, Tab tab, boolean showCloseButton) {
    rightControls.getChildren().clear();
    if (!showCloseButton) {
      return;
    }

    Button close = new Button("×");
    close.getStyleClass().add(STYLE_CLASS_TAB_CLOSE);
    close.setDisable(tab.isDisable());
    close.setOnAction(
        e -> {
          e.consume();
          currentOnCloseRequest.accept(tab);
        });
    rightControls.getChildren().add(close);
  }

  private void animateTabWidth(
      HBox tabNode, double targetWidth, boolean isNew, boolean animateWidthChanges) {
    double currentWidth = tabNode.getPrefWidth();
    if (currentWidth <= 0) {
      currentWidth = targetWidth;
    }

    if (!firstRenderDone) {
      setTabWidth(tabNode, targetWidth);
      return;
    }
    if (!animationsEnabled) {
      setTabWidth(tabNode, targetWidth);
      tabNode.setOpacity(1.0);
      tabNode.setScaleX(1.0);
      tabNode.setScaleY(1.0);
      Timeline existing = widthAnimations.remove(tabNode);
      if (existing != null) {
        existing.stop();
      }
      return;
    }
    if (!animateWidthChanges && !isNew) {
      setTabWidth(tabNode, targetWidth);
      Timeline existing = widthAnimations.remove(tabNode);
      if (existing != null) {
        existing.stop();
      }
      return;
    }

    if (isNew) {
      Timeline existing = widthAnimations.remove(tabNode);
      if (existing != null) {
        existing.stop();
      }
      // Keep new-tab layout deterministic: no opening animation.
      // This avoids intermediate widths that can produce mid-strip reveal.
      setTabWidth(tabNode, targetWidth);
      tabNode.setOpacity(1.0);
      tabNode.setScaleX(1.0);
      tabNode.setScaleY(1.0);
      return;
    }

    if (Math.abs(currentWidth - targetWidth) <= WIDTH_EPSILON) {
      setTabWidth(tabNode, targetWidth);
      return;
    }

    Timeline existing = widthAnimations.remove(tabNode);
    if (existing != null) {
      existing.stop();
    }

    Timeline timeline =
        new Timeline(
            new KeyFrame(
                WIDTH_ANIMATION_DURATION,
                new KeyValue(tabNode.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(tabNode.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(tabNode.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH)));
    widthAnimations.put(tabNode, timeline);
    timeline.setOnFinished(
        e -> {
          widthAnimations.remove(tabNode);
          if (forceRightRevealPending && widthAnimations.isEmpty()) {
            scheduleForceRightReveal();
          }
        });
    timeline.play();
  }

  private void setTabWidth(HBox tabNode, double width) {
    tabNode.setMinWidth(width);
    tabNode.setPrefWidth(width);
    tabNode.setMaxWidth(width);
    HBox.setHgrow(tabNode, Priority.NEVER);
  }

  private void scheduleDeferredRender() {
    deferredRenderScheduled = true;
    Platform.runLater(
        () -> {
          deferredRenderScheduled = false;
          renderInternal();
        });
  }

  private double computeTabWidth(int tabCount) {
    fullWildBaseWidth = 0;
    fullWildRemainder = 0;
    if (tabCount <= 0) {
      return Double.NaN;
    }

    double viewportWidth = resolveViewportWidth();
    if (viewportWidth <= 0) {
      return Double.NaN;
    }

    int availablePixels = Math.max(0, (int) Math.floor(viewportWidth));
    fullWildBaseWidth = availablePixels / tabCount;
    fullWildRemainder = availablePixels % tabCount;
    return fullWildBaseWidth >= MIN_TAB_WIDTH ? fullWildBaseWidth : Double.NaN;
  }

  private double resolveViewportWidth() {
    double viewportWidth = scrollPane.getViewportBounds().getWidth();
    if (viewportWidth <= 0) {
      viewportWidth = scrollPane.getWidth();
    }
    if (viewportWidth <= 0) {
      viewportWidth = getWidth();
    }
    return viewportWidth;
  }

  private void handleHorizontalScroll(ScrollEvent event) {
    stopAutoScrollAnimation();
    double scrollableWidth = getScrollableWidth();
    if (scrollableWidth <= 0) {
      scrollPane.setHvalue(0);
      scrollPane.setVvalue(0);
      event.consume();
      return;
    }

    double delta =
        Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY()) ? -event.getDeltaX() : -event.getDeltaY();
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

  private boolean isSelectedTabLast() {
    return !currentTabs.isEmpty()
        && currentSelectedTab != null
        && currentSelectedTab == currentTabs.get(currentTabs.size() - 1);
  }

  private void scheduleForceRightReveal() {
    if (forceRightRevealScheduled) {
      return;
    }
    forceRightRevealScheduled = true;
    // Wait until track/viewport bounds are stable, then force rightmost position.
    Platform.runLater(
        () ->
            Platform.runLater(
                () -> {
                  forceRightRevealScheduled = false;
                  if (!forceRightRevealPending || currentTabs.isEmpty()) {
                    return;
                  }
                  setScrollHValue(1.0, true);
                  forceRightRevealPending = false;
                }));
  }

  private void updateOverflowState() {
    if (noOverflowExpected) {
      hideOverflowShadow(
          leftOverflowShadow, leftOverflowShadowShowDelay, leftOverflowShadowFadeIn, leftOverflowShadowFadeOut);
      hideOverflowShadow(
          rightOverflowShadow,
          rightOverflowShadowShowDelay,
          rightOverflowShadowFadeIn,
          rightOverflowShadowFadeOut);
      return;
    }

    double scrollableWidth = getScrollableWidth();
    if (scrollableWidth <= 0.5) {
      hideOverflowShadow(
          leftOverflowShadow, leftOverflowShadowShowDelay, leftOverflowShadowFadeIn, leftOverflowShadowFadeOut);
      hideOverflowShadow(
          rightOverflowShadow,
          rightOverflowShadowShowDelay,
          rightOverflowShadowFadeIn,
          rightOverflowShadowFadeOut);
      return;
    }

    boolean canScrollLeft = scrollPane.getHvalue() > SCROLL_EPSILON;
    boolean canScrollRight =
        !isAutoScrollingToRightEdge() && scrollPane.getHvalue() < (1.0 - SCROLL_EPSILON);
    updateOverflowShadow(
        leftOverflowShadow,
        leftOverflowShadowShowDelay,
        leftOverflowShadowFadeIn,
        leftOverflowShadowFadeOut,
        canScrollLeft);
    updateOverflowShadow(
        rightOverflowShadow,
        rightOverflowShadowShowDelay,
        rightOverflowShadowFadeIn,
        rightOverflowShadowFadeOut,
        canScrollRight);
  }

  private boolean isAutoScrollingToRightEdge() {
    return autoScrollAnimation != null
        && autoScrollAnimation.getStatus() == Animation.Status.RUNNING
        && autoScrollTargetHValue >= (1.0 - SCROLL_EPSILON);
  }

  private void updateOverflowShadow(
      Region shadow,
      PauseTransition showDelay,
      FadeTransition fadeIn,
      FadeTransition fadeOut,
      boolean shouldBeVisible) {
    if (shouldBeVisible) {
      if (!shadow.isVisible() && showDelay.getStatus() != Animation.Status.RUNNING) {
        showDelay.playFromStart();
      }
      return;
    }
    hideOverflowShadow(shadow, showDelay, fadeIn, fadeOut);
  }

  private void hideOverflowShadow(
      Region shadow, PauseTransition showDelay, FadeTransition fadeIn, FadeTransition fadeOut) {
    showDelay.stop();
    fadeIn.stop();
    if (!shadow.isVisible() || shadow.getOpacity() <= SCROLL_EPSILON) {
      shadow.setOpacity(0.0);
      shadow.setVisible(false);
      return;
    }
    fadeOut.stop();
    fadeOut.setFromValue(shadow.getOpacity());
    fadeOut.playFromStart();
  }

  private void showOverflowShadow(Region shadow, FadeTransition fadeIn, FadeTransition fadeOut) {
    fadeOut.stop();
    if (!shadow.isVisible()) {
      shadow.setVisible(true);
      shadow.setOpacity(0.0);
    }
    fadeIn.stop();
    fadeIn.setFromValue(shadow.getOpacity());
    fadeIn.playFromStart();
  }

  private FadeTransition createOverflowFade(Region shadow, Duration duration, double toValue) {
    FadeTransition fadeTransition = new FadeTransition(duration, shadow);
    fadeTransition.setInterpolator(Interpolator.EASE_BOTH);
    fadeTransition.setToValue(toValue);
    return fadeTransition;
  }

  private double getScrollableWidth() {
    return tabTrack.getLayoutBounds().getWidth() - scrollPane.getViewportBounds().getWidth();
  }

  private static final class TabNode {
    private final HBox item;
    private final StackPane leadingSlot;
    private final HBox rightControls;
    private final StackPane controlsLayer;
    private final Region leftPad;

    private TabNode(
        HBox item, StackPane leadingSlot, HBox rightControls, StackPane controlsLayer, Region leftPad) {
      this.item = item;
      this.leadingSlot = leadingSlot;
      this.rightControls = rightControls;
      this.controlsLayer = controlsLayer;
      this.leftPad = leftPad;
    }
  }
}
