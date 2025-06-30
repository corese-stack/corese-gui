package fr.inria.corese.demo.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.factory.popup.TemplatePopup;
import fr.inria.corese.demo.manager.ApplicationStateManager;
import fr.inria.corese.demo.model.QueryResult;
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
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
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

    private TabEditorController tabEditorController;
    private Node emptyStateView;
    private HostServices hostServices;
    private TableViewController tableViewController;
    private ResultsPaneController resultsPaneController;
    private GraphViewController graphViewController;
    private TextViewController textViewController;
    private ApplicationStateManager stateManager = ApplicationStateManager.getInstance();

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
                emptyStateView = EmptyStateViewFactory.createEmptyStateView(
                        () -> tabEditorController.addNewTab("untitled", ""),
                        this::onOpenFilesButtonClick,
                        s -> TemplatePopup.show(s, query -> tabEditorController.addNewTab("untitled", query)),
                        stage);
                editorContainer.getChildren().setAll(emptyStateView, tabEditorController.getView());
                updateEmptyStateVisibility();
            }
        });

        tabEditorController.getView().getTabPane().getSelectionModel().selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> handleTabSelectionChange(newTab));

        resultsPaneController.getTextFormatComboBox().getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel()
                                .getSelectedItem();
                        if (selectedTab != null && selectedTab != tabEditorController.getView().getAddTab()) {
                            textViewController.displayData(selectedTab.hashCode(), newVal);
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

    private void handleTabSelectionChange(Tab newTab) {
        resultsPaneController.clearResults();

        if (newTab == null || newTab == tabEditorController.getView().getAddTab()) {
            return;
        }

        final Integer tabId = newTab.hashCode();
        System.out.println("[UI] Tab changed. Looking for cached result for tab ID: " + newTab.hashCode());

        var cachedEntry = stateManager.getCachedResult(tabId);

        if (cachedEntry == null) {
            System.out.println("[UI] No cached result found. Views are clear.");
            resultsPaneController.setTextFormats(List.of(), null);
            return;
        }

        String queryType = cachedEntry.getQueryType();
        System.out.println("[UI] Found cached result of type: " + queryType);

        switch (queryType) {
            case "SELECT":
            case "ASK":
                resultsPaneController.setTextFormats(SELECT_FORMATS, "XML");
                tableViewController.displayData(tabId);
                textViewController.displayData(tabId, "XML");
                resultsTabPane.getSelectionModel().select(tableTab);
                break;

            case "CONSTRUCT":
            case "DESCRIBE":
                resultsPaneController.setTextFormats(GRAPH_FORMATS, "TURTLE");
                graphViewController.displayGraph(tabId);
                textViewController.displayData(tabId, "TURTLE");
                resultsTabPane.getSelectionModel().select(graphTab);
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

        final String queryContent = codeEditorController.getModel().getContent();
        final Integer tabId = selectedTab.hashCode();

        resultsPaneController.clearResults();

        new Thread(() -> {
            try {
                stateManager.executeAndCacheQuery(queryContent, tabId);

                Platform.runLater(() -> {
                    handleTabSelectionChange(selectedTab);
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Query Execution Error", e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void initializeTopBar() {
        List<IconButtonType> buttons = new ArrayList<>(
                List.of(IconButtonType.OPEN_FILE, IconButtonType.DOCUMENTATION, IconButtonType.TEMPLATE));
        topBar.addLeftButtons(buttons);
        topBar.getButton(IconButtonType.OPEN_FILE).setOnAction(e -> onOpenFilesButtonClick());
        topBar.getButton(IconButtonType.TEMPLATE).setOnAction(e -> {
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
        });
        topBar.getButton(IconButtonType.DOCUMENTATION).setOnAction(e -> {
            try {
                if (hostServices != null)
                    hostServices.showDocument("https://www.w3.org/TR/sparql11-query/");
                else
                    java.awt.Desktop.getDesktop().browse(new URI("https://www.w3.org/TR/sparql11-query/"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
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
        if (runButton != null)
            runButton.setOnAction(e -> executeQuery());
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

    /**
     * This method contains the complete set of keyboard shortcuts for the Query
     * View.
     */
    private void setupKeyboardShortcuts() {
        // We add the listener to the scene property to ensure it's active once the UI
        // is fully loaded.
        mainBorderPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    // Open File: Ctrl + O
                    if (new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN).match(event)) {
                        onOpenFilesButtonClick();
                        event.consume();
                    }
                    // New Tab: Ctrl + N
                    else if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN).match(event)) {
                        tabEditorController.addNewTab("untitled", "");
                        event.consume();
                    }
                    // Close Tab: Ctrl + W
                    else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN).match(event)) {
                        Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel()
                                .getSelectedItem();
                        tabEditorController.handleCloseFile(selectedTab);
                        event.consume();
                    }
                    // Save File: Ctrl + S
                    else if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
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
                    }
                    // Open Templates: Ctrl + T
                    else if (new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN).match(event)) {
                        Stage stage = (Stage) mainBorderPane.getScene().getWindow();
                        TemplatePopup.show(stage, query -> tabEditorController.addNewTab("untitled", query));
                        event.consume();
                    }
                });
            }
        });
    }

    public void openQueryFile(File file) {
        // Check if the file is already open in a tab
        for (Tab tab : tabEditorController.getView().getTabPane().getTabs()) {
            if (tab != tabEditorController.getView().getAddTab()) {
                CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
                if (controller != null && file.getAbsolutePath().equals(controller.getModel().getFilePath())) {
                    tabEditorController.getView().getTabPane().getSelectionModel().select(tab);
                    return;
                }
            }
        }
        // If not found, open it in a new tab
        tabEditorController.addNewTab(file);
    }

    public TabEditorController getTabEditorController() {
        return tabEditorController;
    }
}
