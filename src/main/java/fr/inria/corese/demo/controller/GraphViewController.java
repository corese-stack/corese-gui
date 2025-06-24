package fr.inria.corese.demo.controller;

// The correct import for the Graph object from CONSTRUCT results
import fr.inria.corese.core.Graph;

import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.manager.ApplicationStateManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.web.WebView;


public class GraphViewController {

    private final ApplicationStateManager stateManager;
    private ResultsPaneController resultsPaneController; // Reference to the display parent

    public GraphViewController() {
        this.stateManager = ApplicationStateManager.getInstance();
    }

    /**
     * Allows the coordinator (QueryViewController) to inject the display parent.
     * This is the crucial link to the UI.
     * 
     * @param resultsPaneController The single, unified display controller.
     */
    public void setResultsPaneController(ResultsPaneController resultsPaneController) {
        this.resultsPaneController = resultsPaneController;
    }

    /**
     * Called by the coordinator to run a NEW query.
     * 
     * @param queryContent The SPARQL query string.
     * @param tabId        The unique ID of the tab for caching.
     */
    public void runQueryAndDisplay(String queryContent, Integer tabId) {
        if (resultsPaneController == null) {
            System.err.println("FATAL ERROR: ResultsPaneController not set in GraphViewController!");
            return;
        }
        new Thread(() -> {
            try {
                stateManager.executeQuery(queryContent);
                Graph resultGraph = stateManager.getGraph();
                String queryType = stateManager.determineQueryType(queryContent);

                ApplicationStateManager.TabCacheEntry cacheEntry = new ApplicationStateManager.TabCacheEntry(queryType,
                        resultGraph);
                stateManager.cacheTabResult(tabId, cacheEntry);

                String ttlResult = stateManager.formatGraph(resultGraph, ResultFormat.format.TURTLE_FORMAT);

                Platform.runLater(() -> {
                    resultsPaneController.displayGraph(ttlResult);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Query Execution Error", e.getMessage()));
            }
        }).start();
    }

    /**
     * Called by the coordinator to display a CACHED result.
     * 
     * @param cachedMappings The Mappings object retrieved from the cache.
     */

     public void displayCachedGraph(Graph graph){
        if(resultsPaneController == null) return;

        String ttlResult = stateManager.formatGraph(graph, ResultFormat.format.TURTLE_FORMAT);

        resultsPaneController.displayGraph(ttlResult);
     }

    /**
     * Private helper to safely format a Mappings object into a TTL string.
     */
    private String formatResultToTtl(fr.inria.corese.core.kgram.core.Mappings mappings) {
        if (mappings == null)
            return "";
        // The cast to Graph is valid for CONSTRUCT/DESCRIBE results
        Graph resultGraph = (Graph) mappings.getGraph();
        if (resultGraph == null)
            return "";
        // Use the stateless formatter from the state manager
        return stateManager.formatGraph(resultGraph, ResultFormat.format.TURTLE_FORMAT);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}