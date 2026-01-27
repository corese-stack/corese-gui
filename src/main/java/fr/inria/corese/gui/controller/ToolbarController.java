package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.core.ButtonFactory;
import fr.inria.corese.gui.enums.icon.ButtonIcon;
import fr.inria.corese.gui.model.ToolbarModel;
import fr.inria.corese.gui.view.icon.ToolbarView;
import java.util.List;
import javafx.scene.control.Button;

/**
 * A generic controller for managing a toolbar.
 *
 * <p>This controller is responsible for:
 *
 * <ul>
 *   <li>Initializing the toolbar view with buttons
 *   <li>Binding the view's button state (disabled) to the model's state (enabled)
 * </ul>
 *
 * <p>It does NOT handle the business logic of the buttons (what happens when clicked). The actions
 * must be provided in the {@link ButtonConfig}.
 */
public class ToolbarController {

  private final ToolbarView view;
  private final ToolbarModel model;

  /**
   * Creates a new generic ToolbarController.
   *
   * @param buttons The list of button configurations. If null, default configurations are created
   *     from the model.
   * @param model The toolbar model managing state.
   * @param view The toolbar view displaying buttons.
   */
  public ToolbarController(List<ButtonConfig> buttons, ToolbarModel model, ToolbarView view) {
    this.model = model;
    this.view = view;

    List<ButtonConfig> configs =
        (buttons == null || buttons.isEmpty()) ? createConfigsFromModel() : buttons;

    // We just pass the configs to the view. The view handles creating the buttons.
    view.setButtons(configs);

    // Bind the view state to the model state immediately
    bindViewToModel();
  }

  /**
   * Creates default configurations for buttons present in the model. This ensures buttons have
   * standard icons and tooltips even if not explicitly configured.
   */
  private List<ButtonConfig> createConfigsFromModel() {
    return model.getAvailableButtons().stream().map(this::createConfigFromFactory).toList();
  }

  private ButtonConfig createConfigFromFactory(ButtonIcon icon) {
    return switch (icon) {
      case SAVE -> ButtonFactory.save(null);
      case OPEN_FILE -> ButtonFactory.openFile(null);
      case EXPORT -> ButtonFactory.export(null);
      case IMPORT -> ButtonFactory.importFile(null);
      case CLEAR -> ButtonFactory.clear(null);
      case UNDO -> ButtonFactory.undo(null);
      case REDO -> ButtonFactory.redo(null);
      case DOCUMENTATION -> ButtonFactory.documentation(null);
      case DELETE -> ButtonFactory.delete(null);
      case COPY -> ButtonFactory.copy(null);
      case PLAY -> ButtonFactory.play(null);
      case RELOAD -> ButtonFactory.reload(null);
      case LOGS -> ButtonFactory.logs(null);
      default -> new ButtonConfig(icon, null, null);
    };
  }

  /** Binds the View's disable properties to the Model's enabled properties. */
  private void bindViewToModel() {
    for (ButtonIcon icon : model.getAvailableButtons()) {
      Button button = view.getButton(icon);
      if (button != null) {
        // View 'disable' is the inverse of Model 'enabled'
        button.disableProperty().bind(model.enabledProperty(icon).not());
      }
    }
  }

  public ToolbarView getView() {
    return view;
  }

  public ToolbarModel getModel() {
    return model;
  }
}
