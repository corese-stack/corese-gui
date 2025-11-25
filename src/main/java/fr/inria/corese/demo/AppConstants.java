package fr.inria.corese.demo;

import javafx.scene.paint.Color;

/**
 * Application-wide constants for Corese GUI.
 *
 * <p>Contains UI dimensions, application metadata, URLs, and predefined color palettes.
 */
public final class AppConstants {

  // ===== Prevent instantiation =====
  private AppConstants() {
    throw new AssertionError("Constants class cannot be instantiated");
  }

  // ===== Application Window =====
  public static final double DEFAULT_WIDTH = 1400;
  public static final double DEFAULT_HEIGHT = 850;
  public static final String APP_TITLE = "Corese-GUI";

  // ===== Application Info =====
  public static final String APP_NAME = "Corese GUI";
  public static final String APP_VERSION = "5.0.0-SNAPSHOT";
  public static final String APP_DESCRIPTION = "A graphical interface for Corese RDF triple store";

  // ===== URLs =====
  public static final String GITHUB_URL = "https://github.com/corese-stack/corese-gui";
  public static final String WEBSITE_URL = "https://corese-stack.github.io/corese-gui/";
  public static final String ISSUES_URL = "https://github.com/corese-stack/corese-gui/issues";
  public static final String FORUM_URL = "https://github.com/orgs/corese-stack/discussions";

  // ===== Predefined Accent Colors (GNOME-inspired palette) =====
  private static final Color[] ACCENT_COLORS = {
    Color.web("#3584E4"), // Blue
    Color.web("#2190A4"), // Blue Gray
    Color.web("#3A944A"), // Green
    Color.web("#C88800"), // Yellow
    Color.web("#ED5B00"), // Orange
    Color.web("#E62D42"), // Red
    Color.web("#D56199"), // Pink
    Color.web("#9141AC"), // Purple
    Color.web("#6F8396"), // Gray
    Color.web("#986a44"), // Brown
  };

  /**
   * Returns a copy of the predefined accent colors array.
   *
   * @return an array of predefined accent colors
   */
  public static Color[] getAccentColors() {
    return ACCENT_COLORS.clone();
  }
}
