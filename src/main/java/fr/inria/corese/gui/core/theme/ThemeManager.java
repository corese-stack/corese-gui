package fr.inria.corese.gui.core.theme;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.component.layout.GlobalZoomPane;
import fr.inria.corese.gui.utils.AppExecutors;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
	private static final String PREF_UI_SCALE = "app.uiScale";
	private static final String DEFAULT_ACCENT_HEX = "#0078D4";
	private static final String DEFAULT_WEB_THEME_NAME = "default";
	private static final double DEFAULT_UI_SCALE = 1.0;
	private static final double MIN_UI_SCALE = 0.5;
	private static final double MAX_UI_SCALE = 2.0;
	private static final double SCALE_EPSILON = 0.0001;
	private static final String EDITOR_BG_LIGHT = "#FFFFFF";
	private static final String EDITOR_BG_CUPERTINO_DARK = "#1E1E1E";
	private static final String EDITOR_BG_PRIMER_DARK = "#010409";
	private static final String EDITOR_BG_NORD_DARK = "#242933";
	private static final String ROOT_MANAGED_STYLE_BLOCK_KEY = "corese.themeManager.managedStyle";
	private static final Color LOGO_SHADOW_LIGHT = Color.rgb(0, 0, 0, 0.25);
	private static final Color LOGO_SHADOW_DARK = Color.rgb(255, 255, 255, 0.25);
	private static final Color TAB_OVERFLOW_SHADOW_LIGHT = Color.rgb(0, 0, 0, 0.16);
	private static final Color TAB_OVERFLOW_SHADOW_DARK = Color.rgb(255, 255, 255, 0.12);
	private static final Color SIDEBAR_SEPARATOR_LIGHT = Color.rgb(15, 23, 42, 0.14);
	private static final Color SIDEBAR_SEPARATOR_DARK = Color.rgb(255, 255, 255, 0.1);
	private static final Color SIDEBAR_SHADOW_LIGHT = Color.rgb(0, 0, 0, 0.1);
	private static final Color SIDEBAR_SHADOW_DARK = Color.rgb(0, 0, 0, 0.32);
	private static final Color SIDEBAR_HOVER_LIGHT = Color.rgb(0, 0, 0, 0.05);
	private static final Color SIDEBAR_HOVER_DARK = Color.rgb(255, 255, 255, 0.06);
	private static final Color SIDEBAR_PRESSED_LIGHT = Color.rgb(0, 0, 0, 0.1);
	private static final Color SIDEBAR_PRESSED_DARK = Color.rgb(255, 255, 255, 0.11);
	private static final Color NOTIFICATION_SHADOW_LIGHT = Color.rgb(0, 0, 0, 0.2);
	private static final Color NOTIFICATION_SHADOW_DARK = Color.rgb(255, 255, 255, 0.12);

	// ===== Properties =====

	private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(Color.web(DEFAULT_ACCENT_HEX));
	private final BooleanProperty systemThemeEnabled = new SimpleBooleanProperty(false);
	private final BooleanProperty sidebarCollapsed = new SimpleBooleanProperty(false);
	private final DoubleProperty uiScale = new SimpleDoubleProperty(DEFAULT_UI_SCALE);
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

	/** Returns the default global UI scale used when no preference is available. */
	public static double getDefaultUiScale() {
		return DEFAULT_UI_SCALE;
	}

	/** Returns the minimum supported global UI scale. */
	public static double getMinUiScale() {
		return MIN_UI_SCALE;
	}

	/** Returns the maximum supported global UI scale. */
	public static double getMaxUiScale() {
		return MAX_UI_SCALE;
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

		// When UI scale changes, re-apply managed root styles and persist
		uiScale.addListener((obs, oldVal, newVal) -> {
			double requestedScale = newVal == null ? DEFAULT_UI_SCALE : newVal.doubleValue();
			double safeScale = clampUiScale(requestedScale);
			if (Math.abs(safeScale - requestedScale) > SCALE_EPSILON) {
				uiScale.set(safeScale);
				return;
			}
			applyUiScaleInternal();
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
		if (stage == null) {
			return;
		}
		if (this.primaryStage == stage) {
			return;
		}
		this.primaryStage = stage;

		// Listen for scene changes to re-apply accent color
		stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene != null) {
				if (accentColor.get() != null) {
					applyAccentColorInternal(accentColor.get());
				}
				applyUiScaleInternal();
				// Also listen for root changes within the scene
				newScene.rootProperty().addListener((o, oldRoot, newRoot) -> {
					if (newRoot != null) {
						if (accentColor.get() != null) {
							applyAccentColorInternal(accentColor.get());
						}
						applyUiScaleInternal();
					}
				});
			}
		});

		// Re-apply current settings to the new stage
		if (theme.get() != null)
			applyThemeInternal(theme.get());
		if (accentColor.get() != null)
			applyAccentColorInternal(accentColor.get());
		applyUiScaleInternal();
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
					if (!isSystemThemeEnabled()) {
						return;
					}
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
		if (theme == null) {
			return;
		}
		Theme current = this.theme.get();
		if (current != null && current.getClass().equals(theme.getClass())) {
			return;
		}
		this.theme.set(theme);
	}

	public ObjectProperty<Color> accentColorProperty() {
		return accentColor;
	}

	public Color getAccentColor() {
		return accentColor.get();
	}

	public void setAccentColor(Color color) {
		if (color == null) {
			return;
		}
		Color current = this.accentColor.get();
		if (current != null && current.equals(color)) {
			return;
		}
		this.accentColor.set(color);
	}

	public BooleanProperty systemThemeEnabledProperty() {
		return systemThemeEnabled;
	}

	public boolean isSystemThemeEnabled() {
		return systemThemeEnabled.get();
	}

	public void setSystemThemeEnabled(boolean enabled) {
		if (this.systemThemeEnabled.get() == enabled) {
			return;
		}
		this.systemThemeEnabled.set(enabled);
	}

	public BooleanProperty sidebarCollapsedProperty() {
		return sidebarCollapsed;
	}

	public boolean isSidebarCollapsed() {
		return sidebarCollapsed.get();
	}

	public void setSidebarCollapsed(boolean collapsed) {
		if (this.sidebarCollapsed.get() == collapsed) {
			return;
		}
		this.sidebarCollapsed.set(collapsed);
	}

	public DoubleProperty uiScaleProperty() {
		return uiScale;
	}

	public double getUiScale() {
		return uiScale.get();
	}

	public void setUiScale(double scale) {
		double safeScale = clampUiScale(scale);
		if (Math.abs(this.uiScale.get() - safeScale) <= SCALE_EPSILON) {
			return;
		}
		this.uiScale.set(safeScale);
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
		Color tabOverflowShadow = getTabOverflowShadowColor();

		String newAccentStyle = String.format(
				"-color-accent-emphasis: %s; " + "-color-accent-fg: %s; " + "-color-accent-subtle: %s; "
						+ "-color-accent-muted: %s; " + "-color-logo-shadow: %s; " + "-color-tab-overflow-shadow: %s; "
						+ "-color-tab-overflow-shadow-transparent: %s; " + "-color-sidebar-separator: %s; "
						+ "-color-sidebar-shadow: %s; " + "-color-sidebar-hover: %s; " + "-color-sidebar-pressed: %s; "
						+ "-color-notification-shadow: %s;",
				cssColor, cssColor, toCssColor(color.deriveColor(0, 0.3, 1.0, 0.3)),
				toCssColor(color.deriveColor(0, 0.5, 1.0, 0.5)), toCssRgbaColor(getLogoShadowColor()),
				toCssRgbaColor(tabOverflowShadow), toCssRgbaColor(withOpacity(tabOverflowShadow, 0.0)),
				toCssRgbaColor(getSidebarSeparatorColor()), toCssRgbaColor(getSidebarShadowColor()),
				toCssRgbaColor(getSidebarHoverColor()), toCssRgbaColor(getSidebarPressedColor()),
				toCssRgbaColor(getNotificationShadowColor()));

		String previousManagedStyle = (String) root.getProperties().get(ROOT_MANAGED_STYLE_BLOCK_KEY);
		String baseStyle = stripManagedStyle(root.getStyle(), previousManagedStyle);
		root.setStyle(mergeStyle(baseStyle, newAccentStyle));
		root.getProperties().put(ROOT_MANAGED_STYLE_BLOCK_KEY, newAccentStyle);
	}

	private void applyUiScaleInternal() {
		if (primaryStage == null) {
			return;
		}
		var scene = primaryStage.getScene();
		if (scene == null) {
			return;
		}

		Node root = scene.getRoot();
		if (!(root instanceof GlobalZoomPane zoomPane)) {
			return;
		}

		double scaleValue = clampUiScale(getUiScale());
		zoomPane.setZoom(scaleValue);
	}

	private static double clampUiScale(double scale) {
		if (Double.isNaN(scale) || Double.isInfinite(scale)) {
			return DEFAULT_UI_SCALE;
		}
		return Math.max(MIN_UI_SCALE, Math.min(MAX_UI_SCALE, scale));
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

	/**
	 * Converts a JavaFX Color to a CSS rgba() string preserving alpha.
	 *
	 * @param color
	 *            The color to convert.
	 * @return A string in the format "rgba(r, g, b, a)".
	 */
	private String toCssRgbaColor(Color color) {
		if (color == null) {
			color = getDefaultAccentColor();
		}
		int red = (int) Math.round(color.getRed() * 255);
		int green = (int) Math.round(color.getGreen() * 255);
		int blue = (int) Math.round(color.getBlue() * 255);
		return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.3f)", red, green, blue, color.getOpacity());
	}

	private static Color withOpacity(Color color, double opacity) {
		if (color == null) {
			return Color.TRANSPARENT;
		}
		double clampedOpacity = Math.max(0.0, Math.min(1.0, opacity));
		return Color.color(color.getRed(), color.getGreen(), color.getBlue(), clampedOpacity);
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
			return LOGO_SHADOW_DARK;
		}
		return LOGO_SHADOW_LIGHT;
	}

	/**
	 * Returns the sidebar separator color matching the current light/dark context.
	 */
	public Color getSidebarSeparatorColor() {
		if (isCurrentThemeDark()) {
			return SIDEBAR_SEPARATOR_DARK;
		}
		return SIDEBAR_SEPARATOR_LIGHT;
	}

	/**
	 * Returns the sidebar drop-shadow color matching the current light/dark
	 * context.
	 */
	public Color getSidebarShadowColor() {
		if (isCurrentThemeDark()) {
			return SIDEBAR_SHADOW_DARK;
		}
		return SIDEBAR_SHADOW_LIGHT;
	}

	/**
	 * Returns the sidebar hover overlay color matching the current light/dark
	 * context.
	 */
	public Color getSidebarHoverColor() {
		if (isCurrentThemeDark()) {
			return SIDEBAR_HOVER_DARK;
		}
		return SIDEBAR_HOVER_LIGHT;
	}

	/**
	 * Returns the sidebar pressed overlay color matching the current light/dark
	 * context.
	 */
	public Color getSidebarPressedColor() {
		if (isCurrentThemeDark()) {
			return SIDEBAR_PRESSED_DARK;
		}
		return SIDEBAR_PRESSED_LIGHT;
	}

	/**
	 * Returns the toast-notification drop-shadow color matching the current
	 * light/dark context.
	 */
	public Color getNotificationShadowColor() {
		if (isCurrentThemeDark()) {
			return NOTIFICATION_SHADOW_DARK;
		}
		return NOTIFICATION_SHADOW_LIGHT;
	}

	private Color getTabOverflowShadowColor() {
		if (isCurrentThemeDark()) {
			return TAB_OVERFLOW_SHADOW_DARK;
		}
		return TAB_OVERFLOW_SHADOW_LIGHT;
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
	private String getCurrentThemeName() {
		AppThemeRegistry appTheme = getCurrentAppTheme();
		return appTheme != null ? appTheme.name() : null;
	}

	/**
	 * Retrieves a Theme object by its full name.
	 *
	 * @param name
	 *            The name of the theme (e.g., "NORD_DARK" or "Nord Dark").
	 * @return The Theme object, or null if not found.
	 */
	private Theme getThemeByName(String name) {
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
			double savedScale = clampUiScale(preferences.getDouble(PREF_UI_SCALE, DEFAULT_UI_SCALE));

			// Apply settings
			setSystemThemeEnabled(useSystem);
			setSidebarCollapsed(collapsed);
			setUiScale(savedScale);

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
			preferences.putDouble(PREF_UI_SCALE, getUiScale());

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
