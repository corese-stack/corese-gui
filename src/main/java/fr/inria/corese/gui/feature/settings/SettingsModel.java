package fr.inria.corese.gui.feature.settings;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.core.theme.ThemeManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

/**
 * Model for application settings.
 *
 * <p>
 * This model stores user preferences including theme selection and accent
 * color. It uses JavaFX properties to enable automatic UI binding and change
 * notifications.
 *
 * <p>
 * Properties:
 * <ul>
 * <li>{@link #themeProperty()} - The currently selected theme
 * <li>{@link #accentColorProperty()} - The current accent color
 * <li>{@link #useSystemThemeProperty()} - Whether to use system theme detection
 * </ul>
 */
public final class SettingsModel {

	// ===== Properties =====

	// Backing properties (documented on property accessor methods to avoid javadoc
	// duplicate-property warnings).
	private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>();

	private final ObjectProperty<Color> accentColor = new SimpleObjectProperty<>(ThemeManager.getDefaultAccentColor());

	private final BooleanProperty useSystemTheme = new SimpleBooleanProperty(true); // Enabled by default

	private final DoubleProperty uiScale = new SimpleDoubleProperty(ThemeManager.getDefaultUiScale());

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
	 * @param theme
	 *            the theme to set
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
	 * @param color
	 *            the color to set
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
	 * @param useSystemTheme
	 *            true to enable system theme detection
	 */
	public void setUseSystemTheme(boolean useSystemTheme) {
		this.useSystemTheme.set(useSystemTheme);
	}

	/**
	 * Returns the global interface scale property.
	 *
	 * @return the UI scale property
	 */
	public DoubleProperty uiScaleProperty() {
		return uiScale;
	}

	/**
	 * Gets the current global UI scale.
	 *
	 * @return scale factor (1.0 = 100%)
	 */
	public double getUiScale() {
		return uiScale.get();
	}

	/**
	 * Sets the global UI scale.
	 *
	 * @param scale
	 *            scale factor (1.0 = 100%)
	 */
	public void setUiScale(double scale) {
		this.uiScale.set(scale);
	}
}
