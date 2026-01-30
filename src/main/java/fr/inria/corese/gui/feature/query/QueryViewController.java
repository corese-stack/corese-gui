package fr.inria.corese.gui.feature.query;

import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.config.ResultViewConfig;

import fr.inria.corese.gui.core.enums.ButtonIcon;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.manager.QueryManager;
import fr.inria.corese.gui.core.DialogHelper;
import fr.inria.corese.gui.feature.codeeditor.CodeEditorController;
import fr.inria.corese.gui.feature.tabeditor.TabEditorConfig;
import fr.inria.corese.gui.feature.tabeditor.TabEditorController;
import fr.inria.corese.gui.feature.result.ResultController;







import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryViewController {
  private static final Logger logger = LoggerFactory.getLogger(QueryViewController.class);
  private final QueryView view;
  private TabEditorController tabEditorController;
  private final QueryManager stateManager = QueryManager.getInstance();

  public QueryViewController(QueryView view) {
    this.view = view;
    initialize();
  }

  private void initialize() {
    initializeEditor();
  }

  private void initializeEditor() {
    // Configure empty state
    Node emptyState =
        view.createEmptyState(
            () -> tabEditorController.createNewTab("untitled", ""),
            this::onOpenFilesButtonClick,
            null); // Template action removed

    // Build configuration with Builder pattern
    TabEditorConfig config =
        TabEditorConfig.builder()
            .withEditorButtons(
                List.of(
                    new ButtonConfig(ButtonIcon.SAVE, "Save File"),
                    new ButtonConfig(ButtonIcon.CLEAR, "Clear Content"),
                    new ButtonConfig(ButtonIcon.UNDO, "Undo"),
                    new ButtonConfig(ButtonIcon.REDO, "Redo")))
            .withExecution(new ButtonConfig(ButtonIcon.PLAY, "Run Query"), this::executeQuery)
            .withResultView(
                List.of(
                    new ButtonConfig(ButtonIcon.COPY, "Copy to Clipboard"),
                    new ButtonConfig(ButtonIcon.EXPORT, "Export Results")),
                ResultViewConfig.builder().withTextTab().withTableTab().withGraphTab().build())
            .withEmptyState(emptyState)
            .withAllowedExtensions(List.of(".rq", ".sparql"))
            .withMenuItems(
                List.of(
                    new TabEditorConfig.MenuItem("New File", this::onNewFileButtonClick),
                    new TabEditorConfig.MenuItem("Open File", this::onOpenFilesButtonClick)))
            .withPreloadFirstTab()
            .build();

    // Create controller with complete configuration
    tabEditorController = new TabEditorController(config);

    ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxWidth(Double.MAX_VALUE);
    ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxHeight(Double.MAX_VALUE);

    view.setMainContent(tabEditorController.getViewRoot());

    setupTabListeners();
  }

  /**
   * Configures a ResultController for Query-specific needs. Removes Visual tab and adds Table and
   * Graph tabs.
   */
  private void setupTabListeners() {
    tabEditorController.addSelectionListener(
        (obs, oldTab, newTab) -> updateResultsForSelectedQueryTab(newTab));

    tabEditorController.addTabListener(
        (ListChangeListener<Tab>)
            c -> {
              while (c.next()) {
                if (c.wasRemoved()) {
                  for (Tab removedTab : c.getRemoved()) {
                    stateManager.clearCacheForTab(removedTab.hashCode());
                  }
                }
                if (c.wasAdded()) {
                  for (Tab tab : c.getAddedSubList()) {
                    Platform.runLater(() -> configureTab(tab));
                  }
                }
              }
            });
  }

  private void configureTab(Tab tab) {
    // Tab configuration if needed
  }

  private void updateResultsForSelectedQueryTab(Tab selectedQueryTab) {
    if (selectedQueryTab == null) {
      return;
    }

    ResultController resultController = tabEditorController.getCurrentResultController();
    if (resultController == null) return;

    resultController.clearResults();

    final Integer queryTabId = selectedQueryTab.hashCode();
    var cachedEntry = stateManager.getCachedResult(queryTabId);

    if (cachedEntry == null) {
      return;
    }

    String queryType = cachedEntry.getQueryType();

    switch (queryType) {
      case "SELECT", "ASK":
        // Configure tabs: SELECT results have text and table, but not graph
        resultController.configureTabsForResult(
            true, // text: enabled
            false, // visual: disabled (not used for queries)
            true, // table: enabled
            false // graph: disabled for SELECT
            );

        // Configure available formats for SPARQL results (CSV, XML, JSON, etc.)
        resultController.configureTextFormats(
            SerializationFormat.sparqlResultFormats(), SerializationFormat.XML);

        // Set up format change listener for SELECT/ASK results
        resultController.setOnFormatChanged(
            format -> {
              String formattedResult =
                  stateManager.getFormattedCachedQuery(queryTabId, format.getLabel());
              resultController.updateText(formattedResult);
            });

        resultController.getView().selectTableTab();
        String csvResult = stateManager.getFormattedCachedQuery(queryTabId, "CSV");
        resultController.updateTableView(csvResult);

        String xmlResult = stateManager.getFormattedCachedQuery(queryTabId, "XML");
        resultController.updateText(xmlResult);
        break;
      case "CONSTRUCT", "DESCRIBE":
        // Configure tabs: CONSTRUCT/DESCRIBE results have text and graph, but not table
        resultController.configureTabsForResult(
            true, // text: enabled
            false, // visual: disabled (not used for queries)
            false, // table: disabled for CONSTRUCT
            true // graph: enabled
            );

        // Configure available formats for RDF graph results (Turtle, JSON-LD, etc.)
        resultController.configureTextFormats(
            SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);

        // Set up format change listener for CONSTRUCT/DESCRIBE results
        resultController.setOnFormatChanged(
            format -> {
              String formattedResult =
                  stateManager.getFormattedCachedQuery(queryTabId, format.getLabel());
              resultController.updateText(formattedResult);
            });

        resultController.getView().selectGraphTab();
        String turtleResult = stateManager.getFormattedCachedQuery(queryTabId, "TURTLE");
        resultController.displayGraph(turtleResult);
        resultController.updateText(turtleResult);
        break;
      default:
        break;
    }
  }

  public void executeQuery() {
    Tab selectedTab = tabEditorController.getSelectedTab();
    if (selectedTab == null) {
      return;
    }

    ResultController resultController = tabEditorController.getCurrentResultController();
    if (resultController == null) return;

    CodeEditorController codeEditorController =
        tabEditorController.getEditorControllerForTab(selectedTab);
    if (codeEditorController == null) {
      return;
    }

    final String queryContent = codeEditorController.getView().getText();
    final Integer tabId = selectedTab.hashCode();

    resultController.clearResults();
    stateManager.clearCacheForTab(tabId);

    tabEditorController.setExecutionState(true);

    new Thread(
            () -> {
              try {
                stateManager.executeAndCacheQuery(queryContent, tabId);
                var cachedEntry = stateManager.getCachedResult(tabId);

                Platform.runLater(
                    () -> {
                      tabEditorController.setExecutionState(false);
                      if (cachedEntry != null) {
                        tabEditorController.showResultPane();
                        updateResultsForSelectedQueryTab(selectedTab);
                      }
                    });

              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      tabEditorController.setExecutionState(false);
                      showError("Query Execution Error", e.getMessage());
                    });
                logger.error("Error executing query", e);
              }
            })
        .start();
  }

  private void onOpenFilesButtonClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Query File");
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("SPARQL Query", "*.rq", "*.sparql"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
    File file = fileChooser.showOpenDialog(null);
    if (file != null) openQueryFile(file);
  }

  private void showError(String title, String message) {
    DialogHelper.showError(title, message);
  }

  public void openQueryFile(File file) {
    for (Tab tab : tabEditorController.getTabs()) {
      String filePath = tabEditorController.getFilePathForTab(tab);
      if (filePath != null && file.getAbsolutePath().equals(filePath)) {
        tabEditorController.selectTab(tab);
        return;
      }
    }
    tabEditorController.openFile(file);
  }

  private void onNewFileButtonClick() {
    tabEditorController.createNewTab("untitled", "");
  }

  public TabEditorController getTabEditorController() {
    return tabEditorController;
  }
}
