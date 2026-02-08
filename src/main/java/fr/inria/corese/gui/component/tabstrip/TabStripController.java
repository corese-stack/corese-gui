package fr.inria.corese.gui.component.tabstrip;

import fr.inria.corese.gui.core.theme.ThemeManager;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;

/** Controller that binds a {@link TabPane} model to a reusable {@link TabStripView}. */
public class TabStripController {

  private final TabPane tabPane;
  private final ThemeManager themeManager;
  private final TabStripView view;
  private final boolean showCloseButton;
  private final boolean animateTabs;
  private final PauseTransition resizeRefreshDebounce;
  private Window boundWindow;

  private Consumer<Tab> onCloseRequest;

  public TabStripController(TabPane tabPane, ThemeManager themeManager) {
    this(tabPane, themeManager, true, true);
  }

  public TabStripController(TabPane tabPane, ThemeManager themeManager, boolean showCloseButton) {
    this(tabPane, themeManager, showCloseButton, true);
  }

  public TabStripController(
      TabPane tabPane, ThemeManager themeManager, boolean showCloseButton, boolean animateTabs) {
    this.tabPane = tabPane;
    this.themeManager = themeManager;
    this.showCloseButton = showCloseButton;
    this.animateTabs = animateTabs;
    this.view = new TabStripView(animateTabs);
    this.resizeRefreshDebounce = new PauseTransition(Duration.millis(90));
    this.resizeRefreshDebounce.setOnFinished(e -> refresh(false));
    this.onCloseRequest = tab -> this.tabPane.getTabs().remove(tab);
    setupListeners();
    refresh();
  }

  public TabStripView getView() {
    return view;
  }

  public void setOnCloseRequest(Consumer<Tab> onCloseRequest) {
    this.onCloseRequest = onCloseRequest != null ? onCloseRequest : (tab -> tabPane.getTabs().remove(tab));
  }

  public void refresh() {
    refresh(true);
  }

  private void refresh(boolean animateWidthChanges) {
    Color accent = themeManager.getAccentColor();
    view.render(
        tabPane.getTabs(),
        tabPane.getSelectionModel().getSelectedItem(),
        accent,
        showCloseButton,
        animateWidthChanges,
        tab -> tabPane.getSelectionModel().select(tab),
        onCloseRequest);
  }

  private void setupListeners() {
    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> refresh());
    tabPane.getTabs().addListener(
        (ListChangeListener<Tab>)
            change -> {
              refresh();
              // First render can happen before viewport width is finalized.
              // Re-render on next pulse to get correct full-width sizing.
              Platform.runLater(this::refresh);
            });
    tabPane.widthProperty().addListener((obs, oldWidth, newWidth) -> refreshForResize());
    view.widthProperty().addListener((obs, oldWidth, newWidth) -> refreshForResize());
    view
        .sceneProperty()
        .addListener(
            (obs, oldScene, newScene) -> {
              if (newScene == null) {
                return;
              }
              newScene
                  .windowProperty()
                  .addListener(
                      (windowObs, oldWindow, newWindow) -> {
                        if (newWindow != null) {
                          bindWindowRefresh(newWindow);
                        }
                      });
              if (newScene.getWindow() != null) {
                bindWindowRefresh(newScene.getWindow());
              }
            });

    ChangeListener<Color> accentListener = (obs, oldColor, newColor) -> refresh();
    themeManager.accentColorProperty().addListener(accentListener);
  }

  private void bindWindowRefresh(Window window) {
    if (window == boundWindow) {
      return;
    }
    boundWindow = window;
    window
        .showingProperty()
        .addListener(
            (obs, wasShowing, isShowing) -> {
              if (Boolean.TRUE.equals(isShowing)) {
                Platform.runLater(this::refresh);
                Platform.runLater(() -> Platform.runLater(this::refresh));
              }
            });
    if (window.isShowing()) {
      Platform.runLater(this::refresh);
    }
  }

  private void refreshForResize() {
    refresh(false);
    if (!animateTabs) {
      return;
    }
    // Ensure we also refresh with the latest stabilized width after resize animation/layout.
    resizeRefreshDebounce.playFromStart();
    Platform.runLater(() -> refresh(false));
  }
}
