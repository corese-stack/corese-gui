package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.core.ResultViewConfig;
import fr.inria.corese.gui.enums.SerializationFormat;
import fr.inria.corese.gui.model.ValidationReportItem;
import fr.inria.corese.gui.view.ResultView;
import javafx.application.Platform;

import java.util.List;
import java.util.function.Consumer;

/**
 * Orchestrator controller for result display with multiple view types.
 *
 * <p>This controller acts as a facade that composes four specialized sub-controllers:
 *
 * <ul>
 *   <li>{@link TextResultController} - Text results with format selection
 *   <li>{@link TableResultController} - Tabular SPARQL results with pagination
 *   <li>{@link VisualResultController} - SHACL validation reports
 *   <li>{@link GraphResultController} - RDF graph visualization
 * </ul>
 *
 * <p>The controller provides a unified API for updating results while delegating the actual work
 * to specialized sub-controllers. This design follows the Composition pattern, avoiding the "God
 * Class" anti-pattern.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * // Configure which tabs to show
 * ResultViewConfig config = ResultViewConfig.builder()
 *     .withTextTab()
 *     .withTableTab()
 *     .build();
 *
 * // Create controller
 * ResultController controller = new ResultController(buttons, config);
 *
 * // Use unified API
 * controller.updateText(sparqlResults);
 * controller.updateTableView(csvResults);
 * Parent view = controller.getViewRoot();
 * }</pre>
 */
public class ResultController {

  // ==============================================================================================
  // Fields - Sub-Controllers (Composition)
  // ==============================================================================================

  /** Sub-controller for text-based results (optional, created only if TEXT tab is configured). */
  private final TextResultController textController;

  /** Sub-controller for tabular results (optional, created only if TABLE tab is configured). */
  private final TableResultController tableController;

  /**
   * Sub-controller for visual SHACL reports (optional, created only if VISUAL tab is configured).
   */
  private final VisualResultController visualController;

  /** Sub-controller for graph visualization (optional, created only if GRAPH tab is configured). */
  private final GraphResultController graphController;

  // ==============================================================================================
  // Fields - View & Configuration
  // ==============================================================================================

  /** The main view component containing the tab pane. */
  private final ResultView view;

  /** Immutable configuration specifying which tabs are enabled. */
  private final ResultViewConfig config;

  // ==============================================================================================
  // Constructors
  // ==============================================================================================

  /**
   * Constructs a ResultController with specified buttons and configuration.
   *
   * @param buttons List of button configurations for toolbars
   * @param config Configuration specifying which tabs to enable (defaults if null)
   */
  public ResultController(List<ButtonConfig> buttons, ResultViewConfig config) {
    this.config = config != null ? config : ResultViewConfig.defaultConfig();
    this.view = new ResultView();

    // Instantiate only the sub-controllers for configured tabs
    this.textController =
        this.config.hasTab(ResultViewConfig.TabType.TEXT)
            ? new TextResultController(buttons)
            : null;

    this.tableController =
        this.config.hasTab(ResultViewConfig.TabType.TABLE) ? new TableResultController() : null;

    this.visualController =
        this.config.hasTab(ResultViewConfig.TabType.VISUAL) ? new VisualResultController() : null;

    this.graphController =
        this.config.hasTab(ResultViewConfig.TabType.GRAPH) ? new GraphResultController() : null;

    initialize();
  }

  /**
   * Constructs a ResultController with specified buttons and default configuration.
   *
   * @param buttons List of button configurations for toolbars
   */
  public ResultController(List<ButtonConfig> buttons) {
    this(buttons, ResultViewConfig.defaultConfig());
  }

