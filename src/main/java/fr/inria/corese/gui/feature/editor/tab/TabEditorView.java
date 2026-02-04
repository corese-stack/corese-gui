package fr.inria.corese.gui.feature.editor.tab;

import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.view.AbstractView;
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
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

/**
 * View component for the tabbed editor interface.
 *
 * <p>Manages the visual representation of code editor tabs, including:
 * <ul>
 *   <li>The tab bar with a custom "Add Tab" split button.</li>
 *   <li>The content area, decoupled from the tab headers for flexibility.</li>
 *   <li>Visual indicators for modified tabs (dirty state).</li>
 *   <li>Empty state display when no tabs are open.</li>
 *   <li>Animation for showing/hiding result panes.</li>
 * </ul>
 */
public class TabEditorView extends AbstractView {

    // ==============================================================================================
    // Constants
    // ==============================================================================================

    private static final String STYLESHEET = "/css/features/tab-editor.css";
    private static final String TAB_CONTENT_WRAPPER_ID = "tab-content-wrapper";
    private static final String EMPTY_STATE_VIEW_ID = "empty-state-widget";
    private static final String STYLE_CLASS_ADD_BUTTON = "add-tab-button";
    private static final String STYLE_CLASS_TAB_HEADER = "tab-header";

    // Result Pane Animation
    private static final Duration SPLIT_ANIMATION_DURATION = Duration.millis(300);
    private static final double RESULT_PANE_VISIBLE_POSITION = 0.6;
    private static final double RESULT_PANE_HIDDEN_POSITION = 1.0;

    // Modified Tab Icon
    private static final double MODIFIED_CIRCLE_RADIUS = 4.0;

    // Execution Button Layout
    private static final Insets EXECUTION_BUTTON_MARGIN = new Insets(0, 60, 40, 0);

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private final ThemeManager themeManager;
    private final Map<Tab, Node> tabContentMap;

    private final TabPane tabPane;
    private final SplitMenuButton addTabButton;
    private final StackPane contentContainer;
    private final VBox mainContent;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

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
    // Initialization
    // ==============================================================================================

    private TabPane createTabPane() {
        TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        TabPaneUtils.enableFullWidthTabs(pane);
        return pane;
    }

    private SplitMenuButton createAddTabButton() {
        SplitMenuButton button = new SplitMenuButton();
        button.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        button.getStyleClass().add(STYLE_CLASS_ADD_BUTTON);
        return button;
    }

    private void initializeLayout() {
        mainContent.setMinHeight(0);

        HBox tabHeader = createTabHeader();
        mainContent.getChildren().addAll(tabHeader, contentContainer);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);

        StackPane rootStack = (StackPane) getRoot();
        rootStack.getChildren().addAll(mainContent);

