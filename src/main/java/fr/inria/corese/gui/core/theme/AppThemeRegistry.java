package fr.inria.corese.gui.core.theme;

import atlantafx.base.theme.*;

/**
 * Enumeration of available application themes.
 *
 * <p>Each enum constant represents a specific theme variant (light or dark) and holds the necessary
 * configuration (name, darkness, theme instance).
 */
public enum AppThemeRegistry {
  NORD_LIGHT("Nord", false, new NordLight()),
  NORD_DARK("Nord", true, new NordDark()),
  PRIMER_LIGHT("Primer", false, new PrimerLight()),
  PRIMER_DARK("Primer", true, new PrimerDark()),
  CUPERTINO_LIGHT("Cupertino", false, new CupertinoLight()),
  CUPERTINO_DARK("Cupertino", true, new CupertinoDark());

  // ===== Fields =====

  private final String baseName;
  private final boolean isDark;
  private final Theme theme;

  // ==== Constructor =====

  /**
   * Constructs a new AppTheme.
   *
   * @param baseName The family name of the theme (e.g. "Nord")
   * @param isDark True if this is a dark theme
   * @param theme The AtlantaFX Theme instance
   */
  AppThemeRegistry(String baseName, boolean isDark, Theme theme) {
    this.baseName = baseName;
    this.isDark = isDark;
    this.theme = theme;
  }

  // ==== Public Methods =====

  /**
   * Returns the family name of the theme (e.g., "Nord", "Primer").
   *
   * @return the family name
   */
  public String getBaseName() {
    return baseName;
  }

  /**
   * Checks if this is a dark theme variant.
   *
   * @return true if dark, false otherwise
   */
  public boolean isDark() {
    return isDark;
  }

  /**
   * Returns the AtlantaFX Theme instance.
   *
   * @return the theme instance
   */
  public Theme getTheme() {
    return theme;
  }

  /**
   * Finds the AppTheme corresponding to a given AtlantaFX Theme instance.
   *
   * @param theme the AtlantaFX theme
   * @return the matching AppTheme, or null if not found
   */
  public static AppThemeRegistry fromTheme(Theme theme) {
    for (AppThemeRegistry appTheme : values()) {
      if (appTheme.getTheme().getClass().equals(theme.getClass())) {
        return appTheme;
      }
    }
    return null;
  }

  /**
   * Finds a theme variant based on the family name and darkness.
   *
   * @param baseName the family name (e.g., "Nord")
   * @param isDark true for dark mode, false for light mode
   * @return the matching AppTheme, or null if not found
   */
  public static AppThemeRegistry getVariant(String baseName, boolean isDark) {
    for (AppThemeRegistry appTheme : values()) {
      if (appTheme.baseName.equals(baseName) && appTheme.isDark == isDark) {
        return appTheme;
      }
    }
    return null;
  }
}
