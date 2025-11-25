package fr.inria.corese.demo;

import fr.inria.corese.demo.controller.MainController;
import fr.inria.corese.demo.controller.NavigationBarController;
import fr.inria.corese.demo.manager.ViewManager;
import fr.inria.corese.demo.view.MainView;
import fr.inria.corese.demo.view.utils.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point of the Corese-GUI application.
 *
 * <p>Initializes JavaFX, applies the global theme and base styles, and launches the main view and
 * controller.
 */
public final class App extends Application {

  @Override
  public void start(Stage primaryStage) {
    // === Apply global theme ===
    ThemeManager themeManager = ThemeManager.getInstance();
    themeManager.setPrimaryStage(primaryStage);

    // Enable system theme monitoring by default
    themeManager.setSystemThemeEnabled(true);

    // === Build MVC ===
    MainView mainView = new MainView();
    NavigationBarController navigationBar = new NavigationBarController();
    ViewManager viewManager = new ViewManager();

    new MainController(mainView, navigationBar, viewManager);

    // === Create scene ===
    Scene scene =
        new Scene(mainView.getRoot(), AppConstants.DEFAULT_WIDTH, AppConstants.DEFAULT_HEIGHT);
    // === Configure stage ===
    primaryStage.setTitle(AppConstants.APP_TITLE);
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
