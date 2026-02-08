package fr.inria.corese.gui.component.tabstrip;

import fr.inria.corese.gui.core.theme.ThemeManager;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;

/** Controller that binds a {@link TabPane} model to a reusable {@link TabStripView}. */
public class TabStripController {

  private final TabPane tabPane;
  private final ThemeManager themeManager;
  private final TabStripView view;
  private final boolean showCloseButton;

  private Consumer<Tab> onCloseRequest;

  public TabStripController(TabPane tabPane, ThemeManager themeManager) {
    this(tabPane, themeManager, true);
  }

  public TabStripController(TabPane tabPane, ThemeManager themeManager, boolean showCloseButton) {
    this.tabPane = tabPane;
    this.themeManager = themeManager;
    this.showCloseButton = showCloseButton;
    this.view = new TabStripView();
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
    Color accent = themeManager.getAccentColor();
    view.render(
        tabPane.getTabs(),
        tabPane.getSelectionModel().getSelectedItem(),
        accent,
        showCloseButton,
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
    tabPane.widthProperty().addListener((obs, oldWidth, newWidth) -> refresh());
    view.widthProperty().addListener((obs, oldWidth, newWidth) -> refresh());

    ChangeListener<Color> accentListener = (obs, oldColor, newColor) -> refresh();
    themeManager.accentColorProperty().addListener(accentListener);
  }
}
