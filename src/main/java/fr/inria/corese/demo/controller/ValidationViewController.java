package fr.inria.corese.demo.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.manager.GraphManager;
import fr.inria.corese.demo.manager.QueryManager;
import fr.inria.corese.demo.model.ValidationModel;
import fr.inria.corese.demo.view.CustomButton;
import fr.inria.corese.demo.view.EmptyStateViewFactory;
import fr.inria.corese.demo.view.TopBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

public class ValidationViewController {
  private static final Logger logger = LoggerFactory.getLogger(ValidationViewController.class);

  // --- FXML Fields ---
  @FXML private BorderPane mainBorderPane;
  @FXML private SplitPane mainSplitPane;
  @FXML private StackPane editorContainer;
  @FXML private SplitPane resultsSplitPane;
  @FXML private TopBar topBar;
  @FXML private TabPane resultsTabPane;
  @FXML private Tab tableTab;
  @FXML private Tab graphTab;
  @FXML private Tab textTab;

  private TabEditorController tabEditorController;
  private final ValidationModel validationModel = new ValidationModel();
  private final GraphManager graphManager = GraphManager.getInstance();
  private Node emptyStateView;
  private Graph lastReportGraph;

  private ResultsPaneController resultsPaneController;

  private static final List<String> REPORT_FORMATS =
      List.of("TURTLE", "RDF/XML", "JSON-LD", "N-TRIPLES", "N-QUADS", "TRIG");

