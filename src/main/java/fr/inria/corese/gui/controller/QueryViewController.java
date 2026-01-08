package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.factory.popup.TemplatePopup;
import fr.inria.corese.gui.manager.QueryManager;
import fr.inria.corese.gui.view.EmptyStateViewFactory;
import fr.inria.corese.gui.view.QueryView;
import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
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
            IconButtonType.SAVE,
            IconButtonType.CLEAR,
            IconButtonType.UNDO,
            IconButtonType.REDO));
    
    ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxWidth(Double.MAX_VALUE);
    ((javafx.scene.layout.Region) tabEditorController.getViewRoot()).setMaxHeight(Double.MAX_VALUE);

    // Configure execution: floating button + Ctrl+Enter shortcut
    tabEditorController.configureExecution("Run Query (Ctrl+Enter)", this::executeQuery);

    // Configure result view with split pane
    tabEditorController.configureResultView(tab -> createResultController());

    view.setMainContent(tabEditorController.getViewRoot());

    setupTabListeners();
    setupKeyboardShortcuts();
    
    // Configure empty state
    Node emptyState = view.createEmptyState(
        () -> tabEditorController.addNewTab("untitled", ""),
        this::onOpenFilesButtonClick,
        () -> {
             Stage stage = (Stage) view.getRoot().getScene().getWindow();
             TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
        }
    );
    tabEditorController.configureEmptyState(emptyState);

    // Configure add tab menu with Query-specific items
    tabEditorController.configureMenuItems(
        new TabEditorController.MenuItem("New File", 
            () -> tabEditorController.addNewTab("untitled", "")),
        new TabEditorController.MenuItem("Open File", 
            this::onOpenFilesButtonClick),
        new TabEditorController.MenuItem("Templates",
            () -> {
              Stage stage = (Stage) view.getRoot().getScene().getWindow();
              TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
            })
    );
  }

  private ResultController createResultController() {
    ResultController controller = new ResultController(List.of(IconButtonType.COPY, IconButtonType.EXPORT));
    
    // Configure tabs for Query View: Remove Visual, Add Table and Graph
    TabPane resultTabs = controller.getView().getTabPane();
    resultTabs.getTabs().remove(controller.getView().getVisualTab());
    if (!resultTabs.getTabs().contains(controller.getView().getTableTab())) {
      resultTabs.getTabs().add(controller.getView().getTableTab());
    }
    if (!resultTabs.getTabs().contains(controller.getView().getGraphTab())) {
      resultTabs.getTabs().add(controller.getView().getGraphTab());
    }

    controller.setOnFormatChanged(newVal -> {
        Tab selectedQueryTab = tabEditorController.getSelectedTab();
        if (selectedQueryTab != null) {
             // Re-display logic if needed
        }
    });
    
    return controller;
  }

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
    CodeEditorController controller = tabEditorController.getControllerForTab(tab);
    if (controller != null) {
      controller
          .getView()
          .setOnKeyPressed(
              event -> {
                if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)
                    .match(event)) {
                  executeQuery();
                  event.consume();
                }
              });
    }
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
                Platform.runLater(() -> {
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

  private void setupKeyboardShortcuts() {
    view.getRoot()
        .sceneProperty()
        .addListener(
            (obs, oldScene, newScene) -> {
              if (newScene != null) {
                newScene.addEventFilter(
                    KeyEvent.KEY_PRESSED,
                    event -> {
                      if (new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        onOpenFilesButtonClick();
                        event.consume();
                      } else if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        tabEditorController.addNewTab("untitled", "");
                        event.consume();
                      } else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        Tab selectedTab = tabEditorController.getSelectedTab();
                        tabEditorController.handleCloseFile(selectedTab);
                        event.consume();
                      } else if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        Tab selectedTab = tabEditorController.getSelectedTab();
                        if (selectedTab != null) {
                          CodeEditorController controller =
                              tabEditorController.getControllerForTab(selectedTab);
                          if (controller != null) {
                            controller.saveFile();
                          }
                        }
                        event.consume();
                      } else if (new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        Stage stage = (Stage) view.getRoot().getScene().getWindow();
                        TemplatePopup.show(
                            stage, query -> tabEditorController.addNewTab("untitled", query));
                        event.consume();
                      }
                    });
              }
            });
  }

  public void openQueryFile(File file) {
    for (Tab tab : tabEditorController.getTabs()) {
      String filePath = tabEditorController.getFilePathForTab(tab);
      if (filePath != null
          && file.getAbsolutePath().equals(filePath)) {
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
