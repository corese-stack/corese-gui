package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.manager.DataManager;
import fr.inria.corese.demo.manager.QueryManager;
import fr.inria.corese.demo.view.NavigationBarView;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;

/**
 * Controller for the navigation bar.
 *
 * <p>Manages navigation between different views of the application and notifies a parent controller
 * when a navigation action occurs. Some views are cached to preserve their state.
 */
public final class NavigationBarController {

  // ===== Fields =====

  /** Global state and data managers. */
  private final QueryManager stateManager;

  private final DataManager dataManager;

  /** The navigation bar view (sidebar UI). */
  private final NavigationBarView view;

  /** The currently active view name. */
  private String currentViewName;

  /** Cached FXML views (stateful). */
  private final Map<String, Node> cachedViews = new HashMap<>();

  /** Cached controllers for preloaded views. */
  private final Map<String, Object> cachedControllers = new HashMap<>();

  /** Set of view names to preload and cache. */
  private final Set<String> cachedViewNames =
      Set.of("data-view", "query-view", "validation-view", "rdf-editor-view");

  /** Callback used to notify the parent controller when a new view is selected. */
  private Consumer<String> onNavigate;

  // ===== Constructor =====

  /** Creates a new navigation bar controller. */
  public NavigationBarController() {
    this.stateManager = QueryManager.getInstance();
    this.dataManager = DataManager.getInstance();
    this.view = new NavigationBarView();

    // Preload cached views at startup
    for (String viewName : cachedViewNames) {
      preloadView(viewName);
    }

    initializeButtons();
  }

  // ===== Initialization =====

  /**
   * Preloads and caches a specified view and its controller. Called once for all cached views at
   * startup.
   *
   * @param viewName the view name (e.g., "query-view")
   */
  private void preloadView(String viewName) {
    String fxmlPath = "/fr/inria/corese/demo/" + viewName + ".fxml";
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Node content = loader.load();
      Object controller = loader.getController();
      cachedViews.put(viewName, content);
      cachedControllers.put(viewName, controller);
      stateManager.addLogEntry("Preloaded and cached " + viewName);
    } catch (IOException e) {
      stateManager.addLogEntry("Error preloading " + viewName + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Initializes button event handlers to trigger navigation events. */
  private void initializeButtons() {
    view.getDataButton().setOnAction(e -> handleNavigation("data-view"));
    view.getRdfEditorButton().setOnAction(e -> handleNavigation("rdf-editor-view"));
    view.getValidationButton().setOnAction(e -> handleNavigation("validation-view"));
    view.getQueryButton().setOnAction(e -> handleNavigation("query-view"));
    view.getSettingsButton().setOnAction(e -> handleNavigation("settings-view"));
  }

  // ===== Navigation =====

  /**
   * Called when a navigation button is clicked.
   *
   * @param viewName the name of the view to navigate to
   */
  private void handleNavigation(String viewName) {
    stateManager.addLogEntry("Navigation to " + viewName);
    if (onNavigate != null) {
      onNavigate.accept(viewName);
    }
  }

  /**
   * Loads or retrieves from cache the content node corresponding to a view name.
   *
   * @param viewName the name of the view
   * @return the loaded or cached content node, or {@code null} if an error occurred
   */
  public Node loadViewContent(String viewName) {
    try {
      currentViewName = viewName;
      Node content;

      if (cachedViewNames.contains(viewName)) {
        content = cachedViews.get(viewName);
        stateManager.addLogEntry("Using cached " + viewName);
      } else {
        String fxmlPath = "/fr/inria/corese/demo/" + viewName + ".fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        content = loader.load();
        Object controller = loader.getController();

        if (controller instanceof DataViewController) {
          dataManager.restoreState();
          stateManager.addLogEntry("Restored state for DataViewController");
        }
      }

      // Update UI selection
      Button selectedButton = getButtonForView(viewName);
      if (selectedButton != null) {
        view.setButtonSelected(selectedButton);
      }

      return content;

    } catch (IOException e) {
      stateManager.addLogEntry("Error loading view: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  // ===== Utilities =====

  /** Returns the corresponding navigation button for a view name. */
  private Button getButtonForView(String viewName) {
    return switch (viewName) {
      case "data-view" -> view.getDataButton();
      case "validation-view" -> view.getValidationButton();
      case "rdf-editor-view" -> view.getRdfEditorButton();
      case "query-view" -> view.getQueryButton();
      case "settings-view" -> view.getSettingsButton();
      default -> null;
    };
  }

  // ===== Accessors =====

  /** Returns the navigation bar view. */
  public NavigationBarView getView() {
    return view;
  }

  /** Sets the callback invoked when the user selects a new view. */
  public void setOnNavigate(Consumer<String> handler) {
    this.onNavigate = handler;
  }

  /** Returns the cached content node for a given view name. */
  public Node getCachedView(String viewName) {
    return cachedViews.get(viewName);
  }

  /** Returns the controller associated with a cached view, if any. */
  public Object getCachedController(String viewName) {
    return cachedControllers.get(viewName);
  }
}
