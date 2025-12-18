package fr.inria.corese.gui.view;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A generic reusable component for displaying an empty or initial state in the UI.
 *
 * <p>This view typically displays a large centered icon, a descriptive message, and optional action
 * buttons (e.g., "Create New", "Load File").
 *
 * <p>It is designed to be used across different modules (Validation, Query, Data) to provide a
 * consistent user experience when no content is loaded.
 */
public class EmptyStateView extends VBox {

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  private final FontIcon iconView;
  private final Label titleLabel;
  private final Label descriptionLabel;
  private final HBox buttonBox;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new EmptyStateView.
   *
   * @param icon The icon to display (from Ikonli).
   * @param title The main title text (large, bold).
   * @param description The secondary description text (smaller).
   * @param buttons Optional action buttons to display below the message.
   */
  public EmptyStateView(Ikon icon, String title, String description, Node... buttons) {
    this.iconView = new FontIcon(icon);
    this.titleLabel = new Label(title);
    this.descriptionLabel = new Label(description);
    this.buttonBox = new HBox(buttons);

    initialize();
  }

  // ==============================================================================================
  // Helper Methods
  // ==============================================================================================

  /**
   * Helper to create a standard button for the empty state view.
   *
   * @param text The button text.
   * @param icon The button icon (can be null).
   * @param tooltip The button tooltip (can be null).
   * @param action The action to run on click.
   * @return A configured Button.
   */
  public static Button createAction(String text, Ikon icon, String tooltip, Runnable action) {
    Button btn = new Button(text);
    if (icon != null) {
      btn.setGraphic(new FontIcon(icon));
    }
    if (tooltip != null) {
      btn.setTooltip(new Tooltip(tooltip));
    }
    btn.setOnAction(e -> action.run());
    
    // Use AtlantaFX outlined style and a custom class for color overrides
    btn.getStyleClass().addAll(Styles.ACCENT, "empty-state-action");
    
    return btn;
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private void initialize() {
    // Load the CSS for this component
    this.getStylesheets()
        .add(getClass().getResource("/styles/empty-state-view.css").toExternalForm());

    // Main container setup
    this.getStyleClass().add("empty-state-view");
    this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

    // Icon setup
    iconView.getStyleClass().add("empty-state-icon");

    // Title setup
    titleLabel.getStyleClass().add("empty-state-title");

    // Description setup
    descriptionLabel.getStyleClass().add("empty-state-description");
    descriptionLabel.setWrapText(true);

    // Buttons container setup
    buttonBox.getStyleClass().add("empty-state-buttons");

    // Add children
    getChildren().addAll(iconView, titleLabel, descriptionLabel, buttonBox);
  }

  // ==============================================================================================
  // Accessors
  // ==============================================================================================

  /**
   * Updates the main icon of the empty state.
   *
   * @param icon The new icon to display.
   */
  public void setIcon(Ikon icon) {
    this.iconView.setIconCode(icon);
  }

  /**
   * Updates the title text.
   *
   * @param title The new title text.
   */
  public void setTitle(String title) {
    this.titleLabel.setText(title);
  }

  /**
   * Updates the description text.
   *
   * @param description The new description text.
   */
  public void setDescription(String description) {
    this.descriptionLabel.setText(description);
  }
}
