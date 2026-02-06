package fr.inria.corese.gui.component.button;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

import fr.inria.corese.gui.component.button.config.ButtonConfig;

/**
 * A standard icon button component (Widget).
 *
 * <p>
 * This widget renders an icon button with optional tooltip and action. It is
 * based on the {@link
 * ButtonConfig} configuration.
 */
public class IconButtonWidget extends Button {

  private static final int ICON_SIZE = 25;
  private static final String STYLESHEET = "/css/components/icon-button-widget.css";

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Creates an icon button from a configuration object.
   *
   * @param config The button configuration (icon, tooltip, action)
   */
  public IconButtonWidget(ButtonConfig config) {
    if (config == null || config.getIcon() == null) {
      throw new IllegalArgumentException("ButtonConfig must not be null and must have an icon.");
    }

    initializeStyle();
    configureIcon(config);
    configureTooltip(config);
    configureAction(config);
  }

  // ==============================================================================================
  // Initialization Methods
  // ==============================================================================================

  private void initializeStyle() {
    java.net.URL cssResource = getClass().getResource(STYLESHEET);
    if (cssResource != null) {
      getStylesheets().add(cssResource.toExternalForm());
    }
    getStyleClass().add("icon-button");
  }

  private void configureIcon(ButtonConfig config) {
    FontIcon fontIcon = new FontIcon(config.getIcon().getIkon());
    fontIcon.setIconSize(ICON_SIZE);
    fontIcon.getStyleClass().add("icon-button-icon");
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
