package fr.inria.corese.gui.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.gui.enums.icon.IconButtonBarType;
import fr.inria.corese.gui.manager.GraphManager;
import fr.inria.corese.gui.manager.QueryManager;
import fr.inria.corese.gui.model.ValidationModel;
import fr.inria.corese.gui.view.CustomButton;
import fr.inria.corese.gui.view.EmptyStateView;
import fr.inria.corese.gui.view.ValidationView;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class ValidationController {
  private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);

  private final ValidationView view;
  private final ValidationModel validationModel;
  private final GraphManager graphManager;
  
  private TabEditorController tabEditorController;
  private ResultController resultController;
  private Node emptyStateView;
  private Graph lastReportGraph;
  private StackPane editorContainer;

  public ValidationController(ValidationView view) {
    this.view = view;
    this.validationModel = new ValidationModel();
    this.graphManager = GraphManager.getInstance();
    initialize();
  }

  private void initialize() {
    try {
      // Initialize Result Component
      resultController = new ResultController();
      view.setResultView(resultController.getView().getRoot());

      resultController.getTextFormatChoiceBox().getSelectionModel().selectedItemProperty().addListener(
          (obs, oldVal, newVal) -> {
            if (newVal != null && lastReportGraph != null) {
              formatAndDisplayReport(lastReportGraph, newVal);
            }
          });

      // Initialize Editor Component
      initializeTabEditor();
      
      // Setup Empty State
      setupEmptyState();

    } catch (Exception e) {
      logger.error("Error initializing ValidationViewController", e);
    }
  }

  private void initializeTabEditor() {
    tabEditorController = new TabEditorController(IconButtonBarType.VALIDATION);
    tabEditorController.getView().setMaxWidth(Double.MAX_VALUE);
    tabEditorController.getView().setMaxHeight(Double.MAX_VALUE);
    
    // Wrap in StackPane for EmptyState and Floating Button
    editorContainer = new StackPane(tabEditorController.getView());
    view.setEditorView(editorContainer);
    
    setupFloatingValidateButton();
    setupTabListeners();
  }

  private void setupTabListeners() {
    tabEditorController
        .getView()
        .getTabPane()
        .getTabs()
        .addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
              if (c.wasAdded()) {
                for (Tab tab : c.getAddedSubList()) {
                  if (tab != tabEditorController.getView().getAddTab()) {
                    Platform.runLater(() -> configureTab(tab));
                  }
                }
              }
            }
        });
  }

  private void setupFloatingValidateButton() {
    Button validateButton = new Button();
    FontIcon playIcon = new FontIcon(MaterialDesignP.PLAY);
    playIcon.setIconSize(24);
    playIcon.setIconColor(javafx.scene.paint.Color.WHITE);
    validateButton.setGraphic(playIcon);
    
    validateButton.setStyle(
        "-fx-background-color: -color-accent-emphasis; " +
        "-fx-text-fill: -color-fg-emphasis; " +
        "-fx-background-radius: 50%; " +
        "-fx-min-width: 56px; " +
        "-fx-min-height: 56px; " +
        "-fx-max-width: 56px; " +
        "-fx-max-height: 56px; " +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 4); " +
        "-fx-cursor: hand;"
    );
    
    validateButton.setTooltip(new Tooltip("Run Validation (Ctrl+Enter)"));
    validateButton.setOnAction(e -> executeValidation());
    
    // Bind visibility to the editor view visibility (hidden when empty state is shown)
    validateButton.visibleProperty().bind(tabEditorController.getView().visibleProperty());
    validateButton.managedProperty().bind(tabEditorController.getView().visibleProperty());
    
    // Disable button when current editor content is empty
    validateButton.disableProperty().bind(
        javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
            if (selectedTab == null || selectedTab == tabEditorController.getView().getAddTab()) {
                return true;
            }
            CodeEditorController controller = tabEditorController.getModel().getControllerForTab(selectedTab);
            return controller == null || controller.getModel().getContent().trim().isEmpty();
        }, 
        tabEditorController.getView().getTabPane().getSelectionModel().selectedItemProperty(),
        // We need to listen to content changes of the active tab. 
        // This is a simplification; ideally we'd rebind when tab changes.
        // For now, let's just bind to the tab selection and rely on the fact that 
        // we might need a more complex binding if we want real-time updates while typing.
        // To do it properly, we need a listener on the tab selection that updates the binding.
        tabEditorController.getView().getTabPane().getTabs()
        )
    );
    
    // Better approach for disable property:
    tabEditorController.getView().getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
        updateValidateButtonState(validateButton);
    });
    
    StackPane.setAlignment(validateButton, Pos.BOTTOM_RIGHT);
    // Add margins to avoid overlap with scrollbars and toolbars
    StackPane.setMargin(validateButton, new Insets(0, 80, 50, 0));
    
    editorContainer.getChildren().add(validateButton);
  }

  private void updateValidateButtonState(Button validateButton) {
      Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
      if (selectedTab == null || selectedTab == tabEditorController.getView().getAddTab()) {
          validateButton.disableProperty().unbind();
          validateButton.setDisable(true);
          return;
      }
      
      CodeEditorController controller = tabEditorController.getModel().getControllerForTab(selectedTab);
      if (controller != null) {
          validateButton.disableProperty().bind(controller.getModel().contentProperty().isEmpty());
      } else {
          validateButton.disableProperty().unbind();
          validateButton.setDisable(true);
      }
  }
