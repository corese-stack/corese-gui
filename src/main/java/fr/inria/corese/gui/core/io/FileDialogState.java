package fr.inria.corese.gui.core.io;

import java.io.File;
import java.util.List;
import javafx.stage.FileChooser;

/**
 * Remembers the last directory used in file dialogs.
 */
public final class FileDialogState {

	private static final AppPreferences.Node PREFS = AppPreferences.nodeForClass(FileDialogState.class);
	private static final String KEY_LAST_DIR = "fileDialog.lastDir";

	private FileDialogState() {
		// Utility class
	}

	public static void applyInitialDirectory(FileChooser chooser) {
		applyInitialDirectory(chooser, null);
	}

	public static void applyInitialDirectory(FileChooser chooser, String preferredPath) {
		if (chooser == null) {
			return;
		}
		File preferredDir = resolveDirectory(preferredPath);
		if (preferredDir != null && preferredDir.isDirectory()) {
			chooser.setInitialDirectory(preferredDir);
			return;
		}
		File last = getLastDirectory();
		if (last != null && last.isDirectory()) {
			chooser.setInitialDirectory(last);
		}
	}

	public static void updateLastDirectory(File file) {
		File dir = resolveDirectory(file != null ? file.getAbsolutePath() : null);
		if (dir != null && dir.isDirectory()) {
			PREFS.put(KEY_LAST_DIR, dir.getAbsolutePath());
		}
	}

	public static void updateLastDirectory(List<File> files) {
		if (files == null || files.isEmpty()) {
			return;
		}
		updateLastDirectory(files.get(0));
	}

	private static File getLastDirectory() {
		String path = PREFS.get(KEY_LAST_DIR, null);
		if (path == null || path.isBlank()) {
			return null;
		}
		File dir = new File(path);
		return dir.isDirectory() ? dir : null;
	}

	private static File resolveDirectory(String path) {
		if (path == null || path.isBlank()) {
			return null;
		}
		File file = new File(path);
		if (file.isDirectory()) {
			return file;
		}
		File parent = file.getParentFile();
		return (parent != null && parent.isDirectory()) ? parent : null;
	}
}
