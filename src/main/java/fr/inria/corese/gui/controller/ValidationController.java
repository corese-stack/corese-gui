package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.enums.icon.IconButtonBarType;
import fr.inria.corese.gui.model.ValidationModel;
import fr.inria.corese.gui.view.EmptyStateView;
import fr.inria.corese.gui.view.ValidationView;
import java.io.File;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

/**
 * Controller for the Validation View.
 *
 * <p>This controller manages the interaction between the Validation View, the Validation Model, and
 * the user. It handles the setup of the editor, the execution of SHACL validation, and the display
 * of results.
 */
public class ValidationController {

  // Logger
  private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);

  // MVC Components
  private final ValidationView view;
  private final ValidationModel validationModel;

  // Sub-Controllers
  private TabEditorController tabEditorController;
  private ResultController resultController;

  // UI Elements
  private Node emptyStateView;
  private StackPane editorContainer;

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
    try {
      // Initialize Result Component
      resultController = new ResultController();
      view.setResultView(resultController.getViewRoot());

      // Listener for result format changes (e.g., Turtle, JSON-LD)
      resultController
          .getTextFormatChoiceBox()
          .getSelectionModel()
          .selectedItemProperty()
          .addListener(
              (obs, oldVal, newVal) -> {
                if (newVal != null) {
                  updateReportDisplay(newVal);
                }
              });

      // Initialize Editor Component
      initializeTabEditor();

      // Setup Empty State (when no tabs are open)
      setupEmptyState();

    } catch (Exception e) {
      logger.error("Error initializing ValidationController", e);
    }
  }

  /** Initializes the tab editor for SHACL shapes. */
  private void initializeTabEditor() {
    tabEditorController = new TabEditorController(IconButtonBarType.VALIDATION);
    tabEditorController.getView().setMaxWidth(Double.MAX_VALUE);
    tabEditorController.getView().setMaxHeight(Double.MAX_VALUE);

    // Wrap in StackPane for EmptyState and Floating Button
    editorContainer = new StackPane(tabEditorController.getView());
    view.setEditorView(editorContainer);

    setupFloatingValidateButton();
    setupTabListeners();

    // Configure TabEditor Menu Actions
    tabEditorController.getView().getOpenFileItem().setVisible(false);
  }

  /** Sets up listeners for tab changes to configure new tabs. */
  private void setupTabListeners() {
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
                          Platform.runLater(() -> configureTab(tab));
                        }
                      }
                    }
                  }
                });
  }

  /** Sets up the floating action button for triggering validation. */
  private void setupFloatingValidateButton() {
    Button validateButton = new Button();
    FontIcon playIcon = new FontIcon(MaterialDesignP.PLAY);
    playIcon.setIconSize(24);
    playIcon.setIconColor(javafx.scene.paint.Color.WHITE);
    validateButton.setGraphic(playIcon);

    validateButton.getStyleClass().add("floating-validate-button");

    validateButton.setTooltip(new Tooltip("Run Validation (Ctrl+Enter)"));
    validateButton.setOnAction(e -> executeValidation());

    // Bind visibility to the editor view visibility (hidden when empty state is shown)
    validateButton.visibleProperty().bind(tabEditorController.getView().visibleProperty());
    validateButton.managedProperty().bind(tabEditorController.getView().visibleProperty());

    // Enable/Disable based on tab selection
    tabEditorController
        .getView()
        .getTabPane()
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, oldTab, newTab) -> updateValidateButtonState(validateButton));

    StackPane.setAlignment(validateButton, Pos.BOTTOM_RIGHT);
    // Add margins to avoid overlap with scrollbars and toolbars
    StackPane.setMargin(validateButton, new Insets(0, 80, 50, 0));

    editorContainer.getChildren().add(validateButton);
  }

  /** Updates the state (enabled/disabled) of the validate button. */
  private void updateValidateButtonState(Button validateButton) {
    Tab selectedTab =
        tabEditorController.getView().getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab == null || selectedTab == tabEditorController.getView().getAddTab()) {
      validateButton.disableProperty().unbind();
      validateButton.setDisable(true);
      return;
    }

    // Always enable the button when a valid tab is selected
    validateButton.disableProperty().unbind();
    validateButton.setDisable(false);
  }

  /** Configures a newly added tab (shortcuts, etc.). */
  private void configureTab(Tab tab) {
    CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
    if (controller != null) {
      configureEditorShortcuts(controller);
    }
  }

  /** Configures keyboard shortcuts for the editor. */
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

  /** Sets up the empty state view (shown when no tabs are open). */
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

    this.emptyStateView =
        new EmptyStateView(
            MaterialDesignS.SHIELD_CHECK_OUTLINE,
            "No shapes files open.\nCreate a new shapes file or load an existing one.",
            newButton,
            loadButton);

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

  /** Updates the visibility of the empty state view based on the number of open tabs. */
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
