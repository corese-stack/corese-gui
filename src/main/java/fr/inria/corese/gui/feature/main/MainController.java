package fr.inria.corese.gui.feature.main;

import fr.inria.corese.gui.core.enums.ViewId;
import fr.inria.corese.gui.core.shortcut.KeyboardShortcutRegistry;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.feature.data.DataView;
import fr.inria.corese.gui.feature.data.DataViewController;
import fr.inria.corese.gui.feature.main.navigation.NavigationBarController;
import fr.inria.corese.gui.feature.query.QueryView;
import fr.inria.corese.gui.feature.query.QueryViewController;
import fr.inria.corese.gui.feature.validation.ValidationController;
import fr.inria.corese.gui.feature.validation.ValidationView;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main controller for the Corese-GUI application.
 *
 * <p>
 * Coordinates navigation between sidebar and content area, delegates view
 * loading to {@link ViewManager}, and applies smooth transitions.
 */
public final class MainController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
	private static final double GLOBAL_ZOOM_STEP = 0.1;

	// ===== Fields =====

	/** The main application view. */
	private final MainView view;

	/** Controller for the sidebar navigation. */
	private final NavigationBarController navController;

	/** Manager for loading and caching views. */
	private final ViewManager viewManager;
	private ViewId currentViewId = ViewId.DATA;
	private Scene shortcutScene;
	private final EventHandler<KeyEvent> shortcutEventHandler = this::handleShortcutPressed;

	// ===== Constructor =====

	/**
	 * Creates the main controller with the given main view.
	 *
	 * @param view
	 *            the main application view
	 * @param navController
	 *            the navigation bar controller
	 * @param viewManager
	 *            the view manager for loading views
	 */
	public MainController(MainView view, NavigationBarController navController, ViewManager viewManager) {
		this.view = view;
		this.navController = navController;
		this.viewManager = viewManager;
		initialize();
	}

	public void bindScene(Scene scene) {
		if (shortcutScene == scene) {
			return;
		}
		if (shortcutScene != null) {
			shortcutScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutEventHandler);
		}
		shortcutScene = scene;
		if (shortcutScene != null) {
			shortcutScene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutEventHandler);
		}
	}

	// ==== Initialization =====

	/** Initializes layout and connects event handlers. */
	private void initialize() {
		// Set up the navigation bar in the main view
		view.setNavigationRoot(navController.getRoot());

		// Handle navigation actions
		navController.setOnNavigate(this::displayView);

		// Show the default view at startup (instant display)
		this.displayView(ViewId.DATA);

		// Preload other views in background after a short delay to ensure fluidity
		preloadOtherViewsAsync(ViewId.DATA);
	}

	/**
	 * Displays the requested view with a simple fade transition.
	 *
	 * @param viewId
	 *            the identifier of the view to display
	 */
	private void displayView(ViewId viewId) {
		if (viewId == null) {
			return;
		}
		view.setContent(viewManager.getView(viewId));
		navController.selectView(viewId);
		currentViewId = viewId;
	}

	private void handleShortcutPressed(KeyEvent event) {
		if (event == null || event.isConsumed()) {
			return;
		}
		for (KeyboardShortcutRegistry.Shortcut shortcut : KeyboardShortcutRegistry.shortcuts()) {
			if (!shortcut.combination().match(event)) {
				continue;
			}
			boolean handled = executeShortcut(shortcut.action());
			if (handled) {
				event.consume();
			}
			return;
		}
	}

	private boolean executeShortcut(KeyboardShortcutRegistry.Action action) {
		if (action == null) {
			return false;
		}
		return switch (action) {
			case OPEN_DATA_VIEW -> {
				displayView(ViewId.DATA);
				yield true;
			}
			case OPEN_QUERY_VIEW -> {
				displayView(ViewId.QUERY);
				yield true;
			}
			case OPEN_VALIDATION_VIEW -> {
				displayView(ViewId.VALIDATION);
				yield true;
			}
			case OPEN_SYSTEM_LOGS_VIEW -> {
				displayView(ViewId.SYSTEM_LOGS);
				yield true;
			}
			case OPEN_SETTINGS_VIEW -> {
				displayView(ViewId.SETTINGS);
				yield true;
			}
			case TOGGLE_NAVIGATION_BAR -> toggleNavigationBar();
			case GLOBAL_ZOOM_IN -> adjustGlobalZoom(+GLOBAL_ZOOM_STEP);
			case GLOBAL_ZOOM_OUT -> adjustGlobalZoom(-GLOBAL_ZOOM_STEP);
			case GLOBAL_ZOOM_RESET -> resetGlobalZoom();
			case OPEN_FILE -> handleOpenFileShortcut();
			case SAVE_EDITOR -> handleQueryValidationShortcut(QueryViewController::saveFromShortcut,
					ValidationController::saveFromShortcut);
			case EXPORT_CONTEXT -> handleExportContextShortcut();
			case EXPORT_GRAPH -> handleExportGraphShortcut();
			case OPEN_TEMPLATE -> handleTemplateShortcut();
			case CREATE_TAB -> handleQueryValidationShortcut(QueryViewController::createTabFromShortcut,
					ValidationController::createTabFromShortcut);
			case CLOSE_TAB -> handleQueryValidationShortcut(QueryViewController::closeTabFromShortcut,
					ValidationController::closeTabFromShortcut);
			case NEXT_TAB -> handleQueryValidationShortcut(QueryViewController::selectNextTabFromShortcut,
					ValidationController::selectNextTabFromShortcut);
			case PREVIOUS_TAB -> handleQueryValidationShortcut(QueryViewController::selectPreviousTabFromShortcut,
					ValidationController::selectPreviousTabFromShortcut);
			case EXECUTE_PRIMARY_ACTION -> handleQueryValidationShortcut(QueryViewController::executeFromShortcut,
					ValidationController::executeFromShortcut);
			case FOCUS_EDITOR -> handleQueryValidationShortcut(QueryViewController::focusEditorFromShortcut,
					ValidationController::focusEditorFromShortcut);
			case DATA_LOAD_URI ->
				handleDataShortcut(controller -> controller != null && controller.loadUriFromShortcut());
			case DATA_RELOAD_SOURCES ->
				handleDataShortcut(controller -> controller != null && controller.reloadSourcesFromShortcut());
			case DATA_CLEAR_GRAPH ->
				handleDataShortcut(controller -> controller != null && controller.clearGraphFromShortcut());
			case GRAPH_REENERGIZE_LAYOUT -> handleGraphShortcut(DataViewController::reenergizeGraphFromShortcut,
					QueryViewController::reenergizeGraphFromShortcut,
					ValidationController::reenergizeGraphFromShortcut);
			case GRAPH_CENTER_VIEW -> handleGraphShortcut(DataViewController::centerGraphFromShortcut,
					QueryViewController::centerGraphFromShortcut, ValidationController::centerGraphFromShortcut);
		};
	}

	private boolean handleOpenFileShortcut() {
		return switch (currentViewId) {
			case DATA -> handleDataShortcut(controller -> controller != null && controller.loadFilesFromShortcut());
			case QUERY -> {
				QueryViewController controller = resolveQueryController();
				yield controller != null && controller.openFileFromShortcut();
			}
			case VALIDATION -> {
				ValidationController controller = resolveValidationController();
				yield controller != null && controller.openFileFromShortcut();
			}
			default -> false;
		};
	}

	private boolean handleTemplateShortcut() {
		return switch (currentViewId) {
			case QUERY -> {
				QueryViewController controller = resolveQueryController();
				yield controller != null && controller.openTemplateFromShortcut();
			}
			case VALIDATION -> {
				ValidationController controller = resolveValidationController();
				yield controller != null && controller.openTemplateFromShortcut();
			}
			default -> false;
		};
	}

	private boolean handleExportContextShortcut() {
		return switch (currentViewId) {
			case DATA -> handleDataShortcut(controller -> controller != null && controller.exportDataFromShortcut());
			case QUERY -> {
				QueryViewController controller = resolveQueryController();
				yield controller != null && controller.exportFromShortcut();
			}
			case VALIDATION -> {
				ValidationController controller = resolveValidationController();
				yield controller != null && controller.exportFromShortcut();
			}
			default -> false;
		};
	}

	private boolean handleExportGraphShortcut() {
		return switch (currentViewId) {
			case DATA -> handleDataShortcut(controller -> controller != null && controller.exportGraphFromShortcut());
			case QUERY -> {
				QueryViewController controller = resolveQueryController();
				yield controller != null && controller.exportGraphFromShortcut();
			}
			case VALIDATION -> {
				ValidationController controller = resolveValidationController();
				yield controller != null && controller.exportGraphFromShortcut();
			}
			default -> false;
		};
	}

	private boolean adjustGlobalZoom(double delta) {
		ThemeManager themeManager = ThemeManager.getInstance();
		themeManager.setUiScale(themeManager.getUiScale() + delta);
		return true;
	}

	private boolean resetGlobalZoom() {
		ThemeManager.getInstance().setUiScale(ThemeManager.getDefaultUiScale());
		return true;
	}

	private boolean toggleNavigationBar() {
		navController.toggleCollapsed();
		return true;
	}

	private boolean handleQueryValidationShortcut(Predicate<QueryViewController> queryAction,
			Predicate<ValidationController> validationAction) {
		return switch (currentViewId) {
			case QUERY -> {
				QueryViewController controller = resolveQueryController();
				yield controller != null && queryAction.test(controller);
			}
			case VALIDATION -> {
				ValidationController controller = resolveValidationController();
				yield controller != null && validationAction.test(controller);
			}
			default -> false;
		};
	}

	private boolean handleDataShortcut(Predicate<DataViewController> action) {
		if (currentViewId != ViewId.DATA || action == null) {
			return false;
		}
		if (!(viewManager.getView(ViewId.DATA) instanceof DataView dataView)) {
			return false;
		}
		DataViewController controller = dataView.getController();
		return controller != null && action.test(controller);
	}

	private boolean handleGraphShortcut(Predicate<DataViewController> dataAction,
			Predicate<QueryViewController> queryAction, Predicate<ValidationController> validationAction) {
		return switch (currentViewId) {
			case DATA -> handleDataShortcut(dataAction);
			case QUERY -> {
				QueryViewController controller = resolveQueryController();
				yield controller != null && queryAction.test(controller);
			}
			case VALIDATION -> {
				ValidationController controller = resolveValidationController();
				yield controller != null && validationAction.test(controller);
			}
			default -> false;
		};
	}

	private QueryViewController resolveQueryController() {
		if (currentViewId != ViewId.QUERY) {
			return null;
		}
		if (!(viewManager.getView(ViewId.QUERY) instanceof QueryView queryView)) {
			return null;
		}
		return queryView.getController();
	}

	private ValidationController resolveValidationController() {
		if (currentViewId != ViewId.VALIDATION) {
			return null;
		}
		if (!(viewManager.getView(ViewId.VALIDATION) instanceof ValidationView validationView)) {
			return null;
		}
		return validationView.getController();
	}

	/**
	 * Preloads views other than the initial one in a staggered way.
	 *
	 * @param initialView
	 *            the view already loaded
	 */
	private void preloadOtherViewsAsync(ViewId initialView) {
		Thread preloadThread = new Thread(() -> {
			try {
				// Wait for the app to be fully rendered and stable
				Thread.sleep(1000);

				for (ViewId viewId : ViewId.values()) {
					if (viewId != initialView) {
						// Load each view on the FX thread one by one
						Platform.runLater(() -> {
							try {
								viewManager.getView(viewId);
								LOGGER.debug("Background preloaded view: {}", viewId);
							} catch (Exception e) {
								LOGGER.warn("Failed to background preload view: {}", viewId, e);
							}
						});
						// Small gap between loads to keep UI responsive
						Thread.sleep(300);
					}
				}
				LOGGER.debug("All views preloaded successfully in background.");
			} catch (InterruptedException _) {
				Thread.currentThread().interrupt();
				LOGGER.debug("View preloading interrupted");
			}
		}, "ViewPreloader");

		preloadThread.setDaemon(true);
		preloadThread.start();
	}
}
