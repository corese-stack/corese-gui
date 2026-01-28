package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.core.factory.ButtonFactory;
import fr.inria.corese.gui.core.manager.DataManager;
import fr.inria.corese.gui.core.manager.GraphManager;
import fr.inria.corese.gui.core.manager.QueryManager;
import fr.inria.corese.gui.core.manager.RuleManager;
import fr.inria.corese.gui.core.factory.popup.ClearGraphConfirmationPopup;
import fr.inria.corese.gui.core.factory.popup.IPopup;
import fr.inria.corese.gui.core.factory.popup.LogDialog;
import fr.inria.corese.gui.core.factory.popup.PopupFactory;
import fr.inria.corese.gui.core.factory.popup.WarningPopup;
import fr.inria.corese.gui.feature.filelist.FileItem;
import fr.inria.corese.gui.feature.filelist.FileListView;
import fr.inria.corese.gui.feature.rule.RuleViewController;






import java.io.File;
import java.util.List;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;

/** Controller for the data view. Manages file list, rules, and data operations. */
public class DataViewController {
  private final DataManager dataManager;
  private final QueryManager queryManager;
  private final GraphManager graphManager;
  private final RuleManager ruleManager;
  private final PopupFactory popupFactory;

  private final DataView view;
  private RuleViewController ruleViewController;
  private LogDialog logDialog;

  /** Constructor for the data view controller. */
  public DataViewController(DataView view) {
    this.view = view;
    this.dataManager = DataManager.getInstance();
    this.queryManager = QueryManager.getInstance();
    this.graphManager = GraphManager.getInstance();
    this.ruleManager = new RuleManager(this.queryManager);
    this.popupFactory = PopupFactory.getInstance();
    
    initialize();
  }

  /** Initializes the controller. */
  private void initialize() {
    view.getTopBar().addRightButtons(List.of(
        ButtonFactory.logs(this::handleShowLogs),
        ButtonFactory.save(this::handleSaveAs)
    ));

    setupFileList();
    setupRulesView();

    updateView();
  }

  private void setupFileList() {
    FileListView fileListView = view.getFileListView();
    if (fileListView == null) {
      return;
    }
    try {
      fileListView.setModel(dataManager.getFileListModel());
      fileListView.setOnRemoveAction(this::handleRemoveFile);

      fileListView.getClearButton().setOnAction(e -> handleClearGraph());
      fileListView.getReloadButton().setOnAction(e -> handleReload());
      fileListView.getLoadButton().setOnAction(e -> handleLoadFiles());
    } catch (Exception e) {
      queryManager.addLogEntry("Error setting up file list view: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void setupRulesView() {
    if (view.getRuleView() == null) {
      return;
    }
    try {
      ruleViewController = new RuleViewController(view.getRuleView());

      ruleViewController.setRuleManager(this.ruleManager);
      ruleViewController.setParentController(this);
      ruleViewController.setOnRuleToggled(this::updateView);

      ruleViewController.initializeRules();

    } catch (Exception e) {
      queryManager.addLogEntry("Error loading rule view: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void handleSaveAs() {
    FileChooser fileChooser = new FileChooser();
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Turtle Files (*.ttl)", "*.ttl"));
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
    confirmPopup.setMessage(
        "Are you sure you want to clear the graph? This action cannot be undone.");
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
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("RDF & SPARQL", "*.ttl", "*.rdf", "*.n3"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));

    List<File> files = fileChooser.showOpenMultipleDialog(view.getFileListView().getScene().getWindow());

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

    if (view.getProjectStatisticsView() != null) {
      view.getProjectStatisticsView().setTripletCount(graphManager.getTripletCount());
      view.getProjectStatisticsView().setRulesLoadedCount(ruleManager.getLoadedRulesCount());
      view.getProjectStatisticsView().setSemanticElementsCount(0); // Placeholder as in original
      view.getProjectStatisticsView().setGraphCount(0); // Placeholder as in original
    }
  }

  public void loadGraphData(String content) {
    graphManager.loadGraph(content);
    ruleManager.applyRules();
    updateView();
  }

  public String getCurrentContent() {
    return dataManager.getCurrentContent();
  }
}
