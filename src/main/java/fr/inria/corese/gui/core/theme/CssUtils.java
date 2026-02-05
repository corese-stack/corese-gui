package fr.inria.corese.gui.core.theme;

import java.net.URL;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing CSS stylesheets across the application.
 *
 * <p>This class provides centralized methods to apply:
 *
 * <ul>
 *   <li>The base stylesheet (e.g., "/css/base.css")
 *   <li>Scene-level styles
 *   <li>Per-view styles
 * </ul>
 *
 * <p>Theme management is handled separately by {@link ThemeManager}.
 */
public final class CssUtils {

  // ===== Fields =====

  private static final Logger LOGGER = LoggerFactory.getLogger(CssUtils.class);

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
      LOGGER.warn("CssUtils.applyViewStyles(): invalid arguments.");
      return;
    }

    URL css = CssUtils.class.getResource(stylesheetPath);
    if (css == null) {
      LOGGER.warn("View stylesheet not found: {}", stylesheetPath);
      return;
    }

    node.getStylesheets().add(css.toExternalForm());
    LOGGER.debug("Applied stylesheet to view: {}", stylesheetPath);
  }

  /**
   * Converts a JavaFX Color to a CSS hex string.
   *
   * @param color The color to convert.
   * @return A string in the format "#RRGGBB".
   */
  public static String toHex(Color color) {
    if (color == null) return "#000000";
    return String.format(
        "#%02X%02X%02X",
        (int) (color.getRed() * 255),
        (int) (color.getGreen() * 255),
        (int) (color.getBlue() * 255));
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
      LOGGER.warn("CssUtils.applySceneStyles(): invalid arguments.");
      return;
    }

    URL css = CssUtils.class.getResource(stylesheetPath);
    if (css == null) {
      LOGGER.warn("Scene stylesheet not found: {}", stylesheetPath);
      return;
    }

    scene.getStylesheets().add(css.toExternalForm());
    LOGGER.debug("Applied stylesheet to scene: {}", stylesheetPath);
  }
}
