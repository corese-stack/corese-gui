package fr.inria.corese.gui.feature.editor.tab.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import javafx.scene.control.Tab;

/**
 * File matching helpers for tab-based editors.
 */
public final class TabEditorFileMatcher {

	private TabEditorFileMatcher() {
		throw new AssertionError("Utility class");
	}

	public static Tab findOpenTabByFile(List<Tab> tabs, Function<Tab, String> filePathResolver, File file) {
		if (tabs == null || filePathResolver == null || file == null) {
			return null;
		}
		Path normalizedPath = normalizePathSafely(file);
		String absolutePath = file.getAbsolutePath();

		for (Tab tab : tabs) {
			String tabFilePath = filePathResolver.apply(tab);
			if (tabFilePath != null && isMatchingFile(normalizedPath, absolutePath, tabFilePath)) {
				return tab;
			}
		}
		return null;
	}

	private static Path normalizePathSafely(File file) {
		try {
			return file.toPath().toRealPath();
		} catch (IOException _) {
			return null;
		}
	}

	private static boolean isMatchingFile(Path normalizedSearchPath, String absoluteSearchPath, String tabFilePath) {
		if (normalizedSearchPath != null) {
			return matchesNormalizedPath(normalizedSearchPath, tabFilePath);
		}
		return absoluteSearchPath.equals(tabFilePath);
	}

	private static boolean matchesNormalizedPath(Path normalizedPath, String tabFilePath) {
		try {
			Path tabPath = Path.of(tabFilePath).toRealPath();
			return normalizedPath.equals(tabPath);
		} catch (IOException _) {
			return false;
		}
	}
}
