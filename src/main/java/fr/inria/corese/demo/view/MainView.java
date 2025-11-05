package fr.inria.corese.demo.view;

import fr.inria.corese.demo.view.base.AbstractView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * MainView defines the primary layout of the application's user interface.
 *
 * <p>Structure:
 *
 * <ul>
 *   <li>A {@link BorderPane} as the root layout.
 *   <li>A sidebar ({@link VBox}) on the left, used for navigation.
 *   <li>A central {@link BorderPane} to display the main content area.
 * </ul>
 *
 * <p>This class defines only the layout and presentation containers. The {@link
 * fr.inria.corese.demo.controller.MainController} manages all interactions and view updates.
 *
 * <pre>
 * +------------------------------------------------+
 * |  BorderPane (Root)                             |
 * |  +---------------+--------------------------+  |
 * |  |  VBox         |  BorderPane              |  |
 * |  | (Navigation)  |  (Content Area)          |  |
 * |  +---------------+--------------------------+  |
 * +------------------------------------------------+
 * </pre>
 */
public final class MainView extends AbstractView {

  // ===== Constants =====

  /** Path to the stylesheet specific to this view. */
  private static final String STYLESHEET_PATH = "/styles/main-view.css";

  // ===== Fields =====

  /** Sidebar container for navigation components. */
  private final VBox navigationContainer = new VBox();

  /** Central content area where views are dynamically displayed. */
  private final BorderPane contentArea = new BorderPane();

  // ===== Constructor =====

  /** Creates the main view and initializes its layout structure. */
  public MainView() {
    super(new BorderPane(), STYLESHEET_PATH);
    initializeLayout();
  }

  // ===== Initialization =====

  /** Sets up the root layout hierarchy and default structure. */
  private void initializeLayout() {
    BorderPane rootPane = (BorderPane) getRoot();

    // Assign style classes for targeted CSS customization
    navigationContainer.getStyleClass().add("navigation-container");
    contentArea.getStyleClass().add("content-area");

    // Place subcontainers in the main layout
    rootPane.setLeft(navigationContainer);
    rootPane.setCenter(contentArea);
  }

  // ===== Public API =====

  /**
   * Replaces the sidebar navigation with the given navigation view.
   *
   * <p>This method is type-specific because the navigation area is fixed to a single kind of
   * component ({@link NavigationBarView}).
   */
  public void setNavigation(NavigationBarView navView) {
    navigationContainer.getChildren().setAll(navView.getRoot());
  }

  /**
   * Replaces the main content area with the specified view node.
   *
   * <p>This method accepts any kind of view displayed in the center of the application (data,
   * query, validation, etc.).
   */
  public void setContent(Node node) {
    contentArea.setCenter(node);
  }

  // ===== Accessors =====

  /**
   * Returns the sidebar container node.
   *
   * <p>Typically used for advanced layout manipulation or styling.
   */
  public VBox getNavigationContainer() {
    return navigationContainer;
  }

  /**
   * Returns the central content area node.
   *
   * <p>Typically used for transitions or dynamic content injection.
   */
  public BorderPane getContentArea() {
    return contentArea;
  }
}
