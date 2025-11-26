package fr.inria.corese.demo;

import fr.inria.corese.demo.controller.MainController;
import fr.inria.corese.demo.controller.NavigationBarController;
import fr.inria.corese.demo.manager.ViewManager;
import fr.inria.corese.demo.view.MainView;
import fr.inria.corese.demo.view.utils.ThemeManager;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Entry point of the Corese-GUI application.
 *
 * <p>Initializes JavaFX, applies the global theme and base styles, and launches the main view and
 * controller.
 */
public final class App extends Application {

  private static final Logger LOGGER = Logger.getLogger(App.class.getName());

  @Override
  public void start(Stage primaryStage) {
    // === Apply global theme ===
    ThemeManager themeManager = ThemeManager.getInstance();
    themeManager.setPrimaryStage(primaryStage);

    // === Build MVC ===
    MainView mainView = new MainView();
    NavigationBarController navigationBar = new NavigationBarController();
    ViewManager viewManager = new ViewManager();

    new MainController(mainView, navigationBar, viewManager);

    // === Create scene ===
    Scene scene =
        new Scene(mainView.getRoot(), AppConstants.DEFAULT_WIDTH, AppConstants.DEFAULT_HEIGHT);
    // === Configure stage ===
    primaryStage.setTitle(AppConstants.APP_TITLE + " — " + AppConstants.APP_VERSION);
    primaryStage.setMinWidth(AppConstants.MIN_WIDTH);
    primaryStage.setMinHeight(AppConstants.MIN_HEIGHT);
    
    // Set application icon
    try {
        Image icon = new Image(Objects.requireNonNull(
            getClass().getResourceAsStream("/images/corese-gui-logo.png"), 
            "Application icon not found"
        ));
        primaryStage.getIcons().add(icon);
    } catch (Exception e) {
        // Log but don't crash if icon fails
        LOGGER.log(Level.WARNING, "Failed to load application icon", e);
    }

    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