// ...existing code...
  private void configureTab(Tab tab) {
    CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
    if (controller != null) {
      configureEditorShortcuts(controller);
    }
  }

  private void configureEditorShortcuts(CodeEditorController controller) {
    // Only shortcuts, no button in toolbar
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
// ...existing code...

  private void setupEmptyState() {
    Runnable newShapesAction = () -> tabEditorController.addNewTab("untitled-shapes.ttl", "");
    Runnable loadShapesAction = this::onOpenFilesButtonClick;

    Button newButton = new Button("New Shapes File");
    newButton.setTooltip(new Tooltip("CTRL + N"));
    newButton.setOnAction(e -> newShapesAction.run());
    newButton.getStyleClass().add("custom-button");

    Button loadButton = new Button("Load Shapes File");
    loadButton.setTooltip(new Tooltip("CTRL + O"));
    loadButton.setOnAction(e -> loadShapesAction.run());
    loadButton.getStyleClass().add("custom-button");

    this.emptyStateView = new EmptyStateView(
        MaterialDesignS.SHIELD_CHECK_OUTLINE,
        "No shapes files open.\nCreate a new shapes file or load an existing one.",
        newButton, loadButton
    );

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

  public void executeValidation() {
    resultController.clearResults();
    lastReportGraph = null;

    if (graphManager.getGraph().size() == 0) {
      String message = "Cannot validate: No data has been loaded in the 'Data' view.";
      resultController.updateText(message);
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
      resultController.updateText(message);
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
                    resultController.getView().getTabPane().getSelectionModel().select(resultController.getView().getTextTab());

                    if (result.getErrorMessage() != null) {
                      String errorMsg =
                          "Validation Failed: Invalid SHACL Syntax\n\n" + result.getErrorMessage();
                      resultController.updateText(errorMsg);
                      showError("Invalid SHACL Content", result.getErrorMessage());
                    } else {
                      this.lastReportGraph = result.getReportGraph();
                      formatAndDisplayReport(this.lastReportGraph, "TURTLE");
                      resultController.displayReport(this.lastReportGraph);
                    }
                  });
            })
        .start();
  }

  private void formatAndDisplayReport(Graph reportGraph, String format) {
    if (reportGraph == null || format == null) return;

    ResultFormatDef.format coreseFormat;
    switch (format.toUpperCase()) {
      case "RDF/XML":
        coreseFormat = ResultFormatDef.format.RDF_XML_FORMAT;
        break;
      case "JSON-LD":
        coreseFormat = ResultFormatDef.format.JSONLD_FORMAT;
        break;
      case "N-TRIPLES":
        coreseFormat = ResultFormatDef.format.NTRIPLES_FORMAT;
        break;
      case "N-QUADS":
        coreseFormat = ResultFormatDef.format.NQUADS_FORMAT;
        break;
      case "TRIG":
        coreseFormat = ResultFormatDef.format.TRIG_FORMAT;
        break;
      default:
        coreseFormat = ResultFormatDef.format.TURTLE_FORMAT;
        break;
    }
    String formattedReport = QueryManager.getInstance().formatGraph(reportGraph, coreseFormat);
    resultController.updateText(formattedReport);
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
}
