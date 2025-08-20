package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.factory.popup.TemplatePopup;
import fr.inria.corese.demo.manager.QueryManager;
import fr.inria.corese.demo.view.CustomButton;
import fr.inria.corese.demo.view.EmptyStateViewFactory;
import fr.inria.corese.demo.view.TopBar;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class QueryViewController {
    @FXML
    private StackPane editorContainer;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private TabPane resultsTabPane;
    @FXML
    private TopBar topBar;
    @FXML
    private Tab tableTab;
    @FXML
    private Tab graphTab;
    @FXML
    private Tab textTab;

    @FXML
    private StackPane resultsContainer;

    private boolean isSplitView = false;
    @FXML
    private SplitPane resultsSplitPane;
    private Node textViewNode;

    private TabEditorController tabEditorController;
    private Node emptyStateView;
    private HostServices hostServices;
    private TableViewController tableViewController;
    private ResultsPaneController resultsPaneController;
    private GraphViewController graphViewController;
    private TextViewController textViewController;
    private final QueryManager stateManager = QueryManager.getInstance();

    private static final List<String> SELECT_FORMATS = List.of("XML", "JSON", "CSV", "TSV", "MARKDOWN");
    private static final List<String> GRAPH_FORMATS = List.of("TURTLE", "RDF_XML", "JSON-LD", "N-TRIPLES", "N-QUADS",
            "TriG");

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    public void initialize() {
        tabEditorController = new TabEditorController(IconButtonBarType.QUERY);
        resultsPaneController = new ResultsPaneController();
        tableViewController = new TableViewController();
        graphViewController = new GraphViewController();
        textViewController = new TextViewController();

        tableViewController.setResultsPaneController(resultsPaneController);
        graphViewController.setResultsPaneController(resultsPaneController);
        textViewController.setResultsPaneController(resultsPaneController);

        tableTab.setContent(resultsPaneController.getTableBox());
        graphTab.setContent(resultsPaneController.getGraphView());
        textTab.setContent(resultsPaneController.getTextViewBox());

        mainBorderPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) mainBorderPane.getScene().getWindow();
                emptyStateView = EmptyStateViewFactory.createQueryEmptyStateView(
                        () -> tabEditorController.addNewTab("untitled", ""),
                        this::onOpenFilesButtonClick,
                        s -> TemplatePopup.show(s, query -> tabEditorController.addNewTab("untitled", query)),
                        stage);
                editorContainer.getChildren().setAll(emptyStateView, tabEditorController.getView());
                updateEmptyStateVisibility();
            }
        });

        tabEditorController.getView().getTabPane().getSelectionModel().selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> updateResultsForSelectedQueryTab(newTab));

        resultsTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {

        });
        resultsPaneController.getTextFormatComboBox().getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        Tab selectedQueryTab = tabEditorController.getView().getTabPane().getSelectionModel()
                                .getSelectedItem();
                        if (selectedQueryTab != null && selectedQueryTab != tabEditorController.getView().getAddTab()) {
                            textViewController.displayData(selectedQueryTab.hashCode(), newVal);
                        }
                    }
                });

        tabEditorController.getView().getTabPane().getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Tab removedTab : c.getRemoved()) {
                        stateManager.clearCacheForTab(removedTab.hashCode());
                    }
                }
            }
            Platform.runLater(this::updateEmptyStateVisibility);
        });

        setupRunButton();
        setupLayout();
        initializeTopBar();
        setupKeyboardShortcuts();

    }

    /**
     * Toggles the results view between a single TabPane and a split view
     * where the Text view is shown side-by-side.
     */
    @FXML
    private void toggleSplitView() {
        if (this.textViewNode == null) {
            this.textViewNode = textTab.getContent();
        }

        isSplitView = !isSplitView;

        if (isSplitView) {

            if (resultsTabPane.getTabs().contains(textTab)) {
                resultsTabPane.getTabs().remove(textTab);
            }

            if (resultsSplitPane.getItems().size() == 1) {
                TabPane textWrapperPane = new TabPane();
                textWrapperPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
                Tab newTextTab = new Tab("Text", this.textViewNode);
                newTextTab.setClosable(false);
                textWrapperPane.getTabs().add(newTextTab);

                resultsSplitPane.getItems().add(textWrapperPane);
            }

            Platform.runLater(() -> resultsSplitPane.setDividerPositions(0.5));

        } else {
            if (resultsSplitPane.getItems().size() > 1) {
                resultsSplitPane.getItems().remove(1);
            }

            textTab.setContent(textViewNode);
            if (!resultsTabPane.getTabs().contains(textTab)) {
                resultsTabPane.getTabs().add(textTab);
            }
            resultsTabPane.getSelectionModel().select(textTab);

        }
    }

    private void updateResultsForSelectedQueryTab(Tab selectedQueryTab) {
        resultsPaneController.clearResults();

        if (selectedQueryTab == null || selectedQueryTab == tabEditorController.getView().getAddTab()) {
            disableAllResultsTabs();
            return;
        }

        final Integer queryTabId = selectedQueryTab.hashCode();
        var cachedEntry = stateManager.getCachedResult(queryTabId);

        if (cachedEntry == null) {
            disableAllResultsTabs();
            resultsPaneController.setTextFormats(List.of(), null);
            return;
        }

        String queryType = cachedEntry.getQueryType();

        switch (queryType) {
            case "SELECT":
            case "ASK":
                tableTab.setDisable(false);
                graphTab.setDisable(true);
                textTab.setDisable(false);
                resultsPaneController.setTextFormats(SELECT_FORMATS, "XML");
                tableViewController.displayData(queryTabId);
                textViewController.displayData(queryTabId, "XML");
                resultsTabPane.getSelectionModel().select(tableTab);
                break;
            case "CONSTRUCT":
            case "DESCRIBE":
                tableTab.setDisable(true);
                graphTab.setDisable(false);
                textTab.setDisable(false);
                resultsPaneController.setTextFormats(GRAPH_FORMATS, "TURTLE");
                graphViewController.displayGraph(queryTabId);
                textViewController.displayData(queryTabId, "TURTLE");
                resultsTabPane.getSelectionModel().select(graphTab);
                break;
        }
    }

    private void disableAllResultsTabs() {
        tableTab.setDisable(true);
        graphTab.setDisable(true);
        textTab.setDisable(true);
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

        resultsPaneController.clearResults();
        stateManager.clearCacheForTab(tabId);

        new Thread(() -> {
            try {
                stateManager.executeAndCacheQuery(queryContent, tabId);

                Platform.runLater(() -> {
                    updateResultsForSelectedQueryTab(selectedTab);
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Query Execution Error", e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void initializeTopBar() {
        List<IconButtonType> buttons = new ArrayList<>(
                List.of(IconButtonType.OPEN_FILE, IconButtonType.TEMPLATE, IconButtonType.SPLIT));

        topBar.addLeftButtons(buttons);
        topBar.getButton(IconButtonType.OPEN_FILE).setOnAction(e -> onOpenFilesButtonClick());
        topBar.getButton(IconButtonType.TEMPLATE).setOnAction(e -> {
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
        });
        topBar.getButton(IconButtonType.SPLIT).setOnAction(e -> toggleSplitView());
    }

    private void onOpenFilesButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("RDF & SPARQL", "*.ttl", "*.rdf", "*.n3", "*.rq"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null)
            openQueryFile(file);
    }

    private void setupRunButton() {
        tabEditorController.getView().getTabPane().getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab tab : c.getAddedSubList()) {
                        if (tab != tabEditorController.getView().getAddTab()) {
                            Platform.runLater(() -> {
                                CodeEditorController controller = tabEditorController.getModel()
                                        .getControllerForTab(tab);
                                if (controller != null)
                                    configureEditorRunButton(controller);
                            });
                        }
                    }
                }
            }
        });
    }

    private void configureEditorRunButton(CodeEditorController controller) {
        controller.getView().displayRunButton();
        CustomButton runButton = controller.getView().getRunButton();
        if (runButton != null) {
            runButton.setOnAction(e -> {
                controller.getView().getCodeMirrorView().requestFocus();
                executeQuery();
            });
        }
        controller.getView().setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)) {
                executeQuery();
                event.consume();
            }
        });
    }

    private void setupLayout() {
        Platform.runLater(() -> {
            if (mainSplitPane != null)
                mainSplitPane.setDividerPosition(0, 0.6);
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void updateEmptyStateVisibility() {
        long realTabCount = tabEditorController.getView().getTabPane().getTabs().stream()
                .filter(t -> t != tabEditorController.getView().getAddTab()).count();
        if (emptyStateView != null) {
            emptyStateView.setVisible(realTabCount == 0);
            emptyStateView.setManaged(realTabCount == 0);
        }
        tabEditorController.getView().setVisible(realTabCount > 0);
        tabEditorController.getView().setManaged(realTabCount > 0);
    }

    private void setupKeyboardShortcuts() {
        mainBorderPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN).match(event)) {
                        onOpenFilesButtonClick();
                        event.consume();
                    } else if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN).match(event)) {
                        tabEditorController.addNewTab("untitled", "");
                        event.consume();
                    } else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN).match(event)) {
                        Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel()
                                .getSelectedItem();
                        tabEditorController.handleCloseFile(selectedTab);
                        event.consume();
                    } else if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
                        Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel()
                                .getSelectedItem();
                        if (selectedTab != null && selectedTab != tabEditorController.getView().getAddTab()) {
                            CodeEditorController controller = tabEditorController.getModel()
                                    .getControllerForTab(selectedTab);
                            if (controller != null) {
                                controller.saveFile();
                            }
                        }
                        event.consume();
                    } else if (new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN).match(event)) {
                        Stage stage = (Stage) mainBorderPane.getScene().getWindow();
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