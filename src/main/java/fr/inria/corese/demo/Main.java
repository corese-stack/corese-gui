package fr.inria.corese.demo;

import atlantafx.base.theme.NordLight;
import fr.inria.corese.demo.controller.MainController;
import fr.inria.corese.demo.view.MainView;
import fr.inria.corese.demo.view.utils.CssUtils;
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
public final class Main extends Application {

  @Override
  public void start(Stage primaryStage) {
    // === Apply global theme ===
    ThemeManager.getInstance().applyTheme(new NordLight());

    // === Build MVC ===
    MainView view = new MainView();
    MainController controller = new MainController(view);

    // === Create scene ===
    Scene scene = new Scene(controller.getView().getRoot(), 1400, 850);

    CssUtils.applyBaseStyles(scene);

    // === Configure stage ===
    primaryStage.setTitle("Corese-GUI");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
