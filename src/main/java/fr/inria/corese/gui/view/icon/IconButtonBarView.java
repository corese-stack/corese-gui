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

  public void initializeButtons(List<IconButtonType> buttonTypes) {
    getChildren().clear();
    buttons.clear();
    buttonTypes.forEach(
        type -> {
          Button button = new IconButtonView(type);
          buttons.put(type, button);
        });
    getChildren().addAll(buttons.values());
  }

  /**
   * Initializes the sidebar with a list of fully configured buttons (including actions). This is
   * the preferred method for the new architecture.
   *
   * @param configs The list of button configurations
   */
  public void setButtons(List<ButtonConfig> configs) {
    getChildren().clear();
    buttons.clear();

    if (configs == null) return;

    for (ButtonConfig config : configs) {
      if (config.getIcon() == null) continue;

      Button button = new IconButtonView(config.getIcon());

      // Apply Tooltip
      if (config.getTooltip() != null && !config.getTooltip().isBlank()) {
        button.setTooltip(new Tooltip(config.getTooltip()));
      }

      // Bind Action
      if (config.getAction() != null) {
        button.setOnAction(e -> config.getAction().run());
      }

      buttons.put(config.getIcon(), button);
      getChildren().add(button);
    }
  }

  public Button getButton(IconButtonType type) {
    return buttons.get(type);
  }

  public void addCustomButton(Button runButton) {
    getChildren().add(runButton);
  }
}
