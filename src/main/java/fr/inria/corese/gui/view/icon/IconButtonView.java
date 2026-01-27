package fr.inria.corese.gui.view.icon;

import fr.inria.corese.gui.core.ButtonConfig;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A standard icon button component using Ikonli and the unified button configuration system.
 *
 * <p>This view is a lightweight, reusable UI component that renders an icon button with optional
 * tooltip and action. It follows the separation of concerns principle by handling only the visual
 * representation, while delegating configuration to {@link ButtonConfig}.
 *
 * <p><b>Key features:</b>
 *
 * <ul>
 *   <li>Icon rendering with configurable size
 *   <li>AtlantaFX flat button styling
 *   <li>Automatic theme-aware icon coloring
 *   <li>Optional tooltip and click action
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * ButtonConfig config = new ButtonConfig(
 *     IconButtonType.SAVE,
 *     "Save File",
 *     () -> saveFile()
 * );
 * IconButtonView button = new IconButtonView(config);
 * }</pre>
 *
 * @see ButtonConfig
 * @see fr.inria.corese.gui.core.AppButtons
 */
public class IconButtonView extends Button {

  // ===============================================================================
  // Constants
  // ===============================================================================

  private static final int ICON_SIZE = 25;

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /**
   * Creates an icon button from a configuration object.
   *
   * @param config The button configuration (icon, tooltip, action)
   * @throws IllegalArgumentException if config is null or has no icon
   */
  public IconButtonView(ButtonConfig config) {
    if (config == null || config.getIcon() == null) {
      throw new IllegalArgumentException("ButtonConfig must not be null and must have an icon.");
    }

    configureIcon(config);
    configureTooltip(config);
    configureAction(config);
  }

  // ===============================================================================
  // Private Methods
  // ===============================================================================

  /**
   * Configures the button icon with proper styling and theme binding.
   *
   * @param config The button configuration
   */
  private void configureIcon(ButtonConfig config) {
    FontIcon fontIcon = new FontIcon(config.getIcon().getIkon());
    fontIcon.setIconSize(ICON_SIZE);

    // Bind icon color to button text color to respect theme changes
    fontIcon.iconColorProperty().bind(textFillProperty());

    setGraphic(fontIcon);

    // Apply AtlantaFX flat button style
    getStyleClass().add("flat");
  }

  /**
   * Configures the button tooltip if specified in the configuration.
   *
   * @param config The button configuration
   */
  private void configureTooltip(ButtonConfig config) {
    if (config.getTooltip() != null) {
      setTooltip(new Tooltip(config.getTooltip()));
    }
  }

  /**
   * Configures the button click action if specified in the configuration.
   *
   * @param config The button configuration
   */
  private void configureAction(ButtonConfig config) {
    if (config.getAction() != null) {
      setOnAction(e -> config.getAction().run());
    }
  }
}
