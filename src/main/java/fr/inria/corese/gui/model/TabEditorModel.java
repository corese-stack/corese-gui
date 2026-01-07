package fr.inria.corese.gui.model;

import fr.inria.corese.gui.controller.CodeEditorController;
import fr.inria.corese.gui.controller.ResultController;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.Tab;

/**
 * Model class for managing the data and state of the TabEditor component.
 *
 * <p>This model maintains the associations between tabs and their corresponding controllers (both
 * code editor and result controllers). It serves as the data layer in the MVC pattern, handling the
 * storage and retrieval of tab-controller mappings.
 *
 * <p>The model is responsible for: 
 * - Managing tab-to-controller associations
 * - Providing access to controllers based on tabs
 * - Handling tab removal and cleanup operations
 */
public class TabEditorModel {

  // ===============================================================================
  // Fields
  // ===============================================================================

  /** Map associating tabs with their code editor controllers. */
  private final Map<Tab, CodeEditorController> tabControllerMap;

  /** Map associating tabs with their result controllers. */
  private final Map<Tab, ResultController> tabResultControllerMap;

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /** Creates a new TabEditorModel with empty controller maps. */
  public TabEditorModel() {
    this.tabControllerMap = new HashMap<>();
    this.tabResultControllerMap = new HashMap<>();
  }

  // ===============================================================================
  // Tab Controller Management
  // ===============================================================================

  /**
   * Registers a new tab and its associated code editor controller.
   *
   * <p>This method stores the mapping between a tab and its controller, allowing later retrieval of
   * the controller for operations on that tab.
   *
   * @param tab The UI tab to register
   * @param controller The code editor controller for the tab's content
   * @throws NullPointerException if tab or controller is null
   */
  public void addTabModel(Tab tab, CodeEditorController controller) {
    if (tab == null) {
      throw new NullPointerException("Tab cannot be null");
    }
    if (controller == null) {
      throw new NullPointerException("Controller cannot be null");
    }
    tabControllerMap.put(tab, controller);
  }

  /**
   * Registers a result controller for a tab.
   *
   * <p>Result controllers handle the display of query execution results or validation output
   * associated with a specific tab.
   *
   * @param tab The UI tab to associate with the result controller
   * @param controller The result controller for displaying output
   * @throws NullPointerException if tab or controller is null
   */
  public void addTabResultController(Tab tab, ResultController controller) {
    if (tab == null) {
      throw new NullPointerException("Tab cannot be null");
    }
    if (controller == null) {
      throw new NullPointerException("Controller cannot be null");
    }
    tabResultControllerMap.put(tab, controller);
  }

  /**
   * Removes all associations for a given tab.
   *
   * <p>This method removes both the code editor controller and result controller mappings for the
   * specified tab. It should be called when a tab is closed to prevent memory leaks.
   *
   * @param tab The tab to remove from the model (null tabs are ignored)
   */
  public void removeTabModel(Tab tab) {
    if (tab != null) {
      tabControllerMap.remove(tab);
      tabResultControllerMap.remove(tab);
    }
  }

  // ===============================================================================
  // Controller Retrieval
  // ===============================================================================

  /**
   * Retrieves the code editor controller associated with a given tab.
   *
   * <p>This method provides access to the controller that manages the code editor component for the
   * specified tab.
   *
   * @param tab The UI tab to look up
   * @return The associated CodeEditorController, or null if the tab is null or not found
   */
  public CodeEditorController getCodeEditorControllerForTab(Tab tab) {
    if (tab == null) {
      return null;
    }
    return tabControllerMap.get(tab);
  }

  /**
   * Retrieves the result controller associated with a given tab.
   *
   * <p>This method provides access to the controller that manages the result display component for
   * the specified tab.
   *
   * @param tab The UI tab to look up
   * @return The associated ResultController, or null if the tab is null or not found
   */
  public ResultController getResultControllerForTab(Tab tab) {
    if (tab == null) {
      return null;
    }
    return tabResultControllerMap.get(tab);
  }

}
