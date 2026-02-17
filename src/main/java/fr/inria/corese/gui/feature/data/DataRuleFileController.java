package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.DataWorkspaceService;
import fr.inria.corese.gui.core.service.DataWorkspaceStatus;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ReasoningService;
import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import fr.inria.corese.gui.feature.data.dialog.DataClearRuleFilesDialog;
import fr.inria.corese.gui.feature.data.dialog.DataReloadRuleFilesDialog;
import fr.inria.corese.gui.feature.data.dialog.DataRulePreviewDialog;
import fr.inria.corese.gui.feature.data.model.DataRuleFileItem;
import fr.inria.corese.gui.feature.data.support.DataFileSelectionSupport;
import fr.inria.corese.gui.feature.data.support.DataUiMessageUtils;
import fr.inria.corese.gui.utils.AppExecutors;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.stage.FileChooser;

/**
 * Handles rule-file actions and UI synchronization for the Data page.
 */
final class DataRuleFileController {

	private static final String RULE_FILE_ALREADY_LOADED_MESSAGE = "Rule file is already loaded:";
	private static final String RULE_FILE_NOT_FOUND_MESSAGE = "Rule file not found.";
	private static final List<String> RULE_FILE_EXTENSIONS = FileTypeSupport.ruleExtensions();

	private final DataView view;
	private final DataWorkspaceService workspaceService;
	private final ReasoningService reasoningService;
	private final Runnable refreshReasoningUi;
	private final Runnable refreshGraphSnapshot;

	private record RuleFileIssue(String userMessage, Throwable cause) {
		RuleFileIssue {
			userMessage = userMessage == null ? "" : userMessage.trim();
		}
	}

	private record RuleFileLoadResult(int loadedCount, int duplicateCount, List<RuleFileIssue> errors) {
		RuleFileLoadResult {
			errors = errors == null ? List.of() : List.copyOf(errors);
		}
	}

	DataRuleFileController(DataView view, DataWorkspaceService workspaceService, ReasoningService reasoningService,
			Runnable refreshReasoningUi, Runnable refreshGraphSnapshot) {
		this.view = view;
		this.workspaceService = workspaceService;
		this.reasoningService = reasoningService;
		this.refreshReasoningUi = refreshReasoningUi;
		this.refreshGraphSnapshot = refreshGraphSnapshot;
	}

	void initialize() {
		configureToolbar();
		view.setOnRuleFilesDropped(this::handleRuleFilesDropped);
		refreshRuleFileList();
	}

	void refreshRuleFileList() {
		List<RuleFileState> ruleStates = reasoningService.snapshotRuleFiles();
		List<DataRuleFileItem> items = ruleStates.stream().map(this::toRuleFileItem).toList();
		view.updateRuleFiles(items, this::handleRuleFileToggleRequested, this::handleRuleFileReloadRequested,
				this::handleRuleFileViewRequested, this::handleRuleFileRemoveRequested);
		updateToolbarActionStates(ruleStates);
	}

	private void configureToolbar() {
		List<ButtonConfig> buttons = List.of(
				ButtonFactory.custom(ButtonIcon.OPEN_FILE, "Load Rule File (.rul)", this::handleLoadRuleFiles),
				ButtonFactory.custom(ButtonIcon.RELOAD, "Reload Rule Files", this::handleReloadRuleFiles),
				ButtonFactory.custom(ButtonIcon.CLEAR, "Remove All Rule Files", this::handleClearRuleFiles));
		view.setRuleFilesToolbarActions(buttons);
	}

	private void updateToolbarActionStates(List<RuleFileState> ruleStates) {
		boolean hasRules = ruleStates != null && !ruleStates.isEmpty();
		view.setRuleFilesToolbarButtonDisabled(ButtonIcon.OPEN_FILE, false);
		view.setRuleFilesToolbarButtonDisabled(ButtonIcon.RELOAD, !hasRules);
		view.setRuleFilesToolbarButtonDisabled(ButtonIcon.CLEAR, !hasRules);
	}

