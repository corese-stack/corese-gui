package fr.inria.corese.gui.controller;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.manager.QueryManager;

public class GraphViewController {
  private static final Logger logger = LoggerFactory.getLogger(GraphViewController.class);

  private final QueryManager stateManager;
  private ResultsPaneController resultsPaneController;

  public GraphViewController() {
    this.stateManager = QueryManager.getInstance();
  }

  /**
   * Allows the coordinator (QueryViewController) to inject the display parent. This is the crucial
   * link to the UI.
   *
   * @param resultsPaneController The single, unified display controller.
   */
  public void setResultsPaneController(ResultsPaneController resultsPaneController) {
    this.resultsPaneController = resultsPaneController;
  }

  public void displayGraph(Integer tabId) {
    if (resultsPaneController == null) {
      logger.error("FATAL ERROR: ResultsPaneController not set!");
      return;
    }
    try {

      String ttlResult = stateManager.getFormattedCachedQuery(tabId, "TURTLE");
      Platform.runLater(() -> resultsPaneController.displayGraph(ttlResult));
    } catch (Exception e) {
      logger.error("Error displaying graph for tab {}", tabId, e);
    }
  }
}