  /**
   * Constructs a ResultController with default buttons and configuration.
   */
  public ResultController() {
    this(List.of(), ResultViewConfig.defaultConfig());
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /**
   * Initializes the view by assembling configured tabs.
   *
   * <p>Uses high-level methods from ResultView to respect Demeter's Law and reduce coupling.
   */
  private void initialize() {
    view.clearAllTabs();

    // Add TEXT tab if configured
    if (shouldEnableTab(ResultViewConfig.TabType.TEXT, textController)) {
      view.enableTextTab(textController.getView());
    }

    // Add VISUAL tab if configured
    if (shouldEnableTab(ResultViewConfig.TabType.VISUAL, visualController)) {
      view.enableVisualTab(visualController.getView());
    }

    // Add TABLE tab if configured
    if (shouldEnableTab(ResultViewConfig.TabType.TABLE, tableController)) {
      view.enableTableTab(tableController.getView());
    }

    // Add GRAPH tab if configured
    if (shouldEnableTab(ResultViewConfig.TabType.GRAPH, graphController)) {
      view.enableGraphTab(graphController.getView());
    }
  }

  // ==============================================================================================
  // Public API - Text Results
  // ==============================================================================================

  /**
   * Updates the text display with new content.
   *
   * <p>This method delegates to the TextResultController if the TEXT tab is configured.
   * Thread-safe: automatically runs on the JavaFX Application Thread.
   *
   * @param content The text content to display (RDF, SPARQL results, etc.)
   */
  public void updateText(String content) {
    if (textController != null) {
      runOnFxThread(() -> textController.updateText(content));
    }
  }

  /**
   * Sets the callback for format change events in the text view.
   *
   * <p>Thread-safe: automatically runs on the JavaFX Application Thread.
   *
   * @param listener Consumer that receives the newly selected format
   */
  public void setOnFormatChanged(Consumer<SerializationFormat> listener) {
    if (textController != null) {
      runOnFxThread(() -> textController.setOnFormatChanged(listener));
    }
  }

  // ==============================================================================================
  // Public API - Table Results
  // ==============================================================================================

  /**
   * Updates the table view with CSV formatted SPARQL results.
   *
   * <p>This method delegates to the TableResultController if the TABLE tab is configured.
   * Thread-safe: automatically runs on the JavaFX Application Thread.
   *
   * @param csvResult The CSV formatted result string
   */
  public void updateTableView(String csvResult) {
    if (tableController != null) {
      runOnFxThread(() -> tableController.updateTable(csvResult));
    }
  }

  // ==============================================================================================
  // Public API - Visual/SHACL Results
  // ==============================================================================================

  /**
   * Displays SHACL validation report items.
   *
   * <p>This method delegates to the VisualResultController if the VISUAL tab is configured.
   * Thread-safe: automatically runs on the JavaFX Application Thread.
   *
   * @param items List of validation report items to display
   */
  public void displayReportItems(List<ValidationReportItem> items) {
    if (visualController != null) {
      runOnFxThread(() -> visualController.displayReport(items));
    }
  }


  // ==============================================================================================
  // Public API - Graph Visualization
  // ==============================================================================================

  /**
   * Displays an RDF graph visualization from TTL data.
   *
   * <p>This method delegates to the GraphResultController if the GRAPH tab is configured.
   * Thread-safe: automatically runs on the JavaFX Application Thread.
   *
   * @param ttlData The RDF data in Turtle format
   */
  public void displayGraph(String ttlData) {
    if (graphController != null) {
      runOnFxThread(() -> graphController.displayGraph(ttlData));
    }
  }

  // ==============================================================================================
  // Public API - General Operations
  // ==============================================================================================

  /**
   * Clears all results from all configured tabs.
   *
   * <p>Thread-safe: automatically runs on the JavaFX Application Thread.
   */
  public void clearResults() {
    runOnFxThread(
        () -> {
          if (textController != null) {
            textController.clear();
          }
          if (tableController != null) {
            tableController.clear();
          }
          if (visualController != null) {
            visualController.clear();
          }
          if (graphController != null) {
            graphController.clear();
          }
        });
  }

  /**
   * Returns the main ResultView component.
   *
   * @return The ResultView instance
   */
  public ResultView getView() {
    return view;
  }

  /**
   * Returns the root node of the view for embedding in parent containers.
   *
   * @return The root Parent node
   */
  public javafx.scene.Parent getViewRoot() {
    return view.getRoot();
  }

  /**
   * Selects the Text tab programmatically.
   *
   * <p>Thread-safe: automatically runs on the JavaFX Application Thread.
   */
  public void selectTextTab() {
    if (config.hasTab(ResultViewConfig.TabType.TEXT)) {
      runOnFxThread(() -> view.selectTextTab());
    }
  }

  /**
   * Enables or disables a specific tab.
   *
   * <p>When disabled, the tab is shown but grayed out and not selectable.
   * Thread-safe: automatically runs on the JavaFX Application Thread.
   *
   * @param tabType The type of tab to enable/disable
   * @param enabled True to enable the tab, false to disable it
   */
  public void setTabEnabled(ResultViewConfig.TabType tabType, boolean enabled) {
    runOnFxThread(
        () -> {
          javafx.scene.control.Tab tab = getTabForType(tabType);
          view.setTabEnabled(tab, enabled);
        });
  }

  /**
   * Configures tab states based on query result type.
   *
   * <p>This is a convenience method for enabling/disabling multiple tabs at once based on the
   * query result characteristics.
   *
   * @param enableText Enable text tab
   * @param enableVisual Enable visual tab
   * @param enableTable Enable table tab
   * @param enableGraph Enable graph tab
   */
  public void configureTabsForResult(
      boolean enableText, boolean enableVisual, boolean enableTable, boolean enableGraph) {
    if (config.hasTab(ResultViewConfig.TabType.TEXT)) {
      setTabEnabled(ResultViewConfig.TabType.TEXT, enableText);
    }
    if (config.hasTab(ResultViewConfig.TabType.VISUAL)) {
      setTabEnabled(ResultViewConfig.TabType.VISUAL, enableVisual);
    }
    if (config.hasTab(ResultViewConfig.TabType.TABLE)) {
      setTabEnabled(ResultViewConfig.TabType.TABLE, enableTable);
    }
    if (config.hasTab(ResultViewConfig.TabType.GRAPH)) {
      setTabEnabled(ResultViewConfig.TabType.GRAPH, enableGraph);
    }
  }

  // ==============================================================================================
  // Helper Methods
  // ==============================================================================================

  /**
   * Maps a TabType to its corresponding Tab instance.
   *
   * @param tabType The tab type
   * @return The Tab instance, or null if not configured
   */
  private javafx.scene.control.Tab getTabForType(ResultViewConfig.TabType tabType) {
    return view.getTabByType(tabType);
  }

  /**
   * Checks if a tab should be enabled based on configuration and controller availability.
   *
   * @param tabType The type of tab to check
   * @param controller The controller instance (can be null)
   * @return True if the tab is configured and has a non-null controller
   */
  private boolean shouldEnableTab(ResultViewConfig.TabType tabType, Object controller) {
    return config.hasTab(tabType) && controller != null;
  }

  /**
   * Executes a UI update on the JavaFX Application Thread.
   *
   * <p>This method ensures thread-safety by checking if we're already on the FX thread. If not, it
   * schedules the action using Platform.runLater.
   *
   * @param action The UI update action to execute
   */
  private void runOnFxThread(Runnable action) {
    if (Platform.isFxApplicationThread()) {
      action.run();
    } else {
      Platform.runLater(action);
    }
  }
}
