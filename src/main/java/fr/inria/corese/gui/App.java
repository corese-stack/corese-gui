package fr.inria.corese.gui;

import fr.inria.corese.gui.component.layout.GlobalZoomPane;
import fr.inria.corese.gui.core.bootstrap.LinuxInputMethodBootstrap;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.feature.main.MainController;
import fr.inria.corese.gui.feature.main.MainView;
import fr.inria.corese.gui.feature.main.ViewManager;
import fr.inria.corese.gui.feature.main.navigation.NavigationBarController;
import fr.inria.corese.gui.utils.AppExecutors;
import fr.inria.corese.gui.utils.fx.SvgImageLoader;
import java.awt.SplashScreen;
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
	private static final String TYPOGRAPHY_STYLESHEET = "/css/common/typography.css";
	private static final String APP_LOGO_SVG_RESOURCE = "/images/corese-gui-logo.svg";
	private static final String APP_LOGO_PNG_RESOURCE = "/images/corese-gui-logo.png";

	@Override
	public void start(Stage primaryStage) {
		Platform.setImplicitExit(true);
		ThemeManager themeManager = ThemeManager.getInstance();
		initializeAndShowMainStage(primaryStage, themeManager);
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

	private static void initializeAndShowMainStage(Stage primaryStage, ThemeManager themeManager) {
		try {
			MainView mainView = new MainView();
			NavigationBarController navigationController = new NavigationBarController();
			ViewManager viewManager = new ViewManager();
			MainController mainController = new MainController(mainView, navigationController, viewManager);

			Scene scene = new Scene(new GlobalZoomPane(mainView.getRoot()), AppConstants.DEFAULT_WIDTH,
					AppConstants.DEFAULT_HEIGHT);
			CssUtils.applySceneStyles(scene, TYPOGRAPHY_STYLESHEET);
			mainController.bindScene(scene);

			primaryStage.setTitle(AppConstants.APP_NAME + " — " + AppConstants.APP_VERSION);
			primaryStage.setMinWidth(AppConstants.MIN_WIDTH);
			primaryStage.setMinHeight(AppConstants.MIN_HEIGHT);
			applyApplicationIcon(primaryStage);
			themeManager.setPrimaryStage(primaryStage);
			primaryStage.setScene(scene);
			applyInitialWindowSize(primaryStage);
			primaryStage.setOnCloseRequest(event -> Platform.exit());

			primaryStage.show();
			closeNativeSplashIfPresent();
		} catch (RuntimeException e) {
			closeNativeSplashIfPresent();
			throw e;
		}
	}

	private static void closeNativeSplashIfPresent() {
		try {
			SplashScreen splash = SplashScreen.getSplashScreen();
			if (splash != null) {
				splash.close();
			}
		} catch (UnsupportedOperationException _) {
			// No native splash active.
		}
	}

	private static void applyApplicationIcon(Stage stage) {
		try {
			Image icon = SvgImageLoader.loadSvgImage(APP_LOGO_SVG_RESOURCE, 128, 128);
			if (icon != null) {
				stage.getIcons().add(icon);
				return;
			}

			Image pngIcon = new Image(Objects.requireNonNull(App.class.getResourceAsStream(APP_LOGO_PNG_RESOURCE),
					"Application icon not found"));
			stage.getIcons().add(pngIcon);
		} catch (Exception e) {
			LOGGER.warn("Failed to load application icon", e);
		}
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
