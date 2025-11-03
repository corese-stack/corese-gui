package fr.inria.corese.demo;

import atlantafx.base.theme.NordLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
  @Override
  public void start(Stage primaryStage) {

    Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());

    try {
      // Charger main-view.fxml comme vue principale
      String mainView = "/fr/inria/corese/demo/main-view.fxml";

      FXMLLoader loader = new FXMLLoader(getClass().getResource(mainView));
      if (loader.getLocation() == null) {
        System.err.println("Could not find FXML file: " + mainView);
        return;
      }

      Parent root = loader.load();

      Scene scene = new Scene(root);
      primaryStage.setTitle("Corese-GUI");
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (Exception e) {
      System.err.println("Error starting application:");
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
