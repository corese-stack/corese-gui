package fr.inria.corese.gui.core;

import fr.inria.corese.gui.enums.icon.IconButtonType;

/**
 * Unified configuration class for buttons and actions.
 *
 * <p>This class provides a consistent way to configure buttons throughout the application, whether
 * they are toolbar buttons, floating action buttons, or any other UI action. It encapsulates all
 * the information needed to display and describe a button: icon, text, and tooltip.
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
 * // For floating action buttons
 * new ButtonConfig(IconButtonType.PLAY, "Run Query")
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

  // ===============================================================================
  // Constructors
  // ===============================================================================

  /**
   * Creates a ButtonConfig with icon only.
   *
   * @param icon The icon to display
   */
  public ButtonConfig(IconButtonType icon) {
    this.icon = icon;
    this.tooltip = null;
    this.text = null;
  }

  /**
   * Creates a ButtonConfig with icon and tooltip.
   *
   * @param icon The icon to display
   * @param tooltip The tooltip text shown on hover
   */
  public ButtonConfig(IconButtonType icon, String tooltip) {
    this.icon = icon;
    this.tooltip = tooltip;
    this.text = null;
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
}
