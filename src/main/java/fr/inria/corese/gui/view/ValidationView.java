package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

/**
 * View for the Validation screen.
 *
 * <p>Displays a tabbed interface where each tab contains a code editor for SHACL shapes and a
 * results pane for validation reports.
 */
public class ValidationView extends AbstractView {

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  private final StackPane rootStack;
  private final BorderPane mainContent;
  private Node errorOverlay;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Creates the ValidationView. */
  public ValidationView() {
    super(new StackPane(), "/styles/split-editor-view.css");
    this.rootStack = (StackPane) getRoot();
    this.mainContent = new BorderPane();
    this.rootStack.getChildren().add(mainContent);

    // Load additional styles for validation view (e.g., error overlay)
    this.rootStack
        .getStylesheets()
        .add(getClass().getResource("/styles/validation-view.css").toExternalForm());
  }

  // ==============================================================================================
  // Public Methods
  // ==============================================================================================

  /**
   * Sets the main content of the view (typically the TabEditorView).
   *
   * @param node The content node.
   */
  public void setMainContent(Node node) {
    mainContent.setCenter(node);
  }

  /**
   * Displays an error message in a modal overlay.
   *
   * @param title The title of the error.
   * @param header The actionable summary or instruction for the user.
   * @param details The detailed error message or stack trace (optional).
   */
  public void showError(String title, String header, String details) {
    hideError(); // Clear existing error if any

    // Create the overlay container
    VBox overlay = new VBox();
    overlay.getStyleClass().add("error-overlay");

    // Icon
    FontIcon errorIcon = new FontIcon(MaterialDesignA.ALERT);
    errorIcon.getStyleClass().add("error-icon");

    // Title
    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("error-title");

    // Header (Actionable message)
    Label headerLabel = new Label(header);
    headerLabel.setWrapText(true);
    headerLabel.getStyleClass().add("error-header");

    overlay.getChildren().addAll(errorIcon, titleLabel, headerLabel);

    // Details (Optional code block)
    if (details != null && !details.isEmpty()) {
      TextArea detailsArea = new TextArea(details);
      detailsArea.setEditable(false);
      detailsArea.setWrapText(false); // Code block style
      detailsArea.getStyleClass().add("error-details-area");
      detailsArea.setPrefRowCount(8);
      VBox.setVgrow(detailsArea, Priority.ALWAYS);
      overlay.getChildren().add(detailsArea);
    }

    // Close Button
    Button closeButton = new Button("Close");
    closeButton.setGraphic(new FontIcon(MaterialDesignC.CLOSE));
    closeButton.setOnAction(e -> hideError());
    closeButton.getStyleClass().add("error-close-button");

    overlay.getChildren().add(closeButton);

    // Create a background dimmer
    StackPane dimmer = new StackPane(overlay);
    dimmer.getStyleClass().add("error-dimmer");

    // Close on background click
    dimmer.setOnMouseClicked(
        e -> {
          if (e.getTarget() == dimmer) hideError();
        });

    this.errorOverlay = dimmer;
    rootStack.getChildren().add(errorOverlay);
  }

  /** Hides the error overlay. */
  public void hideError() {
    if (errorOverlay != null) {
      rootStack.getChildren().remove(errorOverlay);
      errorOverlay = null;
    }
  }

  /**
   * Creates the empty state view for the validation screen.
   *
   * @param onNewAction Action to perform when "New Shapes File" is clicked.
   * @param onLoadAction Action to perform when "Load Shapes File" is clicked.
   * @return The configured EmptyStateView node.
   */
  public Node createEmptyState(Runnable onNewAction, Runnable onLoadAction) {
    return new EmptyStateView(
        MaterialDesignS.SHIELD_CHECK_OUTLINE,
        "No shapes files open.\nCreate a new shapes file or load an existing one.",
        EmptyStateView.createAction("New Shapes File", "CTRL + N", onNewAction),
        EmptyStateView.createAction("Load Shapes File", "CTRL + O", onLoadAction));
  }
}
