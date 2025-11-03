package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.manager.QueryManager;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Main controller for the application. Manages the main container, navigation, and content area.
 */
public class MainController {
  @FXML private BorderPane mainContainer;

  @FXML private VBox navigationContainer;

  @FXML private BorderPane contentArea;

  private NavigationBarController navigationBarController;
  private QueryManager queryManager;

  /** Initializes the controller. Sets up the state manager and navigation. */
  @FXML
  public void initialize() {
    queryManager = QueryManager.getInstance();

    navigationBarController = new NavigationBarController(contentArea);

    navigationContainer.getChildren().clear();
    navigationContainer.getChildren().add(navigationBarController.getView());

    navigationBarController.selectView("data-view");

    queryManager.addLogEntry("MainController initialization complete");
  }
}
