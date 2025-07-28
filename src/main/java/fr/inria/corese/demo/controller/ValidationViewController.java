package fr.inria.corese.demo.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.manager.QueryManager;
import fr.inria.corese.demo.model.ValidationModel;
import fr.inria.corese.demo.view.CustomButton;
import fr.inria.corese.demo.view.EmptyStateViewFactory;
import fr.inria.corese.demo.view.TopBar;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ValidationViewController {

    // --- FXML Fields (Updated to match the new FXML structure) ---
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private StackPane editorContainer;
    @FXML
    private SplitPane resultsSplitPane;
    @FXML
    private TopBar topBar;
    @FXML
    private TabPane resultsTabPane;
    @FXML
    private Tab tableTab;
    @FXML
    private Tab graphTab;
    @FXML
    private Tab textTab;
    @FXML
    private ComboBox<String> reportFormatComboBox;
    @FXML
    private Button copyReportButton;
    @FXML
    private TextArea reportTextArea;
    @FXML
    private HBox exportButtonContainer;

    private TabEditorController tabEditorController;
    private final ValidationModel validationModel = new ValidationModel();
    private final QueryManager stateManager = QueryManager.getInstance();
    private Node emptyStateView;
    private Graph lastReportGraph;
    private static final List<String> REPORT_FORMATS = List.of("TURTLE", "RDF/XML", "JSON-LD", "N-TRIPLES", "N-QUADS",
            "TRIG");

    @FXML
    public void initialize() {
        checkFXMLInjections();
        try {
            textTab.setText("Text");
            tableTab.setDisable(true);
            graphTab.setDisable(true);
            resultsTabPane.getSelectionModel().select(textTab);

            initializeTabEditor();
            initializeTopBar();
            setupValidateButtonForEachTab();
            initializeReportView();
            setupEmptyState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEmptyState() {
        Runnable newShapesAction = () -> tabEditorController.addNewTab("untitled-shapes.ttl", "");
        Runnable loadShapesAction = this::onOpenFilesButtonClick;

        this.emptyStateView = EmptyStateViewFactory.createValidationEmptyStateView(
                newShapesAction,
                loadShapesAction);

        editorContainer.getChildren().add(0, emptyStateView);
        updateEmptyStateVisibility();
    }

    private void updateEmptyStateVisibility() {
        long realTabCount = tabEditorController.getView().getTabPane().getTabs().stream()
                .filter(t -> t != tabEditorController.getView().getAddTab()).count();

        boolean noTabsOpen = (realTabCount == 0);

        if (emptyStateView != null) {
            emptyStateView.setVisible(noTabsOpen);
            emptyStateView.setManaged(noTabsOpen);
        }
        tabEditorController.getView().setVisible(!noTabsOpen);
        tabEditorController.getView().setManaged(!noTabsOpen);
    }

    private void initializeTopBar() {
        List<IconButtonType> buttons = new ArrayList<>();
        buttons.add(IconButtonType.OPEN_FILE);
        buttons.add(IconButtonType.EXPORT);
        topBar.addLeftButtons(buttons);

        topBar.getButton(IconButtonType.OPEN_FILE).setOnAction(e -> onOpenFilesButtonClick());
    }

    private void initializeReportView() {
        Button exportIconButton = topBar.getButton(IconButtonType.EXPORT);

        if (exportIconButton != null) {
            exportButtonContainer.getChildren().add(exportIconButton);
            exportIconButton.setOnAction(event -> handleExportReport());
        }

        reportFormatComboBox.getItems().setAll(REPORT_FORMATS);
        reportFormatComboBox.getSelectionModel().select("TURTLE");
        reportFormatComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && lastReportGraph != null) {
                formatAndDisplayReport(lastReportGraph, newVal);
            }
        });
        copyReportButton.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(reportTextArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
            String originalText = copyReportButton.getText();
            copyReportButton.setText("Copied!");
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> copyReportButton.setText(originalText));
            pause.play();
        });
    }

    public void executeValidation() {
        reportTextArea.clear();
        lastReportGraph = null;
        if (stateManager.getGraph().size() == 0) {
            String message = "Cannot validate: No data has been loaded in the 'Data' view.";
            reportTextArea.setText(message);
            showError("No Data Loaded", message);
            return;
        }
        Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
        if (selectedTab == null || selectedTab == tabEditorController.getView().getAddTab()) {
            return;
        }
        CodeEditorController codeEditorController = tabEditorController.getModel().getControllerForTab(selectedTab);
        if (codeEditorController == null) {
            return;
        }
        final String shapesContent = codeEditorController.getView().getText();
        if (shapesContent == null || shapesContent.trim().isEmpty()) {
            String message = "Cannot validate: The current tab is empty...";
            reportTextArea.setText(message);
            showError("Empty Shapes", message);
            return;
        }
        new Thread(() -> {
            Graph dataGraph = stateManager.getGraph();
            ValidationModel.ValidationResult result = validationModel.validate(dataGraph, shapesContent);
            Platform.runLater(() -> {
                if (result.getErrorMessage() != null) {
                    reportTextArea.setText("Validation Failed: Invalid SHACL Syntax\n\n" + result.getErrorMessage());
                    showError("Invalid SHACL Content", result.getErrorMessage());
                } else {
                    this.lastReportGraph = result.getReportGraph();
                    formatAndDisplayReport(this.lastReportGraph, reportFormatComboBox.getValue());
                }
            });
        }).start();
    }

    private void formatAndDisplayReport(Graph reportGraph, String format) {
        if (reportGraph == null || format == null)
            return;
        ResultFormat.format coreseFormat;
        switch (format) {
            case "RDF/XML":
                coreseFormat = ResultFormat.format.RDF_XML_FORMAT;
                break;
            case "JSON-LD":
                coreseFormat = ResultFormat.format.JSONLD_FORMAT;
                break;
            case "N-TRIPLES":
                coreseFormat = ResultFormat.format.NTRIPLES_FORMAT;
                break;
            case "N-QUADS":
                coreseFormat = ResultFormat.format.NQUADS_FORMAT;
                break;
            case "TRIG":
                coreseFormat = ResultFormat.format.TRIG_FORMAT;
                break;
            default:
                coreseFormat = ResultFormat.format.TURTLE_FORMAT;
                break;
        }
        String formattedReport = stateManager.formatGraph(reportGraph, coreseFormat);
        reportTextArea.setText(formattedReport);
    }

    private void handleExportReport() {
        if (lastReportGraph == null) {
            showError("Export Error", "There is no validation report to export. Please run a validation first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Validation Report");
        String selectedFormat = reportFormatComboBox.getValue();
        String extension = getExtensionForFormat(selectedFormat);
        fileChooser.setInitialFileName("validation-report" + extension);
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                selectedFormat + " file (*" + extension + ")", "*" + extension);
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showSaveDialog(topBar.getScene().getWindow());

        if (file != null) {
            File fileToSave = file;
            if (!file.getName().toLowerCase().endsWith(extension)) {
                fileToSave = new File(file.getAbsolutePath() + extension);
            }
            try {
                String reportContent = reportTextArea.getText();
                Files.writeString(fileToSave.toPath(), reportContent);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText(
                        "The validation report has been successfully exported to:\n" + fileToSave.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                showError("Export Failed", "An error occurred while saving the file:\n" + e.getMessage());
            }
        }
    }

    private String getExtensionForFormat(String format) {
        return switch (format) {
            case "RDF/XML" -> ".rdf";
            case "JSON-LD" -> ".jsonld";
            case "N-TRIPLES" -> ".nt";
            case "N-QUADS" -> ".nq";
            case "TRIG" -> ".trig";
            default -> ".ttl";
        };
    }

    private void initializeTabEditor() {
        tabEditorController = new TabEditorController(IconButtonBarType.VALIDATION);
        tabEditorController.getView().setMaxWidth(Double.MAX_VALUE);
        tabEditorController.getView().setMaxHeight(Double.MAX_VALUE);
        editorContainer.getChildren().add(tabEditorController.getView());
    }

    private void setupValidateButtonForEachTab() {
        tabEditorController.getView().getTabPane().getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab tab : c.getAddedSubList()) {
                        if (tab != tabEditorController.getView().getAddTab()) {
                            Platform.runLater(() -> {
                                CodeEditorController controller = tabEditorController.getModel()
                                        .getControllerForTab(tab);
                                if (controller != null) {
                                    configureEditorValidateButton(controller);
                                }
                            });
                        }
                    }
                }
            }
            Platform.runLater(this::updateEmptyStateVisibility);
        });
    }

    private void configureEditorValidateButton(CodeEditorController controller) {
        controller.getView().displayRunButton();
        CustomButton validateButton = controller.getView().getRunButton();
        if (validateButton != null) {
            validateButton.setOnAction(e -> executeValidation());
        }
        controller.getView().setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)) {
                executeValidation();
                event.consume();
            }
        });
    }

    private void onOpenFilesButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Shapes File");
        fileChooser.getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("RDF Files", "*.ttl", "*.rdf", "*.n3"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            tabEditorController.addNewTab(file);
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void checkFXMLInjections() {
        StringBuilder missing = new StringBuilder();
        if (mainBorderPane == null)
            missing.append("mainBorderPane, ");
        if (mainSplitPane == null)
            missing.append("mainSplitPane, ");
        if (resultsSplitPane == null)
            missing.append("resultsSplitPane, ");
        if (editorContainer == null)
            missing.append("editorContainer, ");
        if (topBar == null)
            missing.append("topBar, ");
        if (resultsTabPane == null)
            missing.append("resultsTabPane, ");
        if (tableTab == null)
            missing.append("tableTab, ");
        if (graphTab == null)
            missing.append("graphTab, ");
        if (textTab == null)
            missing.append("textTab, ");
        if (reportFormatComboBox == null)
            missing.append("reportFormatComboBox, ");
        if (copyReportButton == null)
            missing.append("copyReportButton, ");
        if (reportTextArea == null)
            missing.append("reportTextArea, ");
        if (exportButtonContainer == null)
            missing.append("exportButtonContainer, ");

        if (missing.length() > 0) {
            String missingFields = missing.substring(0, missing.length() - 2);
            System.err.println("Missing FXML injections in ValidationViewController: " + missingFields);
        }
    }
}