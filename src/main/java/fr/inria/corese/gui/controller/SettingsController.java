package fr.inria.corese.gui.controller;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.model.SettingsModel;
import fr.inria.corese.gui.view.utils.ThemeManager;

import java.util.List;

/**
 * Controller for the Settings view.
 *
 * <p>Acts as a bridge between the SettingsModel (View state) and ThemeManager (App state).
 * Strictly follows MVC by delegating all business logic to the ThemeManager.
 */
public final class SettingsController {

    private final SettingsModel model;
    private final ThemeManager themeManager;

    public SettingsController(SettingsModel model) {
        this.model = model;
        this.themeManager = ThemeManager.getInstance();
        setupBindings();
    }

    private void setupBindings() {
        // 1. Initial Sync: Manager -> Model
        if (themeManager.getTheme() != null) model.setTheme(themeManager.getTheme());
        if (themeManager.getAccentColor() != null) model.setAccentColor(themeManager.getAccentColor());
        model.setUseSystemTheme(themeManager.isSystemThemeEnabled());

        // 2. Model -> Manager (User inputs)
        model.themeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !model.isUseSystemTheme()) {
                themeManager.setTheme(newVal);
            }
        });

        model.accentColorProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                themeManager.setAccentColor(newVal);
            }
        });

        model.useSystemThemeProperty().addListener((obs, oldVal, newVal) -> 
            themeManager.setSystemThemeEnabled(newVal)
        );

        // 3. Manager -> Model (System changes or external updates)
        themeManager.themeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(model.getTheme())) {
                model.setTheme(newVal);
            }
        });

        themeManager.accentColorProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(model.getAccentColor())) {
                model.setAccentColor(newVal);
            }
        });
        
        themeManager.systemThemeEnabledProperty().addListener((obs, oldVal, newVal) -> {
            if (!Boolean.valueOf(model.isUseSystemTheme()).equals(newVal)) {
                model.setUseSystemTheme(Boolean.TRUE.equals(newVal));
            }
        });
    }

    // ===== Data Access for View =====

    public List<String> getBaseThemes() {
        return themeManager.getBaseThemes();
    }

    public String getCurrentThemeName() {
        return themeManager.getCurrentThemeName();
    }
    
    public String getBaseThemeName(String fullThemeName) {
        return themeManager.getBaseThemeName(fullThemeName);
    }
    
    public boolean isDarkTheme(String themeName) {
        return themeManager.isDarkTheme(themeName);
    }
    
    // ===== Actions =====

    public void applyThemeByName(String themeName) {
        Theme theme = themeManager.getThemeByName(themeName);
        if (theme != null) {
            model.setTheme(theme);
        }
    }
    
    public void applyTheme(String baseName, boolean isDark) {
        Theme theme = themeManager.getThemeVariant(baseName, isDark);
        if (theme != null) {
            model.setTheme(theme);
        }
    }
}
