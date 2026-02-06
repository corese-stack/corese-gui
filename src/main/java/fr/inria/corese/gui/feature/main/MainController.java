package fr.inria.corese.gui.feature.main;

import fr.inria.corese.gui.core.enums.ViewId;
import fr.inria.corese.gui.feature.main.navigation.NavigationBarController;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main controller for the Corese-GUI application.
 *
 * <p>Coordinates navigation between sidebar and content area, delegates view loading to {@link
 * ViewManager}, and applies smooth transitions.
 */
public final class MainController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

  // ===== Fields =====

  /** The main application view. */
  private final MainView view;

  /** Controller for the sidebar navigation. */
  private final NavigationBarController navController;

  /** Manager for loading and caching views. */
  private final ViewManager viewManager;

  // ===== Constructor =====

  /**
   * Creates the main controller with the given main view.
   *
   * @param view the main application view
   * @param navController the navigation bar controller
   * @param viewManager the view manager for loading views
   */
  public MainController(
      MainView view, NavigationBarController navController, ViewManager viewManager) {
    this.view = view;
    this.navController = navController;
    this.viewManager = viewManager;
    initialize();
  }

  // ==== Initialization =====

  /** Initializes layout and connects event handlers. */
  private void initialize() {
    // Set up the navigation bar in the main view
    view.setNavigationRoot(navController.getRoot());

    // Handle navigation actions
    navController.setOnNavigate(this::displayView);

    // Show the default view at startup (instant display)
    this.displayView(ViewId.DATA);

    // Preload other views in background after a short delay to ensure fluidity
    preloadOtherViewsAsync(ViewId.DATA);
  }

  /**
   * Displays the requested view with a simple fade transition.
   *
   * @param viewId the identifier of the view to display
   */
  private void displayView(ViewId viewId) {
    view.setContent(viewManager.getView(viewId));
    navController.selectView(viewId);
  }

  /**
   * Preloads views other than the initial one in a staggered way.
   *
   * @param initialView the view already loaded
   */
  private void preloadOtherViewsAsync(ViewId initialView) {
    Thread preloadThread = new Thread(() -> {
      try {
        // Wait for the app to be fully rendered and stable
        Thread.sleep(1000);

        for (ViewId viewId : ViewId.values()) {
          if (viewId != initialView) {
            // Load each view on the FX thread one by one
            Platform.runLater(() -> {
              try {
                viewManager.getView(viewId);
                LOGGER.debug("Background preloaded view: {}", viewId);
              } catch (Exception e) {
                LOGGER.warn("Failed to background preload view: {}", viewId, e);
              }
            });
            // Small gap between loads to keep UI responsive
            Thread.sleep(300);
          }
        }
        LOGGER.debug("All views preloaded successfully in background.");
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
        LOGGER.debug("View preloading interrupted");
      }
    }, "ViewPreloader");

    preloadThread.setDaemon(true);
    preloadThread.start();
  }
}
