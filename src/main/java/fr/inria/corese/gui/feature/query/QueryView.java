package fr.inria.corese.gui.feature.query;

import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.core.view.AbstractView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

/**
 * View for the Query feature.
 *
 * <p>Provides a container for the tabbed query editor and handles the empty state display.
 */
public class QueryView extends AbstractView {

    private static final String STYLESHEET_PATH = "/css/split-editor-view.css";

    public QueryView() {
        super(new BorderPane(), STYLESHEET_PATH);
    }

    /**
     * Sets the main content (typically the TabEditor).
     *
     * @param node The content node.
     */
    public void setMainContent(Node node) {
        ((BorderPane) getRoot()).setCenter(node);
    }

    /**
     * Creates the empty state widget shown when no queries are open.
     *
     * @param onNewAction      Callback for "New Query".
     * @param onLoadAction     Callback for "Load Query".
     * @param onTemplateAction Callback for "Templates" (can be null if disabled).
     * @return The configured EmptyStateWidget.
     */
    public Node createEmptyState(Runnable onNewAction, Runnable onLoadAction, Runnable onTemplateAction) {
        java.util.List<Node> actions = new java.util.ArrayList<>();
        
        actions.add(EmptyStateWidget.createAction("New Query", MaterialDesignP.PLUS, onNewAction));
        actions.add(EmptyStateWidget.createAction("Load Query", MaterialDesignF.FOLDER_OPEN, onLoadAction));

        if (onTemplateAction != null) {
            actions.add(EmptyStateWidget.createAction(
                "Templates", MaterialDesignF.FILE_DOCUMENT_MULTIPLE, onTemplateAction));
        }

        return new EmptyStateWidget(
            MaterialDesignM.MAGNIFY,
            "No queries open",
            "Create a new query, load one, or use a template.",
            actions.toArray(Node[]::new)
        );
    }
}
