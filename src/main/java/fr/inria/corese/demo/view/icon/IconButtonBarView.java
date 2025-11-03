package fr.inria.corese.demo.view.icon;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class IconButtonBarView extends VBox {
  private final Map<IconButtonType, Button> buttons = new LinkedHashMap<>();

  public IconButtonBarView() {
    setSpacing(10);
    setAlignment(Pos.TOP_CENTER);
    setStyle("-fx-background-color: transparent;");
  }

  public void initializeButtons(List<IconButtonType> buttonTypes) {
    getChildren().clear();
    buttonTypes.forEach(
        type -> {
          Button button = new IconButtonView(type);
          buttons.put(type, button);
        });
    getChildren().addAll(buttons.values());
    setHeight(50);
  }

  public Button getButton(IconButtonType type) {
    return buttons.get(type);
  }

  public void addCustomButton(Button runButton) {
    getChildren().add(runButton);
  }
}