	private void handleLoadRuleFiles() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Load Rule File");
		FileDialogState.applyInitialDirectory(fileChooser);
		FileChooser.ExtensionFilter ruleFilter = FileTypeSupport.createExtensionFilter("Rule Files (.rul)",
				RULE_FILE_EXTENSIONS, true);
		fileChooser.getExtensionFilters().addAll(ruleFilter, new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setSelectedExtensionFilter(ruleFilter);

		List<File> files = fileChooser.showOpenMultipleDialog(view.getRoot().getScene().getWindow());
		DataFileSelectionSupport.FileSelectionEvaluation selection = DataFileSelectionSupport.evaluateStrict(files,
				RULE_FILE_EXTENSIONS);
		DataFileSelectionSupport.notifyWarnings(selection, expectedRuleExtensionsHint(),
				DataFileSelectionSupport.InputOrigin.SELECTED);
		if (!selection.hasAcceptedFiles()) {
			return;
		}
		executeLoadRuleFiles(selection.acceptedFiles());
	}

	private void handleRuleFilesDropped(List<File> droppedFiles) {
		DataFileSelectionSupport.FileSelectionEvaluation selection = DataFileSelectionSupport
				.evaluateStrict(droppedFiles, RULE_FILE_EXTENSIONS);
		DataFileSelectionSupport.notifyWarnings(selection, expectedRuleExtensionsHint(),
				DataFileSelectionSupport.InputOrigin.DROPPED);
		if (!selection.hasAcceptedFiles()) {
			return;
		}
		executeLoadRuleFiles(selection.acceptedFiles());
	}

	private void executeLoadRuleFiles(List<File> ruleFiles) {
		List<File> safeFiles = ruleFiles == null
				? List.of()
				: ruleFiles.stream().filter(file -> file != null && file.isFile()).toList();
		if (safeFiles.isEmpty()) {
			return;
		}

		FileDialogState.updateLastDirectory(safeFiles);
		runAsyncWithRefresh("Rule Files", "Loading rule file(s)...", () -> {
			RuleFileLoadResult result = loadRuleFilesWithReport(safeFiles);
			return () -> showRuleFileLoadResult(result);
		});
	}

