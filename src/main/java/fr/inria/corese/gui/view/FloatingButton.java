package fr.inria.corese.gui.view;

import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.ButtonConfig;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A Floating Action Button (FAB) component.
 *
 * <p>This button is circular, elevated, and typically used for the primary action on a screen. It
 * supports a loading state where the icon is replaced by a {@link RingProgressIndicator}. Uses
 * {@link ButtonConfig} for unified configuration across the application.
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * FloatingButton button = new FloatingButton(
 *     new ButtonConfig(IconButtonType.PLAY, "Run Query")
 * );
 * }</pre>
 */
public class FloatingButton extends Button {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  private static final String STYLESHEET = "/styles/floating-button.css";
  private static final String STYLE_CLASS = "fl        oating-button";
  private static final int ICON_SIZE = 24;
  private static final int PROGRESS_SIZE = 24;

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
   * Creates a new FloatingButton with unified button configuration.
   *
   * @param config The button configuration (icon, tooltip)
   */
  public FloatingButton(ButtonConfig config) {
    this.fontIcon = new FontIcon(config.getIcon().getIkon());
    this.progressIndicator = new RingProgressIndicator();

    initialize(config.getTooltip());
    setupListeners();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private void initialize(String tooltipText) {
    // Load styles
    getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
    getStyleClass().addAll(STYLE_CLASS, Styles.BUTTON_CIRCLE);

    // Configure Icon
    fontIcon.setIconSize(ICON_SIZE);

    // Configure Progress Indicator
    progressIndicator.setMinSize(PROGRESS_SIZE, PROGRESS_SIZE);
    progressIndicator.setMaxSize(PROGRESS_SIZE, PROGRESS_SIZE);

    // Set initial graphic
    setGraphic(fontIcon);

    // Configure Tooltip
    if (tooltipText != null && !tooltipText.isBlank()) {
      setTooltip(new Tooltip(tooltipText));
    }
  }

  private void setupListeners() {
    loading.addListener((obs, oldVal, newVal) -> updateGraphic(newVal));
  }

  // ==============================================================================================
  // Helper Methods
  // ==============================================================================================

  private void updateGraphic(boolean isLoading) {
    if (isLoading) {
      // Ensure progress indicator is indeterminate or set progress if needed
      // For a simple loading state, indeterminate is usually what we want,
      // but RingProgressIndicator might default to 0.
      // If it's indeterminate by default or we want -1:
      progressIndicator.setProgress(-1);
      setGraphic(progressIndicator);
      setDisable(true); // Usually good practice to disable FAB while loading
    } else {
      setGraphic(fontIcon);
      setDisable(false);
    }
  }

  // ==============================================================================================
  // Accessors
  // ==============================================================================================

  /**
   * Sets the loading state of the button.
   *
   * @param loading true to show the progress indicator, false to show the icon.
   */
  public void setLoading(boolean loading) {
    this.loading.set(loading);
  }

  /**
   * @return true if the button is in loading state.
   */
  public boolean isLoading() {
    return loading.get();
  }

  /**
   * @return The loading property.
   */
  public BooleanProperty loadingProperty() {
    return loading;
  }
}
