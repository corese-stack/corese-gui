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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
	private static final String PREF_GRAPH_AUTO_RENDER_TRIPLES = "app.graphAutoRenderTriplesLimit";
	private static final String DEFAULT_ACCENT_HEX = "#0078D4";
	private static final String DEFAULT_WEB_THEME_NAME = "default";
	private static final double DEFAULT_UI_SCALE = 1.0;
	private static final double MIN_UI_SCALE = 0.5;
	private static final double MAX_UI_SCALE = 2.0;
	private static final int DEFAULT_GRAPH_AUTO_RENDER_TRIPLES = 800;
	private static final int MIN_GRAPH_AUTO_RENDER_TRIPLES = 100;
	private static final int MAX_GRAPH_AUTO_RENDER_TRIPLES = 50_000;
	private static final double SCALE_EPSILON = 0.0001;
	private static final String ROOT_MANAGED_STYLE_BLOCK_KEY = "corese.themeManager.managedStyle";

	// ===== Properties =====

	private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(Color.web(DEFAULT_ACCENT_HEX));
	private final BooleanProperty systemThemeEnabled = new SimpleBooleanProperty(false);
	private final BooleanProperty sidebarCollapsed = new SimpleBooleanProperty(false);
	private final DoubleProperty uiScale = new SimpleDoubleProperty(DEFAULT_UI_SCALE);
	private final IntegerProperty graphAutoRenderTriplesLimit = new SimpleIntegerProperty(
			DEFAULT_GRAPH_AUTO_RENDER_TRIPLES);
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

	/** Returns the default graph auto-preview triples limit. */
	public static int getDefaultGraphAutoRenderTriplesLimit() {
		return DEFAULT_GRAPH_AUTO_RENDER_TRIPLES;
	}

	/** Returns the minimum graph auto-preview triples limit. */
	public static int getMinGraphAutoRenderTriplesLimit() {
		return MIN_GRAPH_AUTO_RENDER_TRIPLES;
	}

	/** Returns the maximum graph auto-preview triples limit. */
	public static int getMaxGraphAutoRenderTriplesLimit() {
		return MAX_GRAPH_AUTO_RENDER_TRIPLES;
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

		graphAutoRenderTriplesLimit.addListener((obs, oldVal, newVal) -> {
			int requested = newVal == null ? DEFAULT_GRAPH_AUTO_RENDER_TRIPLES : newVal.intValue();
			int safe = clampGraphAutoRenderTriplesLimit(requested);
			if (safe != requested) {
				graphAutoRenderTriplesLimit.set(safe);
				return;
			}
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

		installPrimaryStageListeners(stage);
		reapplyStageThemeState();
	}

	private void installPrimaryStageListeners(Stage stage) {
		stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene == null) {
				return;
			}
			applyAccentAndScale();
			newScene.rootProperty().addListener((o, oldRoot, newRoot) -> {
				if (newRoot != null) {
					applyAccentAndScale();
				}
			});
		});
	}

	private void reapplyStageThemeState() {
		Theme currentTheme = theme.get();
		if (currentTheme != null) {
			applyThemeInternal(currentTheme);
		}
		applyAccentAndScale();
	}

	private void applyAccentAndScale() {
		Color currentAccent = accentColor.get();
		if (currentAccent != null) {
			applyAccentColorInternal(currentAccent);
		}
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

	public IntegerProperty graphAutoRenderTriplesLimitProperty() {
		return graphAutoRenderTriplesLimit;
	}

	public int getGraphAutoRenderTriplesLimit() {
		return graphAutoRenderTriplesLimit.get();
	}

	public void setGraphAutoRenderTriplesLimit(int value) {
		int safeValue = clampGraphAutoRenderTriplesLimit(value);
		if (graphAutoRenderTriplesLimit.get() == safeValue) {
			return;
		}
		graphAutoRenderTriplesLimit.set(safeValue);
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
		ThemeVisualPalette.Palette palette = ThemeVisualPalette.forDarkMode(isCurrentThemeDark());
		String newAccentStyle = ThemeManagedStyleSupport.buildManagedAccentStyle(color, DEFAULT_ACCENT_HEX,
				getDefaultAccentColor(), palette);

		String previousManagedStyle = (String) root.getProperties().get(ROOT_MANAGED_STYLE_BLOCK_KEY);
		String baseStyle = ThemeManagedStyleSupport.stripManagedStyle(root.getStyle(), previousManagedStyle);
		root.setStyle(ThemeManagedStyleSupport.mergeStyle(baseStyle, newAccentStyle));
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
		return Math.clamp(scale, MIN_UI_SCALE, MAX_UI_SCALE);
	}

	private static int clampGraphAutoRenderTriplesLimit(int value) {
		return Math.clamp(value, MIN_GRAPH_AUTO_RENDER_TRIPLES, MAX_GRAPH_AUTO_RENDER_TRIPLES);
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
		return ThemeManagedStyleSupport.toCssColor(getAccentColor(), DEFAULT_ACCENT_HEX);
	}

	/**
	 * Returns the editor surface background color for the active theme.
	 *
	 * <p>
	 * Used for placeholders behind WebView editors during fade-in transitions.
	 */
	public String getEditorBackgroundHex() {
		AppThemeRegistry appTheme = getCurrentAppTheme();
		return ThemeVisualPalette.resolveEditorBackgroundHex(appTheme);
	}

	/**
	 * Returns the logo drop-shadow color matching the current light/dark context.
	 */
	public Color getLogoShadowColor() {
		return ThemeVisualPalette.forDarkMode(isCurrentThemeDark()).logoShadow();
	}

	/**
	 * Returns the sidebar separator color matching the current light/dark context.
	 */
	public Color getSidebarSeparatorColor() {
		return ThemeVisualPalette.forDarkMode(isCurrentThemeDark()).sidebarSeparator();
	}

	/**
	 * Returns the sidebar drop-shadow color matching the current light/dark
	 * context.
	 */
	public Color getSidebarShadowColor() {
		return ThemeVisualPalette.forDarkMode(isCurrentThemeDark()).sidebarShadow();
	}

	/**
	 * Returns the sidebar hover overlay color matching the current light/dark
	 * context.
	 */
	public Color getSidebarHoverColor() {
		return ThemeVisualPalette.forDarkMode(isCurrentThemeDark()).sidebarHover();
	}

	/**
	 * Returns the sidebar pressed overlay color matching the current light/dark
	 * context.
	 */
	public Color getSidebarPressedColor() {
		return ThemeVisualPalette.forDarkMode(isCurrentThemeDark()).sidebarPressed();
	}

	/**
	 * Returns the toast-notification drop-shadow color matching the current
	 * light/dark context.
	 */
	public Color getNotificationShadowColor() {
		return ThemeVisualPalette.forDarkMode(isCurrentThemeDark()).notificationShadow();
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
			int savedGraphAutoRenderTriplesLimit = clampGraphAutoRenderTriplesLimit(
					preferences.getInt(PREF_GRAPH_AUTO_RENDER_TRIPLES, DEFAULT_GRAPH_AUTO_RENDER_TRIPLES));

			// Apply settings
			setSystemThemeEnabled(useSystem);
			setSidebarCollapsed(collapsed);
			setUiScale(savedScale);
			setGraphAutoRenderTriplesLimit(savedGraphAutoRenderTriplesLimit);

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
			preferences.putInt(PREF_GRAPH_AUTO_RENDER_TRIPLES, getGraphAutoRenderTriplesLimit());

			if (!isSystemThemeEnabled()) {
				if (getTheme() != null) {
					String themeName = getCurrentThemeName();
					if (themeName != null) {
						preferences.put(PREF_THEME, themeName);
					}
				}

				if (getAccentColor() != null) {
					preferences.put(PREF_ACCENT_COLOR,
							ThemeManagedStyleSupport.toCssColor(getAccentColor(), DEFAULT_ACCENT_HEX));
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to save preferences", e);
		}
	}
}
