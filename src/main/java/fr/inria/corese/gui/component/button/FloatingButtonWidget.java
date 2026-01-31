package fr.inria.corese.gui.component.button;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.config.ButtonConfig;
import java.net.URL;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A Material Design Floating Action Button (FAB) component.
 *
 * <p>This component provides a circular button with:
 *
 * <ul>
 *   <li>Icon display with configurable tooltip
 *   <li>Loading state with animated progress indicator
 *   <li>Automatic graphic switching between icon and spinner
 *   <li>AtlantaFX theme integration
 *   <li>Material Design elevation shadows
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * ButtonConfig config = new ButtonConfig(ButtonIcon.PLAY, "Execute Query");
 * FloatingButtonWidget fab = new FloatingButtonWidget(config);
 * fab.setOnAction(e -> executeQuery());
 * fab.setLoading(true); // Show spinner during execution
 * }</pre>
 *
 * <p><b>Important:</b> This button's {@code disableProperty} is typically bound by the controller.
 * The loading state only changes the visual appearance without modifying the disabled state.
 *
 * @see ButtonConfig
 * @see RingProgressIndicator
 */
public class FloatingButtonWidget extends Button {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  /** Path to the CSS stylesheet for floating button styles. */
  private static final String STYLESHEET = "/css/floating-button-widget.css";

  /** Primary CSS style class applied to this button. */
  private static final String STYLE_CLASS = "floating-button";

  /** Default icon size in pixels (Material Design standard). */
  private static final int ICON_SIZE = 24;

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  /** The icon displayed when the button is not in loading state. */
  private final FontIcon fontIcon;

  /** The animated progress indicator displayed during loading state. */
  private final RingProgressIndicator progressIndicator;

  /** Property indicating whether the button is in loading state. */
  private final BooleanProperty loading = new SimpleBooleanProperty(false);

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new FloatingButtonWidget with the specified configuration.
   *
   * <p>The button is initialized with:
   *
   * <ul>
   *   <li>Icon from the provided ButtonConfig
   *   <li>Tooltip if specified in the config
   *   <li>Progress indicator configured for indeterminate animation
   *   <li>All necessary event listeners
   * </ul>
   *
   * @param config The button configuration containing icon, tooltip, and other settings
   * @throws NullPointerException if config is null
   */
  public FloatingButtonWidget(ButtonConfig config) {
    Objects.requireNonNull(config, "ButtonConfig cannot be null");

    // Initialize icon
    if (config.getIcon() != null) {
      this.fontIcon = new FontIcon(config.getIcon().getIkon());
    } else {
      this.fontIcon = new FontIcon();
    }

    // Initialize progress indicator with indeterminate animation
    this.progressIndicator = new RingProgressIndicator();
    this.progressIndicator.setProgress(-1);

    initialize(config.getTooltip());
    setupListeners();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /**
   * Initializes the button's visual appearance and components.
   *
   * @param tooltipText The tooltip text to display on hover (optional)
   */
  private void initialize(String tooltipText) {
    // Load stylesheet
    URL cssResource = getClass().getResource(STYLESHEET);
    if (cssResource != null) {
      getStylesheets().add(cssResource.toExternalForm());
    }

    // Apply style classes
    getStyleClass().addAll(STYLE_CLASS, Styles.BUTTON_CIRCLE);

    // Configure icon size
    fontIcon.setIconSize(ICON_SIZE);

    // Set initial graphic (icon, not spinner)
    setGraphic(fontIcon);

    // Configure tooltip if provided
    if (tooltipText != null && !tooltipText.isBlank()) {
      setTooltip(new Tooltip(tooltipText));
    }
  }

  /** Sets up property listeners to handle loading state changes. */
  private void setupListeners() {
    loading.addListener((obs, oldVal, newVal) -> updateGraphic(newVal));
  }

  // ==============================================================================================
  // Helper Methods
  // ==============================================================================================

  /**
   * Updates the button's graphic based on the loading state.
   *
   * <p>When loading:
   *
   * <ul>
   *   <li>Switches from icon to progress indicator
   *   <li>Adds "loading" CSS class for visual feedback
   *   <li>Does NOT modify the disabled state (typically bound by controller)
   * </ul>
   *
   * @param isLoading true to show loading state, false to show normal state
   */
  private void updateGraphic(boolean isLoading) {
    if (isLoading) {
      setGraphic(progressIndicator);
      getStyleClass().add("loading");
    } else {
      setGraphic(fontIcon);
      getStyleClass().remove("loading");
    }
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Sets the loading state of the button.
   *
   * <p>When set to true, the button displays an animated progress indicator instead of the icon.
   * This provides visual feedback during long-running operations.
   *
   * @param loading true to show loading state, false to show normal state
   */
  public void setLoading(boolean loading) {
    this.loading.set(loading);
  }

  /**
   * Returns whether the button is currently in loading state.
   *
   * @return true if the button is showing the progress indicator, false otherwise
   */
  public boolean isLoading() {
    return loading.get();
  }

  /**
   * Returns the loading property for binding.
   *
   * <p>This property can be bound to other properties to automatically synchronize the loading
   * state with external conditions.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * Task<Void> task = createLongRunningTask();
   * floatingButton.loadingProperty().bind(task.runningProperty());
   * }</pre>
   *
   * @return the observable loading property
   */
  public BooleanProperty loadingProperty() {
    return loading;
  }
}
