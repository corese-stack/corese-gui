
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import java.net.URI;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.application.HostServices;

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
    private Button loadQueryButton;
    private Button newTabButtonEmptyState;

    // Pagination-related fields
    private Pagination pagination;
    private TextField rowsPerPageField;
    private int rowsPerPage = 0; // default
    private List<String[]> allRows = new ArrayList<>();
    private HBox controlsPane; // Changed from FlowPane to HBox
    private Label totalRowsLabel; // Total rows label

    // HostServices for opening URLs in the default browser
    private HostServices hostServices;

    private ChangeListener<Number> paginationListener;

    /**
     * Call this method from your Application class to provide HostServices.
     * Example:
     * controller.setHostServices(getHostServices());
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

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
        topBar.addLeftButtons(buttons);

        topBar.getButton(IconButtonType.OPEN_FILE).setOnAction(e -> onOpenFilesButtonClick());
        topBar.getButton(IconButtonType.DOCUMENTATION).setOnAction(e -> {
            try {
                if (hostServices != null) {
                    hostServices.showDocument("https://www.w3.org/TR/sparql11-query/");
                } else {
                    // fallback: try Desktop if HostServices not set (may not work on Linux)
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                        desktop.browse(new URI("https://www.w3.org/TR/sparql11-query/"));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * Handles the open files button click event.
     * Shows a file chooser dialog allowing the user to select a file.
     * If a file is selected, it is added as a new tab to the tab editor.
     * If there is an error opening the file, an error dialog is shown.
     */
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
     * Listens for new tabs being added to the tab pane and configures the "Run"
     * button for each new tab.
     * When a new tab is added, it first checks if the tab is not the "Add Tab" tab
     * (i.e., the tab with the "+" icon).
     * If the tab is a content tab, it gets the associated CodeEditorController and
     * calls configureEditorRunButton
     * to set up the "Run" button for that tab.
     * If the tab is not yet associated with a CodeEditorController, it sets up a
     * listener for the content property
     * of the tab. When the content property is set, it removes the listener and
     * configures the "Run" button in the
     * same way as above.
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

    /**
     * Configures the run button for the given CodeEditorController.
     * Adds an action listener to execute a query when the run button is clicked,
     * and sets up a keyboard shortcut (Ctrl+Enter) to trigger the same action.
     * Logs actions and ensures the run button is displayed.
     *
     * @param codeEditorController the CodeEditorController for which to configure
     *                             the run button
     */

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
     * Sets up the results pane, including the table view and pagination controls.
     * Initializes the table for displaying query results and configures pagination
     * settings, including the rows per page input field and total rows label.
     * Listens for changes in the rows per page field to update pagination settings.
     * Arranges the layout using a VBox containing the controls, table, and
     * pagination.
     */

    private void setupResultsPane() {
        resultTable = new TableView<>();

        // --- Pagination controls ---
        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        pagination.setVisible(false);
        pagination.setManaged(false);

        rowsPerPageField = new TextField(String.valueOf(rowsPerPage));
        rowsPerPageField.setPrefWidth(60);
        Label perPageLabel = new Label("Rows per page:");
        perPageLabel.setLabelFor(rowsPerPageField);

        // Create totalRowsLabel
        totalRowsLabel = new Label("total rows: 0");

        rowsPerPageField.textProperty().addListener((obs, oldVal, newVal) -> {
            int val;
            try {
                val = Integer.parseInt(newVal);
                if (val > 0) {
                    rowsPerPage = val;
                } else {
                    rowsPerPage = 10;
                    rowsPerPageField.setText(String.valueOf(rowsPerPage));
                }
            } catch (NumberFormatException ex) {
                rowsPerPage = 10;
                if (!newVal.isEmpty()) {
                    rowsPerPageField.setText(String.valueOf(rowsPerPage));
                }
            }
            updatePagination();
        });

        controlsPane = new HBox(10);
        controlsPane.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Ensure the totalRowsLabel is right-aligned
        HBox rightAlignedBox = new HBox(totalRowsLabel);
        rightAlignedBox.setAlignment(Pos.CENTER_RIGHT);

        controlsPane.getChildren().addAll(perPageLabel, rowsPerPageField, spacer, rightAlignedBox);

        VBox tableBox = new VBox(5, controlsPane, resultTable, pagination);
        VBox.setVgrow(resultTable, Priority.ALWAYS);
        tableTab.setContent(tableBox);
    }

    /**
     * Configure the XML format combobox.
     * The "XML" format is selected by default.
     * When the selection changes, the XML tab is updated
     * with the new format, if a mapping was previously selected.
     */
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
                
            }
        });
    }

    /**
     * Executes the query currently selected in the tab editor.
     * Logs the query content and execution result.
     * Updates the results view according to the query type.
     * In case of an error, logs the error message and displays an alert.
     */
    public void executeQuery() {

        if (rowsPerPage <= 0) {
            rowsPerPage = 10;
            if (rowsPerPageField != null) {
                rowsPerPageField.setText(String.valueOf(rowsPerPage));
            }
        }
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
            allRows.clear();
            updatePagination();
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

    /**
     * Updates the result table view with the provided formatted result data.
     * 
     * Clears the existing items and columns from the result table, and then
     * populates it with new data parsed from the given formattedResult string.
     * The formattedResult is expected to be in CSV format, with the first line
     * containing the headers. The table columns are dynamically created based
     * on the headers, and the rows are added subsequently. After updating, the
     * table tab is selected in the results tab pane.
     *
     * @param formattedResult A string containing the result data in CSV format.
     */

    private void updateTableView(String formattedResult) {
        Platform.runLater(() -> {
            resultTable.getItems().clear();
            resultTable.getColumns().clear();
            allRows.clear();

            String[] lines = formattedResult.split("\\r?\\n");
            if (lines.length == 0) {
                updatePagination();
                if (totalRowsLabel != null) {
                    totalRowsLabel.setText("total rows: 0");
                }
                return;
            }

            String[] headers = lines[0].split(",", -1);

            int columnCount = headers.length;
            for (int col = 0; col < columnCount; col++) {
                final int colIndex = col;
                TableColumn<String[], String> tableColumn = new TableColumn<>(headers[col].trim());
                tableColumn.setCellValueFactory(cellData -> {
                    String[] row = cellData.getValue();
                    String value = (colIndex < row.length) ? row[colIndex] : "";
                    return new javafx.beans.property.SimpleStringProperty(value);
                });
                tableColumn.prefWidthProperty().bind(resultTable.widthProperty().divide(columnCount));
                resultTable.getColumns().add(tableColumn);
            }

            // Store all rows for pagination
            for (int i = 1; i < lines.length; i++) {
                String[] row = lines[i].split(",", -1);
                allRows.add(row);
            }

            if (totalRowsLabel != null) {
                totalRowsLabel.setText("total rows: " + allRows.size());
            }
            updatePagination();
            resultsTabPane.getSelectionModel().select(tableTab);
        });
    }

    /**
     * Updates the pagination control and table view.
     * Pagination is only shown if there is at least one row.
     */
    private void updatePagination() {
        if (totalRowsLabel != null) {
            totalRowsLabel.setText("total rows: " + allRows.size());
        }

        if (allRows.isEmpty() || rowsPerPage <= 0) {
            pagination.setVisible(false);
            pagination.setManaged(false);
            pagination.setPageCount(0);
            return;
        }

        int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);

        if (pageCount <= 1) {
            pagination.setVisible(false);
            pagination.setManaged(false);
            pagination.setPageCount(0);
        } else {
            pagination.setVisible(true);
            pagination.setManaged(true);
            pagination.setPageCount(pageCount);
        }

        pagination.setCurrentPageIndex(0);
        updateTableForPage(0);

        if (paginationListener != null) {
            pagination.currentPageIndexProperty().removeListener(paginationListener);
        }
        paginationListener = (obs, oldIndex, newIndex) -> updateTableForPage(newIndex.intValue());
        pagination.currentPageIndexProperty().addListener(paginationListener);
    }

    /**
     * Called by Pagination to create a page.
     */
    private Node createPage(int pageIndex) {
        updateTableForPage(pageIndex);
        return new Region();
    }

    private void updateTableForPage(int pageIndex) {
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
        if (fromIndex > toIndex) {
            resultTable.getItems().clear();
            return;
        }
        resultTable.getItems().setAll(allRows.subList(fromIndex, toIndex));
    }

    private void updateGraphView(String content) {
        Platform.runLater(() -> {
            graphView.getEngine().loadContent(
                    String.format("<html><body><pre>%s</pre></body></html>",
                            content.replace("<", "&lt;").replace(">", "&gt;")));
        });
    }

    /**
     * Updates the XML tab with the formatted result of the last SELECT query.
     * 
     * The formatted result is based on the selected format in the combo box
     * above the XML tab. The formats are defined in
     * fr.inria.corese.core.print.ResultFormat.format.
     * If no SELECT query has been executed, the method does nothing.
     * 
     * @param formatLabel The label of the selected format in the combo box.
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
     * Updates the content of the XML result text area with the given content.
     * 
     * The content is displayed in the XML result text area in the results tab.
     * 
     * @param content The content to display in the XML result text area.
     */
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

    /**
     * Creates a view representing an empty state for queries.
     *
     * This view is displayed when no queries are open, providing
     * a message and options to create a new query or load an existing one.
     * It includes an icon, a descriptive label, and buttons for user actions.
     *
     * @return A Node containing the empty state view layout.
     */

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

    /**
     * Sets up keyboard shortcuts for the main application interface.
     *
     * Listens for specific key combinations when the scene is active
     * and performs corresponding actions such as opening files, creating
     * new tabs, or closing the current tab. The shortcuts implemented
     * include:
     * - Ctrl+O: Opens the file dialog to select files.
     * - Ctrl+N: Creates a new tab with the title "untitled".
     * - Ctrl+W: Closes the currently selected tab, excluding the "Add Tab".
     *
     * The method ensures that the appropriate action is taken and the
     * keyboard event is consumed to prevent further handling.
     */

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
                            if (tabEditorController.handleCloseFile(selectedTab)) {
                                tabPane.getTabs().remove(selectedTab); // Remove tab if allowed
                            }
                        }
                        event.consume();
                    }
                });
            }
        });
    }

    /**
     * Opens a query file and displays it in the query tab.
     *
     * If the file is already open, selects the existing tab.
     * Otherwise, creates a new tab with the file name and content.
     *
     * @param file The file to open.
     */
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
