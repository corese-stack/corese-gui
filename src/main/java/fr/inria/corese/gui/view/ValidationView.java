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

  private final StackPane rootStack;
  private final BorderPane mainContent;
  private Node errorOverlay;

  // ===== Constructor =====

  /** Creates the ValidationView. */
  public ValidationView() {
    super(new StackPane(), "/styles/split-editor-view.css");
    this.rootStack = (StackPane) getRoot();
    this.mainContent = new BorderPane();
    this.rootStack.getChildren().add(mainContent);
  }

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

    VBox overlay = new VBox(15);
    overlay.getStyleClass().add("error-overlay");
    overlay.setStyle(
        "-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 8; " +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); " +
        "-fx-max-width: 600; -fx-max-height: 500; -fx-alignment: center;");

    // Icon
    Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");

    Label headerLabel = new Label(header);
    headerLabel.setWrapText(true);
    headerLabel.setStyle("-fx-font-size: 14px; -fx-text-alignment: center;");

    overlay.getChildren().addAll(titleLabel, headerLabel);

    if (details != null && !details.isEmpty()) {
        TextArea detailsArea = new TextArea(details);
        detailsArea.setEditable(false);
        detailsArea.setWrapText(false); // Code block style
        detailsArea.setStyle("-fx-font-family: 'Monospaced'; -fx-control-inner-background: #f5f5f5; -fx-text-fill: #333;");
        detailsArea.setPrefRowCount(8);
        VBox.setVgrow(detailsArea, Priority.ALWAYS);
        overlay.getChildren().add(detailsArea);
    }

    Button closeButton = new Button("Close");
    closeButton.setGraphic(new FontIcon(MaterialDesignC.CLOSE));
    closeButton.setOnAction(e -> hideError());
    closeButton.setStyle(
        "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold;");

    overlay.getChildren().add(closeButton);

    // Create a background dimmer
    StackPane dimmer = new StackPane(overlay);
    dimmer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");
    dimmer.setAlignment(Pos.CENTER);
    
    // Close on background click
    dimmer.setOnMouseClicked(e -> {
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
    Button newButton = new Button("New Shapes File");
    newButton.setTooltip(new Tooltip("CTRL + N"));
    newButton.setOnAction(e -> onNewAction.run());
    newButton.getStyleClass().add("custom-button");

    Button loadButton = new Button("Load Shapes File");
    loadButton.setTooltip(new Tooltip("CTRL + O"));
    loadButton.setOnAction(e -> onLoadAction.run());
    loadButton.getStyleClass().add("custom-button");

    return new EmptyStateView(
        MaterialDesignS.SHIELD_CHECK_OUTLINE,
        "No shapes files open.\nCreate a new shapes file or load an existing one.",
        newButton,
        loadButton);
  }
}
