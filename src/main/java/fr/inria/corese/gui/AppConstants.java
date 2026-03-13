package fr.inria.corese.gui;

import fr.inria.corese.core.util.CoreseInfo;

/**
 * Application-wide constants for Corese GUI.
 *
 * <p>
 * Contains UI dimensions, application metadata, URLs, and predefined color
 * palettes.
 */
public final class AppConstants {

	private static final String DEFAULT_APP_VERSION = "5.0.0";
	private static final String APP_VERSION_OVERRIDE_PROPERTY = "corese.app.version";

	// ===== Prevent instantiation =====
	private AppConstants() {
		throw new AssertionError("Constants class cannot be instantiated");
	}

	// ===== Application Window =====
	public static final double DEFAULT_WIDTH = 1400;
	public static final double DEFAULT_HEIGHT = 850;
	public static final double MIN_WIDTH = 1000;
	public static final double MIN_HEIGHT = 700;

	// ===== Application Info =====
	public static final String APP_NAME = "Corese GUI";
	public static final String APP_VERSION = resolveAppVersion();
	public static final String CORESE_CORE_VERSION = CoreseInfo.getVersion();
	public static final String APP_DESCRIPTION = "A graphical interface for Corese RDF triple store";

	// ===== URLs =====
	public static final String REPOSITORY_URL = "https://github.com/corese-stack/corese-gui";
	public static final String RELEASES_URL = REPOSITORY_URL + "/releases";
	public static final String RELEASES_API_LATEST_URL = "https://api.github.com/repos/corese-stack/corese-gui/releases/latest";
	public static final String WEBSITE_URL = "https://corese-stack.github.io/corese-gui/";
	public static final String PROJECT_URL = "https://project.inria.fr/corese/";
	public static final String ISSUES_URL = "https://github.com/corese-stack/corese-gui/issues";
	public static final String FORUM_URL = "https://github.com/orgs/corese-stack/discussions";
	public static final String LICENSE_URL = REPOSITORY_URL + "/blob/main/LICENSE";

	private static String resolveAppVersion() {
		String override = System.getProperty(APP_VERSION_OVERRIDE_PROPERTY);
		if (override != null && !override.isBlank()) {
			return override.trim();
		}

		Package appPackage = AppConstants.class.getPackage();
		if (appPackage != null) {
			String implementationVersion = appPackage.getImplementationVersion();
			if (implementationVersion != null && !implementationVersion.isBlank()) {
				return implementationVersion.trim();
			}
		}

		return DEFAULT_APP_VERSION;
	}

}
