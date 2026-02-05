package fr.inria.corese.gui.feature.validation;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.core.view.AbstractView;
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
    // Constants
    // ==============================================================================================

    @SuppressWarnings("java:S1075") // Hardcoded URI - not relevant for internal CSS resources
    private static final String STYLESHEET_PATH = "/css/features/validation-view.css";

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    /** Creates the ValidationView. */
    public ValidationView() {
        super(new BorderPane(), STYLESHEET_PATH);
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Sets the main content (typically the TabEditor).
     *
     * @param node The content node.
     */
    public void setMainContent(Node node) {
        ((BorderPane) getRoot()).setCenter(node);
    }

    /**
     * Returns the list of buttons to be displayed in the result view toolbar.
     *
     * @return A list of ButtonConfig.
     */
    public List<ButtonConfig> getResultToolbarButtons() {
        return List.of(
            ButtonFactory.copy(null),
            ButtonFactory.export(null)
        );
    }

    public String getRunValidationLabel() {
        return "Run Validation";
    }

    /**
     * Creates the empty state view for the validation screen.
     *
     * @param onNewAction  Action for "New Shapes File".
     * @param onLoadAction Action for "Load Shapes File".
     * @return The configured EmptyStateWidget.
     */
    public Node createEmptyState(Runnable onNewAction, Runnable onLoadAction) {
        return new EmptyStateWidget(
            MaterialDesignS.SHIELD_CHECK_OUTLINE,
            "No shapes files open",
            "Create a new shapes file or load an existing one",
            EmptyStateWidget.createAction("New Shapes File", MaterialDesignP.PLUS, onNewAction),
            EmptyStateWidget.createAction("Load Shapes File", MaterialDesignF.FOLDER_OPEN, onLoadAction)
        );
    }
}
