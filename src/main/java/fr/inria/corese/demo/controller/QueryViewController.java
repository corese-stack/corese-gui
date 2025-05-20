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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

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
    private TextArea resultTextArea;
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

    /**
     * Constructor for the query view controller.
     */
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        stateManager.addLogEntry("QueryViewController.initialize() started");

        // Initialiser le TabEditor - IMPORTANT: Ne pas ajouter au conteneur ici!
        tabEditorController = new TabEditorController(IconButtonBarType.QUERY);

        setupResultsPane();
        setupRunButton();
        setupLayout();
        initializeTopBar();
        setupXmlFormatComboBox();
        setupCopyXmlButton();

        // Add the editor to the container and create a default tab
        Platform.runLater(() -> {
            if (editorContainer != null) {
                editorContainer.getChildren().clear();
                editorContainer.getChildren().add(tabEditorController.getView());
            } else {
                stateManager.addLogEntry("Error: editorContainer is null");
            }
        });
    }

    /**
     * Sets up the run button listener.
     */
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

    /**
     * Configure le bouton d'exécution de requête.
     *
     * Gère :
     * - L'affichage du bouton Run
     * - Les actions du bouton
     * - Le raccourci clavier Ctrl+Entrée
     */
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
                                // If controller is not available immediately, add a content listener
                                tab.contentProperty().addListener(new ChangeListener<Node>() {
                                    @Override
                                    public void changed(ObservableValue<? extends Node> observable, Node oldValue,
                                            Node newValue) {
                                        if (newValue != null) {
                                            // Remove listener after it fires
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

    /**
     * Configures the run button for a code editor.
     *
     * @param codeEditorController The code editor controller
     */
    private void configureEditorRunButton(CodeEditorController codeEditorController) {
        stateManager.addLogEntry("Configuring editor run button");

        codeEditorController.getView().displayRunButton();
        CustomButton runButton = codeEditorController.getView().getRunButton();

        if (runButton == null) {
            stateManager.addLogEntry("Run button is null");
            return;
        }

        // Clear previous event handlers
        runButton.setOnAction(null);

        // Add new event handler
        runButton.setOnAction(e -> {
            stateManager.addLogEntry("Run button clicked");
            executeQuery();
        });

        // Set up keyboard shortcuts
        codeEditorController.getView().setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                stateManager.addLogEntry("Ctrl+Enter shortcut triggered");
                executeQuery();
            }
        });
    }

    private void setupResultsPane() {
        resultTable = new TableView<>();

        if (tableTab == null) {
            tableTab = new Tab("Table");
            tableTab.setClosable(false);
            resultsTabPane.getTabs().add(0, tableTab);
        } else {
            tableTab.setText("Table");
            tableTab.setClosable(false);
        }
        tableTab.setContent(resultTable);

        if (graphTab != null && graphView != null) {
            graphTab.setContent(graphView);
            graphTab.setClosable(false);
        }
        if (xmlTab != null && xmlResultTextArea != null) {
            BorderPane xmlTabLayout = new BorderPane();

            if (xmlFormatComboBox == null) {
                xmlFormatComboBox = new ComboBox<>();
            }
            
            // Create format selection and copy button container
            BorderPane topContainer = new BorderPane();
            topContainer.setLeft(xmlFormatComboBox);
            topContainer.setRight(copyXmlButton);
            
            xmlTabLayout.setTop(topContainer);
            xmlTabLayout.setCenter(xmlResultTextArea);

            xmlTab.setContent(xmlTabLayout);
            xmlTab.setClosable(false);
        }
    }

    private void setupXmlFormatComboBox() {
        if (xmlFormatComboBox != null) {
            xmlFormatComboBox.getItems().setAll("XML", "JSON", "CSV", "TSV", "MARKDOWN");
            xmlFormatComboBox.getSelectionModel().select("XML");
            xmlFormatComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (lastSelectMappings != null && newVal != null) {
                    updateXMLTabWithFormat(newVal);
                }
            });
        }
    }
    
    /**
     * Sets up the copy button for XML results.
     */
    private void setupCopyXmlButton() {
        if (copyXmlButton != null) {
            copyXmlButton.setOnAction(event -> {
                if (xmlResultTextArea != null) {
                    String text = xmlResultTextArea.getText();
                    if (text != null && !text.isEmpty()) {
                        ClipboardContent content = new ClipboardContent();
                        content.putString(text);
                        Clipboard.getSystemClipboard().setContent(content);
                        
                        // Show visual feedback that copy was successful
                        String originalText = copyXmlButton.getText();
                        copyXmlButton.setText("Copied!");
                        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                        pause.setOnFinished(e -> copyXmlButton.setText(originalText));
                        pause.play();
                    }
                }
            });
        }
    }

    /**
     * Sets up the layout.
     */
    private void setupLayout() {
        Platform.runLater(() -> {
            if (mainSplitPane != null) {
                mainSplitPane.setDividerPosition(0, 0.6);

                // Manage divider positions
                mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, oldPos, newPos) -> {
                    if (newPos.doubleValue() < 0.25) {
                        Platform.runLater(() -> mainSplitPane.setDividerPosition(0, 0.25));
                    } else if (newPos.doubleValue() < 0.4) {
                        Platform.runLater(() -> mainSplitPane.setDividerPosition(0, 0.4));
                    }
                });

                // Handle resizing
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

    /**
     * Executes a SPARQL query.
     */
    public void executeQuery() {
        stateManager.addLogEntry("Executing query");

        // Get the selected tab
        Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab != tabEditorController.getView().getAddTab()) {
            CodeEditorController codeEditorController = tabEditorController.getModel().getControllerForTab(selectedTab);
            if (codeEditorController != null) {
                String queryContent = codeEditorController.getView().getCodeMirrorView().getContent();
                stateManager.addLogEntry("Query content:\n" + queryContent);

                try {
                    // Clear result views
                    clearResultViews();

                    // Execute query
                    Object[] result = stateManager.executeQuery(queryContent);
                    String formattedResult = result[0].toString();
                    String queryType = (String) result[1];

                    stateManager.addLogEntry("Query executed successfully. Type: " + queryType);

                    // Update the appropriate view based on query type
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
                                if (resultsTabPane.getSelectionModel().getSelectedItem() != xmlTab) {
                                    resultsTabPane.getSelectionModel().select(xmlTab);
                                }
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

    /**
     * Clears all result views.
     */
    private void clearResultViews() {
        Platform.runLater(() -> {
            // Clear text area
            if (resultTextArea != null) {
                resultTextArea.clear();
            }

            // Clear graph view
            if (graphView != null) {
                graphView.getEngine().loadContent("");
            }

            // Clear XML view
            if (xmlResultTextArea != null) {
                xmlResultTextArea.clear();
            }

            // Reset last mappings
            lastSelectMappings = null;

            // Clear table view
            if (resultTable != null) {
                resultTable.getItems().clear();
                resultTable.getColumns().clear();
            }
        });
    }

    /**
     * Creates a new query tab.
     *
     * @param title   The tab title
     * @param content The tab content
     * @return The created tab
     */
    public Tab addNewQueryTab(String title, String content) {
        stateManager.addLogEntry("Creating new query tab: " + title);
        CodeEditorController codeEditorController = new CodeEditorController(IconButtonBarType.QUERY, content);
        Tab tab = tabEditorController.getView().addNewEditorTab(title, codeEditorController.getView());
        tabEditorController.getModel().addTabModel(tab, codeEditorController);
        codeEditorController.getView().displayRunButton();
        configureEditorRunButton(codeEditorController);

        return tab;
    }

    /**
     * Updates the TableView with CSV-formatted query results.
     *
     * @param formattedResult The CSV-formatted query result (header in first line)
     */
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

            if (resultsTabPane != null && tableTab != null) {
                resultsTabPane.getSelectionModel().select(tableTab);
            }
        });
    }

    /**
     * Updates the graph view with query results.
     *
     * @param content The graph content
     */
    private void updateGraphView(String content) {
        Platform.runLater(() -> {
            if (graphView != null) {
                graphView.getEngine().loadContent(
                        String.format("<html><body><pre>%s</pre></body></html>",
                                content.replace("<", "&lt;").replace(">", "&gt;")));
            }
        });
    }

    /**
     * Updates the XML tab with the selected format.
     *
     * @param formatLabel The format label (XML, JSON, etc.)
     */
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

    /**
     * Updates the XML view with query results.
     *
     * @param content The XML content
     */
    private void updateXMLView(String content) {
        Platform.runLater(() -> {
            if (xmlResultTextArea != null) {
                xmlResultTextArea.setText(content);
            }
        });
    }

    /**
     * Shows an error dialog.
     *
     * @param title   The error title
     * @param message The error message
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Opens a query file in a new tab.
     *
     * @param file The file to open
     */
    public void openQueryFile(File file) {
        try {
            // Check if the file is already open
            for (Tab tab : tabEditorController.getView().getTabPane().getTabs()) {
                if (tab != tabEditorController.getView().getAddTab()) {
                    CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
                    if (controller != null && file.getPath().equals(controller.getModel().getFilePath())) {
                        // File already open, select its tab
                        tabEditorController.getView().getTabPane().getSelectionModel().select(tab);
                        return;
                    }
                }
            }

            // Read file content
            String content = Files.readString(file.toPath());

            // Create a new tab with the file content
            Tab tab = addNewQueryTab(file.getName(), content);

            // Save the file path in the model
            CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
            if (controller != null) {
                controller.getModel().setFilePath(content);
                controller.getModel().setCurrentSavedContent(content);
                controller.getModel().setModified(false);
            }

        } catch (Exception e) {
            stateManager.addLogEntry("Error opening query file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the tab editor controller.
     *
     * @return The tab editor controller
     */
    public TabEditorController getTabEditorController() {
        return tabEditorController;
    }
}
