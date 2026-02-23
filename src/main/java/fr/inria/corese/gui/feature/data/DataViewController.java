package fr.inria.corese.gui.feature.data;

import atlantafx.base.controls.ToggleSwitch;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderMode;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderStatus;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.ExportHelper;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.DataSource;
import fr.inria.corese.gui.core.service.DataWorkspaceService;
import fr.inria.corese.gui.core.service.DataWorkspaceStatus;
import fr.inria.corese.gui.core.service.DefaultDataWorkspaceService;
import fr.inria.corese.gui.core.service.DefaultReasoningService;
import fr.inria.corese.gui.core.service.GraphMutationBus;
import fr.inria.corese.gui.core.service.GraphMutationEvent;
import fr.inria.corese.gui.core.service.ReasoningProfile;
import fr.inria.corese.gui.core.service.ReasoningService;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.feature.data.dialog.DataClearGraphDialog;
import fr.inria.corese.gui.feature.data.dialog.DataReloadSourcesDialog;
import fr.inria.corese.gui.feature.data.dialog.DataRulePreviewDialog;
import fr.inria.corese.gui.feature.data.dialog.DataUriLoadDialog;
import fr.inria.corese.gui.feature.data.support.DataFileSelectionSupport;
import fr.inria.corese.gui.feature.data.support.DataFileSelectionSupport.InputOrigin;
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
import java.util.function.Supplier;
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
	private static final String MSG_NO_DATA_TO_CLEAR = "No RDF data to clear.";

	private final DataView view;
	private final DataWorkspaceService workspaceService;
	private final ReasoningService reasoningService;
	private final GraphMutationBus mutationBus;
	private final DataRuleFileController ruleFileController;
	private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);
	private final AtomicBoolean reasoningRecomputeScheduled = new AtomicBoolean(false);
	private final AtomicBoolean dataOperationInProgress = new AtomicBoolean(false);
	private final AtomicBoolean syncingReasoningUi = new AtomicBoolean(false);
	private final AtomicBoolean graphSnapshotRefreshRunning = new AtomicBoolean(false);
	private final AtomicBoolean graphSnapshotRefreshRequested = new AtomicBoolean(false);
	private final AtomicBoolean manualGraphRenderInProgress = new AtomicBoolean(false);
	private final Map<ReasoningProfile, ToggleSwitch> reasoningToggles = new EnumMap<>(ReasoningProfile.class);

	private AutoCloseable mutationSubscription;
	private final ThemeManager themeManager;

	private static final long GRAPH_REFRESH_DEBOUNCE_MS = 120L;
	private static final long REASONING_REFRESH_DEBOUNCE_MS = 260L;
	private static final String RENDER_DETAIL_INTERACTION_LOCKED = "Graph interactions disabled for very large graph.";
	private static final List<String> RDF_FILE_EXTENSIONS = FileTypeSupport.rdfExtensions();
	private volatile boolean graphInteractionsLocked = false;

	public DataViewController(DataView view) {
		this.view = view;
		this.view.setController(this);
		this.workspaceService = DefaultDataWorkspaceService.getInstance();
		this.reasoningService = DefaultReasoningService.getInstance();
		this.mutationBus = GraphMutationBus.getInstance();
		this.themeManager = ThemeManager.getInstance();
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
		view.getGraphWidget().setOnRenderStatusChanged(this::handleGraphRenderStatusChanged);
		view.getGraphWidget().setOnManualRenderRequested(this::handleManualGraphRenderRequest);
		applyGraphPreviewLimitToGraphWidget();
	}

	private void configureToolbar() {
		List<ButtonConfig> buttons = List.of(ButtonFactory.openFile(this::handleLoadFile),
				ButtonFactory.openUri(this::handleLoadUri), ButtonFactory.reload(this::handleReloadSources),
				ButtonFactory.exportData(this::handleExportData), ButtonFactory.clearGraph(this::handleClearGraph),
				ButtonFactory.exportGraph(this::handleExportVisualGraph),
				ButtonFactory.resetLayout(view.getGraphWidget()::resetLayout),
				ButtonFactory.centerView(view.getGraphWidget()::centerView),
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
			} catch (InterruptedException _) {
				Thread.currentThread().interrupt();
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
		if (dataOperationInProgress.get()) {
			return;
		}
		if (!refreshScheduled.compareAndSet(false, true)) {
			return;
		}
		AppExecutors.execute(() -> {
			try {
				Thread.sleep(GRAPH_REFRESH_DEBOUNCE_MS);
			} catch (InterruptedException _) {
				Thread.currentThread().interrupt();
			}
			Platform.runLater(() -> {
				refreshScheduled.set(false);
				if (dataOperationInProgress.get()) {
					return;
				}
				refreshGraphSnapshot();
			});
		});
	}

	private void refreshGraphSnapshot() {
		graphSnapshotRefreshRequested.set(true);
		runGraphSnapshotRefreshLoop();
	}

	private void runGraphSnapshotRefreshLoop() {
		if (!graphSnapshotRefreshRunning.compareAndSet(false, true)) {
			return;
		}
		AppExecutors.execute(() -> {
			try {
				while (graphSnapshotRefreshRequested.getAndSet(false)) {
					GraphSnapshotPayload payload = computeGraphSnapshotPayload();
					if (payload == null) {
						continue;
					}
					Platform.runLater(() -> applyGraphSnapshotPayload(payload));
				}
			} finally {
				graphSnapshotRefreshRunning.set(false);
				if (graphSnapshotRefreshRequested.get()) {
					runGraphSnapshotRefreshLoop();
				}
			}
		});
	}

	private void handleGraphRenderStatusChanged(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		graphInteractionsLocked = isGraphInteractionLocked(safeStatus);
		applyGraphPreviewLimitToGraphWidget();
		view.updateGraphRenderStatus(safeStatus);
		updateToolbarActionStates();
	}

	private static boolean isGraphInteractionLocked(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		if (safeStatus.mode() == GraphRenderMode.PAUSED) {
			return true;
		}
		return safeStatus.details().stream().anyMatch(detail -> RENDER_DETAIL_INTERACTION_LOCKED.equals(detail));
	}

	private int resolveGraphPreviewLimit() {
		return Math.max(ThemeManager.getMinGraphAutoRenderTriplesLimit(),
				themeManager.getGraphAutoRenderTriplesLimit());
	}

	private void applyGraphPreviewLimitToGraphWidget() {
		view.getGraphWidget().setMaxAutoRenderTriples(resolveGraphPreviewLimit());
	}

	private GraphSnapshotPayload computeGraphSnapshotPayload() {
		try {
			DataWorkspaceStatus status = workspaceService.getStatus();
			int maxAutoRenderTriples = resolveGraphPreviewLimit();
			boolean skipSnapshotSerialization = status.tripleCount() > 0 && maxAutoRenderTriples > 0
					&& status.tripleCount() > maxAutoRenderTriples;
			String jsonLdSnapshot = skipSnapshotSerialization ? "" : workspaceService.getGraphSnapshotJsonLd();
			return new GraphSnapshotPayload(status, jsonLdSnapshot, skipSnapshotSerialization);
		} catch (Exception e) {
			LOGGER.warn("Failed to refresh graph snapshot", e);
			return null;
		}
	}

	private void applyGraphSnapshotPayload(GraphSnapshotPayload payload) {
		if (payload == null || dataOperationInProgress.get()) {
			return;
		}
		applyGraphPreviewLimitToGraphWidget();
		DataWorkspaceStatus status = payload.status();
		String jsonLdSnapshot = payload.jsonLdSnapshot();
		boolean snapshotSkippedForLimit = payload.snapshotSkippedForLimit();
		if (status.tripleCount() <= 0) {
			graphInteractionsLocked = false;
			view.getGraphWidget().clear();
			view.setGraphEmptyStateVisible(true);
			view.updateStatus(status);
			updateToolbarActionStates();
			return;
		}
		if (snapshotSkippedForLimit) {
			graphInteractionsLocked = true;
			view.getGraphWidget().pausePreviewForLargeGraph(status.tripleCount(), status.namedGraphCount());
			view.setGraphEmptyStateVisible(false);
			view.updateStatus(status);
			updateToolbarActionStates();
			return;
		}
		boolean hasRenderableGraph = jsonLdSnapshot != null && !jsonLdSnapshot.isBlank();
		if (!hasRenderableGraph) {
			view.getGraphWidget().clear();
		} else {
			view.getGraphWidget().displayGraph(jsonLdSnapshot, status.tripleCount());
		}
		view.setGraphEmptyStateVisible(status.tripleCount() <= 0);
		view.updateStatus(status);
		updateToolbarActionStates();
	}

	private void handleManualGraphRenderRequest() {
		if (dataOperationInProgress.get()) {
			view.getGraphWidget().notifyManualRenderFailed();
			return;
		}
		if (!manualGraphRenderInProgress.compareAndSet(false, true)) {
			return;
		}
		AppExecutors.execute(() -> {
			try {
				DataWorkspaceStatus status = workspaceService.getStatus();
				String jsonLdSnapshot = workspaceService.getGraphSnapshotJsonLd();
				Platform.runLater(() -> {
					try {
						applyManualGraphRender(status, jsonLdSnapshot);
					} finally {
						manualGraphRenderInProgress.set(false);
					}
				});
			} catch (Exception e) {
				LOGGER.warn("Failed to render graph manually", e);
				Platform.runLater(() -> {
					try {
						view.getGraphWidget().notifyManualRenderFailed();
						NotificationWidget.getInstance().showErrorWithDetails("Graph Preview",
								"Manual graph rendering failed: " + e.getMessage(), e);
					} finally {
						manualGraphRenderInProgress.set(false);
					}
				});
			}
		});
	}

	private void applyManualGraphRender(DataWorkspaceStatus status, String jsonLdSnapshot) {
		if (status == null) {
			view.getGraphWidget().notifyManualRenderFailed();
			return;
		}
		if (dataOperationInProgress.get()) {
			view.getGraphWidget().notifyManualRenderFailed();
			return;
		}
		if (status.tripleCount() <= 0 || jsonLdSnapshot == null || jsonLdSnapshot.isBlank()) {
			view.getGraphWidget().clear();
			view.setGraphEmptyStateVisible(true);
			view.updateStatus(status);
			updateToolbarActionStates();
			return;
		}
		view.getGraphWidget().displayGraphForced(jsonLdSnapshot, status.tripleCount());
		view.setGraphEmptyStateVisible(false);
		view.updateStatus(status);
		updateToolbarActionStates();
	}

	private record GraphSnapshotPayload(DataWorkspaceStatus status, String jsonLdSnapshot,
			boolean snapshotSkippedForLimit) {
	}

	private void refreshReasoningUiState() {
		syncReasoningToggleStates();
		ruleFileController.refreshRuleFileList();
	}

	private void updateToolbarActionStates() {
		boolean hasData = workspaceService.hasData();
		boolean hasTrackedSources = !workspaceService.getTrackedSources().isEmpty();

		view.setToolbarButtonDisabled(ButtonIcon.RELOAD, !hasTrackedSources);
		setToolbarButtonsDisabled(!hasData, ButtonIcon.EXPORT_DATA, ButtonIcon.EXPORT, ButtonIcon.CLEAR);
		setToolbarButtonsDisabled(!hasData || graphInteractionsLocked, ButtonIcon.LAYOUT_FORCE, ButtonIcon.CENTER_VIEW,
				ButtonIcon.ZOOM_IN, ButtonIcon.ZOOM_OUT);
	}

	private void setToolbarButtonsDisabled(boolean disabled, ButtonIcon... buttonIcons) {
		for (ButtonIcon buttonIcon : buttonIcons) {
			view.setToolbarButtonDisabled(buttonIcon, disabled);
		}
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
		processRdfFileSelection(files, InputOrigin.SELECTED);
	}

	private void handleGraphFilesDropped(List<File> droppedFiles) {
		processRdfFileSelection(droppedFiles, InputOrigin.DROPPED);
	}

	private void processRdfFileSelection(List<File> files, InputOrigin inputOrigin) {
		DataFileSelectionSupport.FileSelectionEvaluation selection = DataFileSelectionSupport.evaluateStrict(files,
				RDF_FILE_EXTENSIONS);
		DataFileSelectionSupport.notifyWarnings(selection, expectedRdfExtensionsHint(), inputOrigin);
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
			int loadedCount = 0;
			int finalTripleCount = workspaceService.getTripleCount();
			try {
				loadedCount = DataLoadingSupport.loadFiles(workspaceService, safeFiles, errors);
				if (loadedCount > 0) {
					DataLoadingSupport.recomputeReasoning(reasoningService, errors);
				}
				finalTripleCount = workspaceService.getTripleCount();
			} catch (Exception e) {
				errors.add(new OperationIssue("Unexpected file loading error: " + e.getMessage(), e));
			}

			int loadedCountSnapshot = loadedCount;
			int tripleCountSnapshot = finalTripleCount;
			List<OperationIssue> operationErrors = List.copyOf(errors);
			return () -> notifyLoadOutcome("file", loadedCountSnapshot, tripleCountSnapshot, operationErrors,
					"Data Load Error");
		});
	}

	private void handleLoadUri() {
		DataUriLoadDialog.show(this::executeLoadUris);
	}

	private void executeLoadUris(List<String> uris) {
		List<String> urisToLoad = uris == null ? List.of() : List.copyOf(uris);
		runAsyncDataOperation("URI Load", "Loading RDF URI(s)...", () -> {
			List<OperationIssue> errors = new ArrayList<>();
			int loadedCount = 0;
			int finalTripleCount = workspaceService.getTripleCount();
			try {
				loadedCount = DataLoadingSupport.loadUris(workspaceService, urisToLoad, errors);
				if (loadedCount > 0) {
					DataLoadingSupport.recomputeReasoning(reasoningService, errors);
				}
				finalTripleCount = workspaceService.getTripleCount();
			} catch (Exception e) {
				errors.add(new OperationIssue("Unexpected URI loading error: " + e.getMessage(), e));
			}
			int loadedCountSnapshot = loadedCount;
			int tripleCountSnapshot = finalTripleCount;
			List<OperationIssue> operationErrors = List.copyOf(errors);
			return () -> notifyLoadOutcome("URI", loadedCountSnapshot, tripleCountSnapshot, operationErrors,
					"URI Load Error");
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
				if (reloaded > 0) {
					return () -> NotificationWidget.getInstance()
							.showSuccess("Reloaded " + DataUiMessageUtils.countLabel(reloaded, "source")
									+ ". Graph now has " + DataUiMessageUtils.countLabel(tripleCount, "triple") + ".");
				}
				return () -> NotificationWidget.getInstance().showInfo("Reload",
						"No source selected. Graph was cleared.");
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Reload Error",
						"Reload failed: " + e.getMessage(), e);
			}
		});
	}

	private void handleExportData() {
		if (!ensureDataAvailable("No RDF data to export.")) {
			return;
		}
		if (!ensureViewAttachedToScene("RDF export unavailable: view is not attached to a scene.")) {
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
		if (!ensureDataAvailable("No graph to export.")) {
			return;
		}
		if (!ensureViewAttachedToScene("Graph export unavailable: view is not attached to a scene.")) {
			return;
		}

		String svgContent = view.getGraphWidget().getSvgContent();
		if (svgContent == null || svgContent.isBlank()) {
			NotificationWidget.getInstance().showWarning("No rendered graph available for export.");
			return;
		}

		ExportHelper.exportGraph(view.getRoot().getScene().getWindow(), svgContent);
	}

	private boolean ensureDataAvailable(String warningMessage) {
		if (workspaceService.hasData()) {
			return true;
		}
		NotificationWidget.getInstance().showWarning(warningMessage);
		return false;
	}

	private boolean ensureViewAttachedToScene(String errorMessage) {
		if (view.getRoot().getScene() != null) {
			return true;
		}
		NotificationWidget.getInstance().showError(errorMessage);
		return false;
	}

	private void handleClearGraph() {
		if (warnIfNoDataToClear()) {
			return;
		}
		DataClearGraphDialog.show(() -> runAsyncDataOperation("Data Clear", "Clearing graph...", () -> {
			try {
				int removedTriples = workspaceService.getTripleCount();
				reasoningService.resetAllProfiles();
				workspaceService.clearGraph();
				if (removedTriples > 0) {
					return () -> NotificationWidget.getInstance().showSuccess(
							"Graph cleared. Removed " + DataUiMessageUtils.countLabel(removedTriples, "triple") + ".");
				}
				return () -> NotificationWidget.getInstance().showSuccess("Graph cleared.");
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Clear Error",
						"Clear failed: " + e.getMessage(), e);
			}
		}));
	}

	private boolean warnIfNoDataToClear() {
		if (workspaceService.hasData()) {
			return false;
		}
		NotificationWidget.getInstance().showWarning(MSG_NO_DATA_TO_CLEAR);
		return true;
	}

	public boolean loadFilesFromShortcut() {
		handleLoadFile();
		return true;
	}

	public boolean loadUriFromShortcut() {
		handleLoadUri();
		return true;
	}

	public boolean reloadSourcesFromShortcut() {
		handleReloadSources();
		return true;
	}

	public boolean clearGraphFromShortcut() {
		handleClearGraph();
		return true;
	}

	public boolean exportDataFromShortcut() {
		handleExportData();
		return true;
	}

	public boolean exportGraphFromShortcut() {
		handleExportVisualGraph();
		return true;
	}

	public boolean reenergizeGraphFromShortcut() {
		view.getGraphWidget().resetLayout();
		return true;
	}

	public boolean centerGraphFromShortcut() {
		view.getGraphWidget().centerView();
		return true;
	}

	private void finishDataOperation() {
		dataOperationInProgress.set(false);
		Platform.runLater(() -> {
			refreshReasoningUiState();
			refreshGraphSnapshot();
		});
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
		runAsyncUiRefreshOperation("Reasoning", buildReasoningToggleLoadingMessage(profile, enabled), () -> {
			try {
				DataWorkspaceStatus beforeStatus = workspaceService.getStatus();
				reasoningService.setEnabled(profile, enabled);
				DataWorkspaceStatus afterStatus = workspaceService.getStatus();
				String profileLabel = profile == null ? "Profile" : profile.label();
				String stateLabel = enabled ? "enabled" : "disabled";
				String deltaMessage = DataUiMessageUtils.buildTripleDeltaMessage(beforeStatus.inferredTripleCount(),
						afterStatus.inferredTripleCount());
				String message = profileLabel + " " + stateLabel + ". " + deltaMessage;
				return () -> NotificationWidget.getInstance().showSuccess("Reasoning Profile", message);
			} catch (Exception e) {
				return () -> NotificationWidget.getInstance().showErrorWithDetails("Reasoning Error",
						"Reasoning update failed for " + profile.label() + ": " + e.getMessage(), e);
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

	private void runAsyncDataOperation(String loadingTitle, String loadingMessage, Supplier<Runnable> operation) {
		runAsyncOperationWithLoading(loadingTitle, loadingMessage, operation, this::finishDataOperation, true);
	}

	private void runAsyncUiRefreshOperation(String loadingTitle, String loadingMessage, Supplier<Runnable> operation) {
		runAsyncOperationWithLoading(loadingTitle, loadingMessage, operation, () -> {
			refreshReasoningUiState();
			refreshGraphSnapshot();
		}, false);
	}

	private void runAsyncOperationWithLoading(String loadingTitle, String loadingMessage, Supplier<Runnable> operation,
			Runnable onFxCompleted, boolean trackDataOperationState) {
		NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading(loadingTitle,
				loadingMessage);
		AppExecutors.execute(() -> {
			if (trackDataOperationState) {
				dataOperationInProgress.set(true);
			}
			Runnable uiFollowUp = null;
			try {
				if (operation != null) {
					uiFollowUp = operation.get();
				}
			} finally {
				Runnable finalUiFollowUp = uiFollowUp;
				// Close the loader first so follow-up toasts are queued behind the dismiss
				// animation instead of overlapping it.
				loadingHandle.closeThen(() -> {
					if (finalUiFollowUp != null) {
						finalUiFollowUp.run();
					}
					if (onFxCompleted != null) {
						onFxCompleted.run();
					}
				});
			}
		});
	}

	private String buildReasoningToggleLoadingMessage(ReasoningProfile profile, boolean enabled) {
		String profileLabel = profile == null ? "profile" : profile.label();
		return enabled ? "Enabling " + profileLabel + "..." : "Disabling " + profileLabel + "...";
	}

	private void notifyLoadOutcome(String sourceLabel, int loadedCount, int tripleCount,
			List<OperationIssue> operationErrors, String errorTitle) {
		if (loadedCount > 0) {
			NotificationWidget.getInstance()
					.showSuccess("Loaded " + DataUiMessageUtils.countLabel(loadedCount, sourceLabel)
							+ ". Graph now has " + DataUiMessageUtils.countLabel(tripleCount, "triple") + ".");
		}
		DataLoadingSupport.showErrors(operationErrors);
		DataLoadingSupport.showPrimaryErrorModalIfNothingLoaded(errorTitle, loadedCount, operationErrors);
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
