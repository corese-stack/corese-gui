package fr.inria.corese.gui.component.emptystate;

import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A generic reusable widget for displaying an empty or initial state in the UI.
 *
 * <p>This widget typically displays a large centered icon, a descriptive message, and optional
 * action buttons (e.g., "Create New", "Load File").
 */
public class EmptyStateWidget extends VBox {

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  private static final String STYLESHEET = "/styles/empty-state-widget.css";
  private static final String STYLE_CLASS_VIEW = "empty-state-widget";
  private static final String STYLE_CLASS_ICON = "empty-state-icon";
  private static final String STYLE_CLASS_TITLE = "empty-state-title";
  private static final String STYLE_CLASS_DESC = "empty-state-description";
  private static final String STYLE_CLASS_BUTTONS = "empty-state-buttons";
  private static final String STYLE_CLASS_ACTION = "empty-state-action";

  private final FontIcon iconView;
  private final Label titleLabel;
  private final Label descriptionLabel;
  private final HBox buttonBox;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new EmptyStateWidget.
   *
   * @param icon The icon to display (from Ikonli).
   * @param title The main title text (large, bold).
   * @param description The secondary description text (smaller).
   * @param buttons Optional action buttons to display below the message.
   */
  public EmptyStateWidget(Ikon icon, String title, String description, Node... buttons) {
    this.iconView = new FontIcon(icon);
    this.titleLabel = new Label(title);
    this.descriptionLabel = new Label(description);
    this.buttonBox = new HBox(buttons);

    // Enforce consistent styling for all buttons
    for (Node node : buttons) {
      if (node instanceof Button button) {
        applyButtonStyle(button);
      }
    }

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
   * @param action The action to run on click.
   * @return A configured Button.
   */
  public static Button createAction(String text, Ikon icon, Runnable action) {
    Button btn = new Button(text);
    if (icon != null) {
      btn.setGraphic(new FontIcon(icon));
    }
    btn.setOnAction(e -> action.run());

    applyButtonStyle(btn);

    return btn;
  }

  /**
   * Applies the standard empty state button styles to a button.
   *
   * @param button The button to style.
   */
  private static void applyButtonStyle(Button button) {
    if (!button.getStyleClass().contains(Styles.ACCENT)) {
      button.getStyleClass().add(Styles.ACCENT);
    }
    if (!button.getStyleClass().contains(STYLE_CLASS_ACTION)) {
      button.getStyleClass().add(STYLE_CLASS_ACTION);
    }
    button.setFocusTraversable(false);
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private void initialize() {
    this.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());

    this.getStyleClass().add(STYLE_CLASS_VIEW);
    iconView.getStyleClass().add(STYLE_CLASS_ICON);
    titleLabel.getStyleClass().add(STYLE_CLASS_TITLE);
    descriptionLabel.getStyleClass().add(STYLE_CLASS_DESC);
    descriptionLabel.setWrapText(true);
    buttonBox.getStyleClass().add(STYLE_CLASS_BUTTONS);

    getChildren().addAll(iconView, titleLabel, descriptionLabel, buttonBox);
  }

  // ==============================================================================================
  // Accessors
  // ==============================================================================================

  public void setIcon(Ikon icon) {
    this.iconView.setIconCode(icon);
  }

  public void setTitle(String title) {
    this.titleLabel.setText(title);
  }

  public void setDescription(String description) {
    this.descriptionLabel.setText(description);
  }
}
