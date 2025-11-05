package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.manager.ViewManager;
import fr.inria.corese.demo.view.MainView;
import fr.inria.corese.demo.view.ViewId;
import fr.inria.corese.demo.view.utils.TransitionUtils;
import javafx.scene.Node;
import javafx.util.Duration;

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
   */
  public MainController(MainView view) {
    this.view = view;
    this.navController = new NavigationBarController();
    this.viewManager = new ViewManager();
    initialize();
  }

  // ==== Initialization =====

  /** Initializes layout and connects event handlers. */
  private void initialize() {
    // Set up the navigation bar in the main view
    view.setNavigation(navController.getView());

    // Handle navigation actions
    navController.setOnNavigate(this::showView);

    // Show the default view at startup
    this.showView(ViewId.DATA);
  }

  /**
   * Displays the requested view with a simple fade transition.
   *
   * @param viewId the identifier of the view to display
   */
  private void showView(ViewId viewId) {
    Node newContent = viewManager.getView(viewId);

    TransitionUtils.fadeReplace(view.getContentArea(), newContent, Duration.millis(150));
    navController.setActiveView(viewId);
  }

  public MainView getView() {
    return view;
  }
}