  @FXML
  public void initialize() {
    checkFXMLInjections();
    try {
      resultsPaneController = new ResultsPaneController();
      tableTab.setContent(resultsPaneController.getTableBox());
      graphTab.setContent(resultsPaneController.getGraphView());
      textTab.setContent(resultsPaneController.getTextViewBox());

      tableTab.setDisable(true);
      graphTab.setDisable(true);
      textTab.setDisable(false);
      resultsTabPane.getSelectionModel().select(textTab);

      resultsPaneController
          .getTextFormatComboBox()
          .getSelectionModel()
          .selectedItemProperty()
          .addListener(
              (obs, oldVal, newVal) -> {
                if (newVal != null && lastReportGraph != null) {
                  formatAndDisplayReport(lastReportGraph, newVal);
                }
              });

      initializeTabEditor();
      initializeTopBar();
      setupValidateButtonForEachTab();
      setupEmptyState();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setupEmptyState() {
    Runnable newShapesAction = () -> tabEditorController.addNewTab("untitled-shapes.ttl", "");
    Runnable loadShapesAction = this::onOpenFilesButtonClick;

    this.emptyStateView =
        EmptyStateViewFactory.createValidationEmptyStateView(newShapesAction, loadShapesAction);

    editorContainer.getChildren().add(0, emptyStateView);
    updateEmptyStateVisibility();

    tabEditorController
        .getView()
        .getTabPane()
        .getTabs()
        .addListener(
            (ListChangeListener<Tab>)
                c -> {
                  while (c.next()) {
                    Platform.runLater(this::updateEmptyStateVisibility);
                  }
                });
  }

  private void updateEmptyStateVisibility() {
    long realTabCount =
        tabEditorController.getView().getTabPane().getTabs().stream()
            .filter(t -> t != tabEditorController.getView().getAddTab())
            .count();
    boolean noTabsOpen = (realTabCount == 0);

    if (emptyStateView != null) {
      emptyStateView.setVisible(noTabsOpen);
      emptyStateView.setManaged(noTabsOpen);
    }
    tabEditorController.getView().setVisible(!noTabsOpen);
    tabEditorController.getView().setManaged(!noTabsOpen);
  }

  private void initializeTopBar() {
    List<IconButtonType> buttons = new ArrayList<>(List.of(IconButtonType.OPEN_FILE));
    topBar.addLeftButtons(buttons);
    topBar.getButton(IconButtonType.OPEN_FILE).setOnAction(e -> onOpenFilesButtonClick());
  }

  public void executeValidation() {
    resultsPaneController.clearResults();
    lastReportGraph = null;

    if (graphManager.getGraph().size() == 0) {
      String message = "Cannot validate: No data has been loaded in the 'Data' view.";
      resultsPaneController.updateXMLView(message);
      showError("No Data Loaded", message);
      return;
    }

    Tab selectedTab =
        tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab == null || selectedTab == tabEditorController.getView().getAddTab()) {
      return;
    }

    CodeEditorController codeEditorController =
        tabEditorController.getModel().getControllerForTab(selectedTab);
    if (codeEditorController == null) {
      return;
    }

    final String shapesContent = codeEditorController.getView().getText();
    if (shapesContent == null || shapesContent.trim().isEmpty()) {
      String message = "Cannot validate: The current tab is empty...";
      resultsPaneController.updateXMLView(message);
      showError("Empty Shapes", message);
      return;
    }

    new Thread(
            () -> {
              Graph dataGraph = graphManager.getGraph();
              ValidationModel.ValidationResult result =
                  validationModel.validate(dataGraph, shapesContent);
              Platform.runLater(
                  () -> {
                    tableTab.setDisable(true);
                    graphTab.setDisable(true);
                    textTab.setDisable(false);
                    resultsPaneController.setTextFormats(REPORT_FORMATS, "TURTLE");
                    resultsTabPane.getSelectionModel().select(textTab);

                    if (result.getErrorMessage() != null) {
                      String errorMsg =
                          "Validation Failed: Invalid SHACL Syntax\n\n" + result.getErrorMessage();
                      resultsPaneController.updateXMLView(errorMsg);
                      showError("Invalid SHACL Content", result.getErrorMessage());
                    } else {
                      this.lastReportGraph = result.getReportGraph();
                      formatAndDisplayReport(this.lastReportGraph, "TURTLE");
                    }
                  });
            })
        .start();
  }

  private void formatAndDisplayReport(Graph reportGraph, String format) {
    if (reportGraph == null || format == null) return;

    ResultFormat.format coreseFormat;
    switch (format.toUpperCase()) {
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
    String formattedReport = QueryManager.getInstance().formatGraph(reportGraph, coreseFormat);
    resultsPaneController.updateXMLView(formattedReport);
  }

  private void initializeTabEditor() {
    tabEditorController = new TabEditorController(IconButtonBarType.VALIDATION);
    tabEditorController.getView().setMaxWidth(Double.MAX_VALUE);
    tabEditorController.getView().setMaxHeight(Double.MAX_VALUE);
    editorContainer.getChildren().add(tabEditorController.getView());
  }

  private void setupValidateButtonForEachTab() {
    tabEditorController
        .getView()
        .getTabPane()
        .getTabs()
        .addListener(
            (ListChangeListener<Tab>)
                c -> {
                  while (c.next()) {
                    if (c.wasAdded()) {
                      for (Tab tab : c.getAddedSubList()) {
                        if (tab != tabEditorController.getView().getAddTab()) {
                          Platform.runLater(
                              () -> {
                                CodeEditorController controller =
                                    tabEditorController.getModel().getControllerForTab(tab);
                                if (controller != null) {
                                  configureEditorValidateButton(controller);
                                }
                              });
                        }
                      }
                    }
                  }
                });
  }

  private void configureEditorValidateButton(CodeEditorController controller) {
    controller.getView().displayRunButton();
    CustomButton validateButton = controller.getView().getRunButton();
    if (validateButton != null) {
      validateButton.setOnAction(e -> executeValidation());
    }
    controller
        .getView()
        .setOnKeyPressed(
            event -> {
              if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)) {
                executeValidation();
                event.consume();
              }
            });
  }

  private void onOpenFilesButtonClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Shapes File");
    fileChooser
        .getExtensionFilters()
        .addAll(new FileChooser.ExtensionFilter("RDF Files", "*.ttl", "*.rdf", "*.n3", "*.shacl"));
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
    if (mainBorderPane == null
        || mainSplitPane == null
        || editorContainer == null
        || topBar == null
        || resultsTabPane == null
        || tableTab == null
        || graphTab == null
        || textTab == null) {
      logger.warn("One or more FXML fields in ValidationViewController were not injected.");
    }
  }
}
