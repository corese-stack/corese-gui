package fr.inria.corese.gui.feature.result.graph;

import javafx.scene.Node;

/**
 * Controller for graph visualization.
 * Delegates rendering to GraphResultView and GraphDisplayWidget.
 */
public class GraphResultController {

  private final GraphResultView view;

  public GraphResultController() {
    this.view = new GraphResultView();
  }

  /**
   * Displays an RDF graph from TTL data.
   *
   * @param ttlData The RDF data in Turtle format
   */
  public void displayGraph(String ttlData) {
    view.getGraphWidget().displayGraph(ttlData);
  }

  /**
   * Clears the graph view.
   */
  public void clear() {
    view.getGraphWidget().clear();
  }

  /**
   * Returns the root view node.
   *
   * @return The view root
   */
  public Node getView() {
    return view.getRoot();
  }
}