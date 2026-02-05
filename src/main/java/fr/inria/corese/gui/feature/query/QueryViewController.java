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
import fr.inria.corese.gui.feature.editor.code.CodeEditorController;
import fr.inria.corese.gui.feature.editor.tab.TabContext;
import fr.inria.corese.gui.feature.editor.tab.TabEditorConfig;
import fr.inria.corese.gui.feature.editor.tab.TabEditorController;
import fr.inria.corese.gui.feature.result.ResultController;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;

/**
 * Controller for the Query feature.
 *
 * <p>
 * Orchestrates the query editing and execution workflow. It embeds a
 * {@link TabEditorController}
 * for the editor interface and manages the interaction with the
 * {@link QueryService} for execution.
 */
public class QueryViewController {

    private static final Logger logger = LoggerFactory.getLogger(QueryViewController.class);

    private final QueryView view;
    private final QueryService queryService = QueryService.getInstance();
    private TabEditorController tabEditorController;

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
                        ButtonFactory.save(null),
                        ButtonFactory.clear(null),
                        ButtonFactory.undo(null),
                        ButtonFactory.redo(null)))
                .withExecution(
                        ButtonFactory.custom(ButtonIcon.PLAY, "Run Query", null),
                        this::executeQuery)
                .withResultView(
                        List.of(
                                ButtonFactory.copy(null),
                                ButtonFactory.export(null)),
                        ResultViewConfig.builder().withTextTab().withTableTab().withGraphTab().build())
                .withEmptyState(emptyState)
                .withAllowedExtensions(List.of(".rq", ".sparql"))
                .withMenuItems(List.of(
                        new TabEditorConfig.MenuItem("New File", this::onNewFileButtonClick),
                        new TabEditorConfig.MenuItem("Open File", this::onOpenFilesButtonClick)))
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
        new Thread(() -> {
            try {
                QueryResultRef resultRef = queryService.executeQuery(queryContent);

                Platform.runLater(() -> {
                    tabEditorController.setExecutionState(false);
                    if (resultRef != null) {
                        // Store result in context (Single Source of Truth)
                        context.setQueryResultRef(resultRef);

                        tabEditorController.showResultPane();
                        updateResultsForSelectedQueryTab(selectedTab);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    tabEditorController.setExecutionState(false);
                    ModalService.getInstance().showError("Query Execution Error", e.getMessage());
                });
                logger.error("Error executing query", e);
            }
        }).start();
    }

    private void updateResultsForSelectedQueryTab(Tab selectedQueryTab) {
        if (selectedQueryTab == null)
            return;

        TabContext context = TabContext.get(selectedQueryTab);
        if (context == null)
            return;

        ResultController resultController = context.getResultController();
        if (resultController == null)
            return;

        resultController.clearResults();

        QueryResultRef resultRef = context.getQueryResultRef();
        if (resultRef == null)
            return;

        QueryType queryType = resultRef.getQueryType();
        // Configure view based on query type (SELECT/ASK vs CONSTRUCT/DESCRIBE)
        switch (queryType) {
            case SELECT, ASK -> configureForTableResult(resultController, resultRef);
            case CONSTRUCT, DESCRIBE -> configureForGraphResult(resultController, resultRef);
            default -> {
                resultController.configureTabsForResult(true, false, false);
                resultController.selectTextTab();
                resultController.updateText("No result available for this query type.");
            }
        }
    }

    private void configureForTableResult(ResultController controller, QueryResultRef resultRef) {
        String resultId = resultRef.getId();
        controller.configureTabsForResult(true, true, false); // Text + Table
        controller.configureTextFormats(SerializationFormat.sparqlResultFormats(), SerializationFormat.XML);

        controller.setOnFormatChanged(format -> {
            String formattedResult = queryService.formatResult(resultId, format);
            controller.updateText(formattedResult);
        });

        // Provide formatting capability to the table controller (for Export/Copy)
        controller.setFormatProvider(format -> queryService.formatResult(resultId, format));

        // Default view: Table
        controller.selectTableTab();
        controller.updateTableView(queryService.formatResult(resultId, SerializationFormat.CSV));
        controller.updateText(queryService.formatResult(resultId, SerializationFormat.XML));
    }

    private void configureForGraphResult(ResultController controller, QueryResultRef resultRef) {
        String resultId = resultRef.getId();
        controller.configureTabsForResult(true, false, true); // Text + Graph
        controller.configureTextFormats(SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);

        controller.setOnFormatChanged(format -> {
            String formattedResult = queryService.formatResult(resultId, format);
            controller.updateText(formattedResult);
        });

        // Default view: Graph
        controller.selectGraphTab();
        controller.displayGraph(queryService.formatResult(resultId, SerializationFormat.JSON_LD));
        controller.updateText(queryService.formatResult(resultId, SerializationFormat.TURTLE));
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
                new FileChooser.ExtensionFilter("All Files", "*.*"));

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