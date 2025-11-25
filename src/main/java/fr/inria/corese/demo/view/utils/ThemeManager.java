package fr.inria.corese.demo.view.utils;

import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the global visual theme and accent color of the Corese-GUI application.
 *
 * <p>This class is a <b>singleton</b> that serves as the single source of truth for
 * application appearance. It handles:
 * <ul>
 *   <li>Theme selection and application (JavaFX User Agent Stylesheet)</li>
 *   <li>Accent color management (CSS variables)</li>
 *   <li>System theme detection and automatic switching</li>
 * </ul>
 */
@SuppressWarnings("java:S6548")
public final class ThemeManager {

    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());

    // ===== Properties =====

    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>();
    private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(Color.web("#0078D4"));
    private final BooleanProperty systemThemeEnabled = new SimpleBooleanProperty(false);

    // ===== Fields =====

    private final SystemThemeMonitor systemThemeMonitor;
    private Stage primaryStage;

    // ===== Singleton =====

    private ThemeManager() {
        this.systemThemeMonitor = new SystemThemeMonitor();
        initializeSystemThemeMonitor();
        setupPropertyListeners();
    }

    private static class Holder {
        private static final ThemeManager INSTANCE = new ThemeManager();
    }

    public static ThemeManager getInstance() {
        return Holder.INSTANCE;
    }

    // ===== Initialization =====

    /**
     * Initializes the system theme monitor listeners.
     * <p>
     * When the monitor detects a change in the system configuration (Dark/Light mode or Accent Color),
     * these listeners update the application settings if "Use System Theme" is enabled.
     */
    private void initializeSystemThemeMonitor() {
        systemThemeMonitor.setThemeChangeListener(isDark -> {
            if (isSystemThemeEnabled()) {
                LOGGER.log(Level.INFO, "System theme changed: {0}", Boolean.TRUE.equals(isDark) ? "Dark" : "Light");
                setTheme(SystemThemeDetector.getSystemTheme());
            }
        });

        systemThemeMonitor.setAccentColorChangeListener(color -> {
            if (isSystemThemeEnabled()) {
                LOGGER.info("System accent color changed");
                setAccentColor(color);
            }
        });
    }

    /**
     * Sets up internal property listeners to react to state changes.
     * <p>
     * This ensures that whenever a property (theme, accent color, system enabled) is modified
     * (whether by the user or the system monitor), the appropriate visual changes are applied.
     */
    private void setupPropertyListeners() {
        // When theme property changes, apply it
        theme.addListener((obs, oldTheme, newTheme) -> {
            if (newTheme != null) {
                applyThemeInternal(newTheme);
            }
        });

        // When accent color property changes, apply it
        accentColor.addListener((obs, oldColor, newColor) -> {
            if (newColor != null) {
                applyAccentColorInternal(newColor);
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
        });
    }

    // ===== Public API =====

    /**
     * Sets the primary stage of the application.
     * <p>
     * This is required to apply CSS variables (accent colors) to the scene's root node.
     * It also sets up listeners to re-apply styles if the Scene or Root node changes.
     *
     * @param stage The primary JavaFX Stage.
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
        if (theme.get() != null) applyThemeInternal(theme.get());
        if (accentColor.get() != null) applyAccentColorInternal(accentColor.get());
    }

    /**
     * Manually triggers detection and application of system settings.
     * <p>
     * This queries the OS for the current theme mode and accent color and applies them
     * to the application properties.
     */
    public void detectAndApplySystemSettings() {
        try {
            setTheme(SystemThemeDetector.getSystemTheme());
            setAccentColor(SystemThemeDetector.getSystemAccentColor());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to detect system settings", e);
        }
    }

    // ===== Property Accessors =====

    public ObjectProperty<Theme> themeProperty() { return theme; }
    public Theme getTheme() { return theme.get(); }
    public void setTheme(Theme theme) { this.theme.set(theme); }

    public ObjectProperty<Color> accentColorProperty() { return accentColor; }
    public Color getAccentColor() { return accentColor.get(); }
    public void setAccentColor(Color color) { this.accentColor.set(color); }

    public BooleanProperty systemThemeEnabledProperty() { return systemThemeEnabled; }
    public boolean isSystemThemeEnabled() { return systemThemeEnabled.get(); }
    public void setSystemThemeEnabled(boolean enabled) { this.systemThemeEnabled.set(enabled); }

    // ===== Internal Application Logic =====

    /**
     * Applies the specified theme to the application.
     * <p>
     * This sets the JavaFX User Agent Stylesheet.
     *
     * @param theme The theme to apply.
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
     * <p>
     * This calculates derived colors (emphasis, subtle, muted) and injects them
     * into the inline style of the root node.
     *
     * @param color The accent color to apply.
     */
    private void applyAccentColorInternal(Color color) {
        if (primaryStage == null || primaryStage.getScene() == null) return;

        Node root = primaryStage.getScene().getRoot();
        String cssColor = toCssColor(color);
        
        String newAccentStyle = String.format(
            "-color-accent-emphasis: %s; " +
            "-color-accent-fg: %s; " +
            "-color-accent-subtle: %s; " +
            "-color-accent-muted: %s;",
            cssColor,
            cssColor,
            toCssColor(color.deriveColor(0, 0.3, 1.0, 0.3)),
            toCssColor(color.deriveColor(0, 0.5, 1.0, 0.5))
        );

        // Get existing styles
        String currentStyle = root.getStyle();
        if (currentStyle == null) currentStyle = "";

        // Remove old accent color definitions if present to avoid duplicates
        String cleanedStyle = currentStyle.replaceAll("-color-accent-[a-z]+:[^;]+;", "").trim();
        
        // Append new styles
        if (!cleanedStyle.isEmpty() && !cleanedStyle.endsWith(";")) {
            cleanedStyle += ";";
        }
        
        root.setStyle(cleanedStyle + (cleanedStyle.isEmpty() ? "" : " ") + newAccentStyle);
    }

    /**
     * Converts a JavaFX Color to a CSS hex string.
     *
     * @param color The color to convert.
     * @return A string in the format "#RRGGBB".
     */
    private String toCssColor(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    // ===== Theme Queries =====

    /**
     * Returns a list of available base theme names (e.g., "Nord", "Primer").
     *
     * @return A sorted list of unique base theme names.
     */
    public List<String> getBaseThemes() {
        return Arrays.stream(AppTheme.values())
            .map(AppTheme::getBaseName)
            .distinct()
            .sorted()
            .toList();
    }

    /**
     * Retrieves a specific theme variant based on its family name and darkness.
     *
     * @param baseName The base name of the theme (e.g., "Nord").
     * @param isDark   True for the dark variant, false for light.
     * @return The corresponding Theme object, or null if not found.
     */
    public Theme getThemeVariant(String baseName, boolean isDark) {
        AppTheme appTheme = AppTheme.getVariant(baseName, isDark);
        return appTheme != null ? appTheme.getTheme() : null;
    }
    
    /**
     * Gets the name of the currently active theme.
     *
     * @return The name of the current theme (e.g., "NORD_DARK"), or null if none set.
     */
    public String getCurrentThemeName() {
        Theme current = getTheme();
        if (current == null) return null;
        AppTheme appTheme = AppTheme.fromTheme(current);
        return appTheme != null ? appTheme.name() : null;
    }

    /**
     * Extracts the base name from a full theme name.
     *
     * @param themeName The full theme name (e.g., "NORD_DARK").
     * @return The base name (e.g., "Nord").
     */
    public String getBaseThemeName(String themeName) {
        try {
            return AppTheme.valueOf(themeName).getBaseName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return themeName;
        }
    }

    /**
     * Checks if a given theme is a dark variant.
     *
     * @param themeName The full theme name.
     * @return True if the theme is dark, false otherwise.
     */
    public boolean isDarkTheme(String themeName) {
        try {
            return AppTheme.valueOf(themeName).isDark();
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
    
    /**
     * Retrieves a Theme object by its full name.
     *
     * @param name The name of the theme (e.g., "NORD_DARK" or "Nord Dark").
     * @return The Theme object, or null if not found.
     */
    public Theme getThemeByName(String name) {
        // Try to find by AppTheme name
        try {
            return AppTheme.valueOf(name).getTheme();
        } catch (IllegalArgumentException e) {
            // Try to find by display name construction (legacy support if needed)
            for (AppTheme t : AppTheme.values()) {
                String constructedName = t.getBaseName() + (t.isDark() ? " Dark" : " Light");
                if (constructedName.equals(name)) {
                    return t.getTheme();
                }
            }
        }
        return null;
    }
}
