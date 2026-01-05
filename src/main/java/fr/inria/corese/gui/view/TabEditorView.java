package fr.inria.corese.gui.view;

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
public class TabEditorView extends VBox {

  private static final String STYLESHEET = "/styles/split-editor-view.css";

  private final TabPane tabPane;
  private final SplitMenuButton addTabButton;
  private final MenuItem newFileItem;
  private final MenuItem openFileItem;
  private final MenuItem templatesItem;
  private final StackPane contentContainer;
  private final ThemeManager themeManager;

  // Map to store tab content separately (TabPane content area is hidden)
  private final Map<Tab, Node> tabContentMap = new HashMap<>();

  public TabEditorView() {
    this.themeManager = ThemeManager.getInstance();

    // Initialize TabPane (header only - content managed separately)
    tabPane = createTabPane();

    // Initialize Add Button
    addTabButton = createAddTabButton();
    newFileItem = new MenuItem("New File");
    openFileItem = new MenuItem("Open File");
    templatesItem = new MenuItem("Templates");
    addTabButton.getItems().addAll(newFileItem, openFileItem, templatesItem);

    // Create header bar with tabs and button
    HBox tabHeader = createTabHeader();
    // Ensure header doesn't grow vertically
    VBox.setVgrow(tabHeader, Priority.NEVER);

    // Content container for tab content and floating elements
    contentContainer = new StackPane();
    VBox.setVgrow(contentContainer, Priority.ALWAYS);

    // Allow the view to shrink if needed
    setMinHeight(0);

    getChildren().addAll(tabHeader, contentContainer);

    // Load stylesheet
    getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());

    // Listen for tab selection changes
    tabPane
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, oldTab, newTab) -> showContentForTab(newTab));

    // Also trigger content update when tabs are added
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

    // Listen for accent color changes
    themeManager
        .accentColorProperty()
        .addListener((obs, oldColor, newColor) -> refreshModifiedTabIcons());
  }

  /** Creates and configures the TabPane. */
  private TabPane createTabPane() {
    TabPane pane = new TabPane();
    pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    TabPaneUtils.enableFullWidth(pane);
    pane.getStyleClass().add("editor-tab-pane");
    return pane;
  }

  /** Creates the add tab button with icon. */
  private SplitMenuButton createAddTabButton() {
    SplitMenuButton button = new SplitMenuButton();

    FontIcon addIcon = new FontIcon(MaterialDesignP.PLUS);
    addIcon.setIconSize(18);

    button.setGraphic(addIcon);
    button.getStyleClass().add("add-tab-button");

    return button;
  }

  /** Creates the header bar containing tabs and add button. */
  private HBox createTabHeader() {
    HBox header = new HBox(4);
    header.setAlignment(Pos.BOTTOM_LEFT);

    // TabPane takes available horizontal space
    HBox.setHgrow(tabPane, Priority.ALWAYS);

    // Bind button visibility to tabPane visibility
    addTabButton.visibleProperty().bind(tabPane.visibleProperty());
    addTabButton.managedProperty().bind(tabPane.managedProperty());

    header.getChildren().addAll(tabPane, addTabButton);

    return header;
  }

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

