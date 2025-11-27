package fr.inria.corese.gui.view;

import java.util.function.Consumer;

import fr.inria.corese.gui.enums.button.ButtonType;
import javafx.scene.control.Button;

/**
 * Customizable button with enhanced functionality.
 *
 * <p>This class provides an advanced button implementation with: - Customizable button type -
 * Configurable style - Customizable click action
 *
 * <p>Uses the Builder pattern for flexible configuration.
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class CustomButton extends Button {
  private final ButtonType type;
  private Consumer<CustomButton> onClick;

  /**
   * Internal constructor for the custom button.
   *
   * @param type The button type defining its behavior and style
   */
  public CustomButton(ButtonType type) {
    super(type.getLabel());
    this.type = type;
    setupStyle();
  }

  /**
   * Builder class for creating customized buttons.
   *
   * <p>Allows detailed and flexible button configuration.
   */
  public static class Builder {
    private final ButtonType type;
    private String customStyle = null;
    private Consumer<CustomButton> onClick = null;
    private boolean disabled = false;
    private String tooltip = null;

    /**
     * Constructor for the Builder.
     *
     * @param type The type of button to construct
     */
    public Builder(ButtonType type) {
      this.type = type;
    }

    /**
     * Sets a custom style for the button.
     *
     * @param style The style class to apply
     * @return The Builder for method chaining
     */
    public Builder withStyle(String style) {
      this.customStyle = style;
      return this;
    }

    /**
     * Sets the click action for the button.
     *
     * @param onClick The function to execute on click
     * @return The Builder for method chaining
     */
    public Builder withOnClick(Consumer<CustomButton> onClick) {
      this.onClick = onClick;
      return this;
    }

    /**
     * Configures the enabled/disabled state of the button.
     *
     * @param disabled Indicates whether the button should be disabled
     * @return The Builder for method chaining
     */
    public Builder withDisabled(boolean disabled) {
      this.disabled = disabled;
      return this;
    }

    /**
     * Sets the tooltip for the button.
     *
     * @param tooltip The tooltip text
     * @return The Builder for method chaining
     */
    public Builder withTooltip(String tooltip) {
      this.tooltip = tooltip;
      return this;
    }

    /**
     * Builds the custom button with the defined configurations.
     *
     * @return The configured custom button
     */
    public CustomButton build() {
      CustomButton button = new CustomButton(type);
      if (customStyle != null) button.getStyleClass().add(customStyle);
      if (onClick != null) button.setOnClick(onClick);
      button.setDisable(disabled);
      if (tooltip != null) button.setTooltip(tooltip);
      return button;
    }
  }

  /** Configures the base style of the button. */
  private void setupStyle() {
    getStyleClass().addAll("custom-button", type.getStyleClass());
  }

  /**
   * Sets the click action for the button.
   *
   * @param onClick The function to execute on click
   */
  public void setOnClick(Consumer<CustomButton> onClick) {
    this.onClick = onClick;
    setOnAction(e -> onClick.accept(this));
  }

  /**
   * Retrieves the button type.
   *
   * @return The button type
   */
  public ButtonType getType() {
    return type;
  }

  /**
   * Sets the tooltip for the button.
   *
   * @param text The tooltip text
   */
  public void setTooltip(String text) {
    setTooltip(new javafx.scene.control.Tooltip(text));
  }
}
