package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

/**
 * View for the Validation screen.
 *
 * <p>Displays a code editor for SHACL shapes and a results pane for validation reports.
 */
public class ValidationView extends AbstractView {

  private final SplitPane mainSplitPane;

  public ValidationView() {
    super(new BorderPane(), null);

    mainSplitPane = new SplitPane();
    mainSplitPane.setOrientation(Orientation.VERTICAL);

    BorderPane root = (BorderPane) getRoot();
    root.setCenter(mainSplitPane);
  }

  public void setEditorView(Node node) {
    if (mainSplitPane.getItems().isEmpty()) {
      mainSplitPane.getItems().add(node);
    } else {
      mainSplitPane.getItems().set(0, node);
    }
  }

  public void setResultView(Node node) {
    if (mainSplitPane.getItems().size() < 1) {
       // Ensure there is a first item
       mainSplitPane.getItems().add(new BorderPane());
    }
    if (mainSplitPane.getItems().size() < 2) {
      mainSplitPane.getItems().add(node);
    } else {
      mainSplitPane.getItems().set(1, node);
    }
  }
  
  public SplitPane getMainSplitPane() {
      return mainSplitPane;
  }
}
