package fr.inria.corese.gui.view;

import atlantafx.base.controls.ModalPane;
import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.component.ErrorDialog;
import java.util.List;
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
   * Returns the list of buttons to be displayed in the editor toolbar.
   *
   * @return A list of ButtonConfig.
   */
  public List<ButtonConfig> getEditorToolbarButtons() {
    return List.of(
        new ButtonConfig(IconButtonType.SAVE),
        new ButtonConfig(IconButtonType.EXPORT),
        new ButtonConfig(IconButtonType.CLEAR),
        new ButtonConfig(IconButtonType.UNDO),
        new ButtonConfig(IconButtonType.REDO));
  }

  /**
   * Returns the list of buttons to be displayed in the result view toolbar.
   *
   * @return A list of ButtonConfig.
   */
  public List<ButtonConfig> getResultToolbarButtons() {
    return List.of(
        new ButtonConfig(IconButtonType.COPY),
        new ButtonConfig(IconButtonType.EXPORT));
  }

  /**
   * Sets the main content of the view (typically the TabEditorView).
   * This allows the controller to inject the editor component dynamically.
   *
   * @param node The content node to be displayed in the center of the layout.
   */
  public void setMainContent(Node node) {
    mainContent.setCenter(node);
  }

  /**
   * Displays an error message in a modal overlay.
   * Uses the ErrorDialog component to show a user-friendly error message.
   *
   * @param title The title of the error dialog.
   * @param header The actionable summary or instruction for the user.
   * @param details The detailed error message or stack trace (optional).
   */
  public void showError(String title, String header, String details) {
    modalPane.show(new ErrorDialog(modalPane, title, header, details).getRoot());
  }

  public void showNoDataLoadedError() {
    showError(
        "No Data Loaded",
        "Validation requires an RDF graph to be loaded.\n"
            + "Please go to the 'Data' view and load an RDF file.",
        null);
  }

  public void showEmptyShapesError() {
    showError(
        "Empty Shapes",
        "The shapes file is empty.\n"
            + "Please write or load SHACL shapes in the editor before validating.",
        null);
  }

  public void showValidationExecutionError(String details) {
    showError(
        "Validation Error",
        "An unexpected error occurred during validation.\n"
            + "Please check the logs for more details.",
        details);
  }

  public void showInvalidSyntaxError(String details) {
    showError(
        "Invalid SHACL Syntax",
        "The SHACL shapes contain syntax errors.\nPlease correct the errors listed below:",
        details);
  }

  public String getRunValidationLabel() {
    return "Run Validation";
  }

  /**
   * Creates the empty state view for the validation screen.
   * This view is shown when no tabs are open.
   *
   * @param onNewAction Action to perform when "New Shapes File" is clicked.
   * @param onLoadAction Action to perform when "Load Shapes File" is clicked.
   * @return The configured EmptyStateView node ready to be displayed.
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
