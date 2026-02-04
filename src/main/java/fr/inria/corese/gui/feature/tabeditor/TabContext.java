package fr.inria.corese.gui.feature.tabeditor;

import fr.inria.corese.gui.component.button.FloatingButtonWidget;
import fr.inria.corese.gui.feature.codeeditor.CodeEditorController;
import fr.inria.corese.gui.feature.result.ResultController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Tab;

/**
 * Context object that holds all components associated with a tab.
 *
 * <p>This class follows the Context Object pattern to encapsulate all tab-related data and
 * controllers in a single object. It is stored in the tab's {@link Tab#setUserData(Object)}
 * property, eliminating the need for parallel Maps and reducing memory leak risks.
 *
 * <p><b>Benefits:</b>
 *
 * <ul>
 *   <li>Single source of truth: all tab data is in one place
 *   <li>No parallel Maps to maintain and synchronize
 *   <li>Automatic cleanup: when tab is garbage collected, context is too
 *   <li>Type-safe access to tab components
 *   <li>Easy to extend with new tab-related data
 * </ul>
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create and attach to tab
 * TabContext context = new TabContext(editorController, resultController, floatingButton);
 * tab.setUserData(context);
 *
 * // Retrieve from tab
 * TabContext context = TabContext.get(tab);
 * CodeEditorController editor = context.getEditorController();
 * }</pre>
 */
public class TabContext {

  // ===============================================================================
  // Fields
  // ===============================================================================

  /** Controller for the code editor in this tab. Always non-null. */
  private final CodeEditorController editorController;

  /** Controller for the result view in this tab. May be null if no result view configured. */
  private final ResultController resultController;

  /** Floating execution button for this tab. May be null if no execution configured. */
  private final FloatingButtonWidget executionButton;

  /** Property indicating if an execution is currently running for this tab. */
  private final BooleanProperty executionRunning = new SimpleBooleanProperty(false);

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /**
   * Creates a new TabContext with the specified components.
   *
   * @param editorController The code editor controller (required)
   * @param resultController The result controller (nullable)
   * @param executionButton The floating execution button (nullable)
   * @throws NullPointerException if editorController is null
   */
  public TabContext(
      CodeEditorController editorController,
      ResultController resultController,
      FloatingButtonWidget executionButton) {
    if (editorController == null) {
      throw new NullPointerException("editorController cannot be null");
    }
    this.editorController = editorController;
    this.resultController = resultController;
    this.executionButton = executionButton;
  }

  // ===============================================================================
  // Static Factory Methods
  // ===============================================================================

  /**
   * Retrieves the TabContext from a tab's userData.
   *
   * @param tab The tab to get context from
   * @return The TabContext, or null if tab is null or has no context
   */
  public static TabContext get(Tab tab) {
    if (tab == null) {
      return null;
    }
    Object userData = tab.getUserData();
    return userData instanceof TabContext tabContext ? tabContext : null;
  }

  /**
   * Checks if a tab has a TabContext attached.
   *
   * @param tab The tab to check
   * @return true if the tab has a TabContext, false otherwise
   */
  public static boolean hasContext(Tab tab) {
    return get(tab) != null;
  }

  // ===============================================================================
  // Getters
  // ===============================================================================

  /**
   * Gets the code editor controller for this tab.
   *
   * @return The editor controller (never null)
   */
  public CodeEditorController getEditorController() {
    return editorController;
  }

  /**
   * Gets the result controller for this tab.
   *
   * @return The result controller, or null if not configured
   */
  public ResultController getResultController() {
    return resultController;
  }

  /**
   * Gets the floating execution button for this tab.
   *
   * @return The execution button, or null if not configured
   */
  public FloatingButtonWidget getExecutionButton() {
    return executionButton;
  }

  /**
   * Checks if this tab has a result controller configured.
   *
   * @return true if result controller is present
   */
  public boolean hasResultController() {
    return resultController != null;
  }

  /**
   * Checks if this tab has an execution button configured.
   *
   * @return true if execution button is present
   */
  public boolean hasExecutionButton() {
    return executionButton != null;
  }

  /**
   * Returns the execution running property.
   *
   * @return the property
   */
  public BooleanProperty executionRunningProperty() {
    return executionRunning;
  }

  /**
   * Checks if execution is running.
   *
   * @return true if running
   */
  public boolean isExecutionRunning() {
    return executionRunning.get();
  }

  // ===============================================================================
  // Resource Management
  // ===============================================================================

  /**
   * Disposes of all resources held by this context to prevent memory leaks.
   *
   * <p>This method should be called when the tab is being closed. It ensures that:
   * <ul>
   *   <li>All controllers are properly disposed
   *   <li>All bindings are unbound
   *   <li>All listeners are removed
   * </ul>
   *
   * <p>After calling dispose(), this context should not be used anymore.
   */
  public void dispose() {
    // Dispose editor controller
    if (editorController != null) {
      editorController.dispose();
    }

    // Dispose result controller if present
    if (resultController instanceof AutoCloseable autoCloseable) {
      try {
        autoCloseable.close();
      } catch (Exception _) {
        // Log but don't throw - we're cleaning up
      }
    }

    // Execution button cleanup (unbind if needed)
    if (executionButton != null) {
      executionButton.disableProperty().unbind();
    }
  }
}
