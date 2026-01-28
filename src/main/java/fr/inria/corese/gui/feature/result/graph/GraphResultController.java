package fr.inria.corese.gui.feature.result.graph;

import fr.inria.corese.gui.utils.ThemeManager;







import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for graph visualization using WebView and JavaScript.
 *
 * <p>This controller manages:
 *
 * <ul>
 *   <li>WebView for rendering RDF graphs
 *   <li>Integration with kg-graph web component
 *   <li>TTL to visual graph conversion
 *   <li>Dynamic graph updates
 *   <li>Theme-aware graph rendering
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * GraphResultController controller = new GraphResultController();
 * controller.displayGraph(ttlData);
 * Node view = controller.getView();
 * }</pre>
 */
public class GraphResultController {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  /** Logger for this class. */
  private static final Logger logger = LoggerFactory.getLogger(GraphResultController.class);

  /** Path to the HTML file containing the graph visualization component. */
  private static final String GRAPH_HTML_PATH = "/web/index.html";

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  /** WebView component for rendering the graph. */
  private final WebView webView;
  
  /** Flag indicating if the page is currently loaded and ready. */
  private boolean pageLoaded = false;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Constructs a new GraphResultController with an initialized WebView. */
  public GraphResultController() {
    this.webView = new WebView();
    setupThemeListener();
  }
  
  /**
   * Sets up a listener for theme changes to update the graph appearance.
   */
  private void setupThemeListener() {
    ThemeManager.getInstance().themeProperty().addListener((obs, oldTheme, newTheme) -> {
      if (newTheme != null && pageLoaded) {
        applyThemeToGraph();
      }
    });
  }
  
  /**
   * Applies the current theme to the loaded graph.
   */
  private void applyThemeToGraph() {
    Platform.runLater(() -> {
      if (pageLoaded) {
        boolean isDark = ThemeManager.getInstance().isDarkTheme(
            ThemeManager.getInstance().getCurrentThemeName());
        String script = "if (typeof applyTheme === 'function') { applyTheme(" + isDark + "); }";
        webView.getEngine().executeScript(script);
      }
    });
  }

  // ==============================================================================================
  // Graph Display
  // ==============================================================================================

  /**
   * Displays an RDF graph from TTL (Turtle) formatted data.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Loads the graph visualization HTML page
   *   <li>Waits for the page to finish loading
   *   <li>Injects the TTL data into the kg-graph web component
   *   <li>Renders the graph visually
   *   <li>Applies the current theme
   * </ol>
   *
   * @param ttlData The RDF data in Turtle format (null or empty clears the view)
   */
  public void displayGraph(String ttlData) {
    Platform.runLater(
        () -> {
          if (ttlData == null || ttlData.isBlank()) {
            webView.getEngine().load("about:blank");
            pageLoaded = false;
            return;
          }

          try {
            String htmlPath = getClass().getResource(GRAPH_HTML_PATH).toExternalForm();

            // Create a one-time listener for page load completion
            final ChangeListener<Worker.State> loadListener =
                new ChangeListener<>() {
                  @Override
                  public void changed(
                      javafx.beans.value.ObservableValue<? extends Worker.State> observable,
                      Worker.State oldValue,
                      Worker.State newValue) {
                    if (newValue == Worker.State.SUCCEEDED) {
                      webView.getEngine().getLoadWorker().stateProperty().removeListener(this);
                      pageLoaded = true;
                      injectGraphData(ttlData);
                      applyThemeToGraph();
                    }
                  }
                };

            webView.getEngine().getLoadWorker().stateProperty().addListener(loadListener);
            webView.getEngine().load(htmlPath);

          } catch (Exception e) {
            logger.error("Failed to display graph", e);
          }
        });
  }

  /**
   * Injects TTL data into the loaded web page.
   *
   * <p>This method generates and executes JavaScript code to:
   *
   * <ol>
   *   <li>Wait for the container element to be ready
   *   <li>Create or recreate the kg-graph custom element
   *   <li>Set the TTL data on the element
   * </ol>
   *
   * @param ttlData The TTL formatted data to inject
   */
  private void injectGraphData(String ttlData) {
    // Escape TTL data for JavaScript string literal
    String escapedTtl =
        ttlData
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "");

    // JavaScript code to inject graph data
    String script =
        "(function() {"
            + "  var container = document.getElementById('container');"
            + "  if (!container) { setTimeout(arguments.callee, 50); return; }"
            + "  var old = document.getElementById('myGraph');"
            + "  if (old && old.parentNode) { old.parentNode.removeChild(old); }"
            + "  var newGraph = document.createElement('kg-graph');"
            + "  newGraph.id = 'myGraph';"
            + "  newGraph.setAttribute('width', '100%');"
            + "  newGraph.setAttribute('height', '100%');"
            + "  container.appendChild(newGraph);"
            + "  function setTTL() {"
            + "    var el = document.getElementById('myGraph');"
            + "    if (el) { el.ttl = '"
            + escapedTtl
            + "'; }"
            + "    else { setTimeout(setTTL, 50); }"
            + "  }"
            + "  setTTL();"
            + "})();";

    webView.getEngine().executeScript(script);
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Clears the graph view.
   */
  public void clear() {
    Platform.runLater(() -> {
      webView.getEngine().load("about:blank");
      pageLoaded = false;
    });
  }

  /**
   * Returns the root view node.
   *
   * @return The WebView component
   */
  public Node getView() {
    return webView;
  }
}
