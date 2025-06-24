package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.manager.ApplicationStateManager;
import fr.inria.corese.demo.manager.RuleManager;
import fr.inria.corese.demo.view.FileListView;
import fr.inria.corese.demo.view.TopBar;
import fr.inria.corese.demo.factory.popup.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controller for the data view.
 * Manages file list, rules, and data operations.
 */
public class DataViewController {
    private final ApplicationStateManager stateManager;
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
        this.stateManager = ApplicationStateManager.getInstance();
        this.popupFactory = PopupFactory.getInstance();
        this.ruleManager = stateManager.getRuleManager();
    }

    /**
     * Initializes the controller.
     * Sets up the top bar and initializes components.
     */
    @FXML
    public void initialize() {
        // Set up top bar buttons
        topBar.addLeftButtons(List.of(
                IconButtonType.OPEN_FILE,
                IconButtonType.SAVE));

        topBar.addRightButtons(List.of(
                IconButtonType.LOGS));

        // Set up button handlers
        topBar.setOnAction(IconButtonType.OPEN_FILE, this::handleOpenProject);
        topBar.setOnAction(IconButtonType.SAVE, this::handleSaveAs);
        topBar.setOnAction(IconButtonType.LOGS, this::handleShowLogs);

        // Initialize components
        initializeComponents();
    }

    /**
     * Initializes components.
     * Sets up the file list and rules view.
     */
    private void initializeComponents() {
        // Initialize file list
        if (fileListContainer != null) {
            setupFileList();
        }

        // Initialize rules
        if (rulesContainer != null) {
            setupRulesView();
        }

        // Update the view
        updateView();
    }

    /**
     * Sets up the file list view.
     */
    private void setupFileList() {
        if (fileListContainer == null) {
            stateManager.addLogEntry("Error: fileListContainer is null in setupFileList");
            return;
        }

        try {
            fileListView = new FileListView();
            fileListView.setModel(stateManager.getFileListModel());

            fileListContainer.getChildren().add(fileListView);
            VBox.setVgrow(fileListView, Priority.ALWAYS);

            // Set up button event handlers
            if (fileListView != null) {
                fileListView.getClearButton().setOnAction(e -> handleClearGraph());
                fileListView.getReloadButton().setOnAction(e -> handleReloadFiles());
                fileListView.getLoadButton().setOnAction(e -> handleLoadFiles());
            }

        } catch (Exception e) {
            stateManager.addLogEntry("Error setting up file list view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets up the rules view.
     */
    private void setupRulesView() {
        try {
            // Load the rule view FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fr/inria/corese/demo/rule-view.fxml"));
            VBox ruleView = loader.load();

            // Get the controller
            ruleViewController = loader.getController();

            // Create a scroll pane for the rule view
            ScrollPane scrollPane = new ScrollPane(ruleView);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.getStyleClass().add("edge-to-edge");

            // Add the scroll pane to the rules container
            rulesContainer.getChildren().add(scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            // Initialize rules
            ruleViewController.initializeRules();

        } catch (IOException e) {
            stateManager.addLogEntry("Error loading rule view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles opening a project.
     */
    private void handleOpenProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            stateManager.loadProject(selectedDirectory);
            updateView();
        }
    }

    /**
     * Handles saving a project.
     */
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                stateManager.saveProject(file);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Project saved successfully!");
                alert.showAndWait();

            } catch (Exception e) {
                System.err.println("Failed to save project: " + e.getMessage());
                e.printStackTrace(); 
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Save Error");
                alert.setHeaderText("Could not save the project.");
                alert.setContentText("An error occurred: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Handles clearing the graph.
     */
    private void handleClearGraph() {
        IPopup confirmPopup = popupFactory.createPopup(PopupFactory.CLEAR_GRAPH_CONFIRMATION);
        confirmPopup.setMessage("Are you sure you want to clear the graph? This action cannot be undone.");
        confirmPopup.displayPopup();

        if (((ClearGraphConfirmationPopup) confirmPopup).getResult()) {
            stateManager.clearGraph();
            stateManager.clearFiles();
            updateView();

            // Show success notification
            IPopup successPopup = popupFactory.createPopup(PopupFactory.TOAST_NOTIFICATION);
            successPopup.setMessage("Graph has been cleared successfully!");
            successPopup.displayPopup();
        }
    }

    /**
     * Handles reloading files.
     */
    private void handleReloadFiles() {
        stateManager.reloadFiles();
        updateView();
    }

    /**
     * Handles showing logs.
     */
    private void handleShowLogs() {
        if (logDialog == null) {
            logDialog = new LogDialog();
        }
        logDialog.updateLogs();
        logDialog.displayPopup();
    }

    /**
     * Handles loading files.
     */
    private void handleLoadFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("TTL files", "*.ttl"));

        // Get window from available components
        javafx.stage.Window window = null;
        if (fileListView != null && fileListView.getScene() != null) {
            window = fileListView.getScene().getWindow();
        } else if (fileListContainer != null && fileListContainer.getScene() != null) {
            window = fileListContainer.getScene().getWindow();
        } else if (topBar != null && topBar.getScene() != null) {
            window = topBar.getScene().getWindow();
        }

        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            try {
                stateManager.loadFile(file);
                updateView();

                // Show success notification
                if (popupFactory != null) {
                    IPopup successPopup = popupFactory.createPopup(PopupFactory.TOAST_NOTIFICATION);
                    successPopup.setMessage("File '" + file.getName() + "' has been successfully loaded!");
                    successPopup.displayPopup();
                } else {
                    // Fallback to standard alerts
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("File '" + file.getName() + "' has been successfully loaded!");
                    alert.showAndWait();
                }

            } catch (Exception e) {
                String errorMessage = "Error loading file: " + e.getMessage();

                // Error handling
                if (popupFactory != null) {
                    IPopup errorPopup = popupFactory.createPopup(PopupFactory.WARNING_POPUP);
                    errorPopup.setMessage(errorMessage);
                    ((WarningPopup) errorPopup).getResult();
                } else {
                    // Fallback to standard alerts
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error Loading File");
                    alert.setContentText(errorMessage);
                    alert.showAndWait();
                }
            }
        }
    }

    /**
     * Updates the view.
     * Updates statistics and rules.
     */
    private void updateView() {
        if (ruleViewController != null) {
            ruleViewController.updateView();
        }

        // Update statistics labels
        if (semanticElementsLabel != null) {
            semanticElementsLabel
                    .setText("Number of semantic elements loaded: " + stateManager.getSemanticElementsCount());
            tripletLabel.setText("Number of triplet: " + stateManager.getTripletCount());
            graphLabel.setText("Number of graph: " + stateManager.getGraphCount());
            rulesLoadedLabel.setText("Number of rules loaded: " + ruleManager.getLoadedRulesCount());
        }
    }
}