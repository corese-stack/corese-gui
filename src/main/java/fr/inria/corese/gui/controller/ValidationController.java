package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.model.ValidationModel;
import fr.inria.corese.gui.model.ValidationResult;
import fr.inria.corese.gui.view.ValidationView;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;

/**
 * Controller for the Validation View.
 *
 * <p>This controller manages the interaction between the Validation View, the Validation Model, and
 * the user. It handles the setup of the editor, the execution of SHACL validation, and the display
 * of results.
 */
public class ValidationController {

  // MVC Components
  private final ValidationView view;
  private final Map<Tab, ValidationModel> tabModels = new HashMap<>();

  // Sub-Controllers
  private TabEditorController tabEditorController;

  // ===== Constructor =====

  /**
   * Constructor.
   *
   * @param view The ValidationView associated with this controller.
   */
  public ValidationController(ValidationView view) {
    this.view = view;
    initialize();
  }

  /** Initializes the controller components and UI. */
  private void initialize() {
    initializeEditor();
  }

  /** Initializes the tab editor for SHACL shapes. */
  private void initializeEditor() {
    tabEditorController =
        new TabEditorController(
            List.of(
                IconButtonType.SAVE,
                IconButtonType.EXPORT,
                IconButtonType.CLEAR,
                IconButtonType.UNDO,
                IconButtonType.REDO));

    // Configure the editor
    tabEditorController.setResultControllerFactory(tab -> createResultController());
    tabEditorController.setOnExecutionRequest(this::executeValidation);
    
    // Create empty state via View
    Node emptyState = view.createEmptyState(
        () -> tabEditorController.addNewTab("untitled-shapes.ttl", ""),
        this::onOpenFilesButtonClick
    );
    tabEditorController.setEmptyState(emptyState);

    // Add floating validate button
    tabEditorController.addExecutionButton("Run Validation");

    // Set the editor view in the main view
    view.setMainContent(tabEditorController.getViewRoot());

    // Listen for tab removal to clean up models
    tabEditorController.getView().getTabPane().getTabs().addListener((ListChangeListener<Tab>) c -> {
        while (c.next()) {
            if (c.wasRemoved()) {
                for (Tab tab : c.getRemoved()) {
                    tabModels.remove(tab);
                }
            }
        }
    });
  }

  private ResultController createResultController() {
    ResultController controller = new ResultController(List.of(IconButtonType.COPY, IconButtonType.EXPORT));
    controller.setOnFormatChanged(this::updateReportDisplay);
    return controller;
  }

  /**
   * Executes the validation logic. Validates the data graph against the shapes in the active tab.
   */
  public void executeValidation() {
    Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab == null) return;

    ResultController resultController = tabEditorController.getCurrentResultController();
    if (resultController == null) return;

    resultController.clearResults();

    ValidationModel model = tabModels.computeIfAbsent(selectedTab, k -> new ValidationModel());

    // Check if data is loaded in the GraphManager
    if (!model.isDataLoaded()) {
      String message = "Cannot validate: No data has been loaded in the 'Data' view.";
      resultController.updateText(message);
      showError("No Data Loaded", message);
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

    // Run validation in a background thread to avoid blocking the UI
    tabEditorController.setExecutionState(true);
    new Thread(
            () -> {
              // Delegate validation to the model
              ValidationResult result = model.validate(shapesContent);

              Platform.runLater(
                  () -> {
                    tabEditorController.setExecutionState(false);
                    tabEditorController.showResultPane();
                    
                    // Select the text tab to show results
                    resultController.selectTextTab();

                    if (result.getErrorMessage() != null) {
                      String errorMsg =
                          "Validation Failed: Invalid SHACL Syntax\n\n" + result.getErrorMessage();
                      resultController.updateText(errorMsg);
                      showError("Invalid SHACL Content", result.getErrorMessage());
                    } else {
                      // Display the report in the default format (Turtle)
                      updateReportDisplay("TURTLE");

                      // Pass the report graph to the result controller for other visualizations
                      resultController.displayReport(result.getReportGraph());
                    }
                  });
            })
        .start();
  }

  /**
   * Updates the text area with the validation report in the specified format.
   *
   * @param format The format to display the report in (e.g., "TURTLE").
   */
  private void updateReportDisplay(String format) {
    Tab selectedTab = tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab == null) return;
    
    ValidationModel model = tabModels.get(selectedTab);
    if (model == null) return;

    ResultController resultController = tabEditorController.getCurrentResultController();
    if (resultController == null) return;

    String formattedReport = model.formatLastReport(format);
    if (formattedReport != null) {
      resultController.updateText(formattedReport);
    }
  }

  /** Opens a file chooser to load shapes files. */
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

  /**
   * Displays an error alert.
   *
   * @param title The title of the alert.
   * @param content The content message of the alert.
   */
  private void showError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
