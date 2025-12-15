package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.model.ValidationModel;
import fr.inria.corese.gui.view.EmptyStateView;
import fr.inria.corese.gui.view.ValidationView;
import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private final ValidationModel validationModel;

  // Sub-Controllers
  private TabEditorController tabEditorController;
  private ResultController resultController;

  // ===== Constructor =====

  /**
   * Constructor.
   *
   * @param view The ValidationView associated with this controller.
   */
  public ValidationController(ValidationView view) {
    this.view = view;
    this.validationModel = new ValidationModel();
    initialize();
  }

  /** Initializes the controller components and UI. */
  private void initialize() {
    initializeResultView();
    initializeEditor();
  }

  private void initializeResultView() {
    // Initialize Result Component
    resultController = new ResultController(List.of(IconButtonType.COPY, IconButtonType.EXPORT));
    view.setResultView(resultController.getViewRoot());

    // Listener for result format changes (e.g., Turtle, JSON-LD)
    resultController.setOnFormatChanged(this::updateReportDisplay);
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
    tabEditorController.setOnExecutionRequest(this::executeValidation);
    tabEditorController.setEmptyState(createEmptyStateView());

    // Add floating validate button
    tabEditorController.addExecutionButton("Run Validation");

    // Set the editor view in the main view
    view.setEditorView(tabEditorController.getViewRoot());
  }

  /** Creates the empty state view (shown when no tabs are open). */
  private Node createEmptyStateView() {
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

    return new EmptyStateView(
        MaterialDesignS.SHIELD_CHECK_OUTLINE,
        "No shapes files open.\nCreate a new shapes file or load an existing one.",
        newButton,
        loadButton);
  }

  /**
   * Executes the validation logic. Validates the data graph against the shapes in the active tab.
   */
  public void executeValidation() {
    resultController.clearResults();

    // Check if data is loaded in the GraphManager
    if (!validationModel.isDataLoaded()) {
      String message = "Cannot validate: No data has been loaded in the 'Data' view.";
      resultController.updateText(message);
      showError("No Data Loaded", message);
      return;
    }

    Tab selectedTab =
        tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab == null) {
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
    new Thread(
            () -> {
              // Delegate validation to the model
              ValidationModel.ValidationResult result = validationModel.validate(shapesContent);

              Platform.runLater(
                  () -> {
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
                      // Note: We pass the Graph object here as required by ResultController,
                      // but we do not store it in this controller.
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
    String formattedReport = validationModel.formatLastReport(format);
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
