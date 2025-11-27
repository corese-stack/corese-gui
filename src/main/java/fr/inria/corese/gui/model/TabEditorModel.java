package fr.inria.corese.gui.model;

import java.util.HashMap;
import java.util.Map;

import fr.inria.corese.gui.controller.CodeEditorController;
import javafx.scene.control.Tab;

public class TabEditorModel {
  private final Map<Tab, CodeEditorController> tabControllerMap;

  public TabEditorModel() {
    this.tabControllerMap = new HashMap<>();
  }

  /**
   * Registers a new tab and its associated controller.
   *
   * @param tab The UI Tab.
   * @param controller The controller for the content of the tab.
   */
  public void addTabModel(Tab tab, CodeEditorController controller) {
    tabControllerMap.put(tab, controller);
  }

  /**
   * Retrieves the controller associated with a given tab.
   *
   * @param tab The UI Tab to look up.
   * @return The associated CodeEditorController, or null if not found.
   */
  public CodeEditorController getControllerForTab(Tab tab) {
    if (tab == null) {
      // This case can happen, e.g., when no tab is selected. It's not an error.
      return null;
    }
    return tabControllerMap.get(tab);
  }

  public void removeTabModel(Tab tab) {
    if (tab != null) {
      tabControllerMap.remove(tab);
    }
  }

  public void clearAll() {
    tabControllerMap.clear();
  }

  public Map<Tab, CodeEditorController> getTabControllers() {
    return tabControllerMap;
  }
}
