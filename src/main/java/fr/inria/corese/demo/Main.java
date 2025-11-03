package fr.inria.corese.demo;

import atlantafx.base.theme.NordLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Corese GUI application.
 * This class initializes the JavaFX application and loads the main view.
 */
public class Main extends Application {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  @Override
  public void start(Stage primaryStage) {
    logger.info("Starting Corese GUI application");

    Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
    logger.debug("Applied NordLight theme");

    try {
      // Charger main-view.fxml comme vue principale
      String mainView = "/fr/inria/corese/demo/main-view.fxml";

      FXMLLoader loader = new FXMLLoader(getClass().getResource(mainView));
      if (loader.getLocation() == null) {
        logger.error("Could not find FXML file: {}", mainView);
        return;
      }

      Parent root = loader.load();
      logger.debug("Successfully loaded main view: {}", mainView);

      Scene scene = new Scene(root);
      primaryStage.setTitle("Corese-GUI");
      primaryStage.setScene(scene);
      primaryStage.show();
      
      logger.info("Corese GUI application started successfully");
    } catch (Exception e) {
      logger.error("Error starting application", e);
    }
  }

  public static void main(String[] args) {
    logger.info("Launching Corese GUI with arguments: {}", (Object) args);
    launch(args);
  }
}
