package fr.inria.corese.gui.feature.data.support;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.service.DataWorkspaceService;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ReasoningService;
import java.io.File;
import java.util.List;

/**
 * Shared loading helpers for Data page file/URI operations.
 */
public final class DataLoadingSupport {

	/**
	 * Structured issue for data loading operations.
	 *
	 * @param userMessage
	 *            short user-facing message
	 * @param cause
	 *            underlying cause (optional)
	 */
	public record OperationIssue(String userMessage, Throwable cause) {
		public OperationIssue {
			userMessage = userMessage == null ? "" : userMessage.trim();
		}
	}

	private DataLoadingSupport() {
		throw new AssertionError("Utility class");
	}

	public static int loadFiles(DataWorkspaceService workspaceService, List<File> files, List<OperationIssue> errors) {
		if (workspaceService == null || files == null || files.isEmpty()) {
			return 0;
		}
		int loadedCount = 0;
		for (File file : files) {
			try {
				workspaceService.loadFile(file);
				loadedCount++;
			} catch (Throwable ex) {
				rethrowIfFatal(ex);
				if (errors != null) {
					errors.add(
							new OperationIssue("File load failed for " + file.getName() + ": " + ex.getMessage(), ex));
				}
			}
		}
		return loadedCount;
	}

	public static int loadUris(DataWorkspaceService workspaceService, List<String> uris, List<OperationIssue> errors) {
		if (workspaceService == null || uris == null || uris.isEmpty()) {
			return 0;
		}
		int loadedCount = 0;
		for (String uri : uris) {
			try {
				workspaceService.loadUri(uri);
				loadedCount++;
			} catch (Throwable ex) {
				rethrowIfFatal(ex);
				if (errors != null) {
					errors.add(new OperationIssue("URI load failed for " + uri + ": " + ex.getMessage(), ex));
				}
			}
		}
		return loadedCount;
	}

	public static void recomputeReasoning(ReasoningService reasoningService, List<OperationIssue> errors) {
		if (reasoningService == null || !reasoningService.hasAnyEnabledProfile()) {
			return;
		}
		try {
			reasoningService.recomputeEnabledProfiles();
		} catch (Throwable e) {
			rethrowIfFatal(e);
			if (errors != null) {
				errors.add(new OperationIssue("Reasoning recompute failed: " + e.getMessage(), e));
			}
		}
	}

	public static void showErrors(List<OperationIssue> errors) {
		if (errors == null || errors.isEmpty()) {
			return;
		}
		for (OperationIssue issue : errors) {
			if (issue == null || issue.userMessage().isBlank()) {
				continue;
			}
			if (issue.cause() != null) {
				NotificationWidget.getInstance().showErrorWithDetails("Data Load Error", issue.userMessage(),
						issue.cause(), true);
			} else {
				NotificationWidget.getInstance().showPersistentError("Data Load Error", issue.userMessage());
			}
		}
	}

	public static void showPrimaryErrorModalIfNothingLoaded(String title, int loadedCount,
			List<OperationIssue> errors) {
		if (loadedCount > 0 || errors == null || errors.isEmpty()) {
			return;
		}
		OperationIssue primaryIssue = firstDisplayableIssue(errors);
		if (primaryIssue == null || countDisplayableIssues(errors) != 1) {
			return;
		}

		String safeTitle = title == null || title.isBlank() ? "Data Load Error" : title.trim();
		if (primaryIssue.cause() != null) {
			ModalService.getInstance().showException(safeTitle, primaryIssue.userMessage(), primaryIssue.cause());
		} else {
			ModalService.getInstance().showError(safeTitle, primaryIssue.userMessage());
		}
	}

	private static OperationIssue firstDisplayableIssue(List<OperationIssue> errors) {
		if (errors == null) {
			return null;
		}
		for (OperationIssue issue : errors) {
			if (issue != null && !issue.userMessage().isBlank()) {
				return issue;
			}
		}
		return null;
	}

	private static int countDisplayableIssues(List<OperationIssue> errors) {
		if (errors == null) {
			return 0;
		}
		int count = 0;
		for (OperationIssue issue : errors) {
			if (issue != null && !issue.userMessage().isBlank()) {
				count++;
			}
		}
		return count;
	}

	private static void rethrowIfFatal(Throwable throwable) {
		if (throwable instanceof VirtualMachineError error) {
			throw error;
		}
		if (throwable instanceof Error error && "java.lang.ThreadDeath".equals(error.getClass().getName())) {
			throw error;
		}
		if (throwable instanceof LinkageError error) {
			throw error;
		}
	}
}
