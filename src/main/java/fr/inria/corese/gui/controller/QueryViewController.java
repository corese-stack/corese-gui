package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.core.ResultViewConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.factory.popup.TemplatePopup;
import fr.inria.corese.gui.manager.QueryManager;
import fr.inria.corese.gui.view.QueryView;
import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class QueryViewController {
  private final QueryView view;
  private TabEditorController tabEditorController;
  private Node emptyStateView;
  private final QueryManager stateManager = QueryManager.getInstance();

  public QueryViewController(QueryView view) {
    this.view = view;
    initialize();
  }

  private void initialize() {
    initializeEditor();
  }

  private void initializeEditor() {
    tabEditorController = new TabEditorController();

    // Configure editor toolbar
    tabEditorController.configureEditor(
        List.of(
            new ButtonConfig(IconButtonType.SAVE, "Save File"),
            new ButtonConfig(IconButtonType.CLEAR, "Clear Content"),
            new ButtonConfig(IconButtonType.UNDO, "Undo"),
            new ButtonConfig(IconButtonType.REDO, "Redo")));

    ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxWidth(Double.MAX_VALUE);
    ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxHeight(Double.MAX_VALUE);

    // Configure execution: floating button
    tabEditorController.configureExecution(
        new ButtonConfig(IconButtonType.PLAY, "Run Query"), this::executeQuery);

    // Configure result view with split pane
    tabEditorController.configureResultView(
        List.of(
            new ButtonConfig(IconButtonType.COPY, "Copy to Clipboard"),
            new ButtonConfig(IconButtonType.EXPORT, "Export Results")),
        ResultViewConfig.builder()
            .withTextTab()
            .withTableTab()
            .withGraphTab()
            .build());

    view.setMainContent(tabEditorController.getViewRoot());

    setupTabListeners();

    // Configure empty state
    Node emptyState =
        view.createEmptyState(
            () -> tabEditorController.addNewTab("untitled", ""),
            this::onOpenFilesButtonClick,
            () -> {
              Stage stage = (Stage) view.getRoot().getScene().getWindow();
              TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
            });
    tabEditorController.configureEmptyState(emptyState);

    // Configure add tab menu with Query-specific items
    tabEditorController.configureMenuItems(
        new TabEditorController.MenuItem(
            "New File", () -> tabEditorController.addNewTab("untitled", "")),
        new TabEditorController.MenuItem("Open File", this::onOpenFilesButtonClick),
        new TabEditorController.MenuItem(
            "Templates",
            () -> {
              Stage stage = (Stage) view.getRoot().getScene().getWindow();
              TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
            }));
  }

  /**
   * Configures a ResultController for Query-specific needs.
   * Removes Visual tab and adds Table and Graph tabs.
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
        resultController
            .getView()
            .getTabPane()
            .getSelectionModel()
            .select(resultController.getView().getTableTab());
        String csvResult = stateManager.getFormattedCachedQuery(queryTabId, "CSV");
        resultController.updateTableView(csvResult);

        String xmlResult = stateManager.getFormattedCachedQuery(queryTabId, "XML");
        resultController.updateText(xmlResult);
        break;
      case "CONSTRUCT", "DESCRIBE":
        resultController
            .getView()
            .getTabPane()
            .getSelectionModel()
            .select(resultController.getView().getGraphTab());
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
        tabEditorController.getControllerForTab(selectedTab);
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
                e.printStackTrace();
              }
            })
        .start();
  }

  private void onOpenFilesButtonClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open File");
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("RDF & SPARQL", "*.ttl", "*.rdf", "*.n3", "*.rq"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
    File file = fileChooser.showOpenDialog(null);
    if (file != null) openQueryFile(file);
  }

  private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  public void openQueryFile(File file) {
    for (Tab tab : tabEditorController.getTabs()) {
      String filePath = tabEditorController.getFilePathForTab(tab);
      if (filePath != null && file.getAbsolutePath().equals(filePath)) {
        tabEditorController.selectTab(tab);
        return;
      }
    }
    tabEditorController.addNewTab(file);
  }

  public TabEditorController getTabEditorController() {
    return tabEditorController;
  }
}
