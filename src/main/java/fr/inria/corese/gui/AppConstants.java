package fr.inria.corese.gui;

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
  public static final double MIN_WIDTH = 1000;
  public static final double MIN_HEIGHT = 700;
  public static final String APP_TITLE = "Corese-GUI";

  // ===== Application Info =====
  public static final String APP_NAME = "Corese GUI";
  public static final String APP_VERSION = "5.0.0-SNAPSHOT";
  public static final String APP_DESCRIPTION = "A graphical interface for Corese RDF triple store";

  // ===== URLs =====
  public static final String REPOSITORY_URL = "https://github.com/corese-stack/corese-gui";
  public static final String WEBSITE_URL = "https://corese-stack.github.io/corese-gui/";
  public static final String PROJECT_URL = "https://project.inria.fr/corese/";
  public static final String ISSUES_URL = "https://github.com/corese-stack/corese-gui/issues";
  public static final String FORUM_URL = "https://github.com/orgs/corese-stack/discussions";
  public static final String LICENSE_URL = REPOSITORY_URL + "/blob/main/LICENSE";

}
