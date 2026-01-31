package fr.inria.corese.gui.feature.tabeditor;

import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.core.DialogHelper;
import fr.inria.corese.gui.utils.TabPaneUtils;
import fr.inria.corese.gui.utils.ThemeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

/**
 * View component for the tabbed editor interface.
 *
 * <p>This view manages code editor tabs with an add button fixed at the right of the tab bar. The
 * content is managed separately from the TabPane to allow the tab header to have a fixed height
 * while the content fills the remaining space.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Dynamic tab management with content container
 *   <li>Floating nodes support (e.g., execution buttons)
 *   <li>Empty state view when no tabs are open
 *   <li>Theme-aware modification indicators
 *   <li>Split menu button for multiple tab creation options
 * </ul>
 */
public class TabEditorView extends AbstractView {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  // Style
  private static final String STYLESHEET = "/css/tab-editor.css";

  // Ids
  private static final String TAB_CONTENT_WRAPPER_ID = "tab-content-wrapper";
  private static final String EMPTY_STATE_VIEW_ID = "empty-state-widget";

  // Result Pane Animation
  private static final javafx.util.Duration SPLIT_ANIMATION_DURATION =
      javafx.util.Duration.millis(300);
  private static final double RESULT_PANE_VISIBLE_POSITION = 0.6;
  private static final double RESULT_PANE_HIDDEN_POSITION = 1.0;

  // Modified Tab Icon
  private static final double MODIFIED_CIRCLE_RADIUS = 4.0;

  // Execution Button Layout
  private static final Insets EXECUTION_BUTTON_MARGIN = new Insets(0, 60, 40, 0);

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  // Theme Manager
  private final ThemeManager themeManager;

  // Tab Content Map
  private final Map<Tab, Node> tabContentMap;

  // UI Components
  private final TabPane tabPane;
  private final SplitMenuButton addTabButton;
  private final StackPane contentContainer;
  private final VBox mainContent;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Constructs a new TabEditorView with all necessary components initialized. */
  public TabEditorView() {
    super(new StackPane(), STYLESHEET);
    this.themeManager = ThemeManager.getInstance();
    this.tabContentMap = new HashMap<>();

    // Initialize UI components
    this.tabPane = createTabPane();
    this.addTabButton = createAddTabButton();
    this.contentContainer = new StackPane();
    this.mainContent = new VBox();

    initializeLayout();
    setupListeners();
  }

  // ==============================================================================================
  // Initialization Methods
  // ==============================================================================================

  /**
   * Creates and configures the TabPane.
   *
   * @return A configured TabPane instance
   */
  private TabPane createTabPane() {
    TabPane pane = new TabPane();
    pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    TabPaneUtils.enableFullWidthTabs(pane);
    return pane;
  }

  /**
   * Creates the split menu button for adding tabs.
   *
   * @return A configured SplitMenuButton instance
   */
  private SplitMenuButton createAddTabButton() {
    SplitMenuButton button = new SplitMenuButton();
    button.setGraphic(new FontIcon(MaterialDesignP.PLUS));
    button.getStyleClass().add("add-tab-button");
    return button;
  }

  /** Initializes the layout structure of the view. */
  private void initializeLayout() {
    mainContent.setMinHeight(0);

    HBox tabHeader = createTabHeader();
    mainContent.getChildren().addAll(tabHeader, contentContainer);
    VBox.setVgrow(contentContainer, Priority.ALWAYS);

    StackPane rootStack = (StackPane) getRoot();
    rootStack.getChildren().addAll(mainContent);

    contentContainer.setId(TAB_CONTENT_WRAPPER_ID);
  }

  /**
   * Creates the header bar containing tabs and add button.
   *
   * @return A configured HBox containing the tab header
   */
  private HBox createTabHeader() {
    HBox header = new HBox();
    header.getStyleClass().add("tab-header");
    header.setAlignment(Pos.BOTTOM_LEFT);

    addTabButton.visibleProperty().bind(tabPane.visibleProperty());
    addTabButton.managedProperty().bind(tabPane.managedProperty());

    header.getChildren().addAll(tabPane, addTabButton);
    return header;
  }

