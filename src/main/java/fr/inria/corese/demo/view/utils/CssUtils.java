package fr.inria.corese.demo.view.utils;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Utility class for managing CSS stylesheets across the application.
 *
 * <p>This class provides centralized methods to apply:
 *
 * <ul>
 *   <li>The base stylesheet (e.g., "/styles/base.css")
 *   <li>Scene-level styles
 *   <li>Per-view styles
 * </ul>
 *
 * <p>Theme management is handled separately by {@link ThemeManager}.
 */
public final class CssUtils {

  // ===== Fields =====

  private static final Logger LOGGER = Logger.getLogger(CssUtils.class.getName());

  // ===== Constructor =====

  private CssUtils() {
    // Utility class: prevent instantiation
  }

  // ===== Public API =====

  /**
   * Applies a stylesheet to a specific view (typically its root layout node).
   *
   * @param node the root node of the view
   * @param stylesheetPath the path to the stylesheet in the classpath
   */
  public static void applyViewStyles(Parent node, String stylesheetPath) {
    if (node == null || stylesheetPath == null || stylesheetPath.isBlank()) {
      LOGGER.warning("CssUtils.applyViewStyles(): invalid arguments.");
      return;
    }

    URL css = CssUtils.class.getResource(stylesheetPath);
    if (css == null) {
      LOGGER.log(Level.WARNING, "View stylesheet not found: {0}", stylesheetPath);
      return;
    }

    node.getStylesheets().add(css.toExternalForm());
    LOGGER.log(Level.FINE, "Applied stylesheet to view: {0}", stylesheetPath);
  }

  // ===== Private Methods =====

  /**
   * Applies a custom stylesheet to a given scene.
   *
   * @param scene the scene to which the stylesheet will be applied
   * @param stylesheetPath the path to the stylesheet in the classpath
   */
  public static void applySceneStyles(Scene scene, String stylesheetPath) {
    if (scene == null || stylesheetPath == null || stylesheetPath.isBlank()) {
      LOGGER.warning("CssUtils.applySceneStyles(): invalid arguments.");
      return;
    }

    URL css = CssUtils.class.getResource(stylesheetPath);
    if (css == null) {
      LOGGER.log(Level.WARNING, "Scene stylesheet not found: {0}", stylesheetPath);
      return;
    }

    scene.getStylesheets().add(css.toExternalForm());
    LOGGER.log(Level.FINE, "Applied stylesheet to scene: {0}", stylesheetPath);
  }
}
