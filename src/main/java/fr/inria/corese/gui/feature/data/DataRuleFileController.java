package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ReasoningService;
import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import fr.inria.corese.gui.feature.data.dialog.DataClearRuleFilesDialog;
import fr.inria.corese.gui.feature.data.dialog.DataReloadRuleFilesDialog;
import fr.inria.corese.gui.feature.data.dialog.DataRulePreviewDialog;
import fr.inria.corese.gui.utils.AppExecutors;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.application.Platform;
import javafx.stage.FileChooser;

/**
 * Handles rule-file actions and UI synchronization for the Data page.
 */
final class DataRuleFileController {

	private static final String RULE_FILE_ALREADY_LOADED_MESSAGE = "Rule file is already loaded:";
	private static final String DROP_WARNING_NONE_ACCEPTED_TEMPLATE = "No compatible files were dropped. %s";
	private static final String DROP_WARNING_IGNORED_TEMPLATE = "Ignored %s. %s";

	private final DataView view;
	private final ReasoningService reasoningService;
	private final Runnable refreshReasoningUi;
	private final Runnable refreshGraphSnapshot;

	DataRuleFileController(DataView view, ReasoningService reasoningService, Runnable refreshReasoningUi,
			Runnable refreshGraphSnapshot) {
		this.view = view;
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
		List<DataView.RuleFileItem> items = ruleStates.stream().map(this::toRuleFileItem).toList();
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
				FileTypeSupport.ruleExtensions(), true);
		fileChooser.getExtensionFilters().addAll(ruleFilter, new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setSelectedExtensionFilter(ruleFilter);

		List<File> files = fileChooser.showOpenMultipleDialog(view.getRoot().getScene().getWindow());
		executeLoadRuleFiles(files);
	}

	private void handleRuleFilesDropped(List<File> droppedFiles) {
		List<File> safeDroppedFiles = droppedFiles == null ? List.of() : List.copyOf(droppedFiles);
		if (safeDroppedFiles.isEmpty()) {
			return;
		}

		List<File> compatibleFiles = new ArrayList<>();
		int ignoredCount = 0;
		for (File file : safeDroppedFiles) {
			if (file != null && file.isFile()
					&& FileTypeSupport.matchesAllowedExtensions(file, FileTypeSupport.ruleExtensions())) {
				compatibleFiles.add(file);
			} else {
				ignoredCount++;
			}
		}

		String expectedExtensionsHint = DataUiMessageUtils
				.buildExpectedExtensionsHint(FileTypeSupport.ruleExtensions());
		if (compatibleFiles.isEmpty()) {
			NotificationWidget.getInstance()
					.showWarning(String.format(DROP_WARNING_NONE_ACCEPTED_TEMPLATE, expectedExtensionsHint));
			return;
		}

		if (ignoredCount > 0) {
			NotificationWidget.getInstance().showWarning(String.format(DROP_WARNING_IGNORED_TEMPLATE,
					DataUiMessageUtils.countLabel(ignoredCount, "dropped file"), expectedExtensionsHint));
		}

		executeLoadRuleFiles(compatibleFiles);
	}

	private void executeLoadRuleFiles(List<File> ruleFiles) {
		List<File> safeFiles = ruleFiles == null
				? List.of()
				: ruleFiles.stream()
						.filter(file -> file != null && file.isFile()
								&& FileTypeSupport.matchesAllowedExtensions(file, FileTypeSupport.ruleExtensions()))
						.toList();
		if (safeFiles.isEmpty()) {
			return;
		}

		FileDialogState.updateLastDirectory(safeFiles);
		AppExecutors.execute(() -> {
			int loadedCount = 0;
			int duplicateCount = 0;
			List<String> errors = new ArrayList<>();
			try {
				for (File file : safeFiles) {
					try {
						reasoningService.addRuleFile(file);
						loadedCount++;
					} catch (IllegalArgumentException e) {
						if (isAlreadyLoadedRuleFileError(e)) {
							duplicateCount++;
						} else {
							errors.add("Rule load failed for " + file.getName() + ": " + e.getMessage());
						}
					} catch (Exception e) {
						errors.add("Rule load failed for " + file.getName() + ": " + e.getMessage());
					}
				}
			} finally {
				int finalLoadedCount = loadedCount;
				int finalDuplicateCount = duplicateCount;
				Platform.runLater(() -> {
					if (finalLoadedCount > 0) {
						NotificationWidget.getInstance().showSuccess(
								"Loaded " + DataUiMessageUtils.countLabel(finalLoadedCount, "rule file") + ".");
					}
					if (finalDuplicateCount > 0) {
						NotificationWidget.getInstance().showInfo("Reasoning", "Skipped "
								+ DataUiMessageUtils.countLabel(finalDuplicateCount, "already loaded rule file") + ".");
					}
					for (String error : errors) {
						NotificationWidget.getInstance().showError(error);
					}
					refreshUiAndGraph();
				});
			}
		});
	}

