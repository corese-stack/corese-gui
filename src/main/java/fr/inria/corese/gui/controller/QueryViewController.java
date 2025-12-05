package fr.inria.corese.gui.controller;

import java.io.File;

import fr.inria.corese.gui.enums.icon.IconButtonBarType;
import fr.inria.corese.gui.factory.popup.TemplatePopup;
import fr.inria.corese.gui.manager.QueryManager;
import fr.inria.corese.gui.view.EmptyStateViewFactory;
import fr.inria.corese.gui.view.QueryView;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

public class QueryViewController {
  private final QueryView view;
  private StackPane editorContainer;
  private TabEditorController tabEditorController;
  private ResultController resultController;
  private Node emptyStateView;
  private final QueryManager stateManager = QueryManager.getInstance();

  public QueryViewController(QueryView view) {
    this.view = view;
    this.editorContainer = view.getEditorContainer();
    initialize();
  }

  private void initialize() {
    tabEditorController = new TabEditorController(IconButtonBarType.QUERY);
    tabEditorController.getView().setMaxWidth(Double.MAX_VALUE);
    tabEditorController.getView().setMaxHeight(Double.MAX_VALUE);

    // Wrap in StackPane for EmptyState and Floating Button
    editorContainer.getChildren().add(tabEditorController.getView());
    view.setEditorView(editorContainer);

    resultController = new ResultController();
    view.setResultView(resultController.getView().getRoot());

    // Configure tabs for Query View: Remove Visual, Add Table and Graph
    TabPane resultTabs = resultController.getView().getTabPane();
    resultTabs.getTabs().remove(resultController.getView().getVisualTab());
    if (!resultTabs.getTabs().contains(resultController.getView().getTableTab())) {
        resultTabs.getTabs().add(resultController.getView().getTableTab());
    }
    if (!resultTabs.getTabs().contains(resultController.getView().getGraphTab())) {
        resultTabs.getTabs().add(resultController.getView().getGraphTab());
    }

    setupFloatingRunButton();
    setupTabListeners();
    setupKeyboardShortcuts();
    setupEmptyState();
    
    // Configure TabEditor Menu Actions
    tabEditorController.getView().getOpenFileItem().setOnAction(e -> onOpenFilesButtonClick());
    tabEditorController.getView().getTemplatesItem().setOnAction(e -> {
         Stage stage = (Stage) view.getRoot().getScene().getWindow();
         TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
    });
    
    // Result format listener
    resultController.setOnFormatChanged(newVal -> {
        Tab selectedQueryTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
        if (selectedQueryTab != null && selectedQueryTab != tabEditorController.getView().getAddTab()) {
            // We need to re-fetch or re-format data. 
            // For now, let's assume we just re-display if we have cached result.
            // But ResultController handles text update. 
            // We might need to ask QueryManager to format again.
            // This part might need refinement as ResultController logic was slightly different.
            // For simplicity, we'll leave it for now or implement if needed.
        }
    });
  }

  private void setupFloatingRunButton() {
    Button runButton = new Button();
    FontIcon playIcon = new FontIcon(MaterialDesignP.PLAY);
    playIcon.setIconSize(24);
    playIcon.setIconColor(javafx.scene.paint.Color.WHITE);
    runButton.setGraphic(playIcon);
    
    runButton.getStyleClass().add("floating-validate-button"); // Reuse the same style class
    
    runButton.setTooltip(new Tooltip("Run Query (Ctrl+Enter)"));
    runButton.setOnAction(e -> executeQuery());
    
    // Bind visibility
    runButton.visibleProperty().bind(tabEditorController.getView().visibleProperty());
    runButton.managedProperty().bind(tabEditorController.getView().visibleProperty());
    
    // Disable logic
    tabEditorController.getView().getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> 
        updateRunButtonState(runButton));
    
    StackPane.setAlignment(runButton, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(runButton, new Insets(0, 80, 50, 0));
    
    editorContainer.getChildren().add(runButton);
  }

  private void updateRunButtonState(Button runButton) {
      Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
      if (selectedTab == null || selectedTab == tabEditorController.getView().getAddTab()) {
          runButton.disableProperty().unbind();
          runButton.setDisable(true);
          return;
      }
      
      CodeEditorController controller = tabEditorController.getModel().getControllerForTab(selectedTab);
      if (controller != null) {
          runButton.disableProperty().bind(controller.getModel().contentProperty().isEmpty());
      } else {
          runButton.disableProperty().unbind();
          runButton.setDisable(true);
      }
  }

  private void setupTabListeners() {
    tabEditorController.getView().getTabPane().getSelectionModel().selectedItemProperty()
        .addListener((obs, oldTab, newTab) -> updateResultsForSelectedQueryTab(newTab));

    tabEditorController.getView().getTabPane().getTabs().addListener((ListChangeListener<Tab>) c -> {
        while (c.next()) {
            if (c.wasRemoved()) {
                for (Tab removedTab : c.getRemoved()) {
                    stateManager.clearCacheForTab(removedTab.hashCode());
                }
            }
            if (c.wasAdded()) {
                for (Tab tab : c.getAddedSubList()) {
                    if (tab != tabEditorController.getView().getAddTab()) {
                        Platform.runLater(() -> configureTab(tab));
                    }
                }
            }
        }
        Platform.runLater(this::updateEmptyStateVisibility);
    });
  }

