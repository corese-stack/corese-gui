package fr.inria.corese.gui.utils.fx;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Shared helpers for file drag-and-drop targets in JavaFX views.
 *
 * <p>
 * Centralizes defensive extraction of dropped files and overlay state updates to
 * keep DnD behavior consistent across views.
 */
public final class FileDragDropSupport {

	private static final Logger LOGGER = Logger.getLogger(FileDragDropSupport.class.getName());
	private static final String OVERLAY_RESET_GUARD_KEY = FileDragDropSupport.class.getName() + ".overlayResetGuard";

	private FileDragDropSupport() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Extracts files from a drag event.
	 *
	 * <p>
	 * Returns an empty list when event/dragboard is null or when JavaFX throws
	 * runtime exceptions while probing mime types.
	 */
	public static List<File> extractDraggedFiles(DragEvent event) {
		if (event == null) {
			return List.of();
		}
		try {
			Dragboard dragboard = event.getDragboard();
			if (dragboard == null) {
				return List.of();
			}
			List<File> files = dragboard.getFiles();
			if (files == null || files.isEmpty()) {
				return List.of();
			}
			return List.copyOf(files);
		} catch (RuntimeException _) {
			// JavaFX/GTK can throw runtime exceptions while probing clipboard mime types
			// during DnD.
			return List.of();
		}
	}

	/**
	 * Returns true when the drag event carries at least one file.
	 */
	public static boolean hasFilesInDragboard(DragEvent event) {
		return !extractDraggedFiles(event).isEmpty();
	}

	/**
	 * Installs shared guards that clear stale drop overlays when a drag sequence
	 * ends or the hosting view loses hover/window attachment.
	 */
	public static void installOverlayResetGuards(Node root, Runnable clearOverlays) {
		if (root == null || clearOverlays == null || root.getProperties().containsKey(OVERLAY_RESET_GUARD_KEY)) {
			return;
		}
		root.getProperties().put(OVERLAY_RESET_GUARD_KEY, new OverlayResetGuard(root, clearOverlays));
	}

	/**
	 * Applies active/inactive visual state for a drop overlay.
	 */
	public static void setOverlayActive(Region overlay, String activeStyleClass, boolean active) {
		if (overlay == null || activeStyleClass == null || activeStyleClass.isBlank()) {
			return;
		}
		if (active) {
			if (!overlay.getStyleClass().contains(activeStyleClass)) {
				overlay.getStyleClass().add(activeStyleClass);
			}
			overlay.setManaged(true);
			overlay.setVisible(true);
			return;
		}
		overlay.getStyleClass().remove(activeStyleClass);
		overlay.setManaged(false);
		overlay.setVisible(false);
	}

	/**
	 * Returns true when the event target node is within the given container.
	 */
	public static boolean isTargetWithin(Object target, Node container) {
		if (!(target instanceof Node node) || container == null) {
			return false;
		}
		for (Node current = node; current != null; current = current.getParent()) {
			if (current == container) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Safely dispatches dropped files to a callback.
	 *
	 * @return true if files were present and callback completed without runtime
	 *         exception
	 */
	public static boolean dispatchDroppedFiles(DragEvent event, Consumer<List<File>> dropHandler) {
		if (dropHandler == null) {
			return false;
		}
		List<File> draggedFiles = extractDraggedFiles(event);
		if (draggedFiles.isEmpty()) {
			return false;
		}
		try {
			dropHandler.accept(draggedFiles);
			return true;
		} catch (RuntimeException exception) {
			LOGGER.log(Level.WARNING, "File drop handler failed.", exception);
			return false;
		}
	}

	private static final class OverlayResetGuard {

		private final Runnable overlayResetAction;
		private Scene boundScene;
		private Window boundWindow;
		private final ChangeListener<Boolean> hoverListener = (obs, previousHover, hovered) -> {
			if (!hovered) {
				clearOverlayState();
			}
		};
		private final ChangeListener<Scene> sceneListener = (obs, previousScene, currentScene) -> {
			bindScene(currentScene);
			clearOverlayState();
		};
		private final ChangeListener<Window> sceneWindowListener = (obs, previousWindow, currentWindow) -> {
			bindWindow(currentWindow);
			clearOverlayState();
		};
		private final ChangeListener<Boolean> windowFocusListener = (obs, previousFocused, focused) -> {
			if (!focused) {
				clearOverlayState();
			}
		};
		private final ChangeListener<Boolean> windowShowingListener = (obs, previousShowing, showing) -> {
			if (!showing) {
				clearOverlayState();
			}
		};

		private OverlayResetGuard(Node root, Runnable clearOverlays) {
			this.overlayResetAction = clearOverlays;
			root.addEventFilter(DragEvent.DRAG_EXITED_TARGET, event -> clearOverlayState());
			root.addEventFilter(DragEvent.DRAG_DONE, event -> clearOverlayState());
			root.addEventFilter(DragEvent.DRAG_DROPPED, event -> clearOverlayState());
			root.hoverProperty().addListener(hoverListener);
			root.sceneProperty().addListener(sceneListener);
			bindScene(root.getScene());
		}

		private void clearOverlayState() {
			overlayResetAction.run();
		}

		private void bindScene(Scene scene) {
			if (scene == boundScene) {
				return;
			}
			if (boundScene != null) {
				boundScene.windowProperty().removeListener(sceneWindowListener);
			}
			boundScene = scene;
			if (boundScene != null) {
				boundScene.windowProperty().addListener(sceneWindowListener);
				bindWindow(boundScene.getWindow());
				return;
			}
			bindWindow(null);
		}

		private void bindWindow(Window window) {
			if (window == boundWindow) {
				return;
			}
			if (boundWindow != null) {
				boundWindow.focusedProperty().removeListener(windowFocusListener);
				boundWindow.showingProperty().removeListener(windowShowingListener);
			}
			boundWindow = window;
			if (boundWindow == null) {
				return;
			}
			boundWindow.focusedProperty().addListener(windowFocusListener);
			boundWindow.showingProperty().addListener(windowShowingListener);
		}
	}
}