	private void handleRuleFileToggleRequested(String ruleId, boolean enabled) {
		AppExecutors.execute(() -> {
			try {
				reasoningService.setRuleFileEnabled(ruleId, enabled);
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showSuccess((enabled ? "Enabled rule file." : "Disabled rule file.")));
			} catch (Exception e) {
				Platform.runLater(
						() -> NotificationWidget.getInstance().showError("Rule file update failed: " + e.getMessage()));
			} finally {
				Platform.runLater(this::refreshUiAndGraph);
			}
		});
	}

	private void handleRuleFileReloadRequested(String ruleId) {
		RuleFileState rule = findRuleFile(ruleId);
		if (rule == null) {
			NotificationWidget.getInstance().showWarning("Rule file not found.");
			refreshReasoningUi.run();
			return;
		}

		AppExecutors.execute(() -> {
			try {
				File ruleFile = Path.of(rule.sourcePath()).toFile();
				if (!ruleFile.isFile()) {
					throw new IllegalArgumentException("Rule file no longer exists: " + rule.sourcePath());
				}
				if (rule.enabled()) {
					reasoningService.recomputeEnabledProfiles();
					Platform.runLater(() -> NotificationWidget.getInstance()
							.showSuccess("Reloaded rule file: " + rule.label() + "."));
				} else {
					Platform.runLater(() -> NotificationWidget.getInstance().showInfo("Reasoning",
							"Rule source is available. Enable this rule file to apply it."));
				}
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Failed to reload rule file " + rule.label() + ": " + e.getMessage()));
			} finally {
				Platform.runLater(this::refreshUiAndGraph);
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
			NotificationWidget.getInstance().showInfo("Reasoning", "No rule file selected for reload.");
			refreshReasoningUi.run();
			return;
		}

		AppExecutors.execute(() -> {
			try {
				List<String> missingRules = safeSelection.stream().filter(rule -> !isReadableFile(rule.sourcePath()))
						.map(RuleFileState::label).toList();
				if (!missingRules.isEmpty()) {
					Platform.runLater(() -> NotificationWidget.getInstance().showError(
							"Cannot reload rule files. Missing source file(s): " + String.join(", ", missingRules)));
					return;
				}

				Set<String> selectedRuleIds = safeSelection.stream().map(RuleFileState::id)
						.collect(java.util.stream.Collectors.toUnmodifiableSet());
				reasoningService.applyRuleFileSelection(selectedRuleIds);
				Platform.runLater(() -> NotificationWidget.getInstance().showSuccess(
						"Reloaded " + DataUiMessageUtils.countLabel(safeSelection.size(), "rule file") + "."));
			} catch (Exception e) {
				Platform.runLater(
						() -> NotificationWidget.getInstance().showError("Rule file reload failed: " + e.getMessage()));
			} finally {
				Platform.runLater(this::refreshUiAndGraph);
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
		AppExecutors.execute(() -> {
			try {
				reasoningService.removeAllRuleFiles();
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showSuccess("Removed " + DataUiMessageUtils.countLabel(removedCount, "rule file") + "."));
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Failed to clear rule files: " + e.getMessage()));
			} finally {
				Platform.runLater(this::refreshUiAndGraph);
			}
		});
	}

	private void handleRuleFileViewRequested(String ruleId) {
		RuleFileState rule = findRuleFile(ruleId);
		if (rule == null) {
			NotificationWidget.getInstance().showWarning("Rule file not found.");
			refreshReasoningUi.run();
			return;
		}

		AppExecutors.execute(() -> {
			try {
				String content = Files.readString(Path.of(rule.sourcePath()));
				Platform.runLater(() -> DataRulePreviewDialog.show(rule.label(), rule.sourcePath(), content));
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Failed to open rule " + rule.label() + ": " + e.getMessage()));
			}
		});
	}

	private void handleRuleFileRemoveRequested(String ruleId) {
		if (ruleId == null || ruleId.isBlank()) {
			return;
		}

		RuleFileState rule = findRuleFile(ruleId);
		if (rule == null) {
			NotificationWidget.getInstance().showWarning("Rule file not found.");
			refreshReasoningUi.run();
			return;
		}

		String ruleLabel = (rule.label() == null || rule.label().isBlank())
				? "this rule file"
				: "\"" + rule.label() + "\"";
		String message = "Remove " + ruleLabel + " from this session?";
		ModalService.getInstance().showConfirmation("Remove Rule File", message, "Remove", false,
				() -> executeRuleFileRemoval(rule.id()));
	}

	private void executeRuleFileRemoval(String ruleId) {
		AppExecutors.execute(() -> {
			try {
				reasoningService.removeRuleFile(ruleId);
				Platform.runLater(() -> NotificationWidget.getInstance().showSuccess("Rule file removed."));
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Failed to remove rule file: " + e.getMessage()));
			} finally {
				Platform.runLater(this::refreshUiAndGraph);
			}
		});
	}

	private DataView.RuleFileItem toRuleFileItem(RuleFileState rule) {
		return new DataView.RuleFileItem(rule.id(), rule.label(), rule.sourcePath(), rule.enabled());
	}

	private boolean isAlreadyLoadedRuleFileError(IllegalArgumentException exception) {
		String message = exception == null ? null : exception.getMessage();
		return message != null && message.startsWith(RULE_FILE_ALREADY_LOADED_MESSAGE);
	}

	private RuleFileState findRuleFile(String ruleId) {
		if (ruleId == null || ruleId.isBlank()) {
			return null;
		}
		return reasoningService.snapshotRuleFiles().stream().filter(rule -> ruleId.equals(rule.id())).findFirst()
				.orElse(null);
	}

	private static boolean isReadableFile(String filePath) {
		if (filePath == null || filePath.isBlank()) {
			return false;
		}
		try {
			return Files.isRegularFile(Path.of(filePath));
		} catch (Exception e) {
			return false;
		}
	}

	private void refreshUiAndGraph() {
		refreshReasoningUi.run();
		refreshGraphSnapshot.run();
	}
}
