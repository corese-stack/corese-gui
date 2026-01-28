package fr.inria.corese.gui.feature.textResult;

import fr.inria.corese.gui.core.manager.QueryManager;






import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TableViewController {
  private static final Logger logger = LoggerFactory.getLogger(TableViewController.class);

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
      logger.error("FATAL ERROR: ResultsPaneController not set!");
      return;
    }
    try {
      String csvResult = stateManager.getFormattedCachedQuery(tabId, "CSV");
      Platform.runLater(() -> resultsPaneController.updateTableView(csvResult));
    } catch (Exception e) {
      logger.error("Error displaying data for tab {}", tabId, e);
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
