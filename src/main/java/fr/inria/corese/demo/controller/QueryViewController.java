
package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.factory.popup.DocumentationPopup;
import fr.inria.corese.demo.manager.ApplicationStateManager;
import fr.inria.corese.demo.view.CustomButton;
import javafx.collections.ListChangeListener;
import fr.inria.corese.demo.view.TopBar;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

/**
 * Controller for the query view.
 * Manages query editor and results display.
 */
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
    private WebView graphView;
    @FXML
    private TextArea xmlResultTextArea;
    @FXML
    private TopBar topBar;
    @FXML
    private Tab tableTab;
    @FXML
    private Tab graphTab;
    @FXML
    private Tab xmlTab;
    @FXML
    private ComboBox<String> xmlFormatComboBox;
    @FXML
    private Button copyXmlButton;

    private TabEditorController tabEditorController;
    private ApplicationStateManager stateManager = ApplicationStateManager.getInstance();
    private TableView<String[]> resultTable;
    private fr.inria.corese.core.kgram.core.Mappings lastSelectMappings = null;
    private Node emptyStateView;
    private Button loadQueryButton; // Reference to the "Load Query" button
    private Button newTabButtonEmptyState; // Reference to the "New Query" button

    @FXML
    public void initialize() {
        stateManager.addLogEntry("QueryViewController.initialize() started");

        setupKeyboardShortcuts();

        // Initialize the TabEditorController
        tabEditorController = new TabEditorController(IconButtonBarType.QUERY);

        // Create and add the empty state view
        emptyStateView = createEmptyStateView();

        setupResultsPane();
        setupRunButton();
        setupLayout();
        initializeTopBar();
        setupXmlFormatComboBox();
        setupCopyXmlButton();

        // Add the editor to the container
        Platform.runLater(() -> {
            editorContainer.getChildren().clear();
            editorContainer.getChildren().add(emptyStateView);
            editorContainer.getChildren().add(tabEditorController.getView());
            updateEmptyStateVisibility();
        });

        // Listen for tab changes to show/hide the empty state
        tabEditorController.getView().getTabPane().getTabs().addListener((ListChangeListener<Tab>) change -> {
            Platform.runLater(this::updateEmptyStateVisibility);
        });
    }

    private void initializeTopBar() {
        List<IconButtonType> buttons = new ArrayList<>();
        buttons.add(IconButtonType.OPEN_FILE);
        buttons.add(IconButtonType.DOCUMENTATION);
        topBar.addRightButtons(buttons);

        topBar.getButton(IconButtonType.OPEN_FILE).setOnAction(e -> onOpenFilesButtonClick());
        topBar.getButton(IconButtonType.DOCUMENTATION).setOnAction(e -> {
            DocumentationPopup documentationPopup = new DocumentationPopup();
            documentationPopup.displayPopup();
        });
    }

    private void onOpenFilesButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("RDF and SPARQL Files", "*.ttl", "*.rdf", "*.n3", "*.rq"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                tabEditorController.addNewTab(file);
            } catch (Exception e) {
                showError("Error Opening File", "Could not open the file: " + e.getMessage());
            }
        }
    }

    private void setupRunButton() {
        tabEditorController.getView().getTabPane().getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tab tab : change.getAddedSubList()) {
                        if (tab != tabEditorController.getView().getAddTab()) {
                            CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
                            if (controller != null) {
                                configureEditorRunButton(controller);
                            } else {
                                tab.contentProperty().addListener(new ChangeListener<Node>() {
                                    @Override
                                    public void changed(ObservableValue<? extends Node> observable, Node oldValue,
                                            Node newValue) {
                                        if (newValue != null) {
                                            tab.contentProperty().removeListener(this);

                                            Platform.runLater(() -> {
                                                CodeEditorController codeEditorController = tabEditorController
                                                        .getModel().getControllerForTab(tab);
                                                if (codeEditorController != null) {
                                                    configureEditorRunButton(codeEditorController);
                                                } else {
                                                    stateManager.addLogEntry(
                                                            "No CodeEditorController for tab: " + tab.getText());
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        });
    }

    private void configureEditorRunButton(CodeEditorController codeEditorController) {
        stateManager.addLogEntry("Configuring editor run button");

        codeEditorController.getView().displayRunButton();
        CustomButton runButton = codeEditorController.getView().getRunButton();

        if (runButton == null) {
            stateManager.addLogEntry("Run button is null");
            return;
        }

        runButton.setOnAction(null);

        runButton.setOnAction(e -> {
            stateManager.addLogEntry("Run button clicked");
            executeQuery();
        });

        codeEditorController.getView().setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                stateManager.addLogEntry("Ctrl+Enter shortcut triggered");
                executeQuery();
            }
        });
    }

    /**
     * Sets up the results pane.
     * Only initializes the TableView as all tabs are already defined in FXML.
     */
    private void setupResultsPane() {
        resultTable = new TableView<>();
        tableTab.setContent(resultTable);
    }

    private void setupXmlFormatComboBox() {
        xmlFormatComboBox.getItems().setAll("XML", "JSON", "CSV", "TSV", "MARKDOWN");
        xmlFormatComboBox.getSelectionModel().select("XML");
        xmlFormatComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (lastSelectMappings != null && newVal != null) {
                updateXMLTabWithFormat(newVal);
            }
        });
    }

    private void setupCopyXmlButton() {
        copyXmlButton.setOnAction(event -> {
            String text = xmlResultTextArea.getText();
            if (text != null && !text.isEmpty()) {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);

                String originalText = copyXmlButton.getText();
                copyXmlButton.setText("Copied!");
                PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(e -> copyXmlButton.setText(originalText));
                pause.play();
            }
        });
    }

    private void setupLayout() {
        Platform.runLater(() -> {
            if (mainSplitPane != null) {
                mainSplitPane.setDividerPosition(0, 0.6);

                mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, oldPos, newPos) -> {
                    if (newPos.doubleValue() < 0.25) {
                        Platform.runLater(() -> mainSplitPane.setDividerPosition(0, 0.25));
                    } else if (newPos.doubleValue() < 0.4) {
                        Platform.runLater(() -> mainSplitPane.setDividerPosition(0, 0.4));
                    }
                });

                mainBorderPane.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                    if (newHeight.doubleValue() > 0) {
                        double currentDividerPos = mainSplitPane.getDividerPositions()[0];
                        double resultsPaneHeight = newHeight.doubleValue() * (1 - currentDividerPos);

                        if (resultsPaneHeight > newHeight.doubleValue() * 0.75) {
                            Platform.runLater(() -> mainSplitPane.setDividerPosition(0, 0.25));
                        }
                    }
                });
            }
        });
    }

    public void executeQuery() {
        stateManager.addLogEntry("Executing query");

        Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab != tabEditorController.getView().getAddTab()) {
            CodeEditorController codeEditorController = tabEditorController.getModel().getControllerForTab(selectedTab);
            if (codeEditorController != null) {
                String queryContent = codeEditorController.getView().getCodeMirrorView().getContent();
                stateManager.addLogEntry("Query content:\n" + queryContent);

                try {
                    clearResultViews();

                    Object[] result = stateManager.executeQuery(queryContent);
                    String formattedResult = result[0].toString();
                    String queryType = (String) result[1];

                    stateManager.addLogEntry("Query executed successfully. Type: " + queryType);

                    Platform.runLater(() -> {
                        switch (queryType) {
                            case "SELECT":
                                if (result.length > 2) {
                                    lastSelectMappings = (fr.inria.corese.core.kgram.core.Mappings) result[2];
                                } else {
                                    lastSelectMappings = null;
                                }
                                updateTableView(formattedResult);
                                updateXMLTabWithFormat(xmlFormatComboBox.getValue());
                                break;
                            case "CONSTRUCT":
                            case "DESCRIBE":
                                lastSelectMappings = null;
                                updateGraphView(formattedResult);
                                resultsTabPane.getSelectionModel().select(graphTab);
                                break;
                            case "ASK":
                                lastSelectMappings = null;
                                updateXMLView(formattedResult);
                                resultsTabPane.getSelectionModel().select(xmlTab);
                                break;
                        }
                    });
                } catch (Exception e) {
                    stateManager.addLogEntry("Query execution error: " + e.getMessage());
                    e.printStackTrace();
                    showError("Query Execution Error", e.getMessage());
                }
            }
        }
    }

    private void clearResultViews() {
        Platform.runLater(() -> {
            graphView.getEngine().loadContent("");
            xmlResultTextArea.clear();
            lastSelectMappings = null;
            resultTable.getItems().clear();
            resultTable.getColumns().clear();
        });
    }

    public Tab addNewQueryTab(String title, String content) {
        stateManager.addLogEntry("Creating new query tab: " + title);
        CodeEditorController codeEditorController = new CodeEditorController(IconButtonBarType.QUERY, content);
        Tab tab = tabEditorController.getView().addNewEditorTab(title, codeEditorController.getView());
        tabEditorController.getModel().addTabModel(tab, codeEditorController);
        codeEditorController.getView().displayRunButton();
        configureEditorRunButton(codeEditorController);

        return tab;
    }

    private void updateTableView(String formattedResult) {
        Platform.runLater(() -> {
            resultTable.getItems().clear();
            resultTable.getColumns().clear();

            String[] lines = formattedResult.split("\\r?\\n");
            if (lines.length == 0)
                return;

            String[] headers = lines[0].split(",", -1);

            for (int col = 0; col < headers.length; col++) {
                final int colIndex = col;
                TableColumn<String[], String> tableColumn = new TableColumn<>(headers[col].trim());
                tableColumn.setCellValueFactory(cellData -> {
                    String[] row = cellData.getValue();
                    String value = (colIndex < row.length) ? row[colIndex] : "";
                    return new javafx.beans.property.SimpleStringProperty(value);
                });
                tableColumn.setPrefWidth(200);
                resultTable.getColumns().add(tableColumn);
            }

            for (int i = 1; i < lines.length; i++) {
                String[] row = lines[i].split(",", -1);
                resultTable.getItems().add(row);
            }

            resultsTabPane.getSelectionModel().select(tableTab);
        });
    }

    private void updateGraphView(String content) {
        Platform.runLater(() -> {
            graphView.getEngine().loadContent(
                    String.format("<html><body><pre>%s</pre></body></html>",
                            content.replace("<", "&lt;").replace(">", "&gt;")));
        });
    }

    private void updateXMLTabWithFormat(String formatLabel) {
        if (lastSelectMappings == null)
            return;

        fr.inria.corese.core.print.ResultFormat.format format = switch (formatLabel) {
            case "XML" -> fr.inria.corese.core.print.ResultFormat.format.XML_FORMAT;
            case "JSON" -> fr.inria.corese.core.print.ResultFormat.format.JSON_FORMAT;
            case "CSV" -> fr.inria.corese.core.print.ResultFormat.format.CSV_FORMAT;
            case "TSV" -> fr.inria.corese.core.print.ResultFormat.format.TSV_FORMAT;
            case "MARKDOWN" -> fr.inria.corese.core.print.ResultFormat.format.MARKDOWN_FORMAT;
            default -> fr.inria.corese.core.print.ResultFormat.format.XML_FORMAT;
        };

        String formatted = stateManager.formatSelectResult(lastSelectMappings, format);
        updateXMLView(formatted);
    }

    private void updateXMLView(String content) {
        Platform.runLater(() -> {
            xmlResultTextArea.setText(content);
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

    private Node createEmptyStateView() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon(MaterialDesignF.FILE_DOCUMENT_OUTLINE);
        icon.setIconSize(80);

        Label message = new Label("No queries open.\nCreate a new query or load an existing one.");
        message.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");

        newTabButtonEmptyState = new Button("New Query");
        newTabButtonEmptyState.setOnAction(e -> tabEditorController.addNewTab("unt"));

        loadQueryButton = new Button("Load Query");
        loadQueryButton.setOnAction(e -> onOpenFilesButtonClick());

        HBox buttonBox = new HBox(10, newTabButtonEmptyState, loadQueryButton);
        buttonBox.setAlignment(Pos.CENTER);

        emptyBox.getChildren().addAll(icon, message, buttonBox);
        emptyBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        return emptyBox;
    }

    private void updateEmptyStateVisibility() {
        TabPane tabPane = tabEditorController.getView().getTabPane();
        int realTabCount = (int) tabPane.getTabs().stream()
                .filter(tab -> tab != tabEditorController.getView().getAddTab())
                .count();

        emptyStateView.setVisible(realTabCount == 0);
        emptyStateView.setManaged(realTabCount == 0);
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
                        tabEditorController.addNewTab("untitled");
                        event.consume();
                    } else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN).match(event)) {
                        // Ctrl+W: Close current tab (except the Add Tab)
                        TabPane tabPane = tabEditorController.getView().getTabPane();
                        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                        if (selectedTab != null && selectedTab != tabEditorController.getView().getAddTab()) {
                            if (tabEditorController.handleCloseFile(selectedTab)) { // Use refactored method
                                tabPane.getTabs().remove(selectedTab); // Remove tab if allowed
                            }
                        }
                        event.consume();
                    }
                });
            }
        });
    }

    public void openQueryFile(File file) {
        try {
            for (Tab tab : tabEditorController.getView().getTabPane().getTabs()) {
                if (tab != tabEditorController.getView().getAddTab()) {
                    CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
                    if (controller != null && file.getPath().equals(controller.getModel().getFilePath())) {
                        tabEditorController.getView().getTabPane().getSelectionModel().select(tab);
                        return;
                    }
                }
            }

            String content = Files.readString(file.toPath());

            Tab tab = addNewQueryTab(file.getName(), content);

            CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
            if (controller != null) {
                controller.getModel().setFilePath(file.getPath());
                controller.getModel().setCurrentSavedContent(content);
                controller.getModel().setModified(false);
            }

        } catch (Exception e) {
            stateManager.addLogEntry("Error opening query file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public TabEditorController getTabEditorController() {
        return tabEditorController;
    }
}
