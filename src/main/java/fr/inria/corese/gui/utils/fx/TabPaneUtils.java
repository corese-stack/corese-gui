package fr.inria.corese.gui.utils.fx;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

/**
 * Utility class for {@link TabPane} enhancements.
 *
 * <p>This class provides methods to enable full-width tabs using dynamic property binding, inspired
 * by the AtlantaFX framework. It ensures that tabs evenly distribute the available width of the
 * TabPane container and dynamically adapt to changes in tab count.
 */
public final class TabPaneUtils {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  /** Space reserved on the right side for additional controls (e.g., add button). */
  private static final double RESERVED_RIGHT_SPACE = 40.0;

  /** Padding adjustment to account for internal tab CSS margins. */
  private static final double TAB_PADDING_ADJUSTMENT = 17.0;

  /** Maximum allowed tab width to ensure flexibility. */
  private static final String MAX_WIDTH_STYLE = "-fx-tab-max-width: 5000;";

  /** CSS padding for the header area. */
  private static final String HEADER_PADDING_STYLE = "-fx-padding: 0 40 0 0;";

  /** CSS selector for the tab header area. */
  private static final String HEADER_AREA_SELECTOR = ".tab-header-area";

  /** CSS selector for the control buttons. */
  private static final String CONTROL_BUTTONS_SELECTOR = ".control-buttons-tab";

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  private TabPaneUtils() {
    // Utility class - prevent instantiation
  }

  // ==============================================================================================
  // Public Methods
  // ==============================================================================================

  /**
   * Enables full-width tabs for the specified TabPane.
   *
   * <p>This method configures the TabPane to automatically distribute its width evenly among all
   * tabs. The configuration includes:
   *
   * <ul>
   *   <li>Setting a maximum tab width to allow flexibility
   *   <li>Waiting for the skin to be loaded before applying bindings
   *   <li>Hiding navigation arrows to maximize available space
   *   <li>Dynamically updating tab widths when tabs are added or removed
   * </ul>
   *
   * @param tabPane the TabPane to configure for full-width tabs
   */
  public static void enableFullWidthTabs(TabPane tabPane) {
    // Set maximum width to allow tabs to grow
    tabPane.setStyle(MAX_WIDTH_STYLE);

    // Wait for the skin to be loaded before installing bindings
    tabPane
        .skinProperty()
        .addListener(
            (obs, oldSkin, newSkin) -> {
              if (newSkin != null) {
                Platform.runLater(() -> install(tabPane));
              }
            });

    // If skin is already present, install immediately
    if (tabPane.getSkin() != null) {
      Platform.runLater(() -> install(tabPane));
    }
  }

  // ==============================================================================================
  // Private Methods
  // ==============================================================================================

  /**
   * Installs the full-width tab behavior on the TabPane.
   *
   * <p>This method performs the following operations:
   *
   * <ul>
   *   <li>Hides the control buttons (arrows) to gain space
   *   <li>Adds a listener to update tab widths when tabs are added or removed
   *   <li>Applies the initial tab width binding
   * </ul>
   *
   * @param tabPane the TabPane to install the behavior on
   */
  private static void install(TabPane tabPane) {
    // Hide the navigation arrow buttons to maximize available space
    Region headerArea = (Region) tabPane.lookup(HEADER_AREA_SELECTOR);
    if (headerArea != null) {
      // Reserve space on the right for custom controls (e.g., add button)
      headerArea.setStyle(HEADER_PADDING_STYLE);

      // Hide and disable the default control buttons
      Node controlButtons = headerArea.lookup(CONTROL_BUTTONS_SELECTOR);
      if (controlButtons != null) {
        controlButtons.setVisible(false);
        controlButtons.setManaged(false);
      }
    }

    // Update tab widths dynamically when tabs are added or removed
    tabPane
        .getTabs()
        .addListener(
            (ListChangeListener<Tab>)
                c -> {
                  // Use runLater to ensure the tab count is accurate after the change
                  Platform.runLater(() -> bindTabWidth(tabPane));
                });

    // Apply initial tab width binding
    bindTabWidth(tabPane);
  }

  /**
   * Binds the tab width properties to evenly distribute the available width.
   *
   * <p>This method calculates the appropriate width for each tab based on:
   *
   * <ul>
   *   <li>The total width of the TabPane
   *   <li>The number of tabs currently present
   *   <li>Reserved space for additional controls
   *   <li>Internal CSS padding adjustments
   * </ul>
   *
   * <p>The calculation formula is:
   *
   * <pre>
   * tabWidth = (tabPaneWidth - reservedSpace) / tabCount - paddingAdjustment
   * </pre>
   *
   * @param tabPane the TabPane to bind tab widths for
   */
  private static void bindTabWidth(TabPane tabPane) {
    int count = tabPane.getTabs().size();

    // Clear any previous bindings to avoid conflicts
    tabPane.tabMinWidthProperty().unbind();
    tabPane.tabMaxWidthProperty().unbind();

    // No tabs, no binding needed
    if (count == 0) {
      return;
    }

    // Calculate the width each tab should occupy
    // Formula: (totalWidth - reservedSpace) / tabCount - padding
    DoubleBinding tabWidthBinding =
        tabPane
            .widthProperty()
            .subtract(RESERVED_RIGHT_SPACE)
            .divide(count)
            .subtract(TAB_PADDING_ADJUSTMENT);

    // Bind both min and max width to enforce equal sizing
    tabPane.tabMinWidthProperty().bind(tabWidthBinding);
    tabPane.tabMaxWidthProperty().bind(tabWidthBinding);
  }
}
