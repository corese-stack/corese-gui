package fr.inria.corese.gui.view.base;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

/**
 * A generic view with a vertical split pane, typically used for an editor (top) and results
 * (bottom).
 */
public class SplitEditorView extends AbstractView {

  // ===== Fields =====

  protected final SplitPane mainSplitPane;

  // ===== Constructor =====

  /**
   * Creates a SplitEditorView with the specified stylesheet.
   *
   * @param stylesheetPath the path to the CSS stylesheet to apply to this view
   */
  public SplitEditorView(String stylesheetPath) {
    super(new BorderPane(), stylesheetPath);

    mainSplitPane = new SplitPane();
    mainSplitPane.setOrientation(Orientation.VERTICAL);

    BorderPane root = (BorderPane) getRoot();
    root.setCenter(mainSplitPane);
  }

  public SplitEditorView() {
    this(null);
  }

  // ===== Methods =====

  /**
   * Sets the editor view in the top/left pane of the split pane.
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
   * Sets the results view in the bottom/right pane of the split pane.
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
