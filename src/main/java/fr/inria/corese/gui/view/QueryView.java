package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

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
   * @return The configured EmptyStateWidget node.
   */
  public Node createEmptyState(
      Runnable onNewAction, Runnable onLoadAction, Runnable onTemplateAction) {
        return new EmptyStateWidget(
            MaterialDesignM.MAGNIFY,
            "No queries open",
            "Create a new query, load one, or use a template.",
            EmptyStateWidget.createAction(
                "New Query", MaterialDesignP.PLUS, onNewAction),
            EmptyStateWidget.createAction(
                "Load Query", MaterialDesignF.FOLDER_OPEN, onLoadAction),
            EmptyStateWidget.createAction(
                "Templates", MaterialDesignF.FILE_DOCUMENT_MULTIPLE, onTemplateAction));
    }
}
