package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;

public class QueryView extends AbstractView {

    public QueryView() {
        super(new BorderPane(), "/styles/split-editor-view.css");
    }

    public void setMainContent(Node node) {
        ((BorderPane) getRoot()).setCenter(node);
    }

    /**
     * Creates the empty state view for the query screen.
     *
     * @param onNewAction Action to perform when "New Query" is clicked.
     * @param onLoadAction Action to perform when "Load Query" is clicked.
     * @param onTemplateAction Action to perform when "Templates" is clicked.
     * @return The configured EmptyStateView node.
     */
    public Node createEmptyState(Runnable onNewAction, Runnable onLoadAction, Runnable onTemplateAction) {
        Button newButton = new Button("New Query");
        newButton.setTooltip(new Tooltip("CTRL + N"));
        newButton.setOnAction(e -> onNewAction.run());
        newButton.getStyleClass().add("custom-button");

        Button loadButton = new Button("Load Query");
        loadButton.setTooltip(new Tooltip("CTRL + O"));
        loadButton.setOnAction(e -> onLoadAction.run());
        loadButton.getStyleClass().add("custom-button");

        Button templateButton = new Button("Templates");
        templateButton.setTooltip(new Tooltip("CTRL + T"));
        templateButton.setOnAction(e -> onTemplateAction.run());
        templateButton.getStyleClass().add("custom-button");

        return new EmptyStateView(
            MaterialDesignM.MAGNIFY,
            "No queries open",
            "Create a new query, load one, or use a template.",
            newButton,
            loadButton,
            templateButton);
    }
}
