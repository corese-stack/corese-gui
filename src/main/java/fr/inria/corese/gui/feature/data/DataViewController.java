package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.ExportHelper;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.DataWorkspaceService;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DefaultDataWorkspaceService;
import fr.inria.corese.gui.core.service.DefaultReasoningService;
import fr.inria.corese.gui.core.service.GraphMutationBus;
import fr.inria.corese.gui.core.service.GraphMutationEvent;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ReasoningProfile;
import fr.inria.corese.gui.core.service.ReasoningService;
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
import javafx.scene.control.CheckBox;
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
	private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);
	private final AtomicBoolean reasoningRecomputeScheduled = new AtomicBoolean(false);
	private final AtomicBoolean dataOperationInProgress = new AtomicBoolean(false);
	private final Map<ReasoningProfile, CheckBox> reasoningToggles = new EnumMap<>(ReasoningProfile.class);

	private AutoCloseable mutationSubscription;

	private static final long GRAPH_REFRESH_DEBOUNCE_MS = 120L;
	private static final long REASONING_REFRESH_DEBOUNCE_MS = 260L;

	public DataViewController(DataView view) {
		this.view = view;
		this.workspaceService = DefaultDataWorkspaceService.getInstance();
		this.reasoningService = DefaultReasoningService.getInstance();
		this.mutationBus = GraphMutationBus.getInstance();
		initialize();
	}

	private void initialize() {
		configureToolbar();
		configureReasoningControls();
		subscribeToGraphMutations();
		refreshGraphSnapshot();
	}

	private void configureToolbar() {
		List<ButtonConfig> buttons = List.of(ButtonFactory.openFile(this::handleLoadFile),
				ButtonFactory.openUri(this::handleLoadUri), ButtonFactory.reload(this::handleReloadSources),
				ButtonFactory.export(this::handleExportGraph), ButtonFactory.clearGraph(this::handleClearGraph),
				ButtonFactory.resetLayout(view.getGraphWidget()::resetLayout),
				ButtonFactory.zoomIn(view.getGraphWidget()::zoomIn),
				ButtonFactory.zoomOut(view.getGraphWidget()::zoomOut));

		view.setToolbarActions(buttons);
		view.markToolbarButtonDanger(ButtonIcon.CLEAR);
	}

	private void configureReasoningControls() {
		List<CheckBox> builtInToggles = view.getBuiltInRuleToggles();
		reasoningToggles.put(ReasoningProfile.RDFS, builtInToggles.get(0));
		reasoningToggles.put(ReasoningProfile.OWL_RL, builtInToggles.get(1));
		reasoningToggles.put(ReasoningProfile.OWL_RL_LITE, builtInToggles.get(2));
		reasoningToggles.put(ReasoningProfile.OWL_RL_EXT, builtInToggles.get(3));

		for (Map.Entry<ReasoningProfile, CheckBox> entry : reasoningToggles.entrySet()) {
			ReasoningProfile profile = entry.getKey();
			CheckBox toggle = entry.getValue();
			toggle.setSelected(reasoningService.isEnabled(profile));
			toggle.setOnAction(event -> handleReasoningToggle(profile, toggle.isSelected()));
		}

		// Custom rules support will be added in the next reasoning milestone.
		view.getLoadCustomRuleButton().setOnAction(
				event -> NotificationWidget.getInstance().showInfo("Reasoning", "Custom .rul support is coming next."));
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
					syncReasoningToggleStates();
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
			String jsonLdSnapshot = workspaceService.getGraphSnapshotJsonLd();
			if (jsonLdSnapshot == null || jsonLdSnapshot.isBlank()) {
				view.getGraphWidget().clear();
			} else {
				view.getGraphWidget().displayGraph(jsonLdSnapshot);
			}
			view.updateStatus(workspaceService.getTripleCount(), workspaceService.getSourceCount());
		} catch (Exception e) {
			LOGGER.warn("Failed to refresh graph snapshot", e);
		}
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

		if (files != null && !files.isEmpty()) {
			FileDialogState.updateLastDirectory(files);
			AppExecutors.execute(() -> {
				int successCount = 0;
				dataOperationInProgress.set(true);
				try {
					for (File file : files) {
						try {
							workspaceService.loadFile(file);
							successCount++;
						} catch (Exception ex) {
							String message = "Error loading " + file.getName() + ": " + ex.getMessage();
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
					Platform.runLater(() -> {
						if (loadedCount > 0) {
							NotificationWidget.getInstance().showSuccess("Loaded " + loadedCount + " file(s).");
						}
					});
					finishDataOperation();
				}
			});
		}
	}

	private void handleLoadUri() {
		DataUriLoadDialog.show(this::executeLoadUris);
	}

	private void executeLoadUris(List<String> uris) {
		List<String> urisToLoad = uris == null ? List.of() : List.copyOf(uris);
		AppExecutors.execute(() -> {
			int successCount = 0;
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
				Platform.runLater(() -> {
					if (loadedCount > 0) {
						NotificationWidget.getInstance().showSuccess("Loaded " + loadedCount + " URI(s).");
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
			NotificationWidget.getInstance().showWarning("No tracked data source to reload.");
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
				Platform.runLater(() -> {
					resetReasoningUiState();
					if (reloaded > 0) {
						NotificationWidget.getInstance().showSuccess("Reloaded " + reloaded + " source(s).");
					} else {
						NotificationWidget.getInstance().showInfo("Graph reloaded with no source selected.");
					}
				});
			} catch (Exception e) {
				Platform.runLater(() -> NotificationWidget.getInstance().showError("Reload failed: " + e.getMessage()));
			} finally {
				finishDataOperation();
			}
		});
	}

	private void handleExportGraph() {
		if (!workspaceService.hasData()) {
			NotificationWidget.getInstance().showWarning("No graph data to export.");
			return;
		}
		if (view.getRoot().getScene() == null) {
			NotificationWidget.getInstance().showError("Export unavailable: view is not attached to a scene.");
			return;
		}

		ExportHelper.exportDataGraph(view.getRoot().getScene().getWindow(), workspaceService.getRdfExportFormats(),
				format -> {
					try {
						return workspaceService.serializeGraph(format);
					} catch (Exception e) {
						Platform.runLater(() -> NotificationWidget.getInstance()
								.showError("RDF export preparation failed: " + e.getMessage()));
						return null;
					}
				}, () -> view.getGraphWidget().getSvgContent());
	}

	private void handleClearGraph() {
		ModalService.getInstance().showConfirmation("Clear Graph",
				"This will permanently remove all graph data and tracked sources.\nDo you want to continue?",
				"Clear Graph", true, () -> AppExecutors.execute(() -> {
					try {
						dataOperationInProgress.set(true);
						reasoningService.resetAllProfiles();
						workspaceService.clearGraph();
						Platform.runLater(() -> {
							resetReasoningUiState();
							NotificationWidget.getInstance().showSuccess("Graph cleared.");
						});
					} catch (Exception e) {
						Platform.runLater(
								() -> NotificationWidget.getInstance().showError("Clear failed: " + e.getMessage()));
					} finally {
						finishDataOperation();
					}
				}));
	}

	private void finishDataOperation() {
		dataOperationInProgress.set(false);
		Platform.runLater(() -> {
			syncReasoningToggleStates();
			refreshGraphSnapshot();
		});
	}

	private void resetReasoningUiState() {
		view.resetBuiltInRuleToggles();
	}

	private void syncReasoningToggleStates() {
		for (Map.Entry<ReasoningProfile, CheckBox> entry : reasoningToggles.entrySet()) {
			entry.getValue().setSelected(reasoningService.isEnabled(entry.getKey()));
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
					syncReasoningToggleStates();
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
