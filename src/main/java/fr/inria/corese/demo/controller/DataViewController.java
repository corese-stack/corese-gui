package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.manager.DataManager;
import fr.inria.corese.demo.manager.QueryManager;
import fr.inria.corese.demo.manager.GraphManager;
import fr.inria.corese.demo.manager.RuleManager;
import fr.inria.corese.demo.view.FileListView;
import fr.inria.corese.demo.view.TopBar;
import fr.inria.corese.demo.factory.popup.*;
import fr.inria.corese.demo.model.fileList.FileItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controller for the data view.
 * Manages file list, rules, and data operations.
 */
public class DataViewController {
    private final DataManager dataManager;
    private final QueryManager queryManager;
    private final GraphManager graphManager;
    private final RuleManager ruleManager;
    private final PopupFactory popupFactory;

    private RuleViewController ruleViewController;
    private LogDialog logDialog;
    private FileListView fileListView;

    // FXML injected components
    @FXML
    private HBox configActionBox;
    @FXML
    private VBox fileListContainer;
    @FXML
    private VBox rulesContainer;
    @FXML
    private Label semanticElementsLabel;
    @FXML
    private Label tripletLabel;
    @FXML
    private Label graphLabel;
    @FXML
    private Label rulesLoadedLabel;
    @FXML
    private TopBar topBar;

    /**
     * Constructor for the data view controller.
     */
    public DataViewController() {
        this.dataManager = DataManager.getInstance();
        this.queryManager = QueryManager.getInstance();
        this.graphManager = GraphManager.getInstance();
        this.ruleManager = new RuleManager(this.queryManager);
        this.popupFactory = PopupFactory.getInstance();
    }

    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        topBar.addLeftButtons(List.of(
                IconButtonType.OPEN_FILE,
                IconButtonType.SAVE));
        topBar.addRightButtons(List.of(
                IconButtonType.LOGS));

        topBar.setOnAction(IconButtonType.OPEN_FILE, this::handleLoadFiles);
        topBar.setOnAction(IconButtonType.SAVE, this::handleSaveAs);
        topBar.setOnAction(IconButtonType.LOGS, this::handleShowLogs);

        setupFileList();
        setupRulesView();

        updateView();
    }

    private void setupFileList() {
        if (fileListContainer == null) {
            return;
        }
        try {
            fileListView = new FileListView();
            fileListView.setModel(dataManager.getFileListModel());
            fileListView.setOnRemoveAction(this::handleRemoveFile);

            fileListContainer.getChildren().add(fileListView);
            VBox.setVgrow(fileListView, Priority.ALWAYS);

            if (fileListView != null) {
                fileListView.getClearButton().setOnAction(e -> handleClearGraph());
                fileListView.getReloadButton().setOnAction(e -> handleReload());
                fileListView.getLoadButton().setOnAction(e -> handleLoadFiles());
            }
        } catch (Exception e) {
            queryManager.addLogEntry("Error setting up file list view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupRulesView() {
        if (rulesContainer == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fr/inria/corese/demo/rule-view.fxml"));
            VBox ruleView = loader.load();
            ruleViewController = loader.getController();

            ruleViewController.setRuleManager(this.ruleManager);
            ruleViewController.setOnRuleToggled(this::updateView);

            ScrollPane scrollPane = new ScrollPane(ruleView);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.getStyleClass().add("edge-to-edge");
            rulesContainer.getChildren().add(scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            ruleViewController.initializeRules();

        } catch (IOException e) {
            queryManager.addLogEntry("Error loading rule view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Turtle Files (*.ttl)", "*.ttl"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                dataManager.saveGraph(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Graph saved successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Save Error");
                alert.setHeaderText("Could not save the graph.");
                alert.setContentText("An error occurred: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void handleClearGraph() {
        IPopup confirmPopup = popupFactory.createPopup(PopupFactory.CLEAR_GRAPH_CONFIRMATION);
        confirmPopup.setMessage("Are you sure you want to clear the graph? This action cannot be undone.");
        confirmPopup.displayPopup();

        if (((ClearGraphConfirmationPopup) confirmPopup).getResult()) {
            dataManager.clearGraphAndFiles();
            updateView();
            IPopup successPopup = popupFactory.createPopup(PopupFactory.TOAST_NOTIFICATION);
            successPopup.setMessage("Graph has been cleared successfully!");
            successPopup.displayPopup();
        }
    }

    private void handleRemoveFile(FileItem item) {
        if (item != null) {
            dataManager.removeFile(item.getFile());
            updateView();
        }
    }

    private void handleReload() {
        dataManager.reloadFiles();
        ruleManager.applyRules();
        updateView();
    }

    private void handleShowLogs() {
        if (logDialog == null) {
            logDialog = new LogDialog();
        }
        logDialog.updateLogs();
        logDialog.displayPopup();
    }

    private void handleLoadFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open RDF Data File(s)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("RDF & SPARQL", "*.ttl", "*.rdf", "*.n3"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        List<File> files = fileChooser.showOpenMultipleDialog(topBar.getScene().getWindow());

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                try {
                    dataManager.loadFile(file);
                    IPopup successPopup = popupFactory.createPopup(PopupFactory.TOAST_NOTIFICATION);
                    successPopup.setMessage("File '" + file.getName() + "' loaded!");
                    successPopup.displayPopup();
                } catch (Exception e) {
                    String errorMessage = "Error loading file '" + file.getName() + "': " + e.getMessage();
                    queryManager.addLogEntry(errorMessage);
                    IPopup errorPopup = popupFactory.createPopup(PopupFactory.WARNING_POPUP);
                    errorPopup.setMessage(errorMessage);
                    ((WarningPopup) errorPopup).getResult();
                }
            }
            updateView(); 
        }
    }


    private void updateView() {
        if (ruleViewController != null) {
            ruleViewController.updateView();
        }

        if (tripletLabel != null) {
            tripletLabel.setText("Number of triplet: " + graphManager.getTripletCount());
            rulesLoadedLabel.setText("Number of rules loaded: " + ruleManager.getLoadedRulesCount());

            semanticElementsLabel.setText("");
            graphLabel.setText("");
        }
    }
}