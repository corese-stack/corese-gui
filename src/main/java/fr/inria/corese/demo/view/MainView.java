package fr.inria.corese.demo.view;

import fr.inria.corese.demo.view.base.AbstractView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * MainView defines the root layout of the application's user interface.
 *
 * <p>Structure:
 *
 * <ul>
 *   <li>A {@link javafx.scene.layout.BorderPane} as the root layout.
 *   <li>A sidebar ({@link javafx.scene.layout.VBox}) on the left.
 *   <li>A central content area ({@link javafx.scene.layout.BorderPane}) in the center.
 * </ul>
 *
 * <p>This class defines only the layout structure. All logic is handled by {@code MainController}.
 *
 * <p>Visual representation:
 *
 * <pre>
 * +------------------------------------------------+
 * |  BorderPane (Root)                             |
 * |  +---------------+--------------------------+  |
 * |  |  VBox         |  BorderPane              |  |
 * |  |  (Navigation) |  (Content Area)          |  |
 * |  +---------------+--------------------------+  |
 * +------------------------------------------------+
 * </pre>
 */
public final class MainView extends AbstractView {

  // ===== Constants =====

  /** Path to the stylesheet specific to this view. */
  private static final String STYLESHEET_PATH = "/styles/main-view.css";

  // ===== Fields =====

  private final VBox navigationContainer = new VBox();
  private final BorderPane contentArea = new BorderPane();

  // ===== Constructor =====

  /** Creates a new instance of the main view. Initializes the UI structure and applies styles. */
  public MainView() {
    super(new BorderPane(), STYLESHEET_PATH);
    initializeLayout();
  }

  // ===== Initialization =====

  /** Configures the layout hierarchy and spacing for the main interface. */
  private void initializeLayout() {
    BorderPane rootPane = (BorderPane) this.getRoot();

    navigationContainer.getStyleClass().addAll("nav-container", "sidebar");

    rootPane.setLeft(navigationContainer);
    rootPane.setCenter(contentArea);
  }

  // ===== Getters =====

  /** Returns the sidebar container (navigation area). */
  public VBox getNavigationContainer() {
    return navigationContainer;
  }

  /** Returns the central content area. */
  public BorderPane getContentArea() {
    return contentArea;
  }
}
