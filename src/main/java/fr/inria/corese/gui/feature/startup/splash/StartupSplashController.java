package fr.inria.corese.gui.feature.startup.splash;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.core.theme.ThemeManager;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Startup splash controller.
 */
public final class StartupSplashController {

	public static final double MIN_DISPLAY_MS = 1200;
	public static final double RENDER_GUARD_MS = 60;

	private static final double PREFERRED_WIDTH = 960;
	private static final double PREFERRED_HEIGHT = 520;
	private static final double MAX_SCREEN_WIDTH_RATIO = 0.86;
	private static final double MAX_SCREEN_HEIGHT_RATIO = 0.68;

	private final ThemeManager themeManager;
	private final StartupSplashWidget widget;
	private final Stage stage;
	private final ChangeListener<Theme> themeListener;
	private final ChangeListener<Color> accentListener;
	private boolean disposed;

	public StartupSplashController(ThemeManager themeManager) {
		this.themeManager = Objects.requireNonNull(themeManager, "themeManager must not be null");
		this.widget = new StartupSplashWidget();
		applySplashSize();
		applyTheme();

		themeListener = (obs, oldTheme, newTheme) -> applyTheme();
		accentListener = (obs, oldColor, newColor) -> applyTheme();
		this.themeManager.themeProperty().addListener(themeListener);
		this.themeManager.accentColorProperty().addListener(accentListener);

		Scene scene = new Scene(widget.getRoot());
		this.stage = new Stage(StageStyle.UNDECORATED);
		this.stage.setScene(scene);
		this.stage.setResizable(false);
		this.stage.setOnHidden(event -> dispose());
		this.stage.sizeToScene();
		this.stage.centerOnScreen();
	}

	public void show() {
		stage.show();
		recenterAfterShow();
	}

	public void close() {
		stage.close();
	}

	private void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		themeManager.themeProperty().removeListener(themeListener);
		themeManager.accentColorProperty().removeListener(accentListener);
	}

	private void applySplashSize() {
		Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
		double targetWidth = Math.min(PREFERRED_WIDTH, bounds.getWidth() * MAX_SCREEN_WIDTH_RATIO);
		double targetHeight = Math.min(PREFERRED_HEIGHT, bounds.getHeight() * MAX_SCREEN_HEIGHT_RATIO);
		widget.setSize(Math.rint(targetWidth), Math.rint(targetHeight));
	}

	private void applyTheme() {
		Runnable applyAction = () -> widget.applyTheme(themeManager.isCurrentThemeDark(), themeManager.getAccentColor());
		if (Platform.isFxApplicationThread()) {
			applyAction.run();
			return;
		}
		Platform.runLater(applyAction);
	}

	private void recenterAfterShow() {
		Platform.runLater(() -> centerStageOnCurrentScreen(stage));
	}

	private static void centerStageOnCurrentScreen(Stage stage) {
		List<Screen> candidateScreens = Screen.getScreensForRectangle(stage.getX(), stage.getY(),
				Math.max(stage.getWidth(), 1), Math.max(stage.getHeight(), 1));
		Screen targetScreen = candidateScreens.isEmpty() ? Screen.getPrimary() : candidateScreens.get(0);
		Rectangle2D bounds = targetScreen.getVisualBounds();
		stage.setX(bounds.getMinX() + ((bounds.getWidth() - stage.getWidth()) / 2.0));
		stage.setY(bounds.getMinY() + ((bounds.getHeight() - stage.getHeight()) / 2.0));
	}
}
