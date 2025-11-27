package fr.inria.corese.gui;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.inria.corese.gui.controller.MainController;
import fr.inria.corese.gui.controller.NavigationBarController;
import fr.inria.corese.gui.manager.ViewManager;
import fr.inria.corese.gui.view.MainView;
import fr.inria.corese.gui.view.utils.SvgImageLoader;
import fr.inria.corese.gui.view.utils.ThemeManager;
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
      // Load SVG icon with high resolution (e.g. 128x128)
      Image icon = SvgImageLoader.loadSvgImage("/images/corese-gui-logo.svg", 128, 128);
      if (icon != null) {
        primaryStage.getIcons().add(icon);
      } else {
        // Fallback to PNG if SVG fails
        Image pngIcon =
            new Image(
                Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/corese-gui-logo.png"),
                    "Application icon not found"));
        primaryStage.getIcons().add(pngIcon);
      }
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
