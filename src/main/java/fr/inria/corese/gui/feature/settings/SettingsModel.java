package fr.inria.corese.gui.feature.settings;

import atlantafx.base.theme.Theme;
import javafx.beans.property.*;
import javafx.scene.paint.Color;

/**
 * Model for application settings.
 *
 * <p>This model stores user preferences including theme selection and accent color.
 * It uses JavaFX properties to enable automatic UI binding and change notifications.
 *
 * <p>Properties:
 * <ul>
 *   <li>{@link #themeProperty()} - The currently selected theme
 *   <li>{@link #accentColorProperty()} - The current accent color
 *   <li>{@link #useSystemThemeProperty()} - Whether to use system theme detection
 * </ul>
 */
public final class SettingsModel {

  // ===== Properties =====

  /** The currently selected application theme. */
  private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>();

  /** The current accent color. */
  private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(Color.web("#0078D4"));

  /** Whether to automatically detect and use the system theme. */
  private final BooleanProperty useSystemTheme = new SimpleBooleanProperty(true); // Enabled by default

  // ===== Constructors =====

  /** Creates a new SettingsModel with default values. */
  public SettingsModel() {
    // Default values are set in property declarations
  }

  // ===== Property Accessors =====

  /**
   * Returns the theme property.
   *
   * @return the theme property
   */
  public ObjectProperty<Theme> themeProperty() {
    return theme;
  }

  /**
   * Gets the current theme.
   *
   * @return the current theme, or null if not set
   */
  public Theme getTheme() {
    return theme.get();
  }

  /**
   * Sets the current theme.
   *
   * @param theme the theme to set
   */
  public void setTheme(Theme theme) {
    this.theme.set(theme);
  }

  /**
   * Returns the accent color property.
   *
   * @return the accent color property
   */
  public ObjectProperty<Color> accentColorProperty() {
    return accentColor;
  }

  /**
   * Gets the current accent color.
   *
   * @return the current accent color
   */
  public Color getAccentColor() {
    return accentColor.get();
  }

  /**
   * Sets the accent color.
   *
   * @param color the color to set
   */
  public void setAccentColor(Color color) {
    this.accentColor.set(color);
  }

  /**
   * Returns the use system theme property.
   *
   * @return the use system theme property
   */
  public BooleanProperty useSystemThemeProperty() {
    return useSystemTheme;
  }

  /**
   * Gets whether system theme detection is enabled.
   *
   * @return true if system theme detection is enabled
   */
  public boolean isUseSystemTheme() {
    return useSystemTheme.get();
  }

  /**
   * Sets whether to use system theme detection.
   *
   * @param useSystemTheme true to enable system theme detection
   */
  public void setUseSystemTheme(boolean useSystemTheme) {
    this.useSystemTheme.set(useSystemTheme);
  }
}
