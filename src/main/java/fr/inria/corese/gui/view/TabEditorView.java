package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.utils.TabPaneUtils;
import fr.inria.corese.gui.view.utils.ThemeManager;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

/**
 * View component for the tabbed editor interface. Displays code editor tabs with an add button
 * fixed at the right of the tab bar.
 *
 * <p>The content is managed separately from the TabPane to allow the tab header to have a fixed
 * height while the content fills the remaining space.
 */
public class TabEditorView extends AbstractView {

  private static final String STYLESHEET = "/styles/tab-editor.css";

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  // Tab
  private final TabPane tabPane;
  private final Map<Tab, Node> tabContentMap = new HashMap<>();

  // Menu button
  private final SplitMenuButton addTabButton;
  private final MenuItem newFileItem;
  private final MenuItem openFileItem;
  private final MenuItem templatesItem;

  // Main container
  private final StackPane contentContainer;

  // Theme
  private final ThemeManager themeManager;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  public TabEditorView() {
    super(new VBox(), STYLESHEET);
    this.themeManager = ThemeManager.getInstance();

    // Initialize Components
    this.tabPane = createConfiguredTabPane();

    this.newFileItem = new MenuItem("New File");
    this.openFileItem = new MenuItem("Open File");
    this.templatesItem = new MenuItem("Templates");
    this.addTabButton = createConfiguredAddButton();

    this.contentContainer = createConfiguredContentContainer();

    // Layout
    initializeLayout();

    // Listeners
    setupListeners();
  }

  // ==============================================================================================
  // Initialization & Configuration
  // ==============================================================================================

  /** Creates and configures the TabPane. */
  private TabPane createConfiguredTabPane() {
    TabPane pane = new TabPane();
    pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    TabPaneUtils.enableFullWidth(pane);
    return pane;
  }

  /** Creates the add tab button with icon. */
  private SplitMenuButton createConfiguredAddButton() {
    SplitMenuButton button = new SplitMenuButton();
    button.setGraphic(new FontIcon(MaterialDesignP.PLUS));
    button.getStyleClass().add("add-tab-button");
    button.getItems().addAll(newFileItem, openFileItem, templatesItem);
    return button;
  }

  /** Creates the content container. */
  private StackPane createConfiguredContentContainer() {
    return new StackPane();
  }

  /** Sets up the layout of the view. */
  private void initializeLayout() {
    // 1. Create header bar with tabs and button
    HBox tabHeader = createTabHeader();

    // 2. Configure content container grow
    VBox.setVgrow(contentContainer, Priority.ALWAYS);

    // 3. Allow the view to shrink if needed
    ((VBox) getRoot()).setMinHeight(0);

    // 4. Add all children
    ((VBox) getRoot()).getChildren().addAll(tabHeader, contentContainer);
  }

  /** Creates the header bar containing tabs and add button. */
  private HBox createTabHeader() {
    HBox header = new HBox(4);
    header.setAlignment(Pos.BOTTOM_LEFT);

    // Bind button visibility to tabPane visibility
    addTabButton.visibleProperty().bind(tabPane.visibleProperty());
    addTabButton.managedProperty().bind(tabPane.managedProperty());

    header.getChildren().addAll(tabPane, addTabButton);

    return header;
  }

  /** Configures internal event listeners. */
  private void setupListeners() {
    // 1. Tab properties listeners
    setupTabListeners();

    // 2. Theme listener
    themeManager
        .accentColorProperty()
        .addListener((obs, oldColor, newColor) -> refreshModifiedTabIcons());
  }

