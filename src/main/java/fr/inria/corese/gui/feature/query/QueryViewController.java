package fr.inria.corese.gui.feature.query;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResultRef;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.QueryService;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.feature.editor.code.CodeEditorController;
import fr.inria.corese.gui.feature.editor.tab.TabContext;
import fr.inria.corese.gui.feature.editor.tab.TabEditorConfig;
import fr.inria.corese.gui.feature.editor.tab.TabEditorController;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.utils.AppExecutors;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;
import java.util.prefs.Preferences;

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
	private static final Preferences PREFS = Preferences.userNodeForPackage(QueryViewController.class);
	private static final String PREF_LAST_TABLE_TAB = "results.lastTab.table";
	private static final String PREF_LAST_GRAPH_TAB = "results.lastTab.graph";
	private static ResultViewConfig.TabType lastTableTab = loadTabPreference(PREF_LAST_TABLE_TAB);
	private static ResultViewConfig.TabType lastGraphTab = loadTabPreference(PREF_LAST_GRAPH_TAB);

	private final QueryView view;
	private final QueryService queryService = QueryService.getInstance();
	private TabEditorController tabEditorController;

	public QueryViewController(QueryView view) {
		this.view = view;
		initialize();
	}

	private void initialize() {
		// 1. Configure Empty State
		Node emptyState = view.createEmptyState(() -> tabEditorController.createNewTab("untitled", ""),
				this::onOpenFilesButtonClick, null // Template action not yet implemented
		);

		// 2. Configure Editor
		TabEditorConfig config = TabEditorConfig.builder()
				.withEditorButtons(List.of(ButtonFactory.save(null), ButtonFactory.undo(null), ButtonFactory.redo(null)))
				.withExecution(ButtonFactory.custom(ButtonIcon.PLAY, "Run Query", null), this::executeQuery)
				.withResultView(List.of(ButtonFactory.copy(null), ButtonFactory.export(null)),
						ResultViewConfig.builder().withTextTab().withTableTab().withGraphTab().build())
				.withEmptyState(emptyState).withAllowedExtensions(List.of(".rq"))
				.withMenuItems(List.of(new TabEditorConfig.MenuItem("New File", this::onNewFileButtonClick),
						new TabEditorConfig.MenuItem("Open File", this::onOpenFilesButtonClick)))
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
		tabEditorController.addSelectionListener((obs, oldTab, newTab) -> updateResultsForSelectedQueryTab(newTab, false));
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

						tabEditorController.showResultPane();
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
		// Configure view based on query type (SELECT/ASK vs CONSTRUCT/DESCRIBE)
		switch (queryType) {
			case SELECT, ASK -> configureForTableResult(resultController, resultRef);
			case CONSTRUCT, DESCRIBE -> configureForGraphResult(resultController, resultRef);
			default -> {
				resultController.configureTabsForResult(true, false, false, ResultViewConfig.TabType.TEXT);
				resultController.updateText("No result available for this query type.");
			}
		}

		context.markResultRendered(resultRef);
	}

	private void configureForTableResult(ResultController controller, QueryResultRef resultRef) {
		String resultId = resultRef.getId();
		controller.configureTabsForResult(true, true, false, getPreferredTab(resultRef.getQueryType())); // Text + Table
		controller.configureTextFormats(SerializationFormat.sparqlResultFormats(), SerializationFormat.XML);
		SerializationFormat preferredFormat = controller
				.getPreferredTextFormat(SerializationFormat.sparqlResultFormats(), SerializationFormat.XML);

		controller.setOnFormatChanged(format -> {
			AppExecutors.execute(() -> {
				String formattedResult = queryService.formatResult(resultId, format);
				Platform.runLater(() -> controller.updateText(formattedResult));
			});
		});

		// Provide formatting capability to the table controller (for Export/Copy)
		controller.setFormatProvider(format -> queryService.formatResult(resultId, format));

		AppExecutors.execute(() -> {
			String csvResult = queryService.formatResult(resultId, SerializationFormat.CSV);
			String textResult = queryService.formatResult(resultId, preferredFormat);
			Platform.runLater(() -> {
				controller.updateTableView(csvResult);
				controller.updateText(textResult);
			});
		});
	}

	private void configureForGraphResult(ResultController controller, QueryResultRef resultRef) {
		String resultId = resultRef.getId();
		controller.configureTabsForResult(true, false, true, getPreferredTab(resultRef.getQueryType())); // Text + Graph
		controller.configureTextFormats(SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);
		SerializationFormat preferredFormat = controller.getPreferredTextFormat(SerializationFormat.rdfFormats(),
				SerializationFormat.TURTLE);

		controller.setOnFormatChanged(format -> {
			AppExecutors.execute(() -> {
				String formattedResult = queryService.formatResult(resultId, format);
				Platform.runLater(() -> controller.updateText(formattedResult));
			});
		});

		AppExecutors.execute(() -> {
			String jsonLdResult = queryService.formatResult(resultId, SerializationFormat.JSON_LD);
			String textResult = queryService.formatResult(resultId, preferredFormat);
			Platform.runLater(() -> {
				controller.displayGraph(jsonLdResult);
				controller.updateText(textResult);
			});
		});
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
			rememberPreferredTab(resultRef.getQueryType(), tabType);
		});
	}

	private static ResultViewConfig.TabType getPreferredTab(QueryType queryType) {
		if (queryType == QueryType.SELECT || queryType == QueryType.ASK) {
			return isTableTab(lastTableTab) ? lastTableTab : null;
		}
		if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
			return isGraphTab(lastGraphTab) ? lastGraphTab : null;
		}
		return null;
	}

	private static void rememberPreferredTab(QueryType queryType, ResultViewConfig.TabType tabType) {
		if (queryType == null || tabType == null) {
			return;
		}
		if (queryType == QueryType.SELECT || queryType == QueryType.ASK) {
			if (!isTableTab(tabType)) {
				return;
			}
			lastTableTab = tabType;
			PREFS.put(PREF_LAST_TABLE_TAB, tabType.name());
		} else if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
			if (!isGraphTab(tabType)) {
				return;
			}
			lastGraphTab = tabType;
			PREFS.put(PREF_LAST_GRAPH_TAB, tabType.name());
		}
	}

	private static boolean isTableTab(ResultViewConfig.TabType tabType) {
		return tabType == ResultViewConfig.TabType.TABLE || tabType == ResultViewConfig.TabType.TEXT;
	}

	private static boolean isGraphTab(ResultViewConfig.TabType tabType) {
		return tabType == ResultViewConfig.TabType.GRAPH || tabType == ResultViewConfig.TabType.TEXT;
	}

	private static ResultViewConfig.TabType loadTabPreference(String key) {
		String value = PREFS.get(key, null);
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return ResultViewConfig.TabType.valueOf(value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	// ==============================================================================================
	// Logic - Actions
	// ==============================================================================================

	private void onNewFileButtonClick() {
		tabEditorController.createNewTab("untitled", "");
	}

	private void onOpenFilesButtonClick() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Query File");
		FileDialogState.applyInitialDirectory(fileChooser);
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("SPARQL Query", "*.rq", "*.sparql"),
				new FileChooser.ExtensionFilter("All Files", "*.*"));

		File file = fileChooser
				.showOpenDialog(view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null);
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			openQueryFile(file);
		}
	}

	private void openQueryFile(File file) {
		// Check if already open
		for (Tab tab : tabEditorController.getTabs()) {
			String filePath = tabEditorController.getFilePathForTab(tab);
			if (filePath != null && file.getAbsolutePath().equals(filePath)) {
				tabEditorController.selectTab(tab);
				return;
			}
		}
		LOGGER.info("Loading query file: {}", file.getAbsolutePath());
		tabEditorController.openFile(file);
	}

	public TabEditorController getTabEditorController() {
		return tabEditorController;
	}
}
