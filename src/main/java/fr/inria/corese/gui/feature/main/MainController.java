package fr.inria.corese.gui.feature.main;

import fr.inria.corese.gui.core.enums.ViewId;

import fr.inria.corese.gui.core.manager.ViewManager;







/**
 * Main controller for the Corese-GUI application.
 *
 * <p>Coordinates navigation between sidebar and content area, delegates view loading to {@link
 * ViewManager}, and applies smooth transitions.
 */
public final class MainController {

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

    // Preload all views to avoid lag/logs on first switch
    viewManager.preloadAllViews();

    // Show the default view at startup
    this.displayView(ViewId.DATA);
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
}