        contentContainer.setId(TAB_CONTENT_WRAPPER_ID);
    }

    private HBox createTabHeader() {
        HBox header = new HBox();
        header.getStyleClass().add(STYLE_CLASS_TAB_HEADER);
        header.setAlignment(Pos.BOTTOM_LEFT);

        // Bind visibility of add button to tab pane
        addTabButton.visibleProperty().bind(tabPane.visibleProperty());
        addTabButton.managedProperty().bind(tabPane.managedProperty());

        header.getChildren().addAll(tabPane, addTabButton);
        return header;
    }

    private void setupListeners() {
        setupTabSelectionListener();
        setupTabListChangeListener();
        setupThemeListener();
    }

    private void setupTabSelectionListener() {
        tabPane.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldTab, newTab) -> showContentForTab(newTab));
    }

    private void setupTabListChangeListener() {
        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                handleTabsRemoved(change);
                handleTabsAdded(change);
            }
            handleAllTabsRemoved();
        });
    }

    private void handleTabsRemoved(ListChangeListener.Change<? extends Tab> change) {
        if (change.wasRemoved()) {
            change.getRemoved().forEach(tabContentMap::remove);
        }
    }

    private void handleTabsAdded(ListChangeListener.Change<? extends Tab> change) {
        if (change.wasAdded()) {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null && change.getAddedSubList().contains(selectedTab)) {
                Platform.runLater(() -> showContentForTab(selectedTab));
            }
        }
    }

    private void handleAllTabsRemoved() {
        if (tabPane.getTabs().isEmpty()) {
            showContentForTab(null);
        }
    }

    private void setupThemeListener() {
        themeManager.accentColorProperty()
            .addListener((obs, oldColor, newColor) -> refreshModifiedTabIcons());
    }

    // ==============================================================================================
    // Content Management
    // ==============================================================================================

    private void showContentForTab(Tab selectedTab) {
        // Remove existing content
        contentContainer.getChildren().removeIf(node -> TAB_CONTENT_WRAPPER_ID.equals(node.getId()));

        if (selectedTab != null) {
            Node content = tabContentMap.get(selectedTab);
            if (content != null) {
                StackPane wrapper = new StackPane(content);
                wrapper.setId(TAB_CONTENT_WRAPPER_ID);
                // Add at index 0 (below floating nodes) or handle z-order
                contentContainer.getChildren().add(0, wrapper);
            }
        }
    }

    private void refreshModifiedTabIcons() {
        Color accentColor = themeManager.getAccentColor();
        if (accentColor == null) return;
        
        tabPane.getTabs().stream()
            .filter(tab -> tab.getGraphic() instanceof Circle)
            .forEach(tab -> ((Circle) tab.getGraphic()).setFill(accentColor));
    }

    private Node getTabContent(Tab tab) {
        return tabContentMap.get(tab);
    }

    // ==============================================================================================
    // Public API - Tabs
    // ==============================================================================================

    /**
     * Creates a configured Tab with the associated content.
     * The content is stored internally and displayed when the tab is selected.
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

    public void addNewEditorTab(Tab tab) {
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        showContentForTab(tab);
    }

    public void selectTab(Tab tab) {
        tabPane.getSelectionModel().select(tab);
    }

    public Tab getSelectedTab() {
        return tabPane.getSelectionModel().getSelectedItem();
    }

    public ObservableList<Tab> getTabs() {
        return tabPane.getTabs();
    }

    // ==============================================================================================
    // Public API - State & UI
    // ==============================================================================================

    public void updateTabIcon(Tab tab, boolean isModified) {
        if (isModified) {
            Circle circle = new Circle(MODIFIED_CIRCLE_RADIUS, themeManager.getAccentColor());
            tab.setGraphic(circle);
        } else {
            tab.setGraphic(null);
        }
    }

    public void setTabsVisible(boolean visible) {
        tabPane.setVisible(visible);
        tabPane.setManaged(visible);
    }

    public void setEmptyStateWidget(Node emptyStateWidget) {
        emptyStateWidget.setId(EMPTY_STATE_VIEW_ID);
        contentContainer.getChildren().removeIf(node -> EMPTY_STATE_VIEW_ID.equals(node.getId()));
        contentContainer.getChildren().add(0, emptyStateWidget);
    }

    public void updateEmptyStateVisibility(boolean visible) {
        contentContainer.getChildren().stream()
            .filter(node -> EMPTY_STATE_VIEW_ID.equals(node.getId()))
            .findFirst()
            .ifPresent(node -> {
                node.setVisible(visible);
                node.setManaged(visible);
            });
    }

    public void addFloatingNode(Node node, Pos position, Insets margin) {
        StackPane.setAlignment(node, position);
        StackPane.setMargin(node, margin);
        contentContainer.getChildren().add(node);
    }

    // ==============================================================================================
    // Public API - Dialogs
    // ==============================================================================================

    public void showError(String title, String message) {
        ModalService.getInstance().showError(title, message);
    }

    public void showError(String title, String message, String details) {
        ModalService.getInstance().showError(title, message, details);
    }

    public void showUnsavedChangesDialog(String fileName, Consumer<ModalService.UnsavedChangesResult> callback) {
        ModalService.getInstance().showUnsavedChangesDialog(fileName, callback);
    }

    // ==============================================================================================
    // Public API - Results Pane
    // ==============================================================================================

    public void showResultPane(Node resultNode) {
        Tab selectedTab = getSelectedTab();
        if (selectedTab == null || resultNode == null) return;

        Node content = getTabContent(selectedTab);
        if (content instanceof SplitPane splitPane && splitPane.getItems().size() < 2) {
            splitPane.getItems().add(resultNode);
            splitPane.setDividerPositions(RESULT_PANE_HIDDEN_POSITION);

            Timeline timeline = new Timeline(
                new KeyFrame(SPLIT_ANIMATION_DURATION,
                    new KeyValue(splitPane.getDividers().get(0).positionProperty(), RESULT_PANE_VISIBLE_POSITION))
            );
            timeline.play();
        }
    }

    public void hideResultPane() {
        Tab selectedTab = getSelectedTab();
        if (selectedTab == null) return;

        Node content = getTabContent(selectedTab);
        if (content instanceof SplitPane splitPane && splitPane.getItems().size() > 1) {
            Timeline timeline = new Timeline(
                new KeyFrame(SPLIT_ANIMATION_DURATION,
                    new KeyValue(splitPane.getDividers().get(0).positionProperty(), RESULT_PANE_HIDDEN_POSITION))
            );
            timeline.setOnFinished(e -> {
                if (splitPane.getItems().size() > 1) {
                    splitPane.getItems().remove(1);
                }
            });
            timeline.play();
        }
    }

    // ==============================================================================================
    // Public API - Menu
    // ==============================================================================================

    public MenuItem addMenuItem(String text, EventHandler<ActionEvent> action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(action);
        addTabButton.getItems().add(item);
        return item;
    }

    public void clearMenuItems() {
        addTabButton.getItems().clear();
    }

    public void setOnAddTabAction(EventHandler<ActionEvent> action) {
        addTabButton.setOnAction(action);
    }

    public void subscribeToTabChanges(ListChangeListener<Tab> listener) {
        tabPane.getTabs().addListener(listener);
    }

    public void addSelectionListener(ChangeListener<Tab> listener) {
        tabPane.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    public static Insets getExecutionButtonMargin() {
        return EXECUTION_BUTTON_MARGIN;
    }
}
