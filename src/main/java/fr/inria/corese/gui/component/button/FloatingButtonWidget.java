package fr.inria.corese.gui.component.button;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.config.ButtonConfig;

import java.net.URL;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A Floating Action Button (FAB) component.
 *
 * <p>
 * This component provides a circular button with:
 *
 * <ul>
 * <li>Icon display with configurable tooltip
 * <li>Loading state with animated progress indicator
 * <li>AtlantaFX theme integration
 * <li>Material Design elevation shadows
 * </ul>
 *
 * @see ButtonConfig
 */
public class FloatingButtonWidget extends Button {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  /** Path to the CSS stylesheet for floating button styles. */
      private static final String STYLESHEET = "/css/components/floating-button-widget.css";
  /** Primary CSS style class applied to this button. */
  private static final String STYLE_CLASS = "floating-button";

  /** Default icon size in pixels (Material Design standard). */
  private static final int ICON_SIZE = 24;

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  private final FontIcon fontIcon;
  private final RingProgressIndicator progressIndicator;
  private final BooleanProperty loading = new SimpleBooleanProperty(false);

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new FloatingButtonWidget with the specified configuration.
   *
   * @param config The button configuration containing icon, tooltip, and action
   * @throws NullPointerException if config is null
   */
  public FloatingButtonWidget(ButtonConfig config) {
    Objects.requireNonNull(config, "ButtonConfig cannot be null");

    // Initialize components
    this.fontIcon = createIcon(config);
    this.progressIndicator = createProgressIndicator();

    initializeStyle();
    initializeTooltip(config);
    initializeAction(config);
    setupListeners();

    // Set initial state
    updateGraphic(false);
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private FontIcon createIcon(ButtonConfig config) {
    FontIcon icon = new FontIcon();
    if (config.getIcon() != null) {
      icon = new FontIcon(config.getIcon().getIkon());
    }
    icon.setIconSize(ICON_SIZE);
    return icon;
  }

  private RingProgressIndicator createProgressIndicator() {
    RingProgressIndicator indicator = new RingProgressIndicator();
    indicator.setProgress(-1); // Indeterminate
    return indicator;
  }

  private void initializeStyle() {
    URL cssResource = getClass().getResource(STYLESHEET);
    if (cssResource != null) {
      getStylesheets().add(cssResource.toExternalForm());
    }
    getStyleClass().addAll(STYLE_CLASS, Styles.BUTTON_CIRCLE);
  }

  private void initializeTooltip(ButtonConfig config) {
    if (config.getTooltip() != null && !config.getTooltip().isBlank()) {
      setTooltip(new Tooltip(config.getTooltip()));
    }
  }

  private void initializeAction(ButtonConfig config) {
    if (config.getAction() != null) {
      setOnAction(e -> {
        if (!loading.get()) {
          config.getAction().run();
        }
      });
    }
  }

  private void setupListeners() {
    loading.addListener((obs, oldVal, newVal) -> updateGraphic(newVal));
  }

  // ==============================================================================================
  // Logic
  // ==============================================================================================

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
   * @return the loading property
   */
  public BooleanProperty loadingProperty() {
    return loading;
  }

}
