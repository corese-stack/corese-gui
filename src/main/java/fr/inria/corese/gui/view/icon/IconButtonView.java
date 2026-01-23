package fr.inria.corese.gui.view.icon;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A standard icon button component using Ikonli.
 * <p>
 * This view is responsible solely for the visual representation of the button (icon, styling).
 * Configuration (actions, tooltips) is handled by the controller or parent view.
 */
public class IconButtonView extends Button {

  /**
   * Creates an icon button from a configuration object.
   * This is the preferred constructor for the unified button system.
   *
   * @param config The button configuration (icon, tooltip, action)
   */
  public IconButtonView(ButtonConfig config) {
    if (config == null || config.getIcon() == null) {
      throw new IllegalArgumentException("ButtonConfig must not be null and must have an icon.");
    }
    
    initialize(config.getIcon());
    
    if (config.getTooltip() != null) {
      setTooltip(new Tooltip(config.getTooltip()));
    }
    
    if (config.getAction() != null) {
      setOnAction(e -> config.getAction().run());
    }
  }

  /**
   * Legacy constructor. Prefer using ButtonConfig.
   *
   * @param type The icon type
   */
  public IconButtonView(IconButtonType type) {
    initialize(type);
  }

  private void initialize(IconButtonType type) {
    FontIcon fontIcon = new FontIcon(type.getIkon());
    fontIcon.setIconSize(25);

    // Use AtlantaFX styles
    getStyleClass().add("flat");

    // Bind icon color to button text color to respect theme
    fontIcon.iconColorProperty().bind(textFillProperty());

    setGraphic(fontIcon);
  }
}
