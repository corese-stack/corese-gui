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
import fr.inria.corese.gui.feature.data.dialog.DataRulePreviewDialog;
import fr.inria.corese.gui.feature.data.dialog.DataUriLoadDialog;
import fr.inria.corese.gui.feature.data.support.DataFileSelectionSupport;
import fr.inria.corese.gui.feature.data.support.DataLoadingSupport;
import fr.inria.corese.gui.feature.data.support.DataLoadingSupport.OperationIssue;
import fr.inria.corese.gui.feature.data.support.DataUiMessageUtils;
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
	private static final List<String> RDF_FILE_EXTENSIONS = FileTypeSupport.rdfExtensions();

	public DataViewController(DataView view) {
		this.view = view;
		this.workspaceService = DefaultDataWorkspaceService.getInstance();
		this.reasoningService = DefaultReasoningService.getInstance();
		this.mutationBus = GraphMutationBus.getInstance();
		this.ruleFileController = new DataRuleFileController(view, workspaceService, reasoningService,
				this::refreshReasoningUiState, this::refreshGraphSnapshot);
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
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			view.setBuiltInRuleViewAction(profile, () -> handleBuiltInRuleViewRequested(profile));
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
				Platform.runLater(() -> NotificationWidget.getInstance().showErrorWithDetails("Reasoning Error",
						"Reasoning recompute failed: " + e.getMessage(), e));
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
		FileChooser.ExtensionFilter rdfFilter = FileTypeSupport.createExtensionFilter("RDF Files", RDF_FILE_EXTENSIONS,
				true);
		fileChooser.getExtensionFilters().addAll(rdfFilter, new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setSelectedExtensionFilter(rdfFilter);

		List<File> files = fileChooser.showOpenMultipleDialog(view.getRoot().getScene().getWindow());
		DataFileSelectionSupport.FileSelectionEvaluation selection = DataFileSelectionSupport.evaluateStrict(files,
				RDF_FILE_EXTENSIONS);
		DataFileSelectionSupport.notifyWarnings(selection, expectedRdfExtensionsHint(),
				DataFileSelectionSupport.InputOrigin.SELECTED);
		if (!selection.hasAcceptedFiles()) {
			return;
		}
		executeLoadFiles(selection.acceptedFiles());
	}

	private void handleGraphFilesDropped(List<File> droppedFiles) {
		DataFileSelectionSupport.FileSelectionEvaluation selection = DataFileSelectionSupport
				.evaluateStrict(droppedFiles, RDF_FILE_EXTENSIONS);
		DataFileSelectionSupport.notifyWarnings(selection, expectedRdfExtensionsHint(),
				DataFileSelectionSupport.InputOrigin.DROPPED);
		if (!selection.hasAcceptedFiles()) {
			return;
		}
		executeLoadFiles(selection.acceptedFiles());
	}

	private void executeLoadFiles(List<File> files) {
		List<File> safeFiles = files == null
				? List.of()
				: files.stream().filter(file -> file != null && file.isFile()).toList();
		if (safeFiles.isEmpty()) {
			return;
		}

		FileDialogState.updateLastDirectory(safeFiles);
		runAsyncDataOperation("Data Load", "Loading RDF file(s)...", () -> {
			List<OperationIssue> errors = new ArrayList<>();
			int loadedCount = DataLoadingSupport.loadFiles(workspaceService, safeFiles, errors);
			if (loadedCount > 0) {
				DataLoadingSupport.recomputeReasoning(reasoningService, errors);
			}

			int finalTripleCount = workspaceService.getTripleCount();
			Platform.runLater(() -> {
				if (loadedCount > 0) {
					NotificationWidget.getInstance().showSuccess(
							"Loaded " + DataUiMessageUtils.countLabel(loadedCount, "file") + ". Graph now has "
									+ DataUiMessageUtils.countLabel(finalTripleCount, "triple") + ".");
				}
				DataLoadingSupport.showErrors(errors);
			});
		});
	}

	private void handleLoadUri() {
		DataUriLoadDialog.show(this::executeLoadUris);
	}

	private void executeLoadUris(List<String> uris) {
		List<String> urisToLoad = uris == null ? List.of() : List.copyOf(uris);
		runAsyncDataOperation("URI Load", "Loading RDF URI(s)...", () -> {
			List<OperationIssue> errors = new ArrayList<>();
			try {
				int loadedCount = DataLoadingSupport.loadUris(workspaceService, urisToLoad, errors);
				if (loadedCount > 0) {
					DataLoadingSupport.recomputeReasoning(reasoningService, errors);
				}

				int finalTripleCount = workspaceService.getTripleCount();
				Platform.runLater(() -> {
					if (loadedCount > 0) {
						NotificationWidget.getInstance()
								.showSuccess("Loaded " + DataUiMessageUtils.countLabel(loadedCount, "URI")
										+ ". Graph now has " + DataUiMessageUtils.countLabel(finalTripleCount, "triple")
										+ ".");
					}
					DataLoadingSupport.showErrors(errors);
				});
			} catch (Exception e) {
				errors.add(new OperationIssue("Unexpected URI loading error: " + e.getMessage(), e));
				Platform.runLater(() -> DataLoadingSupport.showErrors(errors));
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
		runAsyncDataOperation("Data Reload", "Reloading selected data sources...", () -> {
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
				Platform.runLater(() -> NotificationWidget.getInstance().showErrorWithDetails("Reload Error",
						"Reload failed: " + e.getMessage(), e));
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
						Platform.runLater(() -> NotificationWidget.getInstance().showErrorWithDetails("Export Error",
								"RDF export preparation failed: " + e.getMessage(), e));
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
		DataClearGraphDialog.show(() -> runAsyncDataOperation("Data Clear", "Clearing graph...", () -> {
			try {
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
				Platform.runLater(() -> NotificationWidget.getInstance().showErrorWithDetails("Clear Error",
						"Clear failed: " + e.getMessage(), e));
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
		runAsyncUiRefreshOperation(() -> {
			try {
				DataWorkspaceStatus beforeStatus = workspaceService.getStatus();
				reasoningService.setEnabled(profile, enabled);
				DataWorkspaceStatus afterStatus = workspaceService.getStatus();
				String message = DataUiMessageUtils.buildTripleDeltaMessage(beforeStatus.inferredTripleCount(),
						afterStatus.inferredTripleCount());
				Platform.runLater(() -> NotificationWidget.getInstance().showSuccess(message));
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance().showErrorWithDetails("Reasoning Error",
						"Reasoning update failed for " + profile.label() + ": " + e.getMessage(), e));
			}
		});
	}

	private void handleBuiltInRuleViewRequested(ReasoningProfile profile) {
		if (profile == null) {
			return;
		}
		AppExecutors.execute(() -> {
			try {
				ReasoningService.BuiltInProfileSource source = reasoningService.getBuiltInProfileSource(profile);
				Platform.runLater(
						() -> DataRulePreviewDialog.show(source.label(), source.sourcePath(), source.sourceContent()));
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance().showErrorWithDetails("Rule Source Error",
						"Failed to open built-in profile source " + profile.label() + ": " + e.getMessage(), e));
			}
		});
	}

	private void runAsyncDataOperation(String loadingTitle, String loadingMessage, Runnable operation) {
		NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading(loadingTitle,
				loadingMessage);
		AppExecutors.execute(() -> {
			dataOperationInProgress.set(true);
			try {
				operation.run();
			} finally {
				loadingHandle.close();
				finishDataOperation();
			}
		});
	}

	private void runAsyncUiRefreshOperation(Runnable operation) {
		AppExecutors.execute(() -> {
			try {
				operation.run();
			} finally {
				Platform.runLater(() -> {
					refreshReasoningUiState();
					refreshGraphSnapshot();
				});
			}
		});
	}

	private String expectedRdfExtensionsHint() {
		return DataUiMessageUtils.buildExpectedExtensionsHint(RDF_FILE_EXTENSIONS);
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
