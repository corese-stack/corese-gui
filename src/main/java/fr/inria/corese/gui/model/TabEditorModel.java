package fr.inria.corese.gui.model;

import fr.inria.corese.gui.controller.CodeEditorController;
import fr.inria.corese.gui.controller.ResultController;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.Tab;

public class TabEditorModel {
  private final Map<Tab, CodeEditorController> tabControllerMap;
  private final Map<Tab, ResultController> tabResultControllerMap;

  public TabEditorModel() {
    this.tabControllerMap = new HashMap<>();
    this.tabResultControllerMap = new HashMap<>();
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
   * Registers a result controller for a tab.
   *
   * @param tab The UI Tab.
   * @param controller The result controller.
   */
  public void addTabResultController(Tab tab, ResultController controller) {
    tabResultControllerMap.put(tab, controller);
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

  /**
   * Retrieves the result controller associated with a given tab.
   *
   * @param tab The UI Tab to look up.
   * @return The associated ResultController, or null if not found.
   */
  public ResultController getResultControllerForTab(Tab tab) {
    if (tab == null) {
      return null;
    }
    return tabResultControllerMap.get(tab);
  }

  public void removeTabModel(Tab tab) {
    if (tab != null) {
      tabControllerMap.remove(tab);
      tabResultControllerMap.remove(tab);
    }
  }

  public void clearAll() {
    tabControllerMap.clear();
    tabResultControllerMap.clear();
  }

  public Map<Tab, CodeEditorController> getTabControllers() {
    return tabControllerMap;
  }
}
