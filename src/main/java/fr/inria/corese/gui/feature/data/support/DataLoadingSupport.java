package fr.inria.corese.gui.feature.data.support;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.service.DataWorkspaceService;
import fr.inria.corese.gui.core.service.ReasoningService;
import java.io.File;
import java.util.List;

/**
 * Shared loading helpers for Data page file/URI operations.
 */
public final class DataLoadingSupport {

	private DataLoadingSupport() {
		throw new AssertionError("Utility class");
	}

	public static int loadFiles(DataWorkspaceService workspaceService, List<File> files, List<String> errors) {
		if (workspaceService == null || files == null || files.isEmpty()) {
			return 0;
		}
		int loadedCount = 0;
		for (File file : files) {
			try {
				workspaceService.loadFile(file);
				loadedCount++;
			} catch (Exception ex) {
				if (errors != null) {
					errors.add("File load failed for " + file.getName() + ": " + ex.getMessage());
				}
			}
		}
		return loadedCount;
	}

	public static int loadUris(DataWorkspaceService workspaceService, List<String> uris, List<String> errors) {
		if (workspaceService == null || uris == null || uris.isEmpty()) {
			return 0;
		}
		int loadedCount = 0;
		for (String uri : uris) {
			try {
				workspaceService.loadUri(uri);
				loadedCount++;
			} catch (Exception ex) {
				if (errors != null) {
					errors.add("URI load failed for " + uri + ": " + ex.getMessage());
				}
			}
		}
		return loadedCount;
	}

	public static void recomputeReasoning(ReasoningService reasoningService, List<String> errors) {
		if (reasoningService == null || !reasoningService.hasAnyEnabledProfile()) {
			return;
		}
		try {
			reasoningService.recomputeEnabledProfiles();
		} catch (Exception e) {
			if (errors != null) {
				errors.add("Reasoning recompute failed: " + e.getMessage());
			}
		}
	}

	public static void showErrors(List<String> errors) {
		if (errors == null || errors.isEmpty()) {
			return;
		}
		for (String error : errors) {
			NotificationWidget.getInstance().showError(error);
		}
	}
}
