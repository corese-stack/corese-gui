package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.manager.QueryManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class TextViewController {

    private final QueryManager stateManager;
    private ResultsPaneController resultsPaneController;

    public TextViewController() {
        this.stateManager = QueryManager.getInstance();
    }


    public void setResultsPaneController(ResultsPaneController resultsPaneController) {
        this.resultsPaneController = resultsPaneController;
    }


    public void displayData(Integer tabId, String formatLabel) {
        if (resultsPaneController == null) {
            System.err.println("FATAL ERROR: TextViewController - ResultsPaneController not set!");
            return;
        }
        if (tabId == null) {
            resultsPaneController.updateXMLView(""); 
            return;
        }

        try {
            String formattedResult = stateManager.getFormattedCachedQuery(tabId, formatLabel);

            Platform.runLater(() -> {
                resultsPaneController.updateXMLView(formattedResult);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                String errorMsg = "Error formatting result as " + formatLabel + ": " + e.getMessage();
                resultsPaneController.updateXMLView(errorMsg);
                showError("Formatting Error", errorMsg);
                e.printStackTrace();
            });
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