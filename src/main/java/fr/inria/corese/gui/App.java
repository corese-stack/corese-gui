package fr.inria.corese.gui;

import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.component.layout.GlobalZoomPane;
import fr.inria.corese.gui.feature.main.MainController;
import fr.inria.corese.gui.feature.main.MainView;
import fr.inria.corese.gui.feature.main.ViewManager;
import fr.inria.corese.gui.feature.main.navigation.NavigationBarController;
import fr.inria.corese.gui.utils.AppExecutors;
import fr.inria.corese.gui.utils.fx.SvgImageLoader;
import fr.inria.corese.gui.core.bootstrap.LinuxInputMethodBootstrap;
import java.util.Objects;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the Corese-GUI application.
 *
 * <p>
 * Initializes JavaFX, applies the global theme and base styles, and launches
 * the main view and controller.
 */
public final class App extends Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	@Override
	public void start(Stage primaryStage) {
		Platform.setImplicitExit(true);

		// === Apply global theme ===
		ThemeManager themeManager = ThemeManager.getInstance();
		themeManager.setPrimaryStage(primaryStage);

		// === Build MVC ===
		MainView mainView = new MainView();
		NavigationBarController navigationBar = new NavigationBarController();
		ViewManager viewManager = new ViewManager();

		MainController mainController = new MainController(mainView, navigationBar, viewManager);

		// === Create scene ===
		Scene scene = new Scene(new GlobalZoomPane(mainView.getRoot()), AppConstants.DEFAULT_WIDTH,
				AppConstants.DEFAULT_HEIGHT);
		mainController.bindScene(scene);

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
				Image pngIcon = new Image(Objects.requireNonNull(
						getClass().getResourceAsStream("/images/corese-gui-logo.png"), "Application icon not found"));
				primaryStage.getIcons().add(pngIcon);
			}
		} catch (Exception e) {
			// Log but don't crash if icon fails
			LOGGER.warn("Failed to load application icon", e);
		}

		primaryStage.setScene(scene);
		applyInitialWindowSize(primaryStage);
		primaryStage.setOnCloseRequest(event -> Platform.exit());
		primaryStage.show();
	}

	@Override
	public void stop() {
		ThemeManager.getInstance().shutdown();
		AppExecutors.shutdown();
	}

	public static void main(String[] args) {
		LinuxInputMethodBootstrap.relaunchWithXimIfNeeded(args);
		launch(args);
		// Ensure the process exits even if a third-party non-daemon thread remains
		// alive.
		System.exit(0);
	}

	private static void applyInitialWindowSize(Stage stage) {
		Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();

		double defaultWidth = AppConstants.DEFAULT_WIDTH;
		double defaultHeight = AppConstants.DEFAULT_HEIGHT;

		double maxWidth = bounds.getWidth() * 0.92;
		double maxHeight = bounds.getHeight() * 0.92;

		double targetWidth = Math.min(defaultWidth, maxWidth);
		double targetHeight = Math.min(defaultHeight, maxHeight);

		stage.setWidth(Math.max(AppConstants.MIN_WIDTH, targetWidth));
		stage.setHeight(Math.max(AppConstants.MIN_HEIGHT, targetHeight));
		stage.centerOnScreen();
	}
}
