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
 * A vertical toolbar view containing icon buttons.
 *
 * <p>This view displays a column of buttons, typically positioned on the right side of the editor.
 * It is completely generic and relies on {@link ButtonConfig} for content and {@link ToolbarButton}
 * for rendering.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Vertical layout (VBox)</li>
 *   <li>Consistent styling via CSS ("app-toolbar")</li>
 *   <li>Dynamic button creation from configuration</li>
 * </ul>
 */
public class ToolbarView extends AbstractView {

  private static final String STYLESHEET = "/styles/toolbar.css";
  private static final String STYLE_CLASS = "app-toolbar";

  private final VBox container;
  private final Map<ButtonIcon, Button> buttons = new LinkedHashMap<>();

  /**
   * Creates a new generic toolbar view.
   */
  public ToolbarView() {
    super(new VBox(), STYLESHEET);
    this.container = (VBox) getRoot();
    container.getStyleClass().add(STYLE_CLASS);
  }

  /**
   * Sets the buttons to be displayed in the toolbar.
   *
   * <p>This clears the current buttons and rebuilds the toolbar based on the provided list of configurations.
   *
   * @param configs The list of button configurations. If null, the toolbar is cleared.
   */
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

  /**
   * Retrieves a button by its icon type.
   *
   * @param type The type of the button to retrieve.
   * @return The JavaFX Button instance, or null if not found.
   */
  public Button getButton(ButtonIcon type) {
    return buttons.get(type);
  }
}