  /** Configures all internal event listeners. */
  private void setupListeners() {
    setupTabSelectionListener();
    setupTabListChangeListener();
    setupThemeListener();
  }

  /** Sets up the listener for tab selection changes. */
  private void setupTabSelectionListener() {
    tabPane
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, oldTab, newTab) -> showContentForTab(newTab));
  }

  /** Sets up the listener for tab list changes (additions and removals). */
  private void setupTabListChangeListener() {
    tabPane
        .getTabs()
        .addListener(
            (ListChangeListener<Tab>)
                change -> {
                  while (change.next()) {
                    handleTabsRemoved(change);
                    handleTabsAdded(change);
                  }
                  handleAllTabsRemoved();
                });
  }

  /**
   * Handles removed tabs by cleaning up the content map.
   *
   * @param change The list change event
   */
  private void handleTabsRemoved(ListChangeListener.Change<? extends Tab> change) {
    if (change.wasRemoved()) {
      change.getRemoved().forEach(tabContentMap::remove);
    }
  }

  /**
   * Handles added tabs by showing content for newly selected tabs.
   *
   * @param change The list change event
   */
  private void handleTabsAdded(ListChangeListener.Change<? extends Tab> change) {
    if (change.wasAdded()) {
      Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
      if (selectedTab != null && change.getAddedSubList().contains(selectedTab)) {
        Platform.runLater(() -> showContentForTab(selectedTab));
      }
    }
  }

  /** Handles the case when all tabs are removed by clearing the content. */
  private void handleAllTabsRemoved() {
    if (tabPane.getTabs().isEmpty()) {
      showContentForTab(null);
    }
  }

  /** Sets up the listener for theme changes. */
  private void setupThemeListener() {
    themeManager
        .accentColorProperty()
        .addListener((obs, oldColor, newColor) -> refreshModifiedTabIcons());
  }

  // ==============================================================================================
  // Content Management
  // ==============================================================================================

  /**
   * Shows the content for the specified tab.
   *
   * @param selectedTab The tab whose content should be displayed, or null to clear content
   */
  private void showContentForTab(Tab selectedTab) {
    contentContainer.getChildren().removeIf(node -> TAB_CONTENT_WRAPPER_ID.equals(node.getId()));

    if (selectedTab != null) {
      Node content = tabContentMap.get(selectedTab);
      if (content != null) {
        StackPane wrapper = new StackPane(content);
        wrapper.setId(TAB_CONTENT_WRAPPER_ID);
        contentContainer.getChildren().add(0, wrapper);
      }
    }
  }

  /**
   * Refreshes all modified tab icons with the current accent color.
   *
   * <p>This method is called when the theme changes to update the color of modification indicators.
   */
  private void refreshModifiedTabIcons() {
    Color accentColor = themeManager.getAccentColor();
    if (accentColor == null) {
      return;
    }
    tabPane.getTabs().stream()
        .filter(tab -> tab.getGraphic() instanceof Circle)
        .forEach(tab -> ((Circle) tab.getGraphic()).setFill(accentColor));
  }

  /**
   * Gets the content node associated with the given tab.
   *
   * @param tab The tab
   * @return The content node, or null if not found
   */
  private Node getTabContent(Tab tab) {
    return tabContentMap.get(tab);
  }

  // ==============================================================================================
  // Public API - Tab Management
  // ==============================================================================================

  /**
   * Creates a Tab with the given title and content node.
   *
   * @param title The title for the tab
   * @param content The content node to display in the tab
   * @return A new configured Tab instance
   */
  public Tab createEditorTab(String title, Node content) {
    Tab tab = new Tab(title);
    if (content instanceof Region region) {
      region.setMaxWidth(Double.MAX_VALUE);
      region.setMaxHeight(Double.MAX_VALUE);
    }
    tabContentMap.put(tab, content);
    return tab;
  }

  /**
   * Adds a new editor tab and selects it.
   *
   * @param tab The tab to add
   */
  public void addNewEditorTab(Tab tab) {
    tabPane.getTabs().add(tab);
    tabPane.getSelectionModel().select(tab);
    showContentForTab(tab);
  }

  /**
   * Selects the specified tab.
   *
   * @param tab The tab to select
   */
  public void selectTab(Tab tab) {
    tabPane.getSelectionModel().select(tab);
  }

  /**
   * Gets the currently selected tab.
   *
   * @return The selected Tab, or null if none is selected
   */
  public Tab getSelectedTab() {
    return tabPane.getSelectionModel().getSelectedItem();
  }

  /**
   * Gets the list of all tabs.
   *
   * @return The observable list of tabs
   */
  public ObservableList<Tab> getTabs() {
    return tabPane.getTabs();
  }

  // ==============================================================================================
  // Public API - Visual State Management
  // ==============================================================================================

  /**
   * Updates the tab icon to show modification state.
   *
   * @param tab The tab to update
   * @param isModified True to show modified indicator, false to hide it
   */
  public void updateTabIcon(Tab tab, boolean isModified) {
    if (isModified) {
      Circle circle = new Circle(MODIFIED_CIRCLE_RADIUS, themeManager.getAccentColor());
      tab.setGraphic(circle);
    } else {
      tab.setGraphic(null);
    }
  }

  /**
   * Sets the visibility of the tabs.
   *
   * @param visible True to show tabs, false to hide them
   */
  public void setTabsVisible(boolean visible) {
    tabPane.setVisible(visible);
    tabPane.setManaged(visible);
  }

  /**
   * Sets the empty state view displayed when no tabs are open.
   *
   * @param emptyStateWidget The node to display as empty state
   */
  public void setEmptyStateWidget(Node emptyStateWidget) {
    emptyStateWidget.setId(EMPTY_STATE_VIEW_ID);
    contentContainer.getChildren().removeIf(node -> EMPTY_STATE_VIEW_ID.equals(node.getId()));
    contentContainer.getChildren().add(0, emptyStateWidget);
  }

  /**
   * Updates the visibility of the empty state view based on whether tabs are open.
   *
   * @param visible true to show the empty state, false to hide it
   */
  public void updateEmptyStateVisibility(boolean visible) {
    contentContainer.getChildren().stream()
        .filter(node -> EMPTY_STATE_VIEW_ID.equals(node.getId()))
        .findFirst()
        .ifPresent(node -> {
          node.setVisible(visible);
          node.setManaged(visible);
        });
  }

  /**
   * Adds a floating node to the editor view (e.g., execution button).
   *
   * @param node The node to add
   * @param position The position alignment within the container
   * @param margin The margin around the node
   */
  public void addFloatingNode(Node node, Pos position, Insets margin) {
    StackPane.setAlignment(node, position);
    StackPane.setMargin(node, margin);
    contentContainer.getChildren().add(node);
  }

  // ==============================================================================================
  // Public API - Dialogs
  // ==============================================================================================

  /**
   * Shows an error dialog.
   *
   * @param title The dialog title
   * @param message The error message
   */
  public void showError(String title, String message) {
    DialogHelper.showError(title, message);
  }

  /**
   * Shows an error dialog with detailed information.
   *
   * @param title The dialog title
   * @param message The error message
   * @param details The detailed error message (e.g., stack trace)
   */
  public void showError(String title, String message, String details) {
    DialogHelper.showError(title, message, details);
  }

  /**
   * Shows a confirmation dialog for unsaved changes.
   *
   * @param fileName The name of the file with unsaved changes
   * @param callback The callback to receive the user's choice
   */
  public void showUnsavedChangesDialog(
      String fileName, Consumer<DialogHelper.UnsavedChangesResult> callback) {
    DialogHelper.showUnsavedChangesDialog(fileName, callback);
  }

  // ==============================================================================================
  // Public API - Result Pane Animation
  // ==============================================================================================

  /**
   * Shows the result pane for the currently selected tab with a smooth animation.
   *
   * <p>The result pane slides up from the bottom of the split pane.
   *
   * @param resultNode The result view node to display
   */
  public void showResultPane(Node resultNode) {
    Tab selectedTab = getSelectedTab();
    if (selectedTab == null || resultNode == null) {
      return;
    }

    Node content = getTabContent(selectedTab);
    if (content instanceof SplitPane splitPane && splitPane.getItems().size() < 2) {
      splitPane.getItems().add(resultNode);
      splitPane.setDividerPositions(RESULT_PANE_HIDDEN_POSITION);

      Timeline timeline =
          new Timeline(
              new KeyFrame(
                  SPLIT_ANIMATION_DURATION,
                  new KeyValue(
                      splitPane.getDividers().get(0).positionProperty(),
                      RESULT_PANE_VISIBLE_POSITION)));
      timeline.play();
    }
  }

  /**
   * Hides the result pane for the currently selected tab with a smooth animation.
   *
   * <p>The result pane slides down to the bottom before being removed.
   */
  public void hideResultPane() {
    Tab selectedTab = getSelectedTab();
    if (selectedTab == null) {
      return;
    }

    Node content = getTabContent(selectedTab);
    if (content instanceof SplitPane splitPane && splitPane.getItems().size() > 1) {
      Timeline timeline =
          new Timeline(
              new KeyFrame(
                  SPLIT_ANIMATION_DURATION,
                  new KeyValue(
                      splitPane.getDividers().get(0).positionProperty(),
                      RESULT_PANE_HIDDEN_POSITION)));
      timeline.setOnFinished(
          e -> {
            if (splitPane.getItems().size() > 1) {
              splitPane.getItems().remove(1);
            }
          });
      timeline.play();
    }
  }

  // ==============================================================================================
  // Public API - Menu Configuration
  // ==============================================================================================

  /**
   * Adds a menu item to the split button dropdown.
   *
   * <p>This allows different contexts (Query, Validate, etc.) to configure their own menu items.
   *
   * @param text The text to display for the menu item
   * @param action The action to execute when the item is clicked
   * @return The created MenuItem for further customization if needed
   */
  public MenuItem addMenuItem(String text, EventHandler<ActionEvent> action) {
    MenuItem item = new MenuItem(text);
    item.setOnAction(action);
    addTabButton.getItems().add(item);
    return item;
  }

  /**
   * Clears all menu items from the split button dropdown.
   *
   * <p>This can be useful when reconfiguring the menu based on context changes.
   */
  public void clearMenuItems() {
    addTabButton.getItems().clear();
  }

  // ==============================================================================================
  // Public API - Event Handler Registration
  // ==============================================================================================

  /**
   * Sets the action to be performed when the main "Add Tab" button is clicked.
   *
   * <p>This is triggered when clicking the button itself (not the dropdown menu).
   *
   * @param action The action to execute
   */
  public void setOnAddTabAction(EventHandler<ActionEvent> action) {
    addTabButton.setOnAction(action);
  }

  /**
   * Adds a listener to be notified when the list of tabs changes.
   *
   * @param listener The listener to add
   */
  public void subscribeToTabChanges(ListChangeListener<Tab> listener) {
    tabPane.getTabs().addListener(listener);
  }

  /**
   * Adds a listener to be notified when the selected tab changes.
   *
   * @param listener The listener to add
   */
  public void addSelectionListener(ChangeListener<Tab> listener) {
    tabPane.getSelectionModel().selectedItemProperty().addListener(listener);
  }

  // ==============================================================================================
  // Public API - Layout Constants
  // ==============================================================================================

  /**
   * Returns the margin to use for positioning execution buttons.
   *
   * <p>This margin positions the button at the bottom-right corner with appropriate spacing.
   *
   * @return The execution button margin insets
   */
  public static Insets getExecutionButtonMargin() {
    return EXECUTION_BUTTON_MARGIN;
  }
}
