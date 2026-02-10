package fr.inria.corese.gui.core.theme;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.utils.AppExecutors;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the global visual theme and accent color of the Corese-GUI
 * application.
 *
 * <p>
 * This class is a <b>singleton</b> that serves as the single source of truth
 * for application appearance. It handles:
 *
 * <ul>
 * <li>Theme selection and application (JavaFX User Agent Stylesheet)
 * <li>Accent color management (CSS variables)
 * <li>System theme detection and automatic switching
 * <li>Persistence of user preferences
 * </ul>
 */
@SuppressWarnings("java:S6548")
public final class ThemeManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);
	private static final String PREF_THEME = "app.theme";
	private static final String PREF_ACCENT_COLOR = "app.accentColor";
	private static final String PREF_SYSTEM_THEME = "app.systemThemeEnabled";
	private static final String PREF_SIDEBAR_COLLAPSED = "app.sidebarCollapsed";
	private static final String DEFAULT_ACCENT_HEX = "#0078D4";
	private static final String DEFAULT_WEB_THEME_NAME = "default";
	private static final String EDITOR_BG_LIGHT = "#FFFFFF";
	private static final String EDITOR_BG_CUPERTINO_DARK = "#1E1E1E";
	private static final String EDITOR_BG_PRIMER_DARK = "#010409";
	private static final String EDITOR_BG_NORD_DARK = "#242933";
	private static final String ROOT_MANAGED_STYLE_BLOCK_KEY = "corese.themeManager.managedStyle";

	// ===== Properties =====

	private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(Color.web(DEFAULT_ACCENT_HEX));
	private final BooleanProperty systemThemeEnabled = new SimpleBooleanProperty(false);
	private final BooleanProperty sidebarCollapsed = new SimpleBooleanProperty(false);
	private final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);
	private boolean loadingPreferences = false;

	// ===== Fields =====

	private final SystemThemeMonitor systemThemeMonitor;
	private Stage primaryStage;

	// ===== Singleton =====

	private ThemeManager() {
		this.systemThemeMonitor = new SystemThemeMonitor();
		initializeSystemThemeMonitor();
		setupPropertyListeners();
		loadPreferences();
	}

	private static class Holder {
		private static final ThemeManager INSTANCE = new ThemeManager();
	}

	public static ThemeManager getInstance() {
		return Holder.INSTANCE;
	}

	/**
	 * Returns the default accent color hex used when no source color is available.
	 */
	public static String getDefaultAccentHex() {
		return DEFAULT_ACCENT_HEX;
	}

	/** Returns the default accent color used when no source color is available. */
	public static Color getDefaultAccentColor() {
		return Color.web(DEFAULT_ACCENT_HEX);
	}

	/**
	 * Normalized theme payload used by embedded web views (CodeMirror and graph
	 * viewer).
	 *
	 * @param dark
	 *            true if the active theme is dark
	 * @param accentHex
	 *            accent color in #RRGGBB
	 * @param themeName
	 *            lowercase theme family name expected by web assets
	 */
	public record WebThemeInfo(boolean dark, String accentHex, String themeName) {
	}

	// ===== Initialization =====

	/**
	 * Initializes the system theme monitor listeners.
	 *
	 * <p>
	 * When the monitor detects a change in the system configuration (Dark/Light
	 * mode or Accent Color), these listeners update the application settings if
	 * "Use System Theme" is enabled.
	 */
	private void initializeSystemThemeMonitor() {
		systemThemeMonitor.setThemeChangeListener(isDark -> {
			if (isSystemThemeEnabled()) {
				LOGGER.debug("System theme changed: {}", Boolean.TRUE.equals(isDark) ? "Dark" : "Light");
				setTheme(SystemThemeDetector.getSystemTheme());
			}
		});

		systemThemeMonitor.setAccentColorChangeListener(color -> {
			if (isSystemThemeEnabled()) {
				LOGGER.debug("System accent color changed");
				setAccentColor(color);
			}
		});
	}

	/**
	 * Sets up internal property listeners to react to state changes.
	 *
	 * <p>
	 * This ensures that whenever a property (theme, accent color, system enabled)
	 * is modified (whether by the user or the system monitor), the appropriate
	 * visual changes are applied.
	 */
	private void setupPropertyListeners() {
		// When theme property changes, apply it
		theme.addListener((obs, oldTheme, newTheme) -> {
			if (newTheme != null) {
				applyThemeInternal(newTheme);
				savePreferences();
			}
		});

		// When accent color property changes, apply it
		accentColor.addListener((obs, oldColor, newColor) -> {
			if (newColor != null) {
				applyAccentColorInternal(newColor);
				savePreferences();
			}
		});

		// When system theme enabled changes, start/stop monitor
		systemThemeEnabled.addListener((obs, oldVal, newVal) -> {
			if (Boolean.TRUE.equals(newVal)) {
				systemThemeMonitor.start();
				detectAndApplySystemSettings();
			} else {
				systemThemeMonitor.stop();
			}
			savePreferences();
		});

		// When sidebar collapsed changes, save it
		sidebarCollapsed.addListener((obs, oldVal, newVal) -> {
			savePreferences();
		});
	}

	// ===== Public API =====

	/**
	 * Sets the primary stage of the application.
	 *
	 * <p>
	 * This is required to apply CSS variables (accent colors) to the scene's root
	 * node. It also sets up listeners to re-apply styles if the Scene or Root node
	 * changes.
	 *
	 * @param stage
	 *            The primary JavaFX Stage.
	 */
	public void setPrimaryStage(Stage stage) {
		this.primaryStage = stage;

		// Listen for scene changes to re-apply accent color
		stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene != null) {
				if (accentColor.get() != null) {
					applyAccentColorInternal(accentColor.get());
				}
				// Also listen for root changes within the scene
				newScene.rootProperty().addListener((o, oldRoot, newRoot) -> {
					if (newRoot != null && accentColor.get() != null) {
						applyAccentColorInternal(accentColor.get());
					}
				});
			}
		});

		// Re-apply current settings to the new stage
		if (theme.get() != null)
			applyThemeInternal(theme.get());
		if (accentColor.get() != null)
			applyAccentColorInternal(accentColor.get());
	}

	/**
	 * Manually triggers detection and application of system settings.
	 *
	 * <p>
	 * This queries the OS for the current theme mode and accent color and applies
	 * them to the application properties. This is done asynchronously to avoid
	 * blocking the UI.
	 */
	public void detectAndApplySystemSettings() {
		AppExecutors.execute(() -> {
			try {
				Theme systemTheme = SystemThemeDetector.getSystemTheme();
				Color systemAccent = SystemThemeDetector.getSystemAccentColor();

				javafx.application.Platform.runLater(() -> {
					setTheme(systemTheme);
					setAccentColor(systemAccent);
				});
			} catch (Exception e) {
				LOGGER.warn("Failed to detect system settings", e);
			}
		});
	}

	// ===== Property Accessors =====

	public ObjectProperty<Theme> themeProperty() {
		return theme;
	}

	public Theme getTheme() {
		return theme.get();
	}

	public void setTheme(Theme theme) {
		this.theme.set(theme);
	}

	public ObjectProperty<Color> accentColorProperty() {
		return accentColor;
	}

	public Color getAccentColor() {
		return accentColor.get();
	}

	public void setAccentColor(Color color) {
		this.accentColor.set(color);
	}

	public BooleanProperty systemThemeEnabledProperty() {
		return systemThemeEnabled;
	}

	public boolean isSystemThemeEnabled() {
		return systemThemeEnabled.get();
	}

	public void setSystemThemeEnabled(boolean enabled) {
		this.systemThemeEnabled.set(enabled);
	}

	public BooleanProperty sidebarCollapsedProperty() {
		return sidebarCollapsed;
	}

	public boolean isSidebarCollapsed() {
		return sidebarCollapsed.get();
	}

	public void setSidebarCollapsed(boolean collapsed) {
		this.sidebarCollapsed.set(collapsed);
	}

	/** Releases background theme monitoring resources. */
	public void shutdown() {
		systemThemeMonitor.stop();
	}

	// ===== Internal Application Logic =====

	/**
	 * Applies the specified theme to the application.
	 *
	 * <p>
	 * This sets the JavaFX User Agent Stylesheet.
	 *
	 * @param theme
	 *            The theme to apply.
	 */
	private void applyThemeInternal(Theme theme) {
		Runnable applyAction = () -> Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());

		// Apply theme directly without transition
		applyAction.run();

		// Re-apply accent color as theme change might reset styles
		if (accentColor.get() != null) {
			applyAccentColorInternal(accentColor.get());
		}
	}

	/**
	 * Applies the accent color as CSS variables to the root node.
	 *
	 * <p>
	 * This calculates derived colors (emphasis, subtle, muted) and injects them
	 * into the inline style of the root node.
	 *
	 * @param color
	 *            The accent color to apply.
	 */
	private void applyAccentColorInternal(Color color) {
		if (primaryStage == null || primaryStage.getScene() == null)
			return;

		Node root = primaryStage.getScene().getRoot();
		String cssColor = toCssColor(color);

		// Determine theme-aware shadows based on current theme.
		String logoShadowColor = "rgba(0, 0, 0, 0.25)";
		String tabOverflowShadow = "rgba(0, 0, 0, 0.16)";
		String tabOverflowShadowTransparent = "rgba(0, 0, 0, 0.00)";
		if (isCurrentThemeDark()) {
			logoShadowColor = "rgba(255, 255, 255, 0.25)";
			tabOverflowShadow = "rgba(255, 255, 255, 0.12)";
			tabOverflowShadowTransparent = "rgba(255, 255, 255, 0.00)";
		}

		String newAccentStyle = String.format(
				"-color-accent-emphasis: %s; " + "-color-accent-fg: %s; " + "-color-accent-subtle: %s; "
						+ "-color-accent-muted: %s; " + "-color-logo-shadow: %s; " + "-color-tab-overflow-shadow: %s; "
						+ "-color-tab-overflow-shadow-transparent: %s;",
				cssColor, cssColor, toCssColor(color.deriveColor(0, 0.3, 1.0, 0.3)),
				toCssColor(color.deriveColor(0, 0.5, 1.0, 0.5)), logoShadowColor, tabOverflowShadow,
				tabOverflowShadowTransparent);

		String previousManagedStyle = (String) root.getProperties().get(ROOT_MANAGED_STYLE_BLOCK_KEY);
		String baseStyle = stripManagedStyle(root.getStyle(), previousManagedStyle);
		root.setStyle(mergeStyle(baseStyle, newAccentStyle));
		root.getProperties().put(ROOT_MANAGED_STYLE_BLOCK_KEY, newAccentStyle);
	}

	/**
	 * Converts a JavaFX Color to a CSS hex string.
	 *
	 * @param color
	 *            The color to convert.
	 * @return A string in the format "#RRGGBB".
	 */
	private String toCssColor(Color color) {
		if (color == null) {
			return DEFAULT_ACCENT_HEX;
		}
		return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
				(int) (color.getBlue() * 255));
	}

	private static String stripManagedStyle(String currentStyle, String managedStyle) {
		String style = currentStyle == null ? "" : currentStyle.trim();
		if (managedStyle == null || managedStyle.isBlank()) {
			return style;
		}
		String cleaned = style.replace(managedStyle, "").trim();
		while (cleaned.contains(";;")) {
			cleaned = cleaned.replace(";;", ";");
		}
		if (cleaned.startsWith(";")) {
			cleaned = cleaned.substring(1).trim();
		}
		if (cleaned.endsWith(";")) {
			cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
		}
		return cleaned;
	}

	private static String mergeStyle(String baseStyle, String managedStyle) {
		if (baseStyle == null || baseStyle.isBlank()) {
			return managedStyle;
		}
		String normalizedBase = baseStyle.trim();
		if (!normalizedBase.endsWith(";")) {
			normalizedBase += ";";
		}
		return normalizedBase + " " + managedStyle;
	}

	// ===== Theme Queries =====

	/** Returns the enum metadata for the currently active theme, if recognized. */
	public AppThemeRegistry getCurrentAppTheme() {
		Theme current = getTheme();
		if (current == null) {
			return null;
		}
		return AppThemeRegistry.fromTheme(current);
	}

	/** Returns true if the active theme is dark. */
	public boolean isCurrentThemeDark() {
		AppThemeRegistry appTheme = getCurrentAppTheme();
		return appTheme != null && appTheme.isDark();
	}

	/**
	 * Returns the lowercase theme family token used by embedded web views.
	 *
	 * <p>
	 * Examples: {@code primer}, {@code nord}, {@code cupertino}.
	 */
	public String getCurrentThemeToken() {
		AppThemeRegistry appTheme = getCurrentAppTheme();
		if (appTheme == null) {
			return DEFAULT_WEB_THEME_NAME;
		}
		return appTheme.getBaseName().toLowerCase(Locale.ROOT);
	}

	/** Returns the current accent color formatted as #RRGGBB. */
	public String getCurrentAccentHex() {
		return toCssColor(getAccentColor());
	}

	/**
	 * Returns the editor surface background color for the active theme.
	 *
	 * <p>
	 * Used for placeholders behind WebView editors during fade-in transitions.
	 */
	public String getEditorBackgroundHex() {
		AppThemeRegistry appTheme = getCurrentAppTheme();
		if (appTheme == null || !appTheme.isDark()) {
			return EDITOR_BG_LIGHT;
		}
		return switch (appTheme.getBaseName()) {
			case "Primer" -> EDITOR_BG_PRIMER_DARK;
			case "Nord" -> EDITOR_BG_NORD_DARK;
			default -> EDITOR_BG_CUPERTINO_DARK;
		};
	}

	/**
	 * Returns the logo drop-shadow color matching the current light/dark context.
	 */
	public Color getLogoShadowColor() {
		if (isCurrentThemeDark()) {
			return Color.rgb(255, 255, 255, 0.25);
		}
		return Color.rgb(0, 0, 0, 0.25);
	}

	/** Returns normalized theme data for Java -> WebView bridge scripts. */
	public WebThemeInfo getWebThemeInfo() {
		return new WebThemeInfo(isCurrentThemeDark(), getCurrentAccentHex(), getCurrentThemeToken());
	}

	/**
	 * Returns a list of available base theme names (e.g., "Nord", "Primer").
	 *
	 * @return A sorted list of unique base theme names.
	 */
	public List<String> getBaseThemes() {
		return Arrays.stream(AppThemeRegistry.values()).map(AppThemeRegistry::getBaseName).distinct().sorted().toList();
	}

	/**
	 * Retrieves a specific theme variant based on its family name and darkness.
	 *
	 * @param baseName
	 *            The base name of the theme (e.g., "Nord").
	 * @param isDark
	 *            True for the dark variant, false for light.
	 * @return The corresponding Theme object, or null if not found.
	 */
	public Theme getThemeVariant(String baseName, boolean isDark) {
		AppThemeRegistry appTheme = AppThemeRegistry.getVariant(baseName, isDark);
		return appTheme != null ? appTheme.getTheme() : null;
	}

	/**
	 * Gets the name of the currently active theme.
	 *
	 * @return The name of the current theme (e.g., "NORD_DARK"), or null if none
	 *         set.
	 */
	public String getCurrentThemeName() {
		AppThemeRegistry appTheme = getCurrentAppTheme();
		return appTheme != null ? appTheme.name() : null;
	}

	/**
	 * Extracts the base name from a full theme name.
	 *
	 * @param themeName
	 *            The full theme name (e.g., "NORD_DARK").
	 * @return The base name (e.g., "Nord").
	 */
	public String getBaseThemeName(String themeName) {
		try {
			return AppThemeRegistry.valueOf(themeName).getBaseName();
		} catch (IllegalArgumentException | NullPointerException e) {
			return themeName;
		}
	}

	/**
	 * Checks if a given theme is a dark variant.
	 *
	 * @param themeName
	 *            The full theme name.
	 * @return True if the theme is dark, false otherwise.
	 */
	public boolean isDarkTheme(String themeName) {
		try {
			return AppThemeRegistry.valueOf(themeName).isDark();
		} catch (IllegalArgumentException | NullPointerException e) {
			return false;
		}
	}

	/**
	 * Retrieves a Theme object by its full name.
	 *
	 * @param name
	 *            The name of the theme (e.g., "NORD_DARK" or "Nord Dark").
	 * @return The Theme object, or null if not found.
	 */
	public Theme getThemeByName(String name) {
		// Try to find by AppTheme name
		try {
			return AppThemeRegistry.valueOf(name).getTheme();
		} catch (IllegalArgumentException e) {
			// Try to find by display name construction (legacy support if needed)
			for (AppThemeRegistry t : AppThemeRegistry.values()) {
				String constructedName = t.getBaseName() + (t.isDark() ? " Dark" : " Light");
				if (constructedName.equals(name)) {
					return t.getTheme();
				}
			}
		}
		return null;
	}

	// ===== Persistence =====

	private void loadPreferences() {
		loadingPreferences = true;
		try {
			// Load all values first to avoid partial application triggering saves
			// Default to true (System Theme) for first run
			boolean useSystem = preferences.getBoolean(PREF_SYSTEM_THEME, true);
			String themeName = preferences.get(PREF_THEME, null);
			String colorHex = preferences.get(PREF_ACCENT_COLOR, null);
			boolean collapsed = preferences.getBoolean(PREF_SIDEBAR_COLLAPSED, false);

			// Apply settings
			setSystemThemeEnabled(useSystem);
			setSidebarCollapsed(collapsed);

			if (useSystem && getTheme() == null) {
				// Provide a fast, safe fallback so CSS variables are available immediately.
				setTheme(getFallbackSystemTheme());
			}

			if (!useSystem) {
				if (themeName != null) {
					Theme loadedTheme = getThemeByName(themeName);
					if (loadedTheme != null) {
						setTheme(loadedTheme);
					}
				} else if (getTheme() == null) {
					// Default fallback if no theme is set/saved
					setTheme(AppThemeRegistry.PRIMER_LIGHT.getTheme());
				}

				if (colorHex != null) {
					try {
						setAccentColor(Color.web(colorHex));
					} catch (IllegalArgumentException e) {
						LOGGER.warn("Invalid saved accent color: {}", colorHex);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to load preferences", e);
		} finally {
			loadingPreferences = false;
		}
	}

	private Theme getFallbackSystemTheme() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("mac")) {
			return AppThemeRegistry.CUPERTINO_LIGHT.getTheme();
		}
		return AppThemeRegistry.PRIMER_LIGHT.getTheme();
	}

	private void savePreferences() {
		if (loadingPreferences)
			return;

		try {
			preferences.putBoolean(PREF_SYSTEM_THEME, isSystemThemeEnabled());
			preferences.putBoolean(PREF_SIDEBAR_COLLAPSED, isSidebarCollapsed());

			if (!isSystemThemeEnabled()) {
				if (getTheme() != null) {
					String themeName = getCurrentThemeName();
					if (themeName != null) {
						preferences.put(PREF_THEME, themeName);
					}
				}

				if (getAccentColor() != null) {
					preferences.put(PREF_ACCENT_COLOR, toCssColor(getAccentColor()));
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to save preferences", e);
		}
	}
}
