package fr.inria.corese.gui.feature.query;

import fr.inria.corese.gui.core.DialogHelper;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.manager.QueryManager;
import fr.inria.corese.gui.core.model.QueryResultRef;
import fr.inria.corese.gui.feature.codeeditor.CodeEditorController;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.feature.tabeditor.TabEditorConfig;
import fr.inria.corese.gui.feature.tabeditor.TabEditorController;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Query feature.
 *
 * <p>Orchestrates the query editing and execution workflow. It embeds a {@link TabEditorController}
 * for the editor interface and manages the interaction with the {@link QueryManager} for execution.
 */
public class QueryViewController {

    private static final Logger logger = LoggerFactory.getLogger(QueryViewController.class);

    private final QueryView view;
    private final QueryManager stateManager = QueryManager.getInstance();
    private TabEditorController tabEditorController;
    private final Map<Tab, QueryResultRef> tabResults = new HashMap<>();

    public QueryViewController(QueryView view) {
        this.view = view;
        initialize();
    }

    private void initialize() {
        // 1. Configure Empty State
        Node emptyState = view.createEmptyState(
            () -> tabEditorController.createNewTab("untitled", ""),
            this::onOpenFilesButtonClick,
            null // Template action not yet implemented
        );

        // 2. Configure Editor
        TabEditorConfig config = TabEditorConfig.builder()
            .withEditorButtons(List.of(
                new ButtonConfig(ButtonIcon.SAVE, "Save File"),
                new ButtonConfig(ButtonIcon.CLEAR, "Clear Content"),
                new ButtonConfig(ButtonIcon.UNDO, "Undo"),
                new ButtonConfig(ButtonIcon.REDO, "Redo")
            ))
            .withExecution(
                new ButtonConfig(ButtonIcon.PLAY, "Run Query"),
                this::executeQuery
            )
            .withResultView(
                List.of(
                    new ButtonConfig(ButtonIcon.COPY, "Copy to Clipboard"),
                    new ButtonConfig(ButtonIcon.EXPORT, "Export Results")
                ),
                ResultViewConfig.builder().withTextTab().withTableTab().withGraphTab().build()
            )
            .withEmptyState(emptyState)
            .withAllowedExtensions(List.of(".rq", ".sparql"))
            .withMenuItems(List.of(
                new TabEditorConfig.MenuItem("New File", this::onNewFileButtonClick),
                new TabEditorConfig.MenuItem("Open File", this::onOpenFilesButtonClick)
            ))
            .withPreloadFirstTab()
            .build();

        // 3. Create Controller
        this.tabEditorController = new TabEditorController(config);
        
        // Ensure the editor fills the space
        ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxWidth(Double.MAX_VALUE);
        ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxHeight(Double.MAX_VALUE);

        view.setMainContent(tabEditorController.getViewRoot());

        setupTabListeners();
    }

    private void setupTabListeners() {
        tabEditorController.addSelectionListener(
            (obs, oldTab, newTab) -> updateResultsForSelectedQueryTab(newTab));

        tabEditorController.addTabListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Tab removedTab : c.getRemoved()) {
                        QueryResultRef ref = tabResults.remove(removedTab);
                        if (ref != null) {
                            stateManager.releaseResult(ref.getId());
                        }
                    }
                }
                // Added tabs handled by TabEditorController logic mostly, 
                // but we can add specific config here if needed.
            }
        });
    }

    // ==============================================================================================
    // Logic - Query Execution & Results
    // ==============================================================================================

    public void executeQuery() {
        Tab selectedTab = tabEditorController.getSelectedTab();
        if (selectedTab == null) return;

        ResultController resultController = tabEditorController.getCurrentResultController();
        CodeEditorController codeEditor = tabEditorController.getEditorControllerForTab(selectedTab);
        
        if (resultController == null || codeEditor == null) return;

        final String queryContent = codeEditor.getContent();
        QueryResultRef previousRef = tabResults.remove(selectedTab);
        if (previousRef != null) {
            stateManager.releaseResult(previousRef.getId());
        }

        // Prepare UI
        resultController.clearResults();
        tabEditorController.setExecutionState(true);

        // Execute in background
        new Thread(() -> {
            try {
                QueryResultRef resultRef = stateManager.execute(queryContent);

                Platform.runLater(() -> {
                    tabEditorController.setExecutionState(false);
                    if (resultRef != null) {
                        tabResults.put(selectedTab, resultRef);
                        tabEditorController.showResultPane();
                        updateResultsForSelectedQueryTab(selectedTab);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    tabEditorController.setExecutionState(false);
                    DialogHelper.showError("Query Execution Error", e.getMessage());
                });
                logger.error("Error executing query", e);
            }
        }).start();
    }

    private void updateResultsForSelectedQueryTab(Tab selectedQueryTab) {
        if (selectedQueryTab == null) return;

        ResultController resultController = tabEditorController.getCurrentResultController();
        if (resultController == null) return;

        resultController.clearResults();

        QueryResultRef resultRef = tabResults.get(selectedQueryTab);
        if (resultRef == null) return;

        QueryType queryType = resultRef.getQueryType();
        // Configure view based on query type (SELECT/ASK vs CONSTRUCT/DESCRIBE)
        if (queryType == QueryType.SELECT || queryType == QueryType.ASK) {
            configureForTableResult(resultController, resultRef);
        } else if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
            configureForGraphResult(resultController, resultRef);
        } else {
            resultController.configureTabsForResult(true, false, false);
            resultController.selectTextTab();
            resultController.updateText("No result available for this query type.");
        }
    }

    private void configureForTableResult(ResultController controller, QueryResultRef resultRef) {
        String resultId = resultRef.getId();
        controller.configureTabsForResult(true, true, false); // Text + Table
        controller.configureTextFormats(SerializationFormat.sparqlResultFormats(), SerializationFormat.XML);

        controller.setOnFormatChanged(format -> {
            String formattedResult = stateManager.formatResult(resultId, format.getLabel());
            controller.updateText(formattedResult);
        });

        // Default view: Table
        controller.selectTableTab();
        controller.updateTableView(stateManager.formatResult(resultId, "CSV"));
        controller.updateText(stateManager.formatResult(resultId, "XML"));
    }

    private void configureForGraphResult(ResultController controller, QueryResultRef resultRef) {
        String resultId = resultRef.getId();
        controller.configureTabsForResult(true, false, true); // Text + Graph
        controller.configureTextFormats(SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);

        controller.setOnFormatChanged(format -> {
            String formattedResult = stateManager.formatResult(resultId, format.getLabel());
            controller.updateText(formattedResult);
        });

        // Default view: Graph
        controller.selectGraphTab();
        controller.displayGraph(stateManager.formatResult(resultId, "JSON-LD"));
        controller.updateText(stateManager.formatResult(resultId, "TURTLE"));
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
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("SPARQL Query", "*.rq", "*.sparql"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
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
        tabEditorController.openFile(file);
    }

    public TabEditorController getTabEditorController() {
        return tabEditorController;
    }
}