	private void handleRuleFileToggleRequested(String ruleId, boolean enabled) {
		RuleFileState initialRuleState = findRuleFile(ruleId);
		String ruleDisplayName = resolveRuleDisplayName(initialRuleState, ruleId);
		runAsyncWithRefresh("Reasoning", "Updating rule file state...", () -> {
			try {
				DataWorkspaceStatus beforeStatus = workspaceService.getStatus();
				reasoningService.setRuleFileEnabled(ruleId, enabled);
				DataWorkspaceStatus afterStatus = workspaceService.getStatus();
				String stateLabel = enabled ? "enabled" : "disabled";
				String deltaMessage = DataUiMessageUtils.buildTripleDeltaMessage(beforeStatus.inferredTripleCount(),
						afterStatus.inferredTripleCount());
				String message = ruleDisplayName + " " + stateLabel + ". " + deltaMessage;
				return () -> NotificationWidget.getInstance().showSuccess("Rule File", message);
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Rule File Error",
						"Rule file update failed for " + ruleDisplayName + ": " + e.getMessage(), e);
			}
		});
	}

	private void handleRuleFileReloadRequested(String ruleId) {
		RuleFileState rule = requireRuleFile(ruleId);
		if (rule == null) {
			return;
		}

		runAsyncWithRefresh("Rule Reload", "Reloading rule file...", () -> {
			try {
				File ruleFile = Path.of(rule.sourcePath()).toFile();
				if (!ruleFile.isFile()) {
					throw new IllegalArgumentException("Rule file no longer exists: " + rule.sourcePath());
				}
				if (rule.enabled()) {
					reasoningService.recomputeEnabledProfiles();
					return () -> NotificationWidget.getInstance().showSuccess("Rule File",
							"Reloaded " + resolveRuleDisplayName(rule, rule.id()) + ".");
				}
				return () -> NotificationWidget.getInstance().showInfo("Rule File",
						"Rule source is available. Enable this rule file to apply it.");
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Rule Reload Error",
						"Failed to reload rule file " + rule.label() + ": " + e.getMessage(), e);
			}
		});
	}

	private void handleReloadRuleFiles() {
		List<RuleFileState> rules = reasoningService.snapshotRuleFiles();
		if (rules.isEmpty()) {
			NotificationWidget.getInstance().showWarning("No rule files to reload.");
			refreshReasoningUi.run();
			return;
		}

		DataReloadRuleFilesDialog.show(rules, this::executeReloadRuleFiles);
	}

	private void executeReloadRuleFiles(List<RuleFileState> selectedRules) {
		List<RuleFileState> safeSelection = selectedRules == null
				? List.of()
				: selectedRules.stream().filter(rule -> rule != null && rule.id() != null && !rule.id().isBlank())
						.toList();
		if (safeSelection.isEmpty()) {
			NotificationWidget.getInstance().showInfo("Rule Files", "No rule file selected for reload.");
			refreshReasoningUi.run();
			return;
		}

		runAsyncWithRefresh("Rule Reload", "Reloading selected rule files...", () -> {
			try {
				List<String> missingRules = safeSelection.stream().filter(rule -> !isReadableFile(rule.sourcePath()))
						.map(RuleFileState::label).toList();
				if (!missingRules.isEmpty()) {
					return () -> NotificationWidget.getInstance().showError(
							"Cannot reload rule files. Missing source file(s): " + String.join(", ", missingRules));
				}

				Set<String> selectedRuleIds = safeSelection.stream().map(RuleFileState::id)
						.collect(java.util.stream.Collectors.toUnmodifiableSet());
				reasoningService.applyRuleFileSelection(selectedRuleIds);
				return () -> NotificationWidget.getInstance().showSuccess("Rule Files",
						"Reloaded " + DataUiMessageUtils.countLabel(safeSelection.size(), "rule file") + ".");
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Rule Reload Error",
						"Rule file reload failed: " + e.getMessage(), e);
			}
		});
	}

	private void handleClearRuleFiles() {
		List<RuleFileState> ruleStates = reasoningService.snapshotRuleFiles();
		if (ruleStates.isEmpty()) {
			NotificationWidget.getInstance().showWarning("No rule files to clear.");
			refreshReasoningUi.run();
			return;
		}

		int removedCount = ruleStates.size();
		DataClearRuleFilesDialog.show(() -> executeClearRuleFiles(removedCount));
	}

	private void executeClearRuleFiles(int removedCount) {
		runAsyncWithRefresh("Rule Files", "Removing all rule files...", () -> {
			try {
				reasoningService.removeAllRuleFiles();
				return () -> NotificationWidget.getInstance().showSuccess("Rule Files",
						"Removed " + DataUiMessageUtils.countLabel(removedCount, "rule file") + ".");
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Rule File Error",
						"Failed to clear rule files: " + e.getMessage(), e);
			}
		});
	}

	private void handleRuleFileViewRequested(String ruleId) {
		RuleFileState rule = requireRuleFile(ruleId);
		if (rule == null) {
			return;
		}

		AppExecutors.execute(() -> {
			try {
				String content = Files.readString(Path.of(rule.sourcePath()));
				Platform.runLater(() -> DataRulePreviewDialog.show(rule.label(), rule.sourcePath(), content));
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance().showErrorWithDetails("Rule Preview Error",
						"Failed to open rule " + rule.label() + ": " + e.getMessage(), e));
			}
		});
	}

	private void handleRuleFileRemoveRequested(String ruleId) {
		if (ruleId == null || ruleId.isBlank()) {
			return;
		}

		RuleFileState rule = requireRuleFile(ruleId);
		if (rule == null) {
			return;
		}

		String ruleLabel = (rule.label() == null || rule.label().isBlank())
				? "this rule file"
				: "\"" + rule.label() + "\"";
		String message = "Remove " + ruleLabel + " from this session?";
		ModalService.getInstance().showConfirmation("Remove Rule File", message, "Remove", false,
				() -> executeRuleFileRemoval(rule.id(), resolveRuleDisplayName(rule, rule.id())));
	}

	private void executeRuleFileRemoval(String ruleId, String ruleDisplayName) {
		runAsyncWithRefresh("Rule Files", "Removing rule file...", () -> {
			try {
				reasoningService.removeRuleFile(ruleId);
				return () -> NotificationWidget.getInstance().showSuccess("Rule File", ruleDisplayName + " removed.");
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Rule File Error",
						"Failed to remove " + ruleDisplayName + ": " + e.getMessage(), e);
			}
		});
	}

	private DataRuleFileItem toRuleFileItem(RuleFileState rule) {
		return new DataRuleFileItem(rule.id(), rule.label(), rule.sourcePath(), rule.enabled());
	}

	private boolean isAlreadyLoadedRuleFileError(IllegalArgumentException exception) {
		String message = exception == null ? null : exception.getMessage();
		return message != null && message.startsWith(RULE_FILE_ALREADY_LOADED_MESSAGE);
	}

	private RuleFileLoadResult loadRuleFilesWithReport(List<File> ruleFiles) {
		int loadedCount = 0;
		int duplicateCount = 0;
		List<RuleFileIssue> errors = new ArrayList<>();

		for (File file : ruleFiles) {
			try {
				reasoningService.addRuleFile(file);
				loadedCount++;
			} catch (IllegalArgumentException e) {
				if (isAlreadyLoadedRuleFileError(e)) {
					duplicateCount++;
				} else {
					errors.add(new RuleFileIssue("Rule load failed for " + file.getName() + ": " + e.getMessage(), e));
				}
			} catch (Exception e) {
				errors.add(new RuleFileIssue("Rule load failed for " + file.getName() + ": " + e.getMessage(), e));
			}
		}

		return new RuleFileLoadResult(loadedCount, duplicateCount, errors);
	}

	private void showRuleFileLoadResult(RuleFileLoadResult result) {
		if (result.loadedCount() > 0) {
			NotificationWidget.getInstance().showSuccess("Rule Files",
					"Loaded " + DataUiMessageUtils.countLabel(result.loadedCount(), "rule file") + ".");
		}
		if (result.duplicateCount() > 0) {
			NotificationWidget.getInstance().showInfo("Rule Files", "Skipped "
					+ DataUiMessageUtils.countLabel(result.duplicateCount(), "already loaded rule file") + ".");
		}
		for (RuleFileIssue error : result.errors()) {
			if (error == null || error.userMessage().isBlank()) {
				continue;
			}
			if (error.cause() != null) {
				NotificationWidget.getInstance().showErrorWithDetails("Rule File Error", error.userMessage(),
						error.cause());
			} else {
				NotificationWidget.getInstance().showError(error.userMessage());
			}
		}
	}

	private String expectedRuleExtensionsHint() {
		return DataUiMessageUtils.buildExpectedExtensionsHint(RULE_FILE_EXTENSIONS);
	}

	private RuleFileState findRuleFile(String ruleId) {
		if (ruleId == null || ruleId.isBlank()) {
			return null;
		}
		return reasoningService.snapshotRuleFiles().stream().filter(rule -> ruleId.equals(rule.id())).findFirst()
				.orElse(null);
	}

	private RuleFileState requireRuleFile(String ruleId) {
		RuleFileState rule = findRuleFile(ruleId);
		if (rule == null) {
			NotificationWidget.getInstance().showWarning(RULE_FILE_NOT_FOUND_MESSAGE);
			refreshReasoningUi.run();
		}
		return rule;
	}

	private static String resolveRuleDisplayName(RuleFileState rule, String fallbackRuleId) {
		if (rule != null && rule.label() != null && !rule.label().isBlank()) {
			return "\"" + rule.label() + "\"";
		}
		if (fallbackRuleId != null && !fallbackRuleId.isBlank()) {
			return "\"" + fallbackRuleId + "\"";
		}
		return "Rule file";
	}

	private static boolean isReadableFile(String filePath) {
		if (filePath == null || filePath.isBlank()) {
			return false;
		}
		try {
			return Files.isRegularFile(Path.of(filePath));
		} catch (RuntimeException _) {
			return false;
		}
	}

	private void refreshUiAndGraph() {
		refreshReasoningUi.run();
		refreshGraphSnapshot.run();
	}

	private void runAsyncWithRefresh(String loadingTitle, String loadingMessage, Supplier<Runnable> task) {
		NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading(loadingTitle,
				loadingMessage);
		AppExecutors.execute(() -> {
			Runnable uiFollowUp = null;
			try {
				if (task != null) {
					uiFollowUp = task.get();
				}
			} finally {
				Runnable finalUiFollowUp = uiFollowUp;
				// Close loading first so resulting notifications do not overlap the loader
				// exit animation.
				loadingHandle.closeThen(() -> {
					if (finalUiFollowUp != null) {
						finalUiFollowUp.run();
					}
					refreshUiAndGraph();
				});
			}
		});
	}
}
