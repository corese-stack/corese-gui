package fr.inria.corese.gui.feature.result;

import fr.inria.corese.gui.core.config.ResultViewConfig;

import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.TabPaneUtils;






import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/**
 * Main view for displaying query results with multiple tab types (Text, Visual, Table, Graph).
 *
 * <p>This view is intentionally created empty - the controller is responsible for configuring
 * which tabs should be shown based on {@link fr.inria.corese.gui.core.ResultViewConfig}.
 *
 * <p><b>Design rationale:</b>
 * <ul>
 *   <li>No default tabs are added in the constructor (avoiding wasted work)</li>
 *   <li>High-level methods respect Demeter's Law</li>
 *   <li>Getters have package-private visibility to encourage proper API usage</li>
 * </ul>
 */
public class ResultView extends AbstractView {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private final TabPane tabPane;
    private final Tab textTab;
    private final Tab visualTab;
    private final Tab tableTab;
    private final Tab graphTab;

    public ResultView() {
        super(new BorderPane(), null);
        
        tabPane = new TabPane();
        textTab = new Tab(ResultViewConfig.TabType.TEXT.getLabel());
        visualTab = new Tab(ResultViewConfig.TabType.VISUAL.getLabel());
        tableTab = new Tab(ResultViewConfig.TabType.TABLE.getLabel());
        graphTab = new Tab(ResultViewConfig.TabType.GRAPH.getLabel());
        
        // Tab pane starts empty - controller will add configured tabs
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        TabPaneUtils.enableFullWidthTabs(tabPane);

        BorderPane root = (BorderPane) getRoot();
        root.setCenter(tabPane);
    }

    // ==============================================================================================
    // Package-private Accessors (Demeter's Law compliance)
    // ==============================================================================================
    // These getters have package-private visibility to discourage direct manipulation
    // of tabs from outside the view package. Controllers should use high-level methods
    // like enableTextTab() instead of getTabPane().getTabs().add().

    /** Returns the tab pane (package-private - prefer high-level methods). */
    TabPane getTabPane() { return tabPane; }

    /** Returns the text tab (package-private - prefer enableTextTab()). */
    Tab getTextTab() { return textTab; }

    /** Returns the visual tab (package-private - prefer enableVisualTab()). */
    Tab getVisualTab() { return visualTab; }

    /** Returns the table tab (package-private - prefer enableTableTab()). */
    Tab getTableTab() { return tableTab; }

    /** Returns the graph tab (package-private - prefer enableGraphTab()). */
    Tab getGraphTab() { return graphTab; }

    // ==============================================================================================
    // High-level Tab Management (Demeter's Law compliance)
    // ==============================================================================================

    /**
     * Clears all tabs from the tab pane.
     * This method encapsulates the internal structure of the view.
     */
    public void clearAllTabs() {
        tabPane.getTabs().clear();
    }

    /**
     * Enables the text tab with the specified content.
     *
     * @param content The JavaFX node to display in the text tab
     */
    public void enableTextTab(javafx.scene.Node content) {
        textTab.setContent(content);
        if (!tabPane.getTabs().contains(textTab)) {
            tabPane.getTabs().add(textTab);
        }
    }

    /**
     * Enables the visual tab with the specified content.
     *
     * @param content The JavaFX node to display in the visual tab
     */
    public void enableVisualTab(javafx.scene.Node content) {
        visualTab.setContent(content);
        if (!tabPane.getTabs().contains(visualTab)) {
            tabPane.getTabs().add(visualTab);
        }
    }

    /**
     * Enables the table tab with the specified content.
     *
     * @param content The JavaFX node to display in the table tab
     */
    public void enableTableTab(javafx.scene.Node content) {
        tableTab.setContent(content);
        if (!tabPane.getTabs().contains(tableTab)) {
            tabPane.getTabs().add(tableTab);
        }
    }

    /**
     * Enables the graph tab with the specified content.
     *
     * @param content The JavaFX node to display in the graph tab
     */
    public void enableGraphTab(javafx.scene.Node content) {
        graphTab.setContent(content);
        if (!tabPane.getTabs().contains(graphTab)) {
            tabPane.getTabs().add(graphTab);
        }
    }

    /**
     * Programmatically selects the text tab.
     *
     * <p>This method encapsulates the internal tab selection logic.
     */
    public void selectTextTab() {
        tabPane.getSelectionModel().select(textTab);
    }

    /**
     * Programmatically selects the table tab.
     *
     * <p>This method encapsulates the internal tab selection logic.
     */
    public void selectTableTab() {
        tabPane.getSelectionModel().select(tableTab);
    }

    /**
     * Programmatically selects the graph tab.
     *
     * <p>This method encapsulates the internal tab selection logic.
     */
    public void selectGraphTab() {
        tabPane.getSelectionModel().select(graphTab);
    }

    /**
     * Sets whether a specific tab is enabled or disabled.
     *
     * <p>When disabled, the tab is shown but grayed out and not selectable.
     *
     * @param tab The tab to enable/disable (use getTextTab(), getVisualTab(), etc.)
     * @param enabled True to enable the tab, false to disable it
     */
    public void setTabEnabled(Tab tab, boolean enabled) {
        if (tab != null) {
            tab.setDisable(!enabled);
        }
    }

    /**
     * Returns a tab instance by its type.
     *
     * <p>This method provides controlled access to individual tabs for configuration purposes
     * (e.g., enabling/disabling) while keeping the internal tab references encapsulated.
     *
     * @param tabType The type of tab to retrieve
     * @return The Tab instance, or null if the type is not recognized
     */
    public Tab getTabByType(ResultViewConfig.TabType tabType) {
        return switch (tabType) {
            case TEXT -> textTab;
            case VISUAL -> visualTab;
            case TABLE -> tableTab;
            case GRAPH -> graphTab;
        };
    }
}
