package fr.inria.corese.gui.core;

import fr.inria.corese.gui.enums.icon.IconButtonType;

/**
 * Factory for creating standardized ButtonConfig objects.
 *
 * <p>Ensures consistency of tooltips and icons across the application. Use this factory instead of
 * creating ButtonConfig manually when using standard actions.
 */
public final class AppButtons {

  private AppButtons() {
    // Utility class
  }

  /**
   * Creates a standardized Copy to Clipboard button.
   *
   * @param action The action to execute
   * @return A configured ButtonConfig
   */
  public static ButtonConfig copy(Runnable action) {
    return new ButtonConfig(IconButtonType.COPY, "Copy to Clipboard", action);
  }

  /**
   * Creates a standardized Export button.
   *
   * @param action The action to execute
   * @return A configured ButtonConfig
   */
  public static ButtonConfig export(Runnable action) {
    return new ButtonConfig(IconButtonType.EXPORT, "Export to File", action);
  }

  /**
   * Creates a standardized Clear button.
   *
   * @param action The action to execute
   * @return A configured ButtonConfig
   */
  public static ButtonConfig clear(Runnable action) {
    return new ButtonConfig(IconButtonType.CLEAR, "Clear Content", action);
  }

    /**
     * Creates a standardized Save button.
     *
     * @param action The action to execute
     * @return A configured ButtonConfig
     */
    public static ButtonConfig save(Runnable action) {
      return new ButtonConfig(IconButtonType.SAVE, "Save", action);
    }
  
    public static ButtonConfig importFile(Runnable action) {
      return new ButtonConfig(IconButtonType.IMPORT, "Import files", action);
    }
  
    public static ButtonConfig reload(Runnable action) {
      return new ButtonConfig(IconButtonType.RELOAD, "Reload", action);
    }
  
    public static ButtonConfig delete(Runnable action) {
      return new ButtonConfig(IconButtonType.DELETE, "Delete", action);
    }
  
    public static ButtonConfig logs(Runnable action) {
      return new ButtonConfig(IconButtonType.LOGS, "Show logs", action);
    }
  
    public static ButtonConfig documentation(Runnable action) {
      return new ButtonConfig(IconButtonType.DOCUMENTATION, "Documentation", action);
    }
  
    public static ButtonConfig undo(Runnable action) {
      return new ButtonConfig(IconButtonType.UNDO, "Undo", action);
    }
  
    public static ButtonConfig redo(Runnable action) {
      return new ButtonConfig(IconButtonType.REDO, "Redo", action);
    }
  
    public static ButtonConfig play(Runnable action) {
      return new ButtonConfig(IconButtonType.PLAY, "Run", action);
    }
    
    // Add other standard buttons (Undo, Redo, Play...) as needed
  }
