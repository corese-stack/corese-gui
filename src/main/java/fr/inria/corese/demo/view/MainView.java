package fr.inria.corese.demo.view;

import fr.inria.corese.demo.view.base.AbstractView;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
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

    // Place subcontainers in the main layout
    rootPane.setLeft(navigationContainer);
    rootPane.setCenter(contentArea);
  }

  // ===== Public API =====

  /**
   * Sets the navigation sidebar using a root component from a controller.
   *
   * @param navigationRoot the root node of the navigation component
   */
  public void setNavigationRoot(Parent navigationRoot) {
    navigationContainer.getChildren().setAll(navigationRoot);
    VBox.setVgrow(navigationRoot, Priority.ALWAYS);
  }

  /** Replaces the central content area with the given content view. */
  public void setContent(AbstractView contentView) {
    contentArea.setCenter(contentView.getRoot());
  }
}
