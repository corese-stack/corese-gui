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
   * Displays an RDF graph from JSON-LD data.
   *
   * @param jsonLdData The RDF data in JSON-LD format
   */
  public void displayGraph(String jsonLdData) {
    view.getGraphWidget().displayGraph(jsonLdData);
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