package fr.inria.corese.gui.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.gui.enums.button.ButtonType;
import fr.inria.corese.gui.model.ValidationReportItem;
import fr.inria.corese.gui.view.CustomButton;
import fr.inria.corese.gui.view.ResultView;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ResultController {
    private final ResultView view;
    private final TextArea xmlResultTextArea;
    private final TableView<ValidationReportItem> reportTable;
    private final ChoiceBox<String> textFormatChoiceBox;
    private final CustomButton copyButton;
    private final CustomButton exportButton;

    public ResultController() {
        this.view = new ResultView();
        this.view.getRoot().getStylesheets().add(getClass().getResource("/styles/custom-button.css").toExternalForm());

        this.xmlResultTextArea = new TextArea();
        this.reportTable = new TableView<>();
        this.textFormatChoiceBox = new ChoiceBox<>();
        
        this.copyButton = new CustomButton(ButtonType.COPY);
        this.copyButton.setText("");
        this.copyButton.setGraphic(new FontIcon(MaterialDesignC.CONTENT_COPY));
        this.copyButton.setTooltip(new Tooltip("Copy to Clipboard"));
        
        this.exportButton = new CustomButton(ButtonType.EXPORT);
        this.exportButton.setText("");
        this.exportButton.setGraphic(new FontIcon(MaterialDesignE.EXPORT));
        this.exportButton.setTooltip(new Tooltip("Export to File"));

        initialize();
    }

    private void initialize() {
        // Text Tab Content
        xmlResultTextArea.setEditable(false);
        
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

        // Visual Tab Content
        initializeReportTable();
        view.getVisualTab().setContent(reportTable);
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

    private void initializeToolbar() {
        textFormatChoiceBox.getItems().setAll("TURTLE", "RDF/XML", "JSON-LD", "N-TRIPLES", "N-QUADS", "TRIG");
        textFormatChoiceBox.getSelectionModel().select("TURTLE");

        copyButton.setOnAction(event -> handleCopy());
        exportButton.setOnAction(event -> handleExport());

        view.getIconButtonBarView().addCustomButton(copyButton);
        view.getIconButtonBarView().addCustomButton(exportButton);
    }

    private void handleCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(xmlResultTextArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        
        String originalText = copyButton.getText();
        copyButton.setText("Copied!");
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> copyButton.setText(originalText));
        pause.play();
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

    public ChoiceBox<String> getTextFormatChoiceBox() {
        return textFormatChoiceBox;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
