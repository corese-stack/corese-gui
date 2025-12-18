package fr.inria.corese.gui.view;

import atlantafx.base.controls.ModalPane;
import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.component.ErrorDialog;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
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

  private static final String STYLESHEET_PATH = "/styles/validation-view.css";

  private final StackPane rootStack;
  private final BorderPane mainContent;
  private final ModalPane modalPane;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Creates the ValidationView. */
  public ValidationView() {
    super(new StackPane(), STYLESHEET_PATH);
    this.rootStack = (StackPane) getRoot();
    this.mainContent = new BorderPane();
    this.modalPane = new ModalPane();

    this.rootStack.getChildren().addAll(mainContent, modalPane);
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
    modalPane.show(new ErrorDialog(modalPane, title, header, details));
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
        "No shapes files open",
        "Create a new shapes file or load an existing one",
        EmptyStateView.createAction(
            "New Shapes File", MaterialDesignP.PLUS, "CTRL + N", onNewAction),
        EmptyStateView.createAction(
            "Load Shapes File", MaterialDesignF.FOLDER_OPEN, "CTRL + O", onLoadAction));
  }
}
