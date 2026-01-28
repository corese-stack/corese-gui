package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.manager.QueryManager;
import fr.inria.corese.gui.feature.textResult.ResultsPaneController;






import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TextViewController {
  private static final Logger logger = LoggerFactory.getLogger(TextViewController.class);

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
      logger.error("FATAL ERROR: TextViewController - ResultsPaneController not set!");
      return;
    }
    if (tabId == null) {
      resultsPaneController.updateXMLView("");
      return;
    }

    try {
      String formattedResult = stateManager.getFormattedCachedQuery(tabId, formatLabel);

      Platform.runLater(() -> resultsPaneController.updateXMLView(formattedResult));
    } catch (Exception e) {
      Platform.runLater(
          () -> {
            String errorMsg = "Error formatting result as " + formatLabel + ": " + e.getMessage();
            resultsPaneController.updateXMLView(errorMsg);
            showError("Formatting Error", errorMsg);
          });
      logger.error("Error formatting result as {} for tab {}", formatLabel, tabId, e);
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
