package fr.inria.corese.gui.utils.fx;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.Region;

/**
 * Shared helpers for file drag-and-drop targets in JavaFX views.
 *
 * <p>
 * Centralizes defensive extraction of dropped files and overlay state updates to
 * keep DnD behavior consistent across views.
 */
public final class FileDragDropSupport {

	private static final Logger LOGGER = Logger.getLogger(FileDragDropSupport.class.getName());

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
}
