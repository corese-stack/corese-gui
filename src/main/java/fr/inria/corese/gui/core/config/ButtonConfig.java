package fr.inria.corese.gui.core.config;

import fr.inria.corese.gui.component.toolbar.ActionButtonWidget;
import fr.inria.corese.gui.core.enums.ButtonIcon;







/**
 * Configuration class for button components.
 *
 * <p>This immutable class provides a unified way to configure buttons throughout the application,
 * whether they are toolbar buttons, floating action buttons, or any other UI button. It
 * encapsulates all the information needed to display and configure a button: icon, tooltip, and
 * action.
 *
 * <p><b>Design principles:</b>
 *
 * <ul>
 *   <li><b>Immutability:</b> Thread-safe and can be safely shared
 *   <li><b>Separation of concerns:</b> Configuration separate from presentation
 *   <li><b>Flexibility:</b> Supports buttons with or without tooltips/actions
 * </ul>
 *
 * <p><b>Usage examples:</b>
 *
 * <pre>{@code
 * // Simple: just icon
 * new ButtonConfig(ButtonIcon.SAVE)
 *
 * // With tooltip
 * new ButtonConfig(ButtonIcon.SAVE, "Save File")
 *
 * // Complete configuration with action
 * new ButtonConfig(ButtonIcon.SAVE, "Save File", () -> saveFile())
 * }</pre>
 *
 * <p><b>Best practice:</b> Use {@link ButtonFactory} factory methods for standard buttons to ensure
 * consistent tooltips across the application.
 *
 * @see ButtonIcon
 * @see ButtonFactory
 * @see fr.inria.corese.gui.component.toolbar.ActionButtonWidget
 */
public class ButtonConfig {

  // ===============================================================================
  // Fields
  // ===============================================================================

  /** The button icon. */
  private final ButtonIcon icon;

  /** The tooltip text shown on hover. */
  private final String tooltip;

  /** The action to execute when the button is clicked. */
  private final Runnable action;

  // ===============================================================================
  // Constructors
  // ===============================================================================

  /**
   * Creates a button configuration with icon only.
   *
   * @param icon The icon to display
   */
  public ButtonConfig(ButtonIcon icon) {
    this(icon, null, null);
  }

  /**
   * Creates a button configuration with icon and tooltip.
   *
   * @param icon The icon to display
   * @param tooltip The tooltip text shown on hover
   */
  public ButtonConfig(ButtonIcon icon, String tooltip) {
    this(icon, tooltip, null);
  }

  /**
   * Creates a complete button configuration with icon, tooltip, and action.
   *
   * <p>This is the preferred constructor for creating self-contained buttons that handle their own
   * click events.
   *
   * @param icon The icon to display
   * @param tooltip The tooltip text shown on hover
   * @param action The action to execute when clicked
   */
  public ButtonConfig(ButtonIcon icon, String tooltip, Runnable action) {
    this.icon = icon;
    this.tooltip = tooltip;
    this.action = action;
  }

  // ===============================================================================
  // Getters
  // ===============================================================================

  /**
   * Returns the button icon.
   *
   * @return The button icon, or null if no icon is configured
   */
  public ButtonIcon getIcon() {
    return icon;
  }

  /**
   * Returns the tooltip text.
   *
   * @return The tooltip text, or null if no tooltip is configured
   */
  public String getTooltip() {
    return tooltip;
  }

  /**
   * Returns the button action.
   *
   * @return The executable action, or null if the action is handled externally
   */
  public Runnable getAction() {
    return action;
  }
}
