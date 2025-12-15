package fr.inria.corese.gui.view.utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

/**
 * Utility methods for TabPane.
 *
 * <p>Makes tabs stretch to fill the available header width (GNOME-like behavior) without cumulative
 * layout drift or overflow.
 */
public final class TabPaneUtils {

  private TabPaneUtils() {}

  /**
   * Enables full-width tabs on the given TabPane.
   *
   * @param tabPane the TabPane to configure
   */
  public static void enableFullWidth(TabPane tabPane) {
    IntegerBinding tabCountBinding = Bindings.size(tabPane.getTabs());

    // Lookup only works once the skin is installed
    tabPane
        .skinProperty()
        .addListener(
            (obs, oldSkin, newSkin) -> {
              if (newSkin != null) {
                Platform.runLater(() -> bindTabWidth(tabPane, tabCountBinding));
              }
            });

    // In case the skin is already available
    if (tabPane.getSkin() != null) {
      Platform.runLater(() -> bindTabWidth(tabPane, tabCountBinding));
    }
  }

  private static void bindTabWidth(TabPane tabPane, IntegerBinding tabCountBinding) {
    Node headerNode = tabPane.lookup(".tab-header-area");
    Region headerArea = (headerNode instanceof Region) ? (Region) headerNode : null;

    DoubleBinding tabWidthBinding =
        Bindings.createDoubleBinding(
            () -> computeTabWidth(tabPane, headerArea, tabCountBinding.get()),
            tabPane.widthProperty(),
            tabPane.heightProperty(),
            tabPane.sideProperty(),
            tabCountBinding,
            headerArea != null ? headerArea.widthProperty() : tabPane.widthProperty(),
            headerArea != null ? headerArea.heightProperty() : tabPane.heightProperty(),
            headerArea != null ? headerArea.insetsProperty() : tabPane.insetsProperty());

    tabPane.tabMinWidthProperty().bind(tabWidthBinding);
    tabPane.tabMaxWidthProperty().bind(tabWidthBinding);
  }

  private static double computeTabWidth(TabPane tabPane, Region headerArea, int tabCount) {
    if (tabCount <= 0) {
      return Region.USE_COMPUTED_SIZE;
    }

    Side side = tabPane.getSide();
    boolean horizontal = (side == Side.TOP || side == Side.BOTTOM);

    double length =
        horizontal
            ? (headerArea != null ? headerArea.getWidth() : tabPane.getWidth())
            : (headerArea != null ? headerArea.getHeight() : tabPane.getHeight());

    Insets insets = headerArea != null ? headerArea.getInsets() : tabPane.getInsets();
    double insetSum =
        horizontal ? insets.getLeft() + insets.getRight() : insets.getTop() + insets.getBottom();

    double available = Math.max(length - insetSum, 0);

    // Prevent cumulative pixel rounding overflow
    double width = Math.floor(available / tabCount);

    if (Double.isNaN(width) || width <= 0) {
      return Region.USE_COMPUTED_SIZE;
    }

    return width;
  }
}
