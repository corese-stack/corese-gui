package fr.inria.corese.gui.component.toolbar;

import fr.inria.corese.gui.core.ButtonConfig;






import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A standard icon button component (Widget).
 *
 * <p>This widget renders an icon button with optional tooltip and action.
 * It is based on the {@link ButtonConfig} configuration.
 */
public class ActionButtonWidget extends Button {

  private static final int ICON_SIZE = 25;

  /**
   * Creates an icon button from a configuration object.
   *
   * @param config The button configuration (icon, tooltip, action)
   */
  public ActionButtonWidget(ButtonConfig config) {
    if (config == null || config.getIcon() == null) {
      throw new IllegalArgumentException("ButtonConfig must not be null and must have an icon.");
    }

    configureIcon(config);
    configureTooltip(config);
    configureAction(config);
  }

  private void configureIcon(ButtonConfig config) {
    FontIcon fontIcon = new FontIcon(config.getIcon().getIkon());
    fontIcon.setIconSize(ICON_SIZE);
    fontIcon.iconColorProperty().bind(textFillProperty());
    setGraphic(fontIcon);
    getStyleClass().add("flat");
  }

  private void configureTooltip(ButtonConfig config) {
    if (config.getTooltip() != null) {
      setTooltip(new Tooltip(config.getTooltip()));
    }
  }

  private void configureAction(ButtonConfig config) {
    if (config.getAction() != null) {
      setOnAction(e -> config.getAction().run());
    }
  }
}