  private void configureTab(Tab tab) {
      CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
      if (controller != null) {
          controller.getView().setOnKeyPressed(event -> {
              if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)) {
                  executeQuery();
                  event.consume();
              }
          });
      }
  }

  private void setupEmptyState() {
      // Empty state logic similar to ValidationController but for Query
      // We can reuse EmptyStateViewFactory or create custom one
      // For now, let's rely on the listener in initialize() that sets it up when scene is ready
      // or just call it here if scene is available (it might not be yet)
      
      view.getRoot().sceneProperty().addListener((obs, oldScene, newScene) -> {
          if (newScene != null) {
              Stage stage = (Stage) newScene.getWindow();
              emptyStateView = EmptyStateViewFactory.createQueryEmptyStateView(
                  () -> tabEditorController.addNewTab("untitled", ""),
                  this::onOpenFilesButtonClick,
                  s -> TemplatePopup.show(s, query -> tabEditorController.addNewTab("untitled", query)),
                  stage
              );
              editorContainer.getChildren().add(0, emptyStateView);
              updateEmptyStateVisibility();
          }
      });
  }

  private void updateEmptyStateVisibility() {
    long realTabCount = tabEditorController.getView().getTabPane().getTabs().stream()
            .filter(t -> t != tabEditorController.getView().getAddTab())
            .count();
    boolean noTabsOpen = (realTabCount == 0);

    if (emptyStateView != null) {
      emptyStateView.setVisible(noTabsOpen);
      emptyStateView.setManaged(noTabsOpen);
    }
    tabEditorController.getView().setVisible(!noTabsOpen);
    tabEditorController.getView().setManaged(!noTabsOpen);
  }

  private void updateResultsForSelectedQueryTab(Tab selectedQueryTab) {
    resultController.clearResults();

    if (selectedQueryTab == null || selectedQueryTab == tabEditorController.getView().getAddTab()) {
      return;
    }

    final Integer queryTabId = selectedQueryTab.hashCode();
    var cachedEntry = stateManager.getCachedResult(queryTabId);

    if (cachedEntry == null) {
      return;
    }

    String queryType = cachedEntry.getQueryType();
    // We need to access tabs from ResultView via ResultController
    // ResultController should expose methods to select tabs or we access view directly
    
    switch (queryType) {
      case "SELECT", "ASK":
        resultController.getView().getTabPane().getSelectionModel().select(resultController.getView().getTableTab());
        String csvResult = stateManager.getFormattedCachedQuery(queryTabId, "CSV");
        resultController.updateTableView(csvResult);
        
        String xmlResult = stateManager.getFormattedCachedQuery(queryTabId, "XML");
        resultController.updateText(xmlResult);
        break;
      case "CONSTRUCT", "DESCRIBE":
        resultController.getView().getTabPane().getSelectionModel().select(resultController.getView().getGraphTab());
        String turtleResult = stateManager.getFormattedCachedQuery(queryTabId, "TURTLE");
        resultController.displayGraph(turtleResult);
        resultController.updateText(turtleResult);
        break;
      default:
        break;
    }
  }

  public void executeQuery() {
    Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab == null || selectedTab == tabEditorController.getView().getAddTab()) {
      return;
    }

    CodeEditorController codeEditorController = tabEditorController.getModel().getControllerForTab(selectedTab);
    if (codeEditorController == null) {
      return;
    }

    final String queryContent = codeEditorController.getView().getText();
    final Integer tabId = selectedTab.hashCode();

    resultController.clearResults();
    stateManager.clearCacheForTab(tabId);

    new Thread(() -> {
      try {
        stateManager.executeAndCacheQuery(queryContent, tabId);
        var cachedEntry = stateManager.getCachedResult(tabId);
        
        Platform.runLater(() -> {
             if (cachedEntry != null) {
                 updateResultsForSelectedQueryTab(selectedTab);
             }
        });

      } catch (Exception e) {
        Platform.runLater(() -> showError("Query Execution Error", e.getMessage()));
        e.printStackTrace();
      }
    }).start();
  }

  private void onOpenFilesButtonClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open File");
    fileChooser.getExtensionFilters().addAll(
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
    view.getRoot().sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene != null) {
        newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
          if (new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN).match(event)) {
            onOpenFilesButtonClick();
            event.consume();
          } else if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN).match(event)) {
            tabEditorController.addNewTab("untitled", "");
            event.consume();
          } else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN).match(event)) {
            Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
            tabEditorController.handleCloseFile(selectedTab);
            event.consume();
          } else if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
            Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
            if (selectedTab != null && selectedTab != tabEditorController.getView().getAddTab()) {
              CodeEditorController controller = tabEditorController.getModel().getControllerForTab(selectedTab);
              if (controller != null) {
                controller.saveFile();
              }
            }
            event.consume();
          } else if (new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN).match(event)) {
            Stage stage = (Stage) view.getRoot().getScene().getWindow();
            TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
            event.consume();
          }
        });
      }
    });
  }

  public void openQueryFile(File file) {
    for (Tab tab : tabEditorController.getView().getTabPane().getTabs()) {
      if (tab != tabEditorController.getView().getAddTab()) {
        CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
        if (controller != null && file.getAbsolutePath().equals(controller.getModel().getFilePath())) {
          tabEditorController.getView().getTabPane().getSelectionModel().select(tab);
          return;
        }
      }
    }
    tabEditorController.addNewTab(file);
  }

  public TabEditorController getTabEditorController() {
    return tabEditorController;
  }
}
