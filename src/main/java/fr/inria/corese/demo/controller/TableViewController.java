package fr.inria.corese.demo.controller;

import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.manager.ApplicationStateManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class TableViewController {

    private final ApplicationStateManager stateManager;
    private ResultsPaneController resultsPaneController;

    public TableViewController() {
        this.stateManager = ApplicationStateManager.getInstance();
    }

    public void setResultsPaneController(ResultsPaneController resultsPaneController) {
        this.resultsPaneController = resultsPaneController;
    }

    public void runQueryAndDisplay(String queryContent, Integer tabId) {
        if (resultsPaneController == null) {
            System.err.println("FATAL ERROR: ResultsPaneController has not been set in TableViewController.");
            return;
        }

        resultsPaneController.clearResults();

        new Thread(() -> {
            try {

                var mappings = stateManager.executeQuery(queryContent);
                String queryType = stateManager.determineQueryType(queryContent);

                ApplicationStateManager.TabCacheEntry cacheEntry = new ApplicationStateManager.TabCacheEntry(queryType,
                        mappings);
                stateManager.cacheTabResult(tabId, cacheEntry);
                String csvResult = stateManager.formatMappings(mappings, ResultFormat.format.CSV_FORMAT);


                System.out.println("----------------- CSV RESULT FROM CORESE -----------------");
                if (csvResult == null || csvResult.isBlank()) {
                    System.out.println("(Result is null or empty)");
                } else {
                    System.out.println(csvResult);
                }
                System.out.println("----------------------------------------------------------");

                System.out.println("[DEBUG] resultsPaneController.updateTableView() called.");
                resultsPaneController.updateTableView(csvResult);
                

            } catch (Exception e) {
                System.err.println("[DEBUG] An exception occurred in the background thread!");
                Platform.runLater(() -> showError("Query Execution Error", e.getMessage()));
            }
        }).start();
    }

    public void displayCachedQuery(fr.inria.corese.core.kgram.core.Mappings cachedMappings) {
        if (resultsPaneController == null) {
            System.err.println("FATAL ERROR: ResultsPaneController not set!");
            return;
        }

        // 1. Format the already-existing Mappings into a CSV string
        String csvResult = stateManager.formatMappings(cachedMappings, ResultFormat.format.CSV_FORMAT);

        // 2. Pass the final string to the display parent
        resultsPaneController.updateTableView(csvResult);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}