package fr.inria.corese.gui.feature.result;

import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResult;
import fr.inria.corese.gui.core.model.ValidationReportItem;
import fr.inria.corese.gui.feature.result.graph.GraphResultController;
import fr.inria.corese.gui.feature.result.table.TableResultController;
import fr.inria.corese.gui.feature.result.text.TextResultController;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Parent;

/**
 * Main controller for the Results Pane.
 *
 * <p>This controller acts as a facade, orchestrating specialized sub-controllers (Text, Table,
 * Graph) based on the provided configuration.
 *
 * <p>It implements the "Composite Controller" pattern.
 */
public class ResultController {

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

  // State
  private QueryResult currentResult;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a ResultController with the default configuration (all tabs enabled).
   */
  public ResultController() {
    this(ResultViewConfig.allTabs());
  }

  /**
   * Legacy constructor compatibility.
   *
   * @param buttons List of toolbar buttons (ignored for now as they are managed by specific controllers or config)
   * @param config The result view configuration
   */
  public ResultController(List<ButtonConfig> buttons, ResultViewConfig config) {
      this(config);
      // TODO: Handle legacy buttons if necessary, or refactor callers to not pass them if unused
  }

  /**
   * Constructs a ResultController with a specific configuration.
   *
   * @param config The configuration defining active tabs and behaviors.
   */
  public ResultController(ResultViewConfig config) {
    this.config = Objects.requireNonNull(config, "ResultViewConfig cannot be null");
    this.view = new ResultView();

    // Initialize sub-controllers only if their tab is enabled in config
    this.textController =
        this.config.hasTab(ResultViewConfig.TabType.TEXT) ? new TextResultController() : null;

    this.tableController =
        this.config.hasTab(ResultViewConfig.TabType.TABLE) ? new TableResultController() : null;

    this.graphController =
        this.config.hasTab(ResultViewConfig.TabType.GRAPH) ? new GraphResultController() : null;

    initializeView();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private void initializeView() {
    // 1. Setup Text Tab
    if (textController != null) {
      view.enableTextTab(textController.getView());
      textController.setOnFormatChanged(this::handleTextFormatChange);
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
   * Note: This only hides/shows tabs if the controller was initialized with them.
   */
  public void configureTabsForResult(boolean text, boolean table, boolean visual, boolean graph) {
      if (text && textController != null) view.enableTextTab(textController.getView());
      // else view.disableTextTab(); // If we had a disable method

      if (table && tableController != null) view.enableTableTab(tableController.getView());
      if (graph && graphController != null) view.enableGraphTab(graphController.getView());
      
      // Re-evaluate selection
      if (table) view.selectTableTab();
      else if (text) view.selectTextTab();
      else if (graph) view.selectGraphTab();
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

  // ==============================================================================================
  // Facade API - Graph Controller Delegation
  // ==============================================================================================

  public void displayGraph(String jsonLdData) {
      if (graphController != null) {
          graphController.displayGraph(jsonLdData);
      }
  }

  // ==============================================================================================
  // Facade API - Table Controller Delegation
  // ==============================================================================================

  public void updateTableView(String csvData) {
      if (tableController != null) {
          tableController.updateTable(csvData);
      }
  }

  public void displayReportItems(List<ValidationReportItem> items) {
      if (tableController != null) {
          tableController.displayReport(items);
      }
  }

  // ==============================================================================================
  // Data Processing
  // ==============================================================================================

  /**
   * Displays the provided query result in all active tabs.
   *
   * @param result The query result to display.
   */
  public void displayResult(QueryResult result) {
    this.currentResult = result;

    if (result == null) {
      clearAll();
      return;
    }

    // Update Text View
    if (textController != null) {
        // Logic handled by caller usually via updateText, or we implement auto-conversion here
    }
  }

  /** Clears all result views. */
  public void clearAll() {
    clearResults();
  }

  public void clearResults() {
    this.currentResult = null;
    if (textController != null) textController.clearContent();
    if (tableController != null) tableController.clear();
    if (graphController != null) graphController.clear();
  }

  // ==============================================================================================
  // Event Handlers
  // ==============================================================================================

  private void handleTextFormatChange(SerializationFormat newFormat) {
    if (currentResult != null) {
      updateTextContent(newFormat);
    }
  }

  private void updateTextContent(SerializationFormat format) {
    // Placeholder
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Returns the root view node of this controller.
   *
   * @return The Parent node (usually a TabPane wrapper).
   */
  public Parent getViewRoot() {
    return view.getRoot();
  }
  
  /**
   * Returns the view object (internal use).
   */
  public ResultView getView() {
      return view;
  }

  /**
   * Sets a callback for when an export action is triggered in sub-controllers.
   *
   * @param handler The export handler.
   */
  public void setOnExport(Consumer<QueryResult> handler) {
    // propagate to sub-controllers if they support export events
  }
}