  /** Sets up listeners for tab selection and list changes. */
  private void setupTabListeners() {
    // Selection listener
    tabPane
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, oldTab, newTab) -> showContentForTab(newTab));

    // Tab list listener
    tabPane
        .getTabs()
        .addListener(
            (javafx.collections.ListChangeListener<Tab>)
                change -> {
                  while (change.next()) {
                    if (change.wasRemoved()) {
                      for (Tab removedTab : change.getRemoved()) {
                        tabContentMap.remove(removedTab);
                      }
                    }
                    if (change.wasAdded()) {
                      // When a tab is added and selected, show its content
                      Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                      if (selectedTab != null && change.getAddedSubList().contains(selectedTab)) {
                        javafx.application.Platform.runLater(() -> showContentForTab(selectedTab));
                      }
                    }
                  }
                  // If all tabs removed, clear content
                  if (tabPane.getTabs().isEmpty()) {
                    showContentForTab(null);
                  }
                });
  }

  // ==============================================================================================
  // View Logic
  // ==============================================================================================

  /** Shows the content for the selected tab. */
  private void showContentForTab(Tab selectedTab) {
    // Remove previous tab content wrapper
    contentContainer.getChildren().removeIf(node -> "tab-content-wrapper".equals(node.getId()));

    if (selectedTab != null) {
      Node content = tabContentMap.get(selectedTab);
      if (content != null) {
        StackPane wrapper = new StackPane(content);
        wrapper.setId("tab-content-wrapper");
        // Insert at index 0 so floating nodes stay on top
        contentContainer.getChildren().add(0, wrapper);
      }
    }
  }

  /** Adds a floating node to the editor view. */
  public void addFloatingNode(Node node, Pos position, Insets margin) {
    StackPane.setAlignment(node, position);
    StackPane.setMargin(node, margin);
    contentContainer.getChildren().add(node);
  }

  /** Sets the empty state view displayed when no tabs are open. */
  public void setEmptyStateView(Node emptyStateView) {
    emptyStateView.setId("empty-state-view");
    // Remove existing empty state if present
    contentContainer.getChildren().removeIf(node -> "empty-state-view".equals(node.getId()));
    // Insert at index 0 to be behind other content
    contentContainer.getChildren().add(0, emptyStateView);
  }

  /** Creates a Tab with the given title and content node. */
  public Tab createEditorTab(String title, Node content) {
    Tab tab = new Tab(title);
    if (content instanceof javafx.scene.layout.Region) {
      ((javafx.scene.layout.Region) content).setMaxWidth(Double.MAX_VALUE);
      ((javafx.scene.layout.Region) content).setMaxHeight(Double.MAX_VALUE);
    }
    // Store content in map instead of tab.setContent()
    tabContentMap.put(tab, content);
    return tab;
  }

  /** Creates and adds a new editor tab, selecting it. */
  public void addNewEditorTab(Tab tab) {
    tabPane.getTabs().add(tab);
    tabPane.getSelectionModel().select(tab);
    // Force content display
    showContentForTab(tab);
  }

  public void setTabsVisible(boolean visible) {
    tabPane.setVisible(visible);
    tabPane.setManaged(visible);
  }

  /** Updates the tab icon to show modification state. */
  public void updateTabIcon(Tab tab, boolean isModified) {
    if (isModified) {
      Circle circle = new Circle(4, themeManager.getAccentColor());
      tab.setGraphic(circle);
    } else {
      tab.setGraphic(null);
    }
  }

  /** Refreshes all modified tab icons with the current accent color. */
  private void refreshModifiedTabIcons() {
    javafx.scene.paint.Color accentColor = themeManager.getAccentColor();
    if (accentColor == null) {
      return;
    }
    tabPane.getTabs().stream()
        .filter(tab -> tab.getGraphic() instanceof Circle)
        .forEach(tab -> ((Circle) tab.getGraphic()).setFill(accentColor));
  }

  // ==============================================================================================
  // Public API for Controller
  // ==============================================================================================

  /**
   * Sets the action to be performed when the "New File" menu item is clicked.
   *
   * @param action The action to execute.
   */
  public void setOnNewFileAction(javafx.event.EventHandler<javafx.event.ActionEvent> action) {
    newFileItem.setOnAction(action);
  }

  /**
   * Sets the action to be performed when the "Open File" menu item is clicked.
   *
   * @param action The action to execute.
   */
  public void setOnOpenFileAction(javafx.event.EventHandler<javafx.event.ActionEvent> action) {
    openFileItem.setOnAction(action);
  }

  /**
   * Sets the action to be performed when the "Templates" menu item is clicked.
   *
   * @param action The action to execute.
   */
  public void setOnTemplatesAction(javafx.event.EventHandler<javafx.event.ActionEvent> action) {
    templatesItem.setOnAction(action);
  }

  /**
   * Sets the action to be performed when the main "Add Tab" button is clicked.
   *
   * @param action The action to execute.
   */
  public void setOnAddTabAction(javafx.event.EventHandler<javafx.event.ActionEvent> action) {
    addTabButton.setOnAction(action);
  }

  /**
   * Adds a listener to be notified when the list of tabs changes.
   *
   * @param listener The listener to add.
   */
  public void addTabListener(javafx.collections.ListChangeListener<Tab> listener) {
    tabPane.getTabs().addListener(listener);
  }

  /**
   * Adds a listener to be notified when the selected tab changes.
   *
   * @param listener The listener to add.
   */
  public void addSelectionListener(javafx.beans.value.ChangeListener<Tab> listener) {
    tabPane.getSelectionModel().selectedItemProperty().addListener(listener);
  }

  /**
   * Gets the currently selected tab.
   *
   * @return The selected Tab, or null if none.
   */
  public Tab getSelectedTab() {
    return tabPane.getSelectionModel().getSelectedItem();
  }

  /**
   * Selects the specified tab.
   *
   * @param tab The tab to select.
   */
  public void selectTab(Tab tab) {
    tabPane.getSelectionModel().select(tab);
  }

  /**
   * Removes the specified tab.
   *
   * @param tab The tab to remove.
   */
  public void removeTab(Tab tab) {
    tabPane.getTabs().remove(tab);
  }

  /**
   * Gets the content node associated with the given tab.
   *
   * @param tab The tab.
   * @return The content node.
   */
  public Node getTabContent(Tab tab) {
    return tabContentMap.get(tab);
  }

  /**
   * Gets the list of all tabs.
   *
   * @return The list of tabs.
   */
  public javafx.collections.ObservableList<Tab> getTabs() {
    return tabPane.getTabs();
  }
}
