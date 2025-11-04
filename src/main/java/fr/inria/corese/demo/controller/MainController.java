package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.view.MainView;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller for the Corese-GUI application.
 *
 * <p>This controller acts as the central coordinator for the application's user interface. It
 * manages the {@link MainView}, initializes its child controllers, and handles navigation between
 * different content views.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Attach and initialize the navigation bar controller.
 *   <li>Respond to navigation events triggered by the sidebar.
 *   <li>Load and display the corresponding content views in the main content area.
 * </ul>
 *
 * <p>This class focuses solely on coordinating views and controllers; it does not contain any
 * business logic or state management.
 */
public final class MainController {

  // ===== Fields =====

  private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

  /** The root view of the main application window. */
  private final MainView view;

  /** Controller responsible for managing navigation and sidebar interactions. */
  private final NavigationBarController navController;

  // ===== Constructor =====

  /**
   * Creates a new main controller for the specified {@link MainView}.
   *
   * <p>Initializes the navigation bar controller and sets up the default view.
   *
   * @param view the main application view (must not be {@code null})
   * @throws IllegalArgumentException if {@code view} is {@code null}
   */
  public MainController(MainView view) {
    this.view = Objects.requireNonNull(view, "MainView cannot be null");
    this.navController = new NavigationBarController();

    initialize();
  }

  // ===== Initialization =====

  /**
   * Initializes the main interface by attaching the navigation bar and registering event listeners
   * for navigation actions.
   */
  private void initialize() {
    try {
      attachControllers();
      registerEventHandlers();

      // Load the default view on startup
      handleNavigation("data-view");

      LOGGER.info("MainController initialization complete.");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error during MainController initialization", e);
    }
  }

  /** Attaches child controller views to the main layout. */
  private void attachControllers() {
    view.getNavigationContainer().getChildren().setAll(navController.getView());
  }

  /** Registers event listeners between controllers. */
  private void registerEventHandlers() {
    navController.setOnNavigate(this::handleNavigation);
  }

  // ===== Navigation Handling =====

  /**
   * Handles navigation requests triggered by the {@link NavigationBarController}.
   *
   * <p>Loads the corresponding view content and displays it in the central content area.
   *
   * @param viewName the identifier of the view to display (e.g., "data-view")
   */
  private void handleNavigation(String viewName) {
    LOGGER.info(() -> "Navigating to view: " + viewName);

    var content = navController.loadViewContent(viewName);
    if (content != null) {
      view.getContentArea().setCenter(content);
    } else {
      LOGGER.warning(() -> "Failed to load content for view: " + viewName);
    }
  }

  // ===== Accessors =====

  /**
   * Returns the main view managed by this controller.
   *
   * @return the associated {@link MainView} instance
   */
  public MainView getView() {
    return view;
  }
}
