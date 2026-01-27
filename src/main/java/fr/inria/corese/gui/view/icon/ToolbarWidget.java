package fr.inria.corese.gui.view.icon;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.ButtonIcon;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * A vertical toolbar widget containing icon buttons.
 *
 * <p>This simple UI component displays a column of buttons. It is designed to be embedded
 * in other views (like CodeEditorView or TextResultView) and controlled directly by their
 * respective controllers.
 */
public class ToolbarWidget extends VBox {

  private static final String STYLESHEET = "/styles/toolbar.css";
  private static final String STYLE_CLASS = "app-toolbar";

  private final Map<ButtonIcon, Button> buttons = new LinkedHashMap<>();

  /**
   * Creates a new generic toolbar widget.
   */
  public ToolbarWidget() {
    initialize();
  }

  private void initialize() {
    URL cssResource = getClass().getResource(STYLESHEET);
    if (cssResource != null) {
      getStylesheets().add(cssResource.toExternalForm());
    }
    getStyleClass().add(STYLE_CLASS);
  }

  /**
   * Sets the buttons to be displayed in the toolbar.
   *
   * @param configs The list of button configurations.
   */
  public void setButtons(List<ButtonConfig> configs) {
    getChildren().clear();
    buttons.clear();

    if (configs == null) {
      return;
    }

    for (ButtonConfig config : configs) {
      if (config.getIcon() == null) {
        continue;
      }

      ActionButtonWidget button = new ActionButtonWidget(config);
      buttons.put(config.getIcon(), button);
      getChildren().add(button);
    }
  }

  /**
   * Sets the disabled state of a specific button.
   *
   * @param type The button icon type.
   * @param disabled true to disable, false to enable.
   */
  public void setButtonDisabled(ButtonIcon type, boolean disabled) {
    Button button = buttons.get(type);
    if (button != null) {
      button.setDisable(disabled);
    }
  }

  /**
   * Returns the disable property of a button for binding.
   * 
   * @param type The button icon type.
   * @return The disable property, or null if button not found.
   */
  public BooleanProperty buttonDisableProperty(ButtonIcon type) {
      Button button = buttons.get(type);
      return button != null ? button.disableProperty() : null;
  }
  
  /**
   * Retrieves a button instance directly.
   * Prefer using setButtonDisabled for simple state changes.
   */
  public Button getButton(ButtonIcon type) {
    return buttons.get(type);
  }
}