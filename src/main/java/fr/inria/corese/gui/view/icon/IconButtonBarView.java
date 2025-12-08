package fr.inria.corese.gui.view.icon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.inria.corese.gui.enums.icon.IconButtonType;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
    buttonTypes.forEach(
        type -> {
          Button button = new IconButtonView(type);
          buttons.put(type, button);
        });
    getChildren().addAll(buttons.values());
  }

  public Button getButton(IconButtonType type) {
    return buttons.get(type);
  }

  public void addCustomButton(Button runButton) {
    getChildren().add(runButton);
  }
}
