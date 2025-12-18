package fr.inria.corese.gui.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
  private final Label messageLabel;
  private final HBox buttonBox;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new EmptyStateView.
   *
   * @param icon The icon to display (from Ikonli).
   * @param message The message to display below the icon.
   * @param buttons Optional action buttons to display below the message.
   */
  public EmptyStateView(Ikon icon, String message, Node... buttons) {
    this.iconView = new FontIcon(icon);
    this.messageLabel = new Label(message);
    this.buttonBox = new HBox(buttons);

    initialize();
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

    // Message setup
    messageLabel.getStyleClass().add("empty-state-message");

    // Buttons container setup
    buttonBox.getStyleClass().add("empty-state-buttons");

    // Add children
    getChildren().addAll(iconView, messageLabel, buttonBox);
  }
}
