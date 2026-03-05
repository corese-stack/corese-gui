package fr.inria.corese.gui.feature.result;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.feature.result.graph.GraphResultController;
import fr.inria.corese.gui.feature.result.table.TableResultController;
import fr.inria.corese.gui.feature.result.text.TextResultController;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.Parent;
import javafx.scene.control.Tab;

/**
 * Main controller for the Results Pane.
 *
 * <p>
 * This controller acts as a facade, orchestrating specialized sub-controllers
 * (Text, Table, Graph) based on the provided configuration.
 *
 * <p>
 * It implements the "Composite Controller" pattern.
 */
public class ResultController implements AutoCloseable {

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	/** The main view containing the tabs. */
	private final ResultView view;

	/** The configuration driving which tabs are active. */
	private final ResultViewConfig config;

	// Sub-controllers (lazy loaded or initialized based on config)
	private final TextResultController textController;
	private final TableResultController tableController;
	private final GraphResultController graphController;

	private javafx.beans.value.ChangeListener<Tab> tabSelectionListener;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	/**
	 * Constructs a ResultController with the default configuration (all tabs
	 * enabled).
	 */
	public ResultController() {
		this(ResultViewConfig.allTabs());
	}

	/**
	 * Legacy constructor compatibility.
	 *
	 * @param buttons
	 *            List of toolbar buttons (ignored for now as they are managed by
	 *            specific controllers or config)
	 * @param config
	 *            The result view configuration
	 */
	public ResultController(List<ButtonConfig> buttons, ResultViewConfig config) {
		this(config);
	}

	/**
	 * Constructs a ResultController with a specific configuration.
	 *
	 * @param config
	 *            The configuration defining active tabs and behaviors.
	 */
	public ResultController(ResultViewConfig config) {
		this.config = Objects.requireNonNull(config, "ResultViewConfig cannot be null");
		this.view = new ResultView();

		// Initialize sub-controllers only if their tab is enabled in config
		this.textController = this.config.hasTab(ResultViewConfig.TabType.TEXT) ? new TextResultController() : null;

		this.tableController = this.config.hasTab(ResultViewConfig.TabType.TABLE) ? new TableResultController() : null;

		this.graphController = this.config.hasTab(ResultViewConfig.TabType.GRAPH) ? new GraphResultController() : null;

		initializeView();
	}

	// ==============================================================================================
	// Initialization
	// ==============================================================================================

	private void initializeView() {
		// 1. Setup Text Tab
		if (textController != null) {
			view.enableTextTab(textController.getView());
		}

		// 2. Setup Table Tab
		if (tableController != null) {
			view.enableTableTab(tableController.getView());
		}

		// 3. Setup Graph Tab
		if (graphController != null) {
			view.enableGraphTab(graphController.getView());
		}

		// 4. Setup default selection
		selectDefaultTab();
	}

	private void selectDefaultTab() {
		// Select the first available tab based on config order preference
		if (config.hasTab(ResultViewConfig.TabType.TABLE)) {
			view.selectTableTab();
		} else if (config.hasTab(ResultViewConfig.TabType.TEXT)) {
			view.selectTextTab();
		} else if (config.hasTab(ResultViewConfig.TabType.GRAPH)) {
			view.selectGraphTab();
		}
	}

	// ==============================================================================================
	// Facade API - Tab Management
	// ==============================================================================================

	/**
	 * Dynamically configures which tabs should be visible.
	 */
	public void configureTabsForResult(boolean text, boolean table, boolean graph) {
		configureTabsForResult(text, table, graph, null);
	}

	/**
	 * Dynamically configures which tabs should be visible.
	 *
	 * @param text
	 *            enable text tab
	 * @param table
	 *            enable table tab
	 * @param graph
	 *            enable graph tab
	 * @param preferredTab
	 *            preferred tab to select when current selection is not available
	 */
	public void configureTabsForResult(boolean text, boolean table, boolean graph,
			ResultViewConfig.TabType preferredTab) {
		if (textController != null) {
			view.enableTextTab(textController.getView());
			view.setTabEnabled(view.getTabByType(ResultViewConfig.TabType.TEXT), text);
		}
		if (tableController != null) {
			view.enableTableTab(tableController.getView());
			view.setTabEnabled(view.getTabByType(ResultViewConfig.TabType.TABLE), table);
		}
		if (graphController != null) {
			view.enableGraphTab(graphController.getView());
			view.setTabEnabled(view.getTabByType(ResultViewConfig.TabType.GRAPH), graph);
		}

		// Re-evaluate selection
		ResultViewConfig.TabType current = getSelectedTabType();
		if (current != null && isTabEnabled(current)) {
			return;
		}

		if (preferredTab != null && isTabEnabled(preferredTab)) {
			selectTab(preferredTab);
			return;
		}

		if (table) {
			view.selectTableTab();
		} else if (text) {
			view.selectTextTab();
		} else if (graph) {
			view.selectGraphTab();
		}
	}

	public void selectTextTab() {
		view.selectTextTab();
	}

	public void selectTableTab() {
		view.selectTableTab();
	}

	public void selectGraphTab() {
		view.selectGraphTab();
	}

