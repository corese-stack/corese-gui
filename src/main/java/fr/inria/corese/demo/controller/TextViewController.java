package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.manager.ApplicationStateManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class TextViewController {

    private final ApplicationStateManager stateManager;
    private ResultsPaneController resultsPaneController;

    public TextViewController() {
        this.stateManager = ApplicationStateManager.getInstance();
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
            resultsPaneController.updateXMLView(""); // Clear the view if no tab
            return;
        }

        try {
            // Ask the state manager to format the cached result.
            // Note: The getFormattedCachedQuery method is perfect for this.
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