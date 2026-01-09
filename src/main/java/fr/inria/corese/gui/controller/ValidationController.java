package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.manager.ShaclManager;
import fr.inria.corese.gui.model.ValidationModel;
import fr.inria.corese.gui.model.ValidationResult;
import fr.inria.corese.gui.view.ValidationView;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;

/**
 * Controller for the Validation View.
 *
 * <p>This controller acts as the main orchestrator for the SHACL Validation feature. It follows the
 * MVC pattern, mediating between the {@link ValidationView} and the {@link ValidationModel}.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Initializing the editor interface via {@link TabEditorController}.
 *   <li>Managing the lifecycle of {@link ValidationModel} instances associated with each tab.
 *   <li>Handling user actions such as running validation, opening files, and updating results.
 * </ul>
 */
public class ValidationController {

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  /** The main view associated with this controller. */
  private final ValidationView view;

  /**
   * Mapping between UI Tabs and their specific Validation Models. This allows maintaining separate
   * validation states (shapes, results) for each open tab.
   */
  private final Map<Tab, ValidationModel> tabModels = new HashMap<>();

  /** Sub-controller for managing the tabbed code editor interface. */
  private TabEditorController tabEditorController;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new ValidationController.
   *
   * @param view The ValidationView to be managed.
   */
  public ValidationController(ValidationView view) {
    this.view = view;
    initialize();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /**
   * Initializes the controller by setting up the editor, configuring it, and integrating it into
   * the view.
   */
  private void initialize() {
    configureEditor();
    configureEmptyState();
    setupViewIntegration();
  }

  /** Initializes the generic tab editor with specific toolbar buttons. */
  private void configureEditor() {
    tabEditorController = new TabEditorController();

    // Configure editor toolbar
    tabEditorController.configureEditor(view.getEditorToolbarButtons());

    // Configure execution: floating button + Ctrl+Enter shortcut
    tabEditorController.configureExecution(
        new ButtonConfig(IconButtonType.PLAY, view.getRunValidationLabel(), "Ctrl+Enter"),
        this::executeValidation);

    // Configure result view with split pane
    tabEditorController.configureResultView(view.getResultToolbarButtons());

    // Configure menu items for Validation context
    tabEditorController.configureMenuItems(
        new TabEditorController.MenuItem("New File", this::onNewFileButtonClick),
        new TabEditorController.MenuItem("Open File", this::onOpenFilesButtonClick));
  }

  /** Configures the empty state view to be displayed when no tabs are open. */
  private void configureEmptyState() {
    Node emptyState =
        view.createEmptyState(this::onNewFileButtonClick, this::onOpenFilesButtonClick);

    tabEditorController.configureEmptyState(emptyState);
  }

  /** Integrates the editor into the main view and sets up listeners. */
  private void setupViewIntegration() {
    // Embed the editor's view into the main ValidationView
    view.setMainContent(tabEditorController.getViewRoot());

    // Manage memory: Remove the model when a tab is closed
    tabEditorController.addTabListener(this::onTabsChanged);
  }

  // ==============================================================================================
  // Public Actions (User Interactions)
  // ==============================================================================================

  /**
   * Executes the SHACL validation process.
   *
   * <p>This method retrieves the shapes from the active tab, validates them against the data loaded
   * in the GraphManager (via {@link ValidationModel}), and updates the UI with the result.
   * Validation runs on a background thread to keep the UI responsive.
   */
  public void executeValidation() {
    Tab selectedTab = tabEditorController.getSelectedTab();
    if (selectedTab == null) return;

    ResultController resultController = tabEditorController.getCurrentResultController();
    if (resultController == null) return;

    // Clear previous results
    resultController.clearResults();

    // Retrieve or create the model for the current tab
    ValidationModel model = tabModels.computeIfAbsent(selectedTab, k -> new ValidationModel());

    // Pre-check: Ensure data is loaded
    if (!model.isDataLoaded()) {
      tabEditorController.hideResultPane();
      tabEditorController.showError(
          "No Data Loaded",
          "Validation requires an RDF graph to be loaded.\n"
              + "Please go to the 'Data' view and load an RDF file.");
      return;
    }

    // Retrieve shapes content from the editor
    final String shapesContent = tabEditorController.getEditorContent(selectedTab);
    if (shapesContent == null || shapesContent.trim().isEmpty()) {
      tabEditorController.hideResultPane();
      tabEditorController.showError(
          "Empty Shapes",
          "The shapes file is empty.\n"
              + "Please write or load SHACL shapes in the editor before validating.");
      return;
    }

    // UI: Indicate execution start
    tabEditorController.setExecutionState(true);

    // Execute validation asynchronously
    new Thread(() -> runValidationTask(model, shapesContent, resultController)).start();
  }

  // ==============================================================================================
  // Background Task & Callbacks
  // ==============================================================================================

  /**
   * Runs the validation task in a background thread.
   *
   * @param model The validation model.
   * @param shapesContent The SHACL shapes content.
   * @param resultController The controller to update with results.
   */
  private void runValidationTask(
      ValidationModel model, String shapesContent, ResultController resultController) {
    try {
      // Perform validation logic
      ValidationResult result = model.validate(shapesContent);

      // Update UI on JavaFX Application Thread
      Platform.runLater(() -> handleValidationResult(result, resultController));
    } catch (Throwable e) {
      e.printStackTrace();
      Platform.runLater(
          () -> {
            tabEditorController.setExecutionState(false);
            tabEditorController.hideResultPane();
            tabEditorController.showError(
                "Validation Error",
                "An unexpected error occurred during validation.\n"
                    + "Please check the logs for more details.",
                e.getMessage());
          });
    }
  }

  /**
   * Handles the display of validation results on the UI thread.
   *
   * @param result The result object from the validation process.
   * @param resultController The controller managing the result view.
   */
  private void handleValidationResult(ValidationResult result, ResultController resultController) {
    tabEditorController.setExecutionState(false);

    if (result.getErrorMessage() != null) {
      // Handle validation errors (e.g., syntax errors in shapes)
      tabEditorController.hideResultPane();
      tabEditorController.showError(
          "Invalid SHACL Syntax",
          "The SHACL shapes contain syntax errors.\nPlease correct the errors listed below:",
          result.getErrorMessage());
    } else {
      // Success: Display the report
      Tab selectedTab = tabEditorController.getSelectedTab();
      ValidationModel model = tabModels.get(selectedTab);
      
      tabEditorController.showResultPane();

      // Ensure the text tab is visible to show the report
      resultController.selectTextTab();

      // Display initial report in TURTLE format
      String initialReport = model.formatLastReport("TURTLE");
      if (initialReport != null) {
        resultController.updateText(initialReport);
      }

      // Configure callback for format changes
      resultController.setOnFormatChanged(format -> {
        String formattedReport = model.formatLastReport(format);
        if (formattedReport != null) {
          resultController.updateText(formattedReport);
        }
      });

      // Pass the report items for visualization
      resultController.displayReportItems(ShaclManager.getInstance().extractReportItems(result));
    }
  }

  // ==============================================================================================
  // Helper Methods
  // ==============================================================================================

  /**
   * Listener to clean up models when tabs are closed.
   *
   * @param change The change event from the TabPane.
   */
  private void onTabsChanged(ListChangeListener.Change<? extends Tab> change) {
    while (change.next()) {
      if (change.wasRemoved()) {
        for (Tab tab : change.getRemoved()) {
          tabModels.remove(tab);
        }
      }
    }
  }

  /** Creates a new untitled tab for SHACL shapes. */
  private void onNewFileButtonClick() {
    tabEditorController.addNewTab("untitled-shapes.ttl", "");
  }

  /** Opens a file chooser dialog to load SHACL shapes files. */
  private void onOpenFilesButtonClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Shapes File");
    fileChooser
        .getExtensionFilters()
        .addAll(new FileChooser.ExtensionFilter("RDF Files", "*.ttl", "*.rdf", "*.n3", "*.shacl"));

    File file =
        fileChooser.showOpenDialog(
            view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null);

    if (file != null) {
      tabEditorController.addNewTab(file);
    }
  }
}
