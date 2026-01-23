package fr.inria.corese.gui.view.icon;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public class IconButtonBarView extends VBox {
  private static final String STYLESHEET = "/styles/icon-button-bar.css";
  private final Map<IconButtonType, Button> buttons = new LinkedHashMap<>();

  public IconButtonBarView() {
    getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
    setSpacing(10);
    setAlignment(Pos.TOP_CENTER);
    getStyleClass().add("secondary-bar");
    setMaxHeight(Double.MAX_VALUE);
  }

  /**
   * Initializes the sidebar with a list of fully configured buttons (including actions).
   * This is the preferred method for the new architecture.
   *
   * @param configs The list of button configurations
   */
  public void setButtons(List<ButtonConfig> configs) {
    getChildren().clear();
    buttons.clear();

    if (configs == null) return;

    for (ButtonConfig config : configs) {
      if (config.getIcon() == null) continue;

      // Create button directly from config (handles tooltip and action internally)
      Button button = new IconButtonView(config);

      buttons.put(config.getIcon(), button);
      getChildren().add(button);
    }
  }

  public Button getButton(IconButtonType type) {
    return buttons.get(type);
  }
}
