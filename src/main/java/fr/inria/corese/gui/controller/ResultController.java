package fr.inria.corese.gui.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.model.ValidationReportItem;
import fr.inria.corese.gui.view.ResultView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import fr.inria.corese.gui.view.rule.CustomPagination;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultController {
    private static final Logger logger = LoggerFactory.getLogger(ResultController.class);

    private final ResultView view;
    private final TextArea xmlResultTextArea;
    private final TableView<ValidationReportItem> reportTable;
    private final ChoiceBox<String> textFormatChoiceBox;
    private Button copyButton;
    private Button exportButton;

    // SPARQL Result Components
    private final TableView<String[]> resultTable;
    private final CustomPagination customPagination;
    private final TextField rowsPerPageField;
    private final Label totalRowsLabel;
    private final List<String[]> allRows;
    private int rowsPerPage = 50;
    private final WebView graphView;
    private final List<IconButtonType> buttons;

    public ResultController(List<IconButtonType> buttons) {
        this.buttons = buttons;
        this.view = new ResultView();
        this.view.getRoot().getStylesheets().add(getClass().getResource("/styles/custom-button.css").toExternalForm());

        this.xmlResultTextArea = new TextArea();
        this.reportTable = new TableView<>();
        this.textFormatChoiceBox = new ChoiceBox<>();
        
        // Initialize SPARQL components
        this.resultTable = new TableView<>();
        this.allRows = new ArrayList<>();
        this.rowsPerPageField = new TextField("50");
        this.totalRowsLabel = new Label("total rows: 0");
        this.customPagination = new CustomPagination(1, this::updateTableForPage);
        this.graphView = new WebView();

        initialize();
    }

    public ResultController() {
        this(List.of(IconButtonType.COPY, IconButtonType.EXPORT));
    }

    private void initialize() {
        // Text Tab Content
        xmlResultTextArea.setEditable(false);
        
        // Use IconButtonBarView for consistency
        view.getIconButtonBarView().initializeButtons(buttons);
        this.copyButton = view.getIconButtonBarView().getButton(IconButtonType.COPY);
        this.exportButton = view.getIconButtonBarView().getButton(IconButtonType.EXPORT);

        initializeToolbar();
        
        // Wrap TextArea in StackPane to overlay the format choice box
        StackPane textContent = new StackPane();
        textContent.getChildren().add(xmlResultTextArea);
        
        // Configure floating choice box
        StackPane.setAlignment(textFormatChoiceBox, Pos.TOP_RIGHT);
        StackPane.setMargin(textFormatChoiceBox, new Insets(10));
        textContent.getChildren().add(textFormatChoiceBox);
        
        BorderPane textPane = new BorderPane();
        textPane.setCenter(textContent);
        
        textPane.setRight(view.getIconButtonBarView());
        
        view.getTextTab().setContent(textPane);

        // Visual Tab Content (Validation Report)
        initializeReportTable();
        view.getVisualTab().setContent(reportTable);

        // Table Tab Content (SPARQL Result)
        initializeTableTab();

        // Graph Tab Content
        view.getGraphTab().setContent(graphView);
    }

    private void initializeTableTab() {
        customPagination.setVisible(false);
        customPagination.setManaged(false);

        rowsPerPageField.setPrefWidth(60);
        Label perPageLabel = new Label("Rows per page:");
        rowsPerPageField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                rowsPerPage = Math.max(1, Integer.parseInt(newVal));
            } catch (NumberFormatException ex) {
                // ignore
            }
            updatePagination();
        });

        HBox controlsPane = new HBox(10);
        controlsPane.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controlsPane.getChildren().addAll(perPageLabel, rowsPerPageField, spacer, totalRowsLabel);

        VBox tableBox = new VBox(5, controlsPane, resultTable, customPagination);
        VBox.setVgrow(resultTable, Priority.ALWAYS);
        
        view.getTableTab().setContent(tableBox);
    }

    private void initializeReportTable() {
        TableColumn<ValidationReportItem, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        
        TableColumn<ValidationReportItem, String> focusNodeCol = new TableColumn<>("Focus Node");
        focusNodeCol.setCellValueFactory(new PropertyValueFactory<>("focusNode"));
        
        TableColumn<ValidationReportItem, String> resultPathCol = new TableColumn<>("Result Path");
        resultPathCol.setCellValueFactory(new PropertyValueFactory<>("resultPath"));
        
        TableColumn<ValidationReportItem, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        TableColumn<ValidationReportItem, String> messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(new PropertyValueFactory<>("message"));

        reportTable.getColumns().addAll(severityCol, focusNodeCol, resultPathCol, valueCol, messageCol);
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private Consumer<String> onFormatChanged;

    public void setOnFormatChanged(Consumer<String> listener) {
        this.onFormatChanged = listener;
    }

    private void initializeToolbar() {
        textFormatChoiceBox.getItems().setAll("TURTLE", "RDF/XML", "JSON-LD", "N-TRIPLES", "N-QUADS", "TRIG");
        textFormatChoiceBox.getSelectionModel().select("TURTLE");

        textFormatChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onFormatChanged != null) {
                onFormatChanged.accept(newVal);
            }
        });

        copyButton.setOnAction(event -> handleCopy());
        exportButton.setOnAction(event -> handleExport());
    }

    private void handleCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(xmlResultTextArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        
        Tooltip tooltip = copyButton.getTooltip();
        if (tooltip != null) {
            String originalText = tooltip.getText();
            tooltip.setText("Copied!");
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> tooltip.setText(originalText));
            pause.play();
        }
    }

    private void handleExport() {
        String contentToExport = xmlResultTextArea.getText();
        if (contentToExport == null || contentToExport.isBlank()) {
            showError("Export Error", "There is no text content to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Result As");

        String selectedFormat = textFormatChoiceBox.getValue();
        if (selectedFormat == null) {
            selectedFormat = "Text";
        }
        String extension = getExtensionForFormat(selectedFormat);

        fileChooser.setInitialFileName("output" + extension);
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter(selectedFormat + " file (*" + extension + ")", "*" + extension);
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(view.getRoot().getScene().getWindow());

        if (file != null) {
            File fileToSave = file;
            if (!file.getName().toLowerCase().endsWith(extension)) {
                fileToSave = new File(file.getAbsolutePath() + extension);
            }

            try {
                Files.writeString(fileToSave.toPath(), contentToExport);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("The result has been successfully exported to:\n" + fileToSave.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                showError("Export Failed", "An error occurred while saving the file:\n" + e.getMessage());
            }
        }
    }

    private String getExtensionForFormat(String format) {
        return switch (format.toUpperCase()) {
            case "RDF/XML" -> ".rdf";
            case "JSON-LD" -> ".jsonld";
            case "N-TRIPLES" -> ".nt";
            case "N-QUADS" -> ".nq";
            case "TRIG" -> ".trig";
            case "TURTLE" -> ".ttl";
            case "JSON" -> ".json";
            case "CSV" -> ".csv";
            case "TSV" -> ".tsv";
            case "XML" -> ".xml";
            case "MARKDOWN" -> ".md";
            default -> ".ttl";
        };
    }

    public void updateText(String content) {
        Platform.runLater(() -> xmlResultTextArea.setText(content != null ? content : ""));
    }

    public void displayReport(Graph reportGraph) {
        Platform.runLater(() -> {
            reportTable.getItems().clear();
            if (reportGraph == null) return;

            try {
                QueryProcess exec = QueryProcess.create(reportGraph);
                String query = "SELECT ?severity ?focusNode ?resultPath ?value ?message WHERE { " +
                        "?r a <http://www.w3.org/ns/shacl#ValidationResult> ; " +
                        "<http://www.w3.org/ns/shacl#resultSeverity> ?severity ; " +
                        "<http://www.w3.org/ns/shacl#focusNode> ?focusNode . " +
                        "OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultPath> ?resultPath } " +
                        "OPTIONAL { ?r <http://www.w3.org/ns/shacl#value> ?value } " +
                        "OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultMessage> ?message } " +
                        "}";
                
                Mappings map = exec.query(query);
                processMappings(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void processMappings(Mappings map) {
        for (var mapping : map) {
            String severity = mapping.getValue("?severity") != null ? mapping.getValue("?severity").getLabel() : "";
            String focusNode = mapping.getValue("?focusNode") != null ? mapping.getValue("?focusNode").getLabel() : "";
            String resultPath = mapping.getValue("?resultPath") != null ? mapping.getValue("?resultPath").getLabel() : "";
            String value = mapping.getValue("?value") != null ? mapping.getValue("?value").getLabel() : "";
            String message = mapping.getValue("?message") != null ? mapping.getValue("?message").getLabel() : "";
            
            // Simplify severity URI
            if (severity.contains("#")) severity = severity.substring(severity.lastIndexOf("#") + 1);
            
            reportTable.getItems().add(new ValidationReportItem(severity, focusNode, resultPath, value, message));
        }
    }

    public void clearResults() {
        Platform.runLater(() -> {
            xmlResultTextArea.clear();
            reportTable.getItems().clear();
        });
    }

    public ResultView getView() {
        return view;
    }

    /**
     * Returns the root node of the view.
     * This allows the controller to expose the UI component without exposing the View class.
     *
     * @return The root Parent node of the view.
     */
    public javafx.scene.Parent getViewRoot() {
        return view.getRoot();
    }

    /**
     * Selects the Text tab in the view.
     */
    public void selectTextTab() {
        view.getTabPane().getSelectionModel().select(view.getTextTab());
    }

    public void updateTableView(String csvResult) {
        Platform.runLater(() -> {
            resultTable.getItems().clear();
            resultTable.getColumns().clear();
            allRows.clear();
            if (csvResult == null || csvResult.isEmpty()) {
                updatePagination();
                return;
            }
            String[] lines = csvResult.split("\\r?\\n");
            if (lines.length > 0) {
                String[] headers = lines[0].split(",", -1);
                for (int col = 0; col < headers.length; col++) {
                    final int colIndex = col;
                    TableColumn<String[], String> tc = new TableColumn<>(headers[col].trim());
                    tc.setCellValueFactory(cdf -> new javafx.beans.property.SimpleStringProperty(
                            (colIndex < cdf.getValue().length) ? cdf.getValue()[colIndex] : ""));
                    tc.prefWidthProperty().bind(resultTable.widthProperty().divide(Math.max(1, headers.length)));
                    resultTable.getColumns().add(tc);
                }
                for (int i = 1; i < lines.length; i++) {
                    allRows.add(lines[i].split(",", -1));
                }
            }
            updatePagination();
        });
    }

    private void updatePagination() {
        totalRowsLabel.setText("total rows: " + allRows.size());
        if (allRows.isEmpty()) {
            customPagination.setVisible(false);
            customPagination.setManaged(false);
            return;
        }
        int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
        customPagination.setPageCount(pageCount);
        customPagination.setVisible(pageCount > 1);
        customPagination.setManaged(pageCount > 1);
        customPagination.setCurrentPageIndex(0);
    }

    private void updateTableForPage(int pageIndex) {
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
        if (fromIndex >= allRows.size()) {
            resultTable.getItems().clear();
            return;
        }
        resultTable.getItems().setAll(allRows.subList(fromIndex, toIndex));
    }

    public void displayGraph(String ttlData) {
        Platform.runLater(() -> {
            if (ttlData == null || ttlData.isBlank()) {
                graphView.getEngine().load("about:blank");
                return;
            }
            try {
                String htmlPath = getClass().getResource("/web/index.html").toExternalForm();
                final ChangeListener<Worker.State> loadListener = new ChangeListener<>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Worker.State> observable,
                                        Worker.State oldValue, Worker.State newValue) {
                        if (newValue == Worker.State.SUCCEEDED) {
                            graphView.getEngine().getLoadWorker().stateProperty().removeListener(this);

                            String escapedTtl = ttlData.replace("\\", "\\\\")
                                    .replace("'", "\\'")
                                    .replace("\n", "\\n")
                                    .replace("\r", "");

                            String script = "(function() {    var container = document.getElementById('container');  "
                                    + "  if (!container) { setTimeout(arguments.callee, 50); return; }   "
                                    + " var old = document.getElementById('myGraph');    if (old &&"
                                    + " old.parentNode) { old.parentNode.removeChild(old); }    var"
                                    + " newGraph = document.createElement('kg-graph');    newGraph.id ="
                                    + " 'myGraph';    newGraph.setAttribute('width', '100%');   "
                                    + " newGraph.setAttribute('height', '100%');   "
                                    + " container.appendChild(newGraph);    function setTTL() {      var"
                                    + " el = document.getElementById('myGraph');      if (el) { el.ttl ="
                                    + " '" + escapedTtl + "'; }"
                                    + "      else { setTimeout(setTTL, 50); }"
                                    + "    }"
                                    + "    setTTL();"
                                    + "  })();";

                            graphView.getEngine().executeScript(script);
                        }
                    }
                };
                graphView.getEngine().getLoadWorker().stateProperty().addListener(loadListener);
                graphView.getEngine().load(htmlPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
