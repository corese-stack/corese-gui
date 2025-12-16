package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

/**
 * View for the Validation screen.
 *
 * <p>Displays a tabbed interface where each tab contains a code editor for SHACL shapes
 * and a results pane for validation reports.
 */
public class ValidationView extends AbstractView {

  // ===== Constructor =====

  /**
   * Creates the ValidationView.
   */
  public ValidationView() {
    super(new BorderPane(), "/styles/split-editor-view.css");
  }

  /**
   * Sets the main content of the view (typically the TabEditorView).
   *
   * @param node The content node.
   */
  public void setMainContent(Node node) {
    ((BorderPane) getRoot()).setCenter(node);
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
