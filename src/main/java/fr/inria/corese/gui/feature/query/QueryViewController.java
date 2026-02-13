package fr.inria.corese.gui.feature.query;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResultRef;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.QueryService;
import fr.inria.corese.gui.core.service.RdfDataService;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.feature.editor.code.CodeEditorController;
import fr.inria.corese.gui.feature.editor.tab.TabContext;
import fr.inria.corese.gui.feature.editor.tab.TabEditorConfig;
import fr.inria.corese.gui.feature.editor.tab.TabEditorController;
import fr.inria.corese.gui.feature.query.support.QueryExecutionSupport;
import fr.inria.corese.gui.feature.query.support.QueryResultTabPreferenceSupport;
import fr.inria.corese.gui.feature.query.support.QueryResultRenderSupport;
import fr.inria.corese.gui.feature.query.template.QueryTemplateDialog;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.utils.AppExecutors;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;

/**
 * Controller for the Query feature.
 *
 * <p>
 * Orchestrates the query editing and execution workflow. It embeds a
 * {@link TabEditorController} for the editor interface and manages the
 * interaction with the {@link QueryService} for execution.
 */
public class QueryViewController {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryViewController.class);
	private static final String QUERY_NOTIFICATION_TITLE = "Query";
	private static final String MSG_NO_SELECT_RESULTS = "No results found.";
	private static final String MSG_NO_GRAPH_RESULTS = "No triples produced by this query.";

	private final QueryView view;
	private final QueryService queryService = QueryService.getInstance();
	private final QueryResultTabPreferenceSupport tabPreferenceSupport = new QueryResultTabPreferenceSupport();
	private TabEditorController tabEditorController;

	public QueryViewController(QueryView view) {
		this.view = view;
		initialize();
	}

	private void initialize() {
		// 1. Configure Empty State
		Node emptyState = view.createEmptyState(() -> tabEditorController.createNewTab(), this::onOpenFileButtonClick,
				this::onTemplateButtonClick);

		// 2. Configure Editor
		TabEditorConfig config = TabEditorConfig.builder()
				.withEditorButtons(List.of(ButtonFactory.save(), ButtonFactory.undo(), ButtonFactory.redo()))
				.withExecution(ButtonFactory.custom(ButtonIcon.PLAY, "Run Query"), this::executeQuery)
				.withResultView(List.of(ButtonFactory.copy(), ButtonFactory.export()),
						ResultViewConfig.builder().withTextTab().withTableTab().withGraphTab().build())
				.withEmptyState(emptyState).withAllowedExtensions(FileTypeSupport.queryExtensions())
				.withOpenFileAction(this::onOpenFileButtonClick).withTemplateAction(this::onTemplateButtonClick)
				.withPreloadFirstTab().build();

		// 3. Create Controller
		this.tabEditorController = new TabEditorController(config);

		// Ensure the editor fills the space
		((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxWidth(Double.MAX_VALUE);
		((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxHeight(Double.MAX_VALUE);

		view.setMainContent(tabEditorController.getViewRoot());

		setupTabListeners();
	}

	private void setupTabListeners() {
		tabEditorController
				.addSelectionListener((obs, oldTab, newTab) -> updateResultsForSelectedQueryTab(newTab, false));
		tabEditorController.addTabListener(change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					for (Tab tab : change.getAddedSubList()) {
						registerResultTabPreference(tab);
					}
				}
			}
		});

		for (Tab tab : tabEditorController.getTabs()) {
			registerResultTabPreference(tab);
		}

		// No need for a removal listener here anymore.
		// TabContext.dispose() handles result release automatically when
		// TabEditorController closes a tab.
	}

	// ==============================================================================================
	// Logic - Query Execution & Results
	// ==============================================================================================

	public void executeQuery() {
		Tab selectedTab = tabEditorController.getSelectedTab();
		if (selectedTab == null)
			return;

		TabContext context = TabContext.get(selectedTab);
		if (context == null)
			return;

		ResultController resultController = context.getResultController();
		CodeEditorController codeEditor = context.getEditorController();

		if (resultController == null || codeEditor == null)
			return;

		final String queryContent = codeEditor.getContent();
		if (!RdfDataService.getInstance().hasData() && QueryExecutionSupport.looksLikeReadQuery(queryContent)) {
			resultController.clearResults();
			tabEditorController.hideResultPane();
			tabEditorController.showError("No Data Loaded", "Query execution requires an RDF graph to be loaded.\n"
					+ "Please go to the 'Data' view and load an RDF file.");
			return;
		}

		// Release previous result for this tab if any
		QueryResultRef previousRef = context.getQueryResultRef();
		if (previousRef != null) {
			queryService.releaseResult(previousRef.getId());
			context.setQueryResultRef(null);
		}

		// Prepare UI
		resultController.clearResults();
		tabEditorController.setExecutionState(true);

		// Execute in background
		AppExecutors.execute(() -> {
			try {
				QueryResultRef resultRef = queryService.executeQuery(queryContent);

				Platform.runLater(() -> {
					tabEditorController.setExecutionState(false);
					if (resultRef != null) {
						// Store result in context (Single Source of Truth)
						context.setQueryResultRef(resultRef);
						updateResultsForSelectedQueryTab(selectedTab, true);
					}
				});
			} catch (Exception e) {
				Platform.runLater(() -> {
					tabEditorController.setExecutionState(false);
					ModalService.getInstance().showError("Query Execution Error", e.getMessage());
				});
				LOGGER.error("Error executing query", e);
			}
		});
	}

	private void updateResultsForSelectedQueryTab(Tab selectedQueryTab, boolean forceRefresh) {
		if (selectedQueryTab == null)
			return;

		TabContext context = TabContext.get(selectedQueryTab);
		if (context == null)
			return;

		ResultController resultController = context.getResultController();
		if (resultController == null)
			return;

		QueryResultRef resultRef = context.getQueryResultRef();
		if (resultRef == null)
			return;

		if (!forceRefresh && context.isResultRendered(resultRef)) {
			return;
		}

		resultController.clearResults();

		QueryType queryType = resultRef.getQueryType();
		// Configure results by query family.
		switch (queryType) {
			case SELECT -> {
				if (showNoResultNotificationIfEmpty(resultRef, MSG_NO_SELECT_RESULTS)) {
					break;
				}
				tabEditorController.showResultPane();
				configureForTableResult(resultController, resultRef);
			}
			case ASK -> {
				tabEditorController.hideResultPane();
				QueryExecutionSupport.showAskOutcomeNotification(resultRef);
			}
			case CONSTRUCT, DESCRIBE -> {
				if (showNoResultNotificationIfEmpty(resultRef, MSG_NO_GRAPH_RESULTS)) {
					break;
				}
				tabEditorController.showResultPane();
				configureForGraphResult(resultController, resultRef);
			}
			case UPDATE -> {
				tabEditorController.hideResultPane();
				QueryExecutionSupport.showUpdateSummaryNotification(resultRef);
			}
			case UNKNOWN -> {
				tabEditorController.hideResultPane();
				NotificationWidget.getInstance().showWarning("Query executed, but result type is unknown.");
			}
		}

		context.markResultRendered(resultRef);
	}

	private boolean showNoResultNotificationIfEmpty(QueryResultRef resultRef, String message) {
		if (resultRef.getResultCount() > 0) {
			return false;
		}
		tabEditorController.hideResultPane();
		NotificationWidget.getInstance().showInfo(QUERY_NOTIFICATION_TITLE, message);
		return true;
	}

	private void configureForTableResult(ResultController controller, QueryResultRef resultRef) {
		String resultId = resultRef.getId();
		controller.configureTabsForResult(true, true, false,
				tabPreferenceSupport.preferredTab(resultRef.getQueryType())); // Text + Table
		controller.configureTextFormats(SerializationFormat.sparqlResultFormats(), SerializationFormat.XML);
		SerializationFormat preferredFormat = controller
				.getPreferredTextFormat(SerializationFormat.sparqlResultFormats(), SerializationFormat.XML);

		QueryResultRenderSupport.bindOnFormatChanged(controller, resultId, queryService);

		// Provide formatting capability to the table controller (for Export/Copy)
		controller.setFormatProvider(format -> queryService.formatResult(resultId, format));

		QueryResultRenderSupport.loadTableAndTextAsync(controller, resultId, preferredFormat, queryService);
	}

	private void configureForGraphResult(ResultController controller, QueryResultRef resultRef) {
		String resultId = resultRef.getId();
		controller.configureTabsForResult(true, false, true,
				tabPreferenceSupport.preferredTab(resultRef.getQueryType())); // Text + Graph
		controller.configureTextFormats(SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);
		SerializationFormat preferredFormat = controller.getPreferredTextFormat(SerializationFormat.rdfFormats(),
				SerializationFormat.TURTLE);

		QueryResultRenderSupport.bindOnFormatChanged(controller, resultId, queryService);
		QueryResultRenderSupport.loadGraphAndTextAsync(controller, resultId, preferredFormat, queryService);
	}

	private void registerResultTabPreference(Tab tab) {
		TabContext context = TabContext.get(tab);
		if (context == null) {
			return;
		}
		ResultController resultController = context.getResultController();
		if (resultController == null) {
			return;
		}
		resultController.setOnTabSelected(tabType -> {
			QueryResultRef resultRef = context.getQueryResultRef();
			if (resultRef == null) {
				return;
			}
			tabPreferenceSupport.rememberPreferredTab(resultRef.getQueryType(), tabType);
		});
	}

	// ==============================================================================================
	// Logic - Actions
	// ==============================================================================================

	private void onOpenFileButtonClick() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Query File");
		FileDialogState.applyInitialDirectory(fileChooser);
		FileChooser.ExtensionFilter sparqlFilter = FileTypeSupport
				.createExtensionFilter("SPARQL Query (*.rq, *.sparql)", FileTypeSupport.queryExtensions(), true);
		fileChooser.getExtensionFilters().addAll(sparqlFilter, new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setSelectedExtensionFilter(sparqlFilter);

		List<File> files = fileChooser.showOpenMultipleDialog(
				view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null);
		if (files == null || files.isEmpty()) {
			return;
		}

		FileDialogState.updateLastDirectory(files.get(files.size() - 1));
		for (File file : files) {
			openQueryFile(file);
		}
	}

	private void onTemplateButtonClick() {
		QueryTemplateDialog.show(this::insertTemplateInNewTab);
	}

	private void insertTemplateInNewTab(String queryText) {
		if (queryText == null || queryText.isBlank()) {
			return;
		}
		Tab tab = tabEditorController.createNewTab();
		updateTabContent(tab, queryText);
	}

	private void updateTabContent(Tab tab, String content) {
		if (tab == null || content == null) {
			return;
		}
		CodeEditorController editorController = tabEditorController.getEditorControllerForTab(tab);
		if (editorController == null) {
			return;
		}
		editorController.setContent(content);
	}

	private void openQueryFile(File file) {
		LOGGER.info("Loading query file: {}", file.getAbsolutePath());
		tabEditorController.openFile(file);
	}

	public TabEditorController getTabEditorController() {
		return tabEditorController;
	}
}
