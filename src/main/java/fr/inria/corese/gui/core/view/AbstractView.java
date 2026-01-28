package fr.inria.corese.gui.core.view;

import fr.inria.corese.gui.utils.CssUtils;





import javafx.scene.Parent;

/**
 * Base class for all application views.
 *
 * <p>This class provides a consistent structure for managing JavaFX view components. It handles
 * common functionality such as root management and CSS loading, so that concrete views can focus on
 * their layout and content.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * public final class MyView extends AbstractView {
 *     public MyView() {
 *         super(new BorderPane(), "/styles/my-view.css");
 *         initializeLayout();
 *     }
 *
 *     private void initializeLayout() {
 *         BorderPane pane = (BorderPane) getRoot();
 *         // configure layout...
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractView {

  // ===== Fields =====

  /** The root node of this view. */
  private final Parent root;

  // ===== Constructor =====

  /**
   * Creates a view with a given root container and an optional stylesheet.
   *
   * @param root the root node (layout container) of the view
   * @param stylesheetPath the classpath path to the stylesheet (nullable)
   */
  protected AbstractView(Parent root, String stylesheetPath) {
    this.root = root;
    if (stylesheetPath != null && !stylesheetPath.isBlank()) {
      CssUtils.applyViewStyles(root, stylesheetPath);
    }
  }

  // ===== Public API =====

  /**
   * Returns the root node of this view. This node can be attached to a {@link javafx.scene.Scene}.
   *
   * @return the root node
   */
  public Parent getRoot() {
    return root;
  }
}
