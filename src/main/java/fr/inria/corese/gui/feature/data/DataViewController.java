package fr.inria.corese.gui.feature.data;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.ExportHelper;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.DataWorkspaceService;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import fr.inria.corese.gui.core.service.DefaultDataWorkspaceService;
import fr.inria.corese.gui.core.service.DefaultReasoningService;
import fr.inria.corese.gui.core.service.GraphMutationBus;
import fr.inria.corese.gui.core.service.GraphMutationEvent;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ReasoningProfile;
import fr.inria.corese.gui.core.service.ReasoningService;
import fr.inria.corese.gui.utils.AppExecutors;
import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
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
	private static final String LOAD_URI_DIALOG_TITLE = "Load RDF from URI";
	private static final String LOAD_URI_DIALOG_HEADER = "Load graph data from a URI";
	private static final String RELOAD_MODAL_TITLE = "Reload Sources";
	private static final String RELOAD_WARNING_TEXT = "Reload rebuilds the graph from selected sources.\n"
			+ "All modifications made via SPARQL UPDATE queries will be lost.\n"
			+ "Reasoning toggles will be reset to OFF.";

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
				dataOperationInProgress.set(false);
				Platform.runLater(() -> {
					if (loadedCount > 0) {
						NotificationWidget.getInstance().showSuccess("Loaded " + loadedCount + " file(s).");
					}
					syncReasoningToggleStates();
					refreshGraphSnapshot();
				});
			});
		}
	}

	private void handleLoadUri() {
		TextInputDialog dialog = new TextInputDialog("https://");
		dialog.setTitle(LOAD_URI_DIALOG_TITLE);
		dialog.setHeaderText(LOAD_URI_DIALOG_HEADER);
		dialog.setContentText("URI:");

		Optional<String> uriChoice = dialog.showAndWait();
		if (uriChoice.isEmpty()) {
			return;
		}

		String uri = uriChoice.get().trim();
		if (uri.isBlank()) {
			NotificationWidget.getInstance().showWarning("URI cannot be empty.");
			return;
		}

		AppExecutors.execute(() -> {
			dataOperationInProgress.set(true);
			try {
				workspaceService.loadUri(uri);
				if (reasoningService.hasAnyEnabledProfile()) {
					reasoningService.recomputeEnabledProfiles();
				}
				Platform.runLater(() -> NotificationWidget.getInstance().showSuccess("Loaded URI: " + uri));
			} catch (Exception e) {
				Platform.runLater(
						() -> NotificationWidget.getInstance().showError("URI load failed: " + e.getMessage()));
			} finally {
				dataOperationInProgress.set(false);
				Platform.runLater(() -> {
					syncReasoningToggleStates();
					refreshGraphSnapshot();
				});
			}
		});
	}

	private void handleReloadSources() {
		List<DataSource> trackedSources = workspaceService.getTrackedSources();
		if (trackedSources.isEmpty()) {
			NotificationWidget.getInstance().showWarning("No tracked data source to reload.");
			return;
		}
		showReloadModal(trackedSources);
	}

	private void showReloadModal(List<DataSource> trackedSources) {
		VBox sourceList = new VBox(8);
		sourceList.setFillWidth(true);

		Map<CheckBox, DataSource> sourceSelections = new LinkedHashMap<>();
		for (DataSource source : trackedSources) {
			CheckBox checkBox = new CheckBox(formatSourceLabel(source));
			checkBox.setSelected(true);
			checkBox.setWrapText(true);
			sourceSelections.put(checkBox, source);
			sourceList.getChildren().add(checkBox);
		}

		ScrollPane sourceScroll = new ScrollPane(sourceList);
		sourceScroll.setFitToWidth(true);
		sourceScroll.setPrefViewportHeight(180);
		sourceScroll.setMinViewportHeight(120);

		Label warningLabel = new Label(RELOAD_WARNING_TEXT);
		warningLabel.setWrapText(true);

		Label sourceLabel = new Label("Select sources to reload:");
		sourceLabel.getStyleClass().add(Styles.TEXT_BOLD);

		VBox content = new VBox(10, warningLabel, sourceLabel, sourceScroll);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button reloadButton = new Button("Reload");
		reloadButton.getStyleClass().add(Styles.ACCENT);
		reloadButton.setOnAction(event -> {
			List<DataSource> selectedSources = sourceSelections.entrySet().stream()
					.filter(entry -> entry.getKey().isSelected()).map(Map.Entry::getValue).toList();
			ModalService.getInstance().hide();
			executeReloadSources(selectedSources);
		});

		ModalService.getInstance().show(new DialogLayout(RELOAD_MODAL_TITLE, content, cancelButton, reloadButton));
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
				dataOperationInProgress.set(false);
				Platform.runLater(() -> {
					syncReasoningToggleStates();
					refreshGraphSnapshot();
				});
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
						dataOperationInProgress.set(false);
						Platform.runLater(() -> {
							syncReasoningToggleStates();
							refreshGraphSnapshot();
						});
					}
				}));
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

	private String formatSourceLabel(DataSource source) {
		if (source.type() == SourceType.FILE) {
			String fileName = new File(source.location()).getName();
			return "File: " + fileName + "  (" + source.location() + ")";
		}
		return "URI: " + source.location();
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
