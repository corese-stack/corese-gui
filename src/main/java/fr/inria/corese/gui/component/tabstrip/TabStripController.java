package fr.inria.corese.gui.component.tabstrip;

import fr.inria.corese.gui.core.theme.ThemeManager;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Controller that binds a {@link TabPane} model to a reusable
 * {@link TabStripView}.
 */
public class TabStripController implements AutoCloseable {

	private final TabPane tabPane;
	private final ThemeManager themeManager;
	private final TabStripView view;
	private final boolean showCloseButton;
	private final boolean animateTabs;
	private final PauseTransition resizeRefreshDebounce;
	private final ChangeListener<Tab> selectionListener;
	private final ListChangeListener<Tab> tabsListener;
	private final ChangeListener<Number> tabPaneWidthListener;
	private final ChangeListener<Number> viewWidthListener;
	private final ChangeListener<Scene> sceneListener;
	private final ChangeListener<Window> sceneWindowListener;
	private final ChangeListener<Boolean> windowShowingListener;
	private final ChangeListener<Color> accentListener;
	private boolean disposed = false;
	private Scene boundScene;
	private Window boundWindow;

	private Consumer<Tab> onCloseRequest;

	public TabStripController(TabPane tabPane, ThemeManager themeManager) {
		this(tabPane, themeManager, true, true);
	}

	public TabStripController(TabPane tabPane, ThemeManager themeManager, boolean showCloseButton) {
		this(tabPane, themeManager, showCloseButton, true);
	}

	public TabStripController(TabPane tabPane, ThemeManager themeManager, boolean showCloseButton,
			boolean animateTabs) {
		this.tabPane = tabPane;
		this.themeManager = themeManager;
		this.showCloseButton = showCloseButton;
		this.animateTabs = animateTabs;
		this.view = new TabStripView(animateTabs);
		this.resizeRefreshDebounce = new PauseTransition(Duration.millis(90));
		this.resizeRefreshDebounce.setOnFinished(e -> refresh(false));
		this.onCloseRequest = tab -> this.tabPane.getTabs().remove(tab);
		this.selectionListener = (obs, oldTab, newTab) -> refresh();
		this.tabsListener = change -> {
			refresh();
			// First render can happen before viewport width is finalized.
			// Re-render on next pulse to get correct full-width sizing.
			Platform.runLater(this::refresh);
		};
		this.tabPaneWidthListener = (obs, oldWidth, newWidth) -> refreshForResize();
		this.viewWidthListener = (obs, oldWidth, newWidth) -> refreshForResize();
		this.sceneWindowListener = (windowObs, oldWindow, newWindow) -> {
			if (newWindow != null) {
				bindWindowRefresh(newWindow);
			} else {
				bindWindowRefresh(null);
			}
		};
		this.sceneListener = (obs, oldScene, newScene) -> {
			if (newScene == null) {
				bindSceneRefresh(null);
				return;
			}
			bindSceneRefresh(newScene);
		};
		this.windowShowingListener = (obs, wasShowing, isShowing) -> {
			if (Boolean.TRUE.equals(isShowing)) {
				Platform.runLater(this::refresh);
				Platform.runLater(() -> Platform.runLater(this::refresh));
			}
		};
		this.accentListener = (obs, oldColor, newColor) -> refresh();
		setupListeners();
		refresh();
	}

	public TabStripView getView() {
		return view;
	}

	public void setOnCloseRequest(Consumer<Tab> onCloseRequest) {
		this.onCloseRequest = onCloseRequest != null ? onCloseRequest : (tab -> tabPane.getTabs().remove(tab));
	}

	public void refresh() {
		refresh(true);
	}

	private void refresh(boolean animateWidthChanges) {
		if (disposed) {
			return;
		}
		Color accent = themeManager.getAccentColor();
		view.render(tabPane.getTabs(), tabPane.getSelectionModel().getSelectedItem(), accent, showCloseButton,
				animateWidthChanges, tab -> tabPane.getSelectionModel().select(tab), onCloseRequest);
	}

	private void setupListeners() {
		tabPane.getSelectionModel().selectedItemProperty().addListener(selectionListener);
		tabPane.getTabs().addListener(tabsListener);
		tabPane.widthProperty().addListener(tabPaneWidthListener);
		view.widthProperty().addListener(viewWidthListener);
		view.sceneProperty().addListener(sceneListener);
		themeManager.accentColorProperty().addListener(accentListener);
		bindSceneRefresh(view.getScene());
	}

	private void bindSceneRefresh(Scene scene) {
		if (scene == boundScene) {
			return;
		}
		if (boundScene != null) {
			boundScene.windowProperty().removeListener(sceneWindowListener);
		}
		boundScene = scene;
		if (boundScene != null) {
			boundScene.windowProperty().addListener(sceneWindowListener);
			bindWindowRefresh(boundScene.getWindow());
			return;
		}
		bindWindowRefresh(null);
	}

	private void bindWindowRefresh(Window window) {
		if (window == boundWindow) {
			return;
		}
		if (boundWindow != null) {
			boundWindow.showingProperty().removeListener(windowShowingListener);
		}
		boundWindow = window;
		if (boundWindow == null) {
			return;
		}
		boundWindow.showingProperty().addListener(windowShowingListener);
		if (boundWindow.isShowing()) {
			Platform.runLater(this::refresh);
		}
	}

	private void refreshForResize() {
		if (disposed) {
			return;
		}
		refresh(false);
		if (!animateTabs) {
			return;
		}
		// Ensure we also refresh with the latest stabilized width after resize
		// animation/layout.
		resizeRefreshDebounce.playFromStart();
		Platform.runLater(() -> refresh(false));
	}

	@Override
	public void close() {
		if (disposed) {
			return;
		}
		disposed = true;
		resizeRefreshDebounce.stop();

		tabPane.getSelectionModel().selectedItemProperty().removeListener(selectionListener);
		tabPane.getTabs().removeListener(tabsListener);
		tabPane.widthProperty().removeListener(tabPaneWidthListener);
		view.widthProperty().removeListener(viewWidthListener);
		view.sceneProperty().removeListener(sceneListener);

		if (boundScene != null) {
			boundScene.windowProperty().removeListener(sceneWindowListener);
			boundScene = null;
		}
		if (boundWindow != null) {
			boundWindow.showingProperty().removeListener(windowShowingListener);
			boundWindow = null;
		}

		themeManager.accentColorProperty().removeListener(accentListener);
	}
}
