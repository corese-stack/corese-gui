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
import java.net.URL;
import java.util.List;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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
	private static final String STARTUP_SPLASH_RESOURCE = "/images/startup-splash-primer-dark.png";
	private static final double STARTUP_SPLASH_PREFERRED_WIDTH = 802;
	private static final double STARTUP_SPLASH_MAX_SCREEN_WIDTH_RATIO = 0.82;
	private static final double STARTUP_SPLASH_MAX_SCREEN_HEIGHT_RATIO = 0.55;
	private static final double STARTUP_SPLASH_MIN_DISPLAY_MS = 1200;
	private static final double STARTUP_SPLASH_RENDER_GUARD_MS = 60;

	@Override
	public void start(Stage primaryStage) {
		Platform.setImplicitExit(true);

		Stage splashStage = createStartupSplashStage();
		if (splashStage != null) {
			splashStage.show();
			recenterSplashAfterShow(splashStage);
		}

		long startupStartNanos = System.nanoTime();
		PauseTransition initializeDelay = new PauseTransition(Duration.millis(STARTUP_SPLASH_RENDER_GUARD_MS));
		initializeDelay
				.setOnFinished(event -> initializeAndShowMainStage(primaryStage, splashStage, startupStartNanos));
		initializeDelay.play();
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

	private static void initializeAndShowMainStage(Stage primaryStage, Stage splashStage, long startupStartNanos) {
		try {
			ThemeManager themeManager = ThemeManager.getInstance();
			MainView mainView = new MainView();
			NavigationBarController navigationController = new NavigationBarController();
			ViewManager viewManager = new ViewManager();
			MainController mainController = new MainController(mainView, navigationController, viewManager);

			Scene scene = new Scene(new GlobalZoomPane(mainView.getRoot()), AppConstants.DEFAULT_WIDTH,
					AppConstants.DEFAULT_HEIGHT);
			CssUtils.applySceneStyles(scene, TYPOGRAPHY_STYLESHEET);
			mainController.bindScene(scene);

			primaryStage.setTitle(AppConstants.APP_TITLE + " — " + AppConstants.APP_VERSION);
			primaryStage.setMinWidth(AppConstants.MIN_WIDTH);
			primaryStage.setMinHeight(AppConstants.MIN_HEIGHT);
			applyApplicationIcon(primaryStage);
			themeManager.setPrimaryStage(primaryStage);
			primaryStage.setScene(scene);
			applyInitialWindowSize(primaryStage);
			primaryStage.setOnCloseRequest(event -> Platform.exit());

			schedulePrimaryStageShow(primaryStage, splashStage, startupStartNanos);
		} catch (RuntimeException e) {
			closeSplashStage(splashStage);
			throw e;
		}
	}

	private static void schedulePrimaryStageShow(Stage primaryStage, Stage splashStage, long startupStartNanos) {
		double elapsedMs = (System.nanoTime() - startupStartNanos) / 1_000_000.0;
		double remainingMs = STARTUP_SPLASH_MIN_DISPLAY_MS - elapsedMs;
		if (splashStage == null || remainingMs <= 0) {
			showPrimaryStageAndCloseSplash(primaryStage, splashStage);
			return;
		}
		PauseTransition delay = new PauseTransition(Duration.millis(remainingMs));
		delay.setOnFinished(event -> showPrimaryStageAndCloseSplash(primaryStage, splashStage));
		delay.play();
	}

	private static void showPrimaryStageAndCloseSplash(Stage primaryStage, Stage splashStage) {
		primaryStage.show();
		closeSplashStage(splashStage);
	}

	private static void closeSplashStage(Stage splashStage) {
		if (splashStage == null) {
			return;
		}
		splashStage.close();
	}

	private static Stage createStartupSplashStage() {
		Image splashImage = loadStartupSplashImage();
		if (splashImage == null) {
			return null;
		}

		ImageView splashImageView = new ImageView(splashImage);
		splashImageView.setPreserveRatio(true);
		splashImageView.setSmooth(false);
		applySplashSize(splashImageView, splashImage);

		StackPane root = new StackPane(splashImageView);
		root.setStyle("-fx-background-color: transparent;");

		boolean transparentWindowSupported = Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW);
		Scene scene = new Scene(root);
		if (transparentWindowSupported) {
			scene.setFill(Color.TRANSPARENT);
		}

		StageStyle stageStyle = transparentWindowSupported ? StageStyle.TRANSPARENT : StageStyle.UNDECORATED;
		Stage splashStage = new Stage(stageStyle);
		splashStage.setScene(scene);
		splashStage.setResizable(false);
		splashStage.sizeToScene();
		splashStage.centerOnScreen();
		return splashStage;
	}

	private static Image loadStartupSplashImage() {
		try {
			URL imageUrl = App.class.getResource(STARTUP_SPLASH_RESOURCE);
			if (imageUrl == null) {
				LOGGER.warn("Startup splash image not found: {}", STARTUP_SPLASH_RESOURCE);
				return null;
			}

			Image splashImage = new Image(imageUrl.toExternalForm(), false);
			if (isUsableSplashImage(splashImage)) {
				return splashImage;
			}
			LOGGER.warn("Startup splash image is unusable: {}", STARTUP_SPLASH_RESOURCE, splashImage.getException());
			return null;
		} catch (Exception e) {
			LOGGER.warn("Unable to load startup splash image: {}", STARTUP_SPLASH_RESOURCE, e);
			return null;
		}
	}

	private static boolean isUsableSplashImage(Image image) {
		return image != null && !image.isError() && image.getWidth() > 0 && image.getHeight() > 0;
	}

	private static void applySplashSize(ImageView splashImageView, Image splashImage) {
		Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
		double maxWidth = Math.min(STARTUP_SPLASH_PREFERRED_WIDTH,
				bounds.getWidth() * STARTUP_SPLASH_MAX_SCREEN_WIDTH_RATIO);
		double maxHeight = bounds.getHeight() * STARTUP_SPLASH_MAX_SCREEN_HEIGHT_RATIO;
		double widthScale = maxWidth / splashImage.getWidth();
		double heightScale = maxHeight / splashImage.getHeight();
		double scale = Math.min(1.0, Math.min(widthScale, heightScale));
		if (scale < 1.0) {
			splashImageView.setFitWidth(Math.rint(splashImage.getWidth() * scale));
			splashImageView.setFitHeight(Math.rint(splashImage.getHeight() * scale));
		}
	}

	private static void recenterSplashAfterShow(Stage splashStage) {
		Platform.runLater(() -> centerStageOnCurrentScreen(splashStage));
	}

	private static void centerStageOnCurrentScreen(Stage stage) {
		List<Screen> candidateScreens = Screen.getScreensForRectangle(stage.getX(), stage.getY(),
				Math.max(stage.getWidth(), 1), Math.max(stage.getHeight(), 1));
		Screen targetScreen = candidateScreens.isEmpty() ? Screen.getPrimary() : candidateScreens.get(0);
		Rectangle2D bounds = targetScreen.getVisualBounds();
		stage.setX(bounds.getMinX() + ((bounds.getWidth() - stage.getWidth()) / 2.0));
		stage.setY(bounds.getMinY() + ((bounds.getHeight() - stage.getHeight()) / 2.0));
	}

	private static void applyApplicationIcon(Stage stage) {
		try {
			// Load SVG icon with high resolution (e.g. 128x128)
			Image icon = SvgImageLoader.loadSvgImage("/images/corese-gui-logo.svg", 128, 128);
			if (icon != null) {
				stage.getIcons().add(icon);
				return;
			}

			// Fallback to PNG if SVG fails
			Image pngIcon = new Image(Objects.requireNonNull(
					App.class.getResourceAsStream("/images/corese-gui-logo.png"), "Application icon not found"));
			stage.getIcons().add(pngIcon);
		} catch (Exception e) {
			// Log but don't crash if icon fails
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
