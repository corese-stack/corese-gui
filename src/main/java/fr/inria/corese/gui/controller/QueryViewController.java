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
    tabEditorController =
        new TabEditorController(
            List.of(
                IconButtonType.SAVE,
                IconButtonType.CLEAR,
                IconButtonType.UNDO,
                IconButtonType.REDO));
    tabEditorController.getView().setMaxWidth(Double.MAX_VALUE);
    tabEditorController.getView().setMaxHeight(Double.MAX_VALUE);

    tabEditorController.setResultControllerFactory(tab -> createResultController());
    tabEditorController.setOnExecutionRequest(this::executeQuery);

    view.setMainContent(tabEditorController.getViewRoot());

    tabEditorController.addExecutionButton("Run Query (Ctrl+Enter)");
    setupTabListeners();
    setupKeyboardShortcuts();
    
    // Create empty state via View
    Node emptyState = view.createEmptyState(
        () -> tabEditorController.addNewTab("untitled", ""),
        this::onOpenFilesButtonClick,
        () -> {
             Stage stage = (Stage) view.getRoot().getScene().getWindow();
             TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
        }
    );
    tabEditorController.setEmptyState(emptyState);

    // Configure TabEditor Menu Actions
    tabEditorController.setOnOpenFileAction(e -> onOpenFilesButtonClick());
    tabEditorController
        .getView()
        .getTemplatesItem()
        .setOnAction(
            e -> {
              Stage stage = (Stage) view.getRoot().getScene().getWindow();
              TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
            });
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
        Tab selectedQueryTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
        if (selectedQueryTab != null) {
             // Re-display logic if needed
        }
    });
    
    return controller;
  }

  private void setupTabListeners() {
    tabEditorController
        .getView()
        .getTabPane()
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, oldTab, newTab) -> updateResultsForSelectedQueryTab(newTab));

    tabEditorController
        .getView()
        .getTabPane()
        .getTabs()
        .addListener(
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
    CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
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
    Tab selectedTab =
        tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab == null) {
      return;
    }
    
    ResultController resultController = tabEditorController.getCurrentResultController();
    if (resultController == null) return;

    CodeEditorController codeEditorController =
        tabEditorController.getModel().getControllerForTab(selectedTab);
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
                        Tab selectedTab =
                            tabEditorController
                                .getView()
                                .getTabPane()
                                .getSelectionModel()
                                .getSelectedItem();
                        tabEditorController.handleCloseFile(selectedTab);
                        event.consume();
                      } else if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        Tab selectedTab =
                            tabEditorController
                                .getView()
                                .getTabPane()
                                .getSelectionModel()
                                .getSelectedItem();
                        if (selectedTab != null) {
                          CodeEditorController controller =
                              tabEditorController.getModel().getControllerForTab(selectedTab);
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
    for (Tab tab : tabEditorController.getView().getTabPane().getTabs()) {
      CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
      if (controller != null
          && file.getAbsolutePath().equals(controller.getModel().getFilePath())) {
        tabEditorController.getView().getTabPane().getSelectionModel().select(tab);
        return;
      }
    }
    tabEditorController.addNewTab(file);
  }

  public TabEditorController getTabEditorController() {
    return tabEditorController;
  }
}
