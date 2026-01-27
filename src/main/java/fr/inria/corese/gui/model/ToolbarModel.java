package fr.inria.corese.gui.model;

import fr.inria.corese.gui.enums.icon.ButtonIcon;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Model for managing the state of the application toolbar.
 *
 * <p>This model acts as the "source of truth" for the toolbar's UI state. It maintains
 * observable properties for each button's enabled/disabled state.
 *
 * <p><b>MVC Role:</b>
 * <ul>
 *   <li><b>Model:</b> Holds the state (is the "Save" button enabled?).
 *   <li><b>Observability:</b> Exposes {@link BooleanProperty} so the View (or Controller) can bind to it.
 * </ul>
 */
public class ToolbarModel {

  private final Map<ButtonIcon, BooleanProperty> buttonStates = new LinkedHashMap<>();

  public ToolbarModel(List<ButtonIcon> buttons) {
    if (buttons != null) {
      buttons.forEach(button -> buttonStates.put(button, new SimpleBooleanProperty(true)));
    }
  }

  public List<ButtonIcon> getAvailableButtons() {
    return List.copyOf(buttonStates.keySet());
  }

  public BooleanProperty enabledProperty(ButtonIcon type) {
    if (!buttonStates.containsKey(type)) {
      throw new IllegalArgumentException("Button " + type + " is not managed by this model.");
    }
    return buttonStates.get(type);
  }

  public void setButtonEnabled(ButtonIcon type, boolean enabled) {
    enabledProperty(type).set(enabled);
  }
}