package fr.inria.corese.gui.view;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.view.base.AbstractView;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
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

  private final BorderPane mainContent;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Creates the ValidationView. */
  public ValidationView() {
    super(new BorderPane(), STYLESHEET_PATH);
    this.mainContent = (BorderPane) getRoot();
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
