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

  // ===== Fields =====

  private final SplitPane mainSplitPane;

  // ===== Constructor =====

  /**
   * Creates the ValidationView with a horizontal split pane containing the editor and results
   * areas.
   */
  public ValidationView() {
    super(new BorderPane(), "/styles/validation-view.css");

    mainSplitPane = new SplitPane();
    mainSplitPane.setOrientation(Orientation.VERTICAL);

    BorderPane root = (BorderPane) getRoot();
    root.setCenter(mainSplitPane);
  }

  // ==== Public Methods =====

  /**
   * Sets the editor view in the left pane of the split pane.
   *
   * @param node the editor view node to set
   */
  public void setEditorView(Node node) {
    if (mainSplitPane.getItems().isEmpty()) {
      mainSplitPane.getItems().add(node);
    } else {
      mainSplitPane.getItems().set(0, node);
    }
  }

  /**
   * Sets the results view in the right pane of the split pane.
   *
   * @param node the results view node to set
   */
  public void setResultView(Node node) {
    if (mainSplitPane.getItems().isEmpty()) {
      // Ensure there is a first item
      mainSplitPane.getItems().add(new BorderPane());
    }
    if (mainSplitPane.getItems().size() < 2) {
      mainSplitPane.getItems().add(node);
    } else {
      mainSplitPane.getItems().set(1, node);
    }
  }
}
