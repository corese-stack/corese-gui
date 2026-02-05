package fr.inria.corese.gui.component.toolbar;

import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.layout.VBox;

/**
 * A vertical toolbar widget containing icon buttons.
 *
 * <p>This simple UI component displays a column of buttons. It is designed to be embedded in other
 * views (like CodeEditorView or TextResultView) and controlled directly by their respective
 * controllers.
 */
public class ToolbarWidget extends VBox {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

      private static final String STYLESHEET = "/css/features/toolbar.css";  private static final String STYLE_CLASS = "app-toolbar";

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  /** Map for quick access to buttons by their icon type. */
  private final Map<ButtonIcon, IconButtonWidget> buttonMap = new LinkedHashMap<>();

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Creates a new generic toolbar widget. */
  public ToolbarWidget() {
    initialize();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private void initialize() {
    CssUtils.applyViewStyles(this, STYLESHEET);
    getStyleClass().add(STYLE_CLASS);
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Sets the buttons to be displayed in the toolbar.
   *
   * @param configs The list of button configurations.
   */
  public void setButtons(List<ButtonConfig> configs) {
    getChildren().clear();
    buttonMap.clear();

    if (configs == null) {
      return;
    }

    for (ButtonConfig config : configs) {
      if (config.getIcon() == null) {
        continue;
      }

      IconButtonWidget button = new IconButtonWidget(config);
      buttonMap.put(config.getIcon(), button);
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
    IconButtonWidget button = buttonMap.get(type);
    if (button != null) {
      button.setDisable(disabled);
    }
  }

  /**
   * Retrieves a button instance directly. Prefer using setButtonDisabled for simple state changes.
   *
   * @param type The button icon type.
   * @return The button widget, or null if not found.
   */
  public IconButtonWidget getButton(ButtonIcon type) {
    return buttonMap.get(type);
  }
}
