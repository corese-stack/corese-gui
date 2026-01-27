package fr.inria.corese.gui.core;

import fr.inria.corese.gui.enums.icon.ButtonIcon;

/**
 * Factory for creating standardized button configurations.
 *
 * <p>This utility class provides factory methods for creating {@link ButtonConfig} instances with
 * consistent icons and tooltips across the application. Using this factory instead of creating
 * ButtonConfig instances manually ensures:
 *
 * <ul>
 *   <li><b>Consistency:</b> Same tooltip wording for the same action everywhere
 *   <li><b>Centralization:</b> Single place to update tooltip text
 *   <li><b>Type safety:</b> Clear method names for each button type
 * </ul>
 *
 * <p><b>Design pattern:</b> This follows the Factory pattern for creating pre-configured objects.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * List<ButtonConfig> buttons = List.of(
 *     AppButtons.save(() -> saveFile()),
 *     AppButtons.undo(() -> undo()),
 *     AppButtons.redo(() -> redo())
 * );
 *
 * // Or with null action to be wired later by controller
 * ButtonConfig config = AppButtons.save(null);
 * }</pre>
 *
 * @see ButtonConfig
 * @see ButtonIcon
 */
public final class ButtonFactory {

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private ButtonFactory() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  // ===============================================================================
  // File Operations
  // ===============================================================================

  /**
   * Creates a standardized Save button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with save icon and tooltip
   */
  public static ButtonConfig save(Runnable action) {
    return new ButtonConfig(ButtonIcon.SAVE, "Save", action);
  }

  /**
   * Creates a standardized Import button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with import icon and tooltip
   */
  public static ButtonConfig importFile(Runnable action) {
    return new ButtonConfig(ButtonIcon.IMPORT, "Import files", action);
  }

  /**
   * Creates a standardized Export button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with export icon and tooltip
   */
  public static ButtonConfig export(Runnable action) {
    return new ButtonConfig(ButtonIcon.EXPORT, "Export to File", action);
  }

  /**
   * Creates a standardized Open File button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with open file icon and tooltip
   */
  public static ButtonConfig openFile(Runnable action) {
    return new ButtonConfig(ButtonIcon.OPEN_FILE, "Open File", action);
  }

  // ===============================================================================
  // Editor Operations
  // ===============================================================================

  /**
   * Creates a standardized Clear button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with clear icon and tooltip
   */
  public static ButtonConfig clear(Runnable action) {
    return new ButtonConfig(ButtonIcon.CLEAR, "Clear Content", action);
  }

  /**
   * Creates a standardized Undo button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with undo icon and tooltip
   */
  public static ButtonConfig undo(Runnable action) {
    return new ButtonConfig(ButtonIcon.UNDO, "Undo", action);
  }

  /**
   * Creates a standardized Redo button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with redo icon and tooltip
   */
  public static ButtonConfig redo(Runnable action) {
    return new ButtonConfig(ButtonIcon.REDO, "Redo", action);
  }

  /**
   * Creates a standardized Copy to Clipboard button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with copy icon and tooltip
   */
  public static ButtonConfig copy(Runnable action) {
    return new ButtonConfig(ButtonIcon.COPY, "Copy to Clipboard", action);
  }

  // ===============================================================================
  // Data Operations
  // ===============================================================================

  /**
   * Creates a standardized Reload button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with reload icon and tooltip
   */
  public static ButtonConfig reload(Runnable action) {
    return new ButtonConfig(ButtonIcon.RELOAD, "Reload", action);
  }

  /**
   * Creates a standardized Delete button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with delete icon and tooltip
   */
  public static ButtonConfig delete(Runnable action) {
    return new ButtonConfig(ButtonIcon.DELETE, "Delete", action);
  }

  // ===============================================================================
  // Execution Operations
  // ===============================================================================

  /**
   * Creates a standardized Play/Run button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with play icon and tooltip
   */
  public static ButtonConfig play(Runnable action) {
    return new ButtonConfig(ButtonIcon.PLAY, "Run", action);
  }

  // ===============================================================================
  // Navigation & Information
  // ===============================================================================

  /**
   * Creates a standardized Documentation button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with documentation icon and tooltip
   */
  public static ButtonConfig documentation(Runnable action) {
    return new ButtonConfig(ButtonIcon.DOCUMENTATION, "Documentation", action);
  }

  /**
   * Creates a standardized Logs button configuration.
   *
   * @param action The action to execute when clicked, or null if wired later
   * @return A configured ButtonConfig with logs icon and tooltip
   */
  public static ButtonConfig logs(Runnable action) {
    return new ButtonConfig(ButtonIcon.LOGS, "Show logs", action);
  }
}