	public void setOnTabSelected(Consumer<ResultViewConfig.TabType> listener) {
		if (tabSelectionListener != null) {
			view.getTabPane().getSelectionModel().selectedItemProperty().removeListener(tabSelectionListener);
			tabSelectionListener = null;
		}
		if (listener == null) {
			return;
		}
		tabSelectionListener = (obs, oldTab, newTab) -> {
			ResultViewConfig.TabType tabType = getTabType(newTab);
			if (tabType != null) {
				listener.accept(tabType);
			}
		};
		view.getTabPane().getSelectionModel().selectedItemProperty().addListener(tabSelectionListener);
	}

	private ResultViewConfig.TabType getSelectedTabType() {
		Tab selected = view.getTabPane().getSelectionModel().getSelectedItem();
		return getTabType(selected);
	}

	private ResultViewConfig.TabType getTabType(Tab tab) {
		if (tab == null) {
			return null;
		}
		if (tab == view.getTabByType(ResultViewConfig.TabType.TEXT)) {
			return ResultViewConfig.TabType.TEXT;
		}
		if (tab == view.getTabByType(ResultViewConfig.TabType.TABLE)) {
			return ResultViewConfig.TabType.TABLE;
		}
		if (tab == view.getTabByType(ResultViewConfig.TabType.GRAPH)) {
			return ResultViewConfig.TabType.GRAPH;
		}
		return null;
	}

	private boolean isTabEnabled(ResultViewConfig.TabType tabType) {
		Tab tab = view.getTabByType(tabType);
		return tab != null && !tab.isDisable();
	}

	private void selectTab(ResultViewConfig.TabType tabType) {
		switch (tabType) {
			case TEXT -> view.selectTextTab();
			case TABLE -> view.selectTableTab();
			case GRAPH -> view.selectGraphTab();
		}
	}

	// ==============================================================================================
	// Facade API - Text Controller Delegation
	// ==============================================================================================

	public void configureTextFormats(SerializationFormat[] formats, SerializationFormat defaultFormat) {
		if (textController != null) {
			textController.setAvailableFormats(formats, defaultFormat);
		}
	}

	public void setOnFormatChanged(Consumer<SerializationFormat> listener) {
		if (textController != null) {
			textController.setOnFormatChanged(listener);
		}
	}

	public void updateText(String content) {
		if (textController != null) {
			textController.setContent(content);
		}
	}

	public SerializationFormat getPreferredTextFormat(SerializationFormat[] formats,
			SerializationFormat defaultFormat) {
		if (textController == null) {
			return defaultFormat;
		}
		return textController.getPreferredFormat(formats, defaultFormat);
	}

	// ==============================================================================================
	// Facade API - Graph Controller Delegation
	// ==============================================================================================

	public void displayGraph(String jsonLdData) {
		displayGraph(jsonLdData, -1);
	}

	public void displayGraph(String jsonLdData, int tripleCountHint) {
		if (graphController != null) {
			graphController.displayGraph(jsonLdData, tripleCountHint);
		}
	}

	public boolean exportSelectedTabFromShortcut() {
		ResultViewConfig.TabType selectedTabType = getSelectedTabType();
		if (selectedTabType == null || !isTabEnabled(selectedTabType)) {
			return false;
		}
		return switch (selectedTabType) {
			case TEXT -> textController != null && textController.exportFromShortcut();
			case TABLE -> tableController != null && tableController.exportFromShortcut();
			case GRAPH -> graphController != null && graphController.exportGraphFromShortcut();
		};
	}

	public boolean exportGraphFromShortcut() {
		if (graphController == null || getSelectedTabType() != ResultViewConfig.TabType.GRAPH
				|| !isTabEnabled(ResultViewConfig.TabType.GRAPH)) {
			return false;
		}
		return graphController.exportGraphFromShortcut();
	}

	public boolean reenergizeGraphFromShortcut() {
		if (graphController == null || getSelectedTabType() != ResultViewConfig.TabType.GRAPH
				|| !isTabEnabled(ResultViewConfig.TabType.GRAPH)) {
			return false;
		}
		return graphController.reenergizeLayoutFromShortcut();
	}

	public boolean centerGraphFromShortcut() {
		if (graphController == null || getSelectedTabType() != ResultViewConfig.TabType.GRAPH
				|| !isTabEnabled(ResultViewConfig.TabType.GRAPH)) {
			return false;
		}
		return graphController.centerGraphFromShortcut();
	}

	// ==============================================================================================
	// Facade API - Table Controller Delegation
	// ==============================================================================================

	public void updateTableView(String tableData) {
		if (tableController != null) {
			tableController.updateTable(tableData);
		}
	}

	/**
	 * Sets the content provider for the table view (used for export/copy).
	 */
	public void setFormatProvider(Function<SerializationFormat, String> provider) {
		if (tableController != null) {
			tableController.setFormatProvider(provider);
		}
	}

	public void clearResults() {
		if (textController != null)
			textController.clearContent();
		if (tableController != null)
			tableController.clear();
		if (graphController != null)
			graphController.clear();
	}

	public Parent getViewRoot() {
		return view.getRoot();
	}

	@Override
	public void close() {
		setOnTabSelected(null);
		if (textController != null) {
			textController.close();
		}
		if (tableController != null) {
			tableController.setFormatProvider(null);
			tableController.clear();
		}
		if (graphController != null) {
			graphController.close();
		}
		view.close();
	}

}
