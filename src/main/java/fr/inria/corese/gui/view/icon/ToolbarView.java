package fr.inria.corese.gui.view.icon;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.ButtonIcon;
import fr.inria.corese.gui.view.base.AbstractView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * A vertical bar containing icon buttons, typically used as a secondary toolbar.
 *
 * <p>This view provides a vertical layout of icon buttons.
 */
public class ToolbarView extends AbstractView {

  private static final String STYLESHEET = "/styles/icon-button-bar.css";

  private final VBox container;
  private final Map<ButtonIcon, Button> buttons = new LinkedHashMap<>();

  public ToolbarView() {
    super(new VBox(), STYLESHEET);
    this.container = (VBox) getRoot();
    container.getStyleClass().add("secondary-bar");
  }

  public void setButtons(List<ButtonConfig> configs) {
    container.getChildren().clear();
    buttons.clear();

    if (configs == null) {
      return;
    }

    for (ButtonConfig config : configs) {
      if (config.getIcon() == null) {
        continue;
      }

      Button button = new ToolbarButton(config);
      buttons.put(config.getIcon(), button);
      container.getChildren().add(button);
    }
  }

  public Button getButton(ButtonIcon type) {
    return buttons.get(type);
  }
}