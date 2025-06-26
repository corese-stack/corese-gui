package fr.inria.corese.demo.controller;

// The correct import for the Graph object from CONSTRUCT results
import fr.inria.corese.core.Graph;

import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.manager.ApplicationStateManager;
import fr.inria.corese.demo.model.FormattedResult;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.web.WebView;

public class GraphViewController {

    private final ApplicationStateManager stateManager;
    private ResultsPaneController resultsPaneController;

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

    public void displayGraph(Integer tabId) {
        if (resultsPaneController == null) {
            System.err.println("FATAL ERROR: ResultsPaneController not set!");
            return;
        }
        try {

            String ttlResult = stateManager.getFormattedCachedQuery(tabId, "TURTLE");
            Platform.runLater(() -> {
                resultsPaneController.displayGraph(ttlResult);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}