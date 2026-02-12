package fr.inria.corese.gui.feature.data;

import atlantafx.base.controls.ToggleSwitch;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.ExportHelper;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataWorkspaceService;
import fr.inria.corese.gui.core.service.DataWorkspaceStatus;
import fr.inria.corese.gui.core.service.DefaultDataWorkspaceService;
import fr.inria.corese.gui.core.service.DefaultReasoningService;
import fr.inria.corese.gui.core.service.GraphMutationBus;
import fr.inria.corese.gui.core.service.GraphMutationEvent;
import fr.inria.corese.gui.core.service.ReasoningProfile;
import fr.inria.corese.gui.core.service.ReasoningService;
import fr.inria.corese.gui.feature.data.dialog.DataClearGraphDialog;
import fr.inria.corese.gui.feature.data.dialog.DataReloadSourcesDialog;
import fr.inria.corese.gui.feature.data.dialog.DataUriLoadDialog;
import fr.inria.corese.gui.utils.AppExecutors;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Data page.
 *
 * <p>
 * Coordinates UI actions with {@link DataWorkspaceService}, keeping GUI logic
 * decoupled from core graph internals.
 */
public class DataViewController implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataViewController.class);
	private static final String DROP_WARNING_NONE_ACCEPTED_TEMPLATE = "No compatible files were dropped. %s";
	private static final String DROP_WARNING_IGNORED_TEMPLATE = "Ignored %s. %s";

	private final DataView view;
	private final DataWorkspaceService workspaceService;
	private final ReasoningService reasoningService;
	private final GraphMutationBus mutationBus;
	private final DataRuleFileController ruleFileController;
	private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);
	private final AtomicBoolean reasoningRecomputeScheduled = new AtomicBoolean(false);
	private final AtomicBoolean dataOperationInProgress = new AtomicBoolean(false);
	private final AtomicBoolean syncingReasoningUi = new AtomicBoolean(false);
	private final Map<ReasoningProfile, ToggleSwitch> reasoningToggles = new EnumMap<>(ReasoningProfile.class);

	private AutoCloseable mutationSubscription;

	private static final long GRAPH_REFRESH_DEBOUNCE_MS = 120L;
	private static final long REASONING_REFRESH_DEBOUNCE_MS = 260L;

	public DataViewController(DataView view) {
		this.view = view;
		this.workspaceService = DefaultDataWorkspaceService.getInstance();
		this.reasoningService = DefaultReasoningService.getInstance();
		this.mutationBus = GraphMutationBus.getInstance();
		this.ruleFileController = new DataRuleFileController(view, reasoningService, this::refreshReasoningUiState,
				this::refreshGraphSnapshot);
		initialize();
	}

	private void initialize() {
		configureToolbar();
		configureGraphSurface();
		configureReasoningControls();
		subscribeToGraphMutations();
		refreshGraphSnapshot();
	}

	private void configureGraphSurface() {
		view.configureGraphEmptyState(this::handleLoadFile, this::handleLoadUri);
		view.setOnGraphFilesDropped(this::handleGraphFilesDropped);
	}

	private void configureToolbar() {
		List<ButtonConfig> buttons = List.of(ButtonFactory.openFile(this::handleLoadFile),
				ButtonFactory.openUri(this::handleLoadUri), ButtonFactory.reload(this::handleReloadSources),
				ButtonFactory.exportData(this::handleExportData), ButtonFactory.clearGraph(this::handleClearGraph),
				ButtonFactory.exportGraph(this::handleExportVisualGraph),
				ButtonFactory.resetLayout(view.getGraphWidget()::resetLayout),
				ButtonFactory.zoomIn(view.getGraphWidget()::zoomIn),
				ButtonFactory.zoomOut(view.getGraphWidget()::zoomOut));

		view.setToolbarActions(buttons);
		view.insertToolbarSeparatorAfter(ButtonIcon.CLEAR);
		view.markToolbarButtonDanger(ButtonIcon.CLEAR);
		updateToolbarActionStates();
	}

	private void configureReasoningControls() {
		List<ToggleSwitch> builtInToggles = view.getBuiltInRuleToggles();
		reasoningToggles.put(ReasoningProfile.RDFS, builtInToggles.get(0));
		reasoningToggles.put(ReasoningProfile.OWL_RL, builtInToggles.get(1));
		reasoningToggles.put(ReasoningProfile.OWL_RL_LITE, builtInToggles.get(2));
		reasoningToggles.put(ReasoningProfile.OWL_RL_EXT, builtInToggles.get(3));

		for (Map.Entry<ReasoningProfile, ToggleSwitch> entry : reasoningToggles.entrySet()) {
			ReasoningProfile profile = entry.getKey();
			ToggleSwitch toggle = entry.getValue();
			toggle.setSelected(reasoningService.isEnabled(profile));
			toggle.selectedProperty().addListener((observable, previous, selected) -> {
				if (syncingReasoningUi.get()) {
					return;
				}
				handleReasoningToggle(profile, Boolean.TRUE.equals(selected));
			});
		}
		ruleFileController.initialize();
	}

	private void subscribeToGraphMutations() {
		mutationSubscription = mutationBus.subscribe(event -> {
			scheduleGraphRefresh();
			maybeScheduleReasoningRecompute(event);
		});
	}

	private void maybeScheduleReasoningRecompute(GraphMutationEvent event) {
		if (event == null) {
			return;
		}
		if (!reasoningService.hasAnyEnabledProfile()) {
			return;
		}
		if (dataOperationInProgress.get()) {
			return;
		}
		if (event.source() != GraphMutationEvent.Source.GRAPH_LISTENER || !event.hasStructuralChange()) {
			return;
		}
		scheduleReasoningRecompute();
	}

	private void scheduleReasoningRecompute() {
		if (!reasoningRecomputeScheduled.compareAndSet(false, true)) {
			return;
		}
		AppExecutors.execute(() -> {
			try {
				Thread.sleep(REASONING_REFRESH_DEBOUNCE_MS);
				reasoningService.recomputeEnabledProfiles();
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Reasoning recompute failed: " + e.getMessage()));
			} finally {
				reasoningRecomputeScheduled.set(false);
				Platform.runLater(() -> {
					refreshReasoningUiState();
					refreshGraphSnapshot();
				});
			}
		});
	}

	private void scheduleGraphRefresh() {
		if (!refreshScheduled.compareAndSet(false, true)) {
			return;
		}
		AppExecutors.execute(() -> {
			try {
				Thread.sleep(GRAPH_REFRESH_DEBOUNCE_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			Platform.runLater(() -> {
				refreshScheduled.set(false);
				refreshGraphSnapshot();
			});
		});
	}

	private void refreshGraphSnapshot() {
		try {
			DataWorkspaceStatus status = workspaceService.getStatus();
			String jsonLdSnapshot = workspaceService.getGraphSnapshotJsonLd();
			boolean hasRenderableGraph = jsonLdSnapshot != null && !jsonLdSnapshot.isBlank();
			if (!hasRenderableGraph) {
				view.getGraphWidget().clear();
			} else {
				view.getGraphWidget().displayGraph(jsonLdSnapshot);
			}
			view.setGraphEmptyStateVisible(!hasRenderableGraph);
			view.updateStatus(status);
			updateToolbarActionStates();
		} catch (Exception e) {
			LOGGER.warn("Failed to refresh graph snapshot", e);
		}
	}

	private void refreshReasoningUiState() {
		syncReasoningToggleStates();
		ruleFileController.refreshRuleFileList();
	}

	private void updateToolbarActionStates() {
		boolean hasData = workspaceService.hasData();
		boolean hasTrackedSources = !workspaceService.getTrackedSources().isEmpty();

		view.setToolbarButtonDisabled(ButtonIcon.RELOAD, !hasTrackedSources);
		view.setToolbarButtonDisabled(ButtonIcon.EXPORT_DATA, !hasData);
		view.setToolbarButtonDisabled(ButtonIcon.EXPORT, !hasData);
		view.setToolbarButtonDisabled(ButtonIcon.CLEAR, !hasData);
		view.setToolbarButtonDisabled(ButtonIcon.LAYOUT_FORCE, !hasData);
		view.setToolbarButtonDisabled(ButtonIcon.ZOOM_IN, !hasData);
		view.setToolbarButtonDisabled(ButtonIcon.ZOOM_OUT, !hasData);
	}

	private void handleLoadFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open RDF Data File");
		FileDialogState.applyInitialDirectory(fileChooser);
		FileChooser.ExtensionFilter rdfFilter = FileTypeSupport.createExtensionFilter("RDF Files",
				FileTypeSupport.rdfExtensions(), true);
		fileChooser.getExtensionFilters().addAll(rdfFilter, new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setSelectedExtensionFilter(rdfFilter);

		List<File> files = fileChooser.showOpenMultipleDialog(view.getRoot().getScene().getWindow());
		executeLoadFiles(files);
	}

	private void handleGraphFilesDropped(List<File> droppedFiles) {
		List<File> safeDroppedFiles = droppedFiles == null ? List.of() : List.copyOf(droppedFiles);
		if (safeDroppedFiles.isEmpty()) {
			return;
		}

		List<File> compatibleFiles = new ArrayList<>();
		int ignoredCount = 0;
		for (File file : safeDroppedFiles) {
			if (file != null && file.isFile()
					&& FileTypeSupport.matchesAllowedExtensions(file, FileTypeSupport.rdfExtensions())) {
				compatibleFiles.add(file);
			} else {
				ignoredCount++;
			}
		}

		String expectedExtensionsHint = DataUiMessageUtils.buildExpectedExtensionsHint(FileTypeSupport.rdfExtensions());
		if (compatibleFiles.isEmpty()) {
			NotificationWidget.getInstance()
					.showWarning(String.format(DROP_WARNING_NONE_ACCEPTED_TEMPLATE, expectedExtensionsHint));
			return;
		}

		if (ignoredCount > 0) {
			NotificationWidget.getInstance().showWarning(String.format(DROP_WARNING_IGNORED_TEMPLATE,
					DataUiMessageUtils.countLabel(ignoredCount, "dropped file"), expectedExtensionsHint));
		}

		executeLoadFiles(compatibleFiles);
	}

	private void executeLoadFiles(List<File> files) {
		List<File> safeFiles = files == null
				? List.of()
				: files.stream().filter(file -> file != null && file.isFile()).toList();
		if (safeFiles.isEmpty()) {
			return;
		}

		FileDialogState.updateLastDirectory(safeFiles);
		AppExecutors.execute(() -> {
			int successCount = 0;
			int graphTripleCount = workspaceService.getTripleCount();
			dataOperationInProgress.set(true);
			try {
				for (File file : safeFiles) {
					try {
						workspaceService.loadFile(file);
						successCount++;
					} catch (Exception ex) {
						String message = "File load failed for " + file.getName() + ": " + ex.getMessage();
						Platform.runLater(() -> NotificationWidget.getInstance().showError(message));
					}
				}

				int loadedCount = successCount;
				if (loadedCount > 0 && reasoningService.hasAnyEnabledProfile()) {
					try {
						reasoningService.recomputeEnabledProfiles();
					} catch (Exception e) {
						Platform.runLater(() -> NotificationWidget.getInstance()
								.showError("Reasoning recompute failed: " + e.getMessage()));
					}
				}
			} finally {
				int loadedCount = successCount;
				graphTripleCount = workspaceService.getTripleCount();
				int finalTripleCount = graphTripleCount;
				Platform.runLater(() -> {
					if (loadedCount > 0) {
						NotificationWidget.getInstance()
								.showSuccess("Loaded " + DataUiMessageUtils.countLabel(loadedCount, "file")
										+ ". Graph now has " + DataUiMessageUtils.countLabel(finalTripleCount, "triple")
										+ ".");
					}
				});
				finishDataOperation();
			}
		});
	}

	private void handleLoadUri() {
		DataUriLoadDialog.show(this::executeLoadUris);
	}

	private void executeLoadUris(List<String> uris) {
		List<String> urisToLoad = uris == null ? List.of() : List.copyOf(uris);
		AppExecutors.execute(() -> {
			int successCount = 0;
			int graphTripleCount = workspaceService.getTripleCount();
			List<String> errors = new ArrayList<>();
			dataOperationInProgress.set(true);
			try {
				for (String uri : urisToLoad) {
					try {
						workspaceService.loadUri(uri);
						successCount++;
					} catch (Exception ex) {
						errors.add("URI load failed for " + uri + ": " + ex.getMessage());
					}
				}
				if (successCount > 0 && reasoningService.hasAnyEnabledProfile()) {
					try {
						reasoningService.recomputeEnabledProfiles();
					} catch (Exception e) {
						errors.add("Reasoning recompute failed: " + e.getMessage());
					}
				}
			} catch (Exception e) {
				errors.add("Unexpected URI loading error: " + e.getMessage());
			} finally {
				int loadedCount = successCount;
				graphTripleCount = workspaceService.getTripleCount();
				int finalTripleCount = graphTripleCount;
				Platform.runLater(() -> {
					if (loadedCount > 0) {
						NotificationWidget.getInstance()
								.showSuccess("Loaded " + DataUiMessageUtils.countLabel(loadedCount, "URI")
										+ ". Graph now has " + DataUiMessageUtils.countLabel(finalTripleCount, "triple")
										+ ".");
					}
					if (!errors.isEmpty()) {
						for (String error : errors) {
							NotificationWidget.getInstance().showError(error);
						}
					}
				});
				finishDataOperation();
			}
		});
	}

	private void handleReloadSources() {
		List<DataSource> trackedSources = workspaceService.getTrackedSources();
		if (trackedSources.isEmpty()) {
			NotificationWidget.getInstance().showWarning("No tracked data sources to reload.");
			return;
		}
		DataReloadSourcesDialog.show(trackedSources, this::executeReloadSources);
	}

	private void executeReloadSources(List<DataSource> selectedSources) {
		AppExecutors.execute(() -> {
			dataOperationInProgress.set(true);
			try {
				reasoningService.resetAllProfiles();
				int reloaded = workspaceService.reloadSources(selectedSources);
				int tripleCount = workspaceService.getTripleCount();
				Platform.runLater(() -> {
					resetReasoningUiState();
					if (reloaded > 0) {
						NotificationWidget.getInstance()
								.showSuccess("Reloaded " + DataUiMessageUtils.countLabel(reloaded, "source")
										+ ". Graph now has " + DataUiMessageUtils.countLabel(tripleCount, "triple")
										+ ".");
					} else {
						NotificationWidget.getInstance().showInfo("Reload", "No source selected. Graph was cleared.");
					}
				});
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance().showError("Reload failed: " + e.getMessage()));
			} finally {
				finishDataOperation();
			}
		});
	}

	private void handleExportData() {
		if (!workspaceService.hasData()) {
			NotificationWidget.getInstance().showWarning("No RDF data to export.");
			return;
		}
		if (view.getRoot().getScene() == null) {
			NotificationWidget.getInstance().showError("RDF export unavailable: view is not attached to a scene.");
			return;
		}

		ExportHelper.exportResult(view.getRoot().getScene().getWindow(), workspaceService.getRdfExportFormats(),
				format -> {
					try {
						return workspaceService.serializeGraph(format);
					} catch (Exception e) {
						Platform.runLater(() -> NotificationWidget.getInstance()
								.showError("RDF export preparation failed: " + e.getMessage()));
						return null;
					}
				});
	}

	private void handleExportVisualGraph() {
		if (!workspaceService.hasData()) {
			NotificationWidget.getInstance().showWarning("No graph to export.");
			return;
		}
		if (view.getRoot().getScene() == null) {
			NotificationWidget.getInstance().showError("Graph export unavailable: view is not attached to a scene.");
			return;
		}

		String svgContent = view.getGraphWidget().getSvgContent();
		if (svgContent == null || svgContent.isBlank()) {
			NotificationWidget.getInstance().showWarning("No rendered graph available for export.");
			return;
		}

		ExportHelper.exportGraph(view.getRoot().getScene().getWindow(), svgContent);
	}

	private void handleClearGraph() {
		DataClearGraphDialog.show(() -> AppExecutors.execute(() -> {
			try {
				dataOperationInProgress.set(true);
				int removedTriples = workspaceService.getTripleCount();
				reasoningService.resetAllProfiles();
				workspaceService.clearGraph();
				Platform.runLater(() -> {
					resetReasoningUiState();
					if (removedTriples > 0) {
						NotificationWidget.getInstance().showSuccess("Graph cleared. Removed "
								+ DataUiMessageUtils.countLabel(removedTriples, "triple") + ".");
					} else {
						NotificationWidget.getInstance().showSuccess("Graph cleared.");
					}
				});
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance().showError("Clear failed: " + e.getMessage()));
			} finally {
				finishDataOperation();
			}
		}));
	}

	private void finishDataOperation() {
		dataOperationInProgress.set(false);
		Platform.runLater(() -> {
			refreshReasoningUiState();
			refreshGraphSnapshot();
		});
	}

	private void resetReasoningUiState() {
		refreshReasoningUiState();
	}

	private void syncReasoningToggleStates() {
		syncingReasoningUi.set(true);
		try {
			for (Map.Entry<ReasoningProfile, ToggleSwitch> entry : reasoningToggles.entrySet()) {
				entry.getValue().setSelected(reasoningService.isEnabled(entry.getKey()));
			}
		} finally {
			syncingReasoningUi.set(false);
		}
	}

	private void handleReasoningToggle(ReasoningProfile profile, boolean enabled) {
		AppExecutors.execute(() -> {
			try {
				reasoningService.setEnabled(profile, enabled);
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showSuccess((enabled ? "Enabled " : "Disabled ") + profile.label() + "."));
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance()
						.showError("Reasoning update failed for " + profile.label() + ": " + e.getMessage()));
			} finally {
				Platform.runLater(() -> {
					refreshReasoningUiState();
					refreshGraphSnapshot();
				});
			}
		});
	}

	@Override
	public void close() {
		if (mutationSubscription != null) {
			try {
				mutationSubscription.close();
			} catch (Exception e) {
				LOGGER.debug("Failed to close graph mutation subscription", e);
			}
			mutationSubscription = null;
		}
	}
}
