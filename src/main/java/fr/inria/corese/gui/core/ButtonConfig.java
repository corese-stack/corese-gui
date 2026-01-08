package fr.inria.corese.gui.core;

import fr.inria.corese.gui.enums.icon.IconButtonType;

/**
 * Unified configuration class for buttons and actions.
 *
 * <p>This class provides a consistent way to configure buttons throughout the application, whether
 * they are toolbar buttons, floating action buttons, or any other UI action. It encapsulates all
 * the information needed to display and describe a button: icon, text, tooltip, and keyboard
 * shortcut.
 *
 * <p><b>Usage examples:</b>
 *
 * <pre>{@code
 * // Simple: just icon
 * new ButtonConfig(IconButtonType.SAVE)
 *
 * // With tooltip
 * new ButtonConfig(IconButtonType.SAVE, "Save File")
 *
 * // With tooltip and shortcut
 * new ButtonConfig(IconButtonType.SAVE, "Save File", "Ctrl+S")
 *
 * // For floating action buttons
 * new ButtonConfig(IconButtonType.PLAY, "Run Query", "Ctrl+Enter")
 * }</pre>
 *
 * <p>This class is immutable and thread-safe.
 */
public class ButtonConfig {

  // ===============================================================================
  // Fields
  // ===============================================================================

  private final IconButtonType icon;
  private final String text;
  private final String tooltip;
  private final String shortcut;

  // ===============================================================================
  // Constructors
  // ===============================================================================

  /**
   * Creates a ButtonConfig with icon only.
   *
   * @param icon The icon to display
   */
  public ButtonConfig(IconButtonType icon) {
    this(icon, null, null, null);
  }

  /**
   * Creates a ButtonConfig with icon and tooltip.
   *
   * @param icon The icon to display
   * @param tooltip The tooltip text shown on hover
   */
  public ButtonConfig(IconButtonType icon, String tooltip) {
    this(icon, tooltip, null, null);
  }

  /**
   * Creates a ButtonConfig with icon, tooltip and shortcut.
   *
   * @param icon The icon to display
   * @param tooltip The tooltip text shown on hover
   * @param shortcut The keyboard shortcut description (e.g., "Ctrl+S")
   */
  public ButtonConfig(IconButtonType icon, String tooltip, String shortcut) {
    this(icon, tooltip, shortcut, null);
  }

  /**
   * Creates a ButtonConfig with all parameters.
   *
   * @param icon The IconButtonType to display (can be null)
   * @param tooltip The tooltip text shown on hover (can be null)
   * @param shortcut The keyboard shortcut description (can be null)
   * @param text The button text/label (can be null for icon-only buttons)
   */
  private ButtonConfig(IconButtonType icon, String tooltip, String shortcut, String text) {
    this.icon = icon;
    this.text = text;
    this.tooltip = tooltip;
    this.shortcut = shortcut;
  }

  // ===============================================================================
  // Getters
  // ===============================================================================

  /**
   * Returns the icon for this button configuration.
   *
   * @return The IconButtonType, or null if no icon is configured
   */
  public IconButtonType getIcon() {
    return icon;
  }

  /**
   * Returns the text/label for this button configuration.
   *
   * @return The text, or null if this is an icon-only button
   */
  public String getText() {
    return text;
  }

  /**
   * Returns the tooltip for this button configuration.
   *
   * @return The tooltip text, or null if no tooltip is configured
   */
  public String getTooltip() {
    return tooltip;
  }

  /**
   * Returns the keyboard shortcut description for this button configuration.
   *
   * @return The shortcut description (e.g., "Ctrl+S"), or null if no shortcut is configured
   */
  public String getShortcut() {
    return shortcut;
  }
}
