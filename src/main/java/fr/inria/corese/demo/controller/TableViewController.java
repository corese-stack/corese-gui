package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.manager.QueryManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class TableViewController {

  private final QueryManager stateManager;
  private ResultsPaneController resultsPaneController;

  public TableViewController() {
    this.stateManager = QueryManager.getInstance();
  }

  public void setResultsPaneController(ResultsPaneController resultsPaneController) {
    this.resultsPaneController = resultsPaneController;
  }

  public void displayData(Integer tabId) {
    if (resultsPaneController == null) {
      System.err.println("FATAL ERROR: ResultsPaneController not set!");
      return;
    }
    try {
      String csvResult = stateManager.getFormattedCachedQuery(tabId, "CSV");
      Platform.runLater(
          () -> {
            resultsPaneController.updateTableView(csvResult);
          });
    } catch (Exception e) {
      e.printStackTrace();
      showError("Error displaying data", e.getMessage());
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
