package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.model.TabEditorModel;
import fr.inria.corese.gui.view.FloatingButton;
import fr.inria.corese.gui.view.TabEditorView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

/**
 * Controller for the tabbed editor interface.
 *
 * <p>This controller manages multiple code editor tabs, providing functionality for:
 *
 * <ul>
 *   <li>Tab lifecycle management (create, open, close)
 *   <li>Split pane with result view integration
 *   <li>Floating execution buttons per tab
 *   <li>Keyboard shortcuts (Ctrl+S, Ctrl+Enter)
 *   <li>Empty state management
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * // 1. Create controller
 * TabEditorController controller = new TabEditorController();
 *
 * // 2. Configure editor toolbar buttons
 * controller.configureEditor(
 *     List.of(
 *         new ButtonConfig(IconButtonType.SAVE, "Save File", "Ctrl+S"),
 *         new ButtonConfig(IconButtonType.CLEAR, "Clear Content"),
 *         new ButtonConfig(IconButtonType.UNDO, "Undo", "Ctrl+Z"),
 *         new ButtonConfig(IconButtonType.REDO, "Redo", "Ctrl+Y")
 *     )
 * );
 *
 * // 3. Configure execution (Run button + action) - Optional
 * controller.configureExecution(
 *     new ButtonConfig(IconButtonType.PLAY, "Run Query", "Ctrl+Enter"),
 *     this::executeQuery
 * );
 *
 * // 4. Configure result view - Optional
 * controller.configureResultView(
 *     List.of(
 *         new ButtonConfig(IconButtonType.COPY, "Copy to Clipboard"),
 *         new ButtonConfig(IconButtonType.EXPORT, "Export Results")
 *     )
 * );
 *
 * // 5. Configure empty state - Optional
 * controller.configureEmptyState(emptyStateNode);
 *
 * // 6. Configure menu items - Optional
 * controller.configureMenuItems(
 *     new MenuItem("New File", () -> controller.addNewTab("Untitled", "")),
 *     new MenuItem("Open File", this::openFile)
 * );
 *
 * // 7. Get the view to integrate
 * Parent view = controller.getViewRoot();
 * }</pre>
 *
 * <p><b>Configuration details:</b>
 *
 * <ul>
 *   <li><b>Editor toolbar:</b> Buttons displayed in each tab's code editor (e.g., Save, Clear,
 *       Undo, Redo)
 *   <li><b>Execution:</b> Floating "Play" button at bottom-right + Ctrl+Enter shortcut
 *   <li><b>Result view:</b> Splits each tab vertically with editor on top and result view on bottom
 *   <li><b>Empty state:</b> Custom view shown when no tabs are open
 *   <li><b>Menu items:</b> Dropdown menu on the "+" button to add new tabs
 * </ul>
 */
public class TabEditorController {

  // ===============================================================================
  // Constants
  // ===============================================================================

  private static final Insets EXECUTION_BUTTON_MARGIN = new Insets(0, 60, 40, 0);
  private static final String DIALOG_TITLE_UNSAVED_CHANGES = "Unsaved Changes";
  private static final String DIALOG_BUTTON_SAVE = "Save";
  private static final String DIALOG_BUTTON_DONT_SAVE = "Don't Save";
  private static final String DIALOG_BUTTON_CANCEL = "Cancel";
  private static final String ERROR_FILE_READ = "Could not read file: ";
  private static final String DEFAULT_TAB_TITLE = "Untitled";

  // ===============================================================================
  // Fields
  // ===============================================================================

  private final TabEditorView view;
  private final TabEditorModel model;
  private final Map<Tab, FloatingButton> tabExecutionButtons;

  private List<ButtonConfig> editorToolbarButtons;
  private List<ButtonConfig> resultToolbarButtons;
  private java.util.function.Consumer<ResultController> resultConfigurer;

  private Runnable onExecutionRequest;
  private ButtonConfig executionButtonConfig;
  private Node emptyStateNode;
  private Function<Tab, ResultController> resultControllerFactory;

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /**
   * Constructs a new TabEditorController.
   *
   * <p>After construction, use the {@code configure*()} methods to set up the controller:
   *
   * <ul>
   *   <li>{@link #configureEditor(List)} - Configure editor toolbar buttons
   *   <li>{@link #configureExecution(String, Runnable)} - Configure execution button and action
   *   <li>{@link #configureResultView(List)} - Configure result view toolbar buttons
   *   <li>{@link #configureEmptyState(Node)} - Configure empty state
   *   <li>{@link #configureMenuItems(MenuItem...)} - Configure menu items
   * </ul>
   */
  public TabEditorController() {
    this.view = new TabEditorView();
    this.model = new TabEditorModel();
    this.tabExecutionButtons = new HashMap<>();

    initializeTabPane();
    initializeKeyboardShortcuts();
  }

  // ===============================================================================
  // Initialization Methods
  // ===============================================================================

  /** Initializes the tab pane with listeners. */
  private void initializeTabPane() {
    view.addTabListener(
        (ListChangeListener<Tab>) c -> Platform.runLater(this::updateEmptyStateVisibility));

    view.setOnAddTabAction(e -> addNewTab(DEFAULT_TAB_TITLE, ""));

    // Configure default menu items (can be overridden with configureMenuItems)
    configureMenuItems(new MenuItem("New File", () -> addNewTab(DEFAULT_TAB_TITLE, "")));
  }

  /** Initializes keyboard shortcuts for the editor. */
  private void initializeKeyboardShortcuts() {
    view.getRoot().addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyboardShortcut);
  }

  /**
   * Handles keyboard shortcut events.
   *
   * @param event The keyboard event
   */
  private void handleKeyboardShortcut(KeyEvent event) {
    if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
      handleSaveShortcut();
      event.consume();
    }
  }

  /** Handles the Ctrl+S save shortcut. */
  private void handleSaveShortcut() {
    Tab selectedTab = view.getSelectedTab();
    if (selectedTab != null) {
      CodeEditorController activeController = model.getEditorControllerForTab(selectedTab);
      if (activeController != null) {
        activeController.saveFile();
      }
    }
  }

  // ===============================================================================
  // Configuration Methods
  // ===============================================================================

  /**
   * Configures the code editor toolbar buttons for each tab.
   *
   * <p>These buttons are displayed in the toolbar of each tab's code editor (e.g., Save, Clear,
   * Undo, Redo). They are distinct from the floating execution button and result view.
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * // Simple: just icons
   * controller.configureEditor(
   *     List.of(
   *         new ButtonConfig(IconButtonType.SAVE),
   *         new ButtonConfig(IconButtonType.CLEAR),
   *         new ButtonConfig(IconButtonType.UNDO),
   *         new ButtonConfig(IconButtonType.REDO)
   *     )
   * );
   *
   * // Advanced: with tooltips and shortcuts
   * controller.configureEditor(
   *     List.of(
   *         new ButtonConfig(IconButtonType.SAVE, "Save File", "Ctrl+S"),
   *         new ButtonConfig(IconButtonType.CLEAR, "Clear Content")
   *     )
   * );
   * }</pre>
   *
   * @param toolbarButtons The list of button configurations for the code editor toolbar
   */
  public void configureEditor(List<ButtonConfig> toolbarButtons) {
    this.editorToolbarButtons = toolbarButtons;
  }

  /**
   * Configures the execution system: adds a floating "Run" button and binds the Ctrl+Enter
   * shortcut.
   *
   * <p>This is a convenience method that combines:
   *
   * <ul>
   *   <li>Adding a floating "Play" button at the bottom-right of each tab
   *   <li>Binding the execution action to Ctrl+Enter keyboard shortcut
   *   <li>Auto-disabling the button when editor is empty or execution is in progress
   * </ul>
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * // With custom icon
   * controller.configureExecution(
   *     new ButtonConfig(IconButtonType.PLAY, "Run Query", "Ctrl+Enter"),
   *     this::executeQuery
   * );
   *
   * // Without icon (uses default Play icon)
   * controller.configureExecution(
   *     new ButtonConfig(null, "Run Query", "Ctrl+Enter"),
   *     this::executeQuery
   * );
   * }</pre>
   *
   * @param buttonConfig The button configuration (icon, tooltip, shortcut)
   * @param executionAction The action to execute when button is clicked or Ctrl+Enter is pressed
   */
  public void configureExecution(ButtonConfig buttonConfig, Runnable executionAction) {
    this.executionButtonConfig = buttonConfig;
    this.onExecutionRequest = executionAction;
  }

  /**
   * Configures the result view for each tab.
   *
   * <p>When configured, each tab will be split vertically with:
   *
   * <ul>
   *   <li>Code editor on top
   *   <li>Result view on bottom (animated slide up/down)
   * </ul>
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * controller.configureResultView(
   *     List.of(
   *         new ButtonConfig(IconButtonType.COPY, "Copy to Clipboard"),
   *         new ButtonConfig(IconButtonType.EXPORT, "Export Results")
   *     )
   * );
   * }</pre>
   *
   * @param toolbarButtons The list of button configurations for the result view toolbar
   */
  public void configureResultView(List<ButtonConfig> toolbarButtons) {
    configureResultView(toolbarButtons, null);
  }

  /**
   * Configures the result view for each tab with additional configuration.
   *
   * <p>This overload allows post-configuration of each ResultController after creation.
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * controller.configureResultView(
   *     List.of(
   *         new ButtonConfig(IconButtonType.COPY),
   *         new ButtonConfig(IconButtonType.EXPORT)
   *     ),
   *     resultController -> {
   *         // Custom configuration per ResultController
   *         resultController.getView().getTabPane().getTabs().remove(...);
   *     }
   * );
   * }</pre>
   *
   * @param toolbarButtons The list of button configurations for the result view toolbar
   * @param configurer Optional function to configure each ResultController after creation
   */
  public void configureResultView(
      List<ButtonConfig> toolbarButtons, 
      java.util.function.Consumer<ResultController> configurer) {
    this.resultToolbarButtons = toolbarButtons;
    this.resultConfigurer = configurer;
    // Create a factory that uses the configured buttons and configurer
    this.resultControllerFactory = tab -> {
      ResultController controller = new ResultController(resultToolbarButtons);
      if (resultConfigurer != null) {
        resultConfigurer.accept(controller);
      }
      return controller;
    };
  }

  /**
   * Configures the empty state view displayed when no tabs are open.
   *
   * <p>The empty state is automatically shown/hidden based on tab count.
   *
   * @param emptyStateNode The node to display when no tabs are open
   */
  public void configureEmptyState(Node emptyStateNode) {
    this.emptyStateNode = emptyStateNode;
    view.setEmptyStateView(emptyStateNode);
    updateEmptyStateVisibility();
  }

  /**
   * Configures the menu items for the "+" (add tab) button.
   *
   * <p>This replaces any existing menu items. Each menu item should specify its text and action.
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * controller.configureMenuItems(
   *     new TabEditorController.MenuItem("New File", () -> controller.addNewTab("Untitled", "")),
   *     new TabEditorController.MenuItem("Open File", this::openFile),
   *     new TabEditorController.MenuItem("Templates", this::showTemplates)
   * );
   * }</pre>
   *
   * @param items The menu items to display (text + action pairs)
   */
  public void configureMenuItems(MenuItem... items) {
    view.clearMenuItems();
    for (MenuItem item : items) {
      view.addMenuItem(item.text, e -> item.action.run());
    }
  }

  /** Simple data class for menu item configuration. */
  public static class MenuItem {
    private final String text;
    private final Runnable action;

    public MenuItem(String text, Runnable action) {
      this.text = text;
      this.action = action;
    }
  }

  // ===============================================================================
  // Tab Management
  // ===============================================================================

  /**
   * Creates and adds a new tab with the specified title and content.
   *
   * @param title The title for the new tab
   * @param content The initial content for the editor
   * @return The created Tab instance
   */
  public Tab addNewTab(String title, String content) {
    return addNewTabHelper(title, content, null);
  }

  /**
   * Opens a file in a new tab. If the file is already open, selects that tab instead.
   *
   * @param file The file to open
   * @return The created or selected Tab instance, or null if the file could not be read
   */
  public Tab addNewTab(File file) {
    // Check if file is already open
    String absolutePath = file.getAbsolutePath();
    for (Tab tab : view.getTabs()) {
      CodeEditorController controller = model.getEditorControllerForTab(tab);
      if (controller != null && absolutePath.equals(controller.getModel().getFilePath())) {
        view.selectTab(tab);
        return tab;
      }
    }

    try {
      String content = Files.readString(file.toPath());
      return addNewTabHelper(file.getName(), content, file.getPath());
    } catch (IOException e) {
      showError(ERROR_FILE_READ + e.getMessage());
      return null;
    }
  }

  /**
   * Handles closing a tab with unsaved changes confirmation.
   *
   * @param tab The tab to close
   * @return true if the tab was closed, false if the user cancelled
   */
  public boolean handleCloseFile(Tab tab) {
    if (tab == null) {
      return false;
    }

    CodeEditorController controller = model.getEditorControllerForTab(tab);
    if (controller == null || !controller.getModel().isModified()) {
      closeTab(tab);
      return true;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(DIALOG_TITLE_UNSAVED_CHANGES);
    alert.setHeaderText("Save changes to " + controller.getModel().getDisplayName() + "?");

    ButtonType save = new ButtonType(DIALOG_BUTTON_SAVE);
    ButtonType dontSave = new ButtonType(DIALOG_BUTTON_DONT_SAVE);
    ButtonType cancel = new ButtonType(DIALOG_BUTTON_CANCEL);
    alert.getButtonTypes().setAll(save, dontSave, cancel);

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent()) {
      if (result.get() == save) {
        controller.saveFile();
        if (!controller.getModel().isModified()) {
          closeTab(tab);
          return true;
        }
      } else if (result.get() == dontSave) {
        closeTab(tab);
        return true;
      }
    }
    return false;
  }

  /**
   * Closes a tab without confirmation.
   *
   * @param tab The tab to close
   */
  private void closeTab(Tab tab) {
    view.getTabs().remove(tab);
    model.removeTab(tab);
    tabExecutionButtons.remove(tab);
  }

  /**
   * Internal helper method to create a new tab with full configuration.
   *
   * @param title The tab title
   * @param content The initial content
   * @param filePath The file path (null if not associated with a file)
   * @return The created Tab instance
   */
  private Tab addNewTabHelper(String title, String content, String filePath) {
    CodeEditorController codeEditorController =
        new CodeEditorController(editorToolbarButtons, content);

    StackPane editorWrapper = new StackPane(codeEditorController.getView());
    Node tabContent = editorWrapper;
    ResultController resultController = null;

    // Create split pane if result factory is configured
    if (resultControllerFactory != null) {
      resultController = resultControllerFactory.apply(null);
      if (resultController != null) {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().add(editorWrapper);
        tabContent = splitPane;
      }
    }

    Tab tab = view.createEditorTab(title, tabContent);
    model.addTabEditorController(tab, codeEditorController);

    if (resultController != null) {
      model.addTabResultController(tab, resultController);
    }

    // Add execution button if configured
    if (executionButtonConfig != null) {
      FloatingButton runButton = createExecutionButton(tab);
      tabExecutionButtons.put(tab, runButton);
      StackPane.setAlignment(runButton, Pos.BOTTOM_RIGHT);
      StackPane.setMargin(runButton, EXECUTION_BUTTON_MARGIN);
      editorWrapper.getChildren().add(runButton);
    }

    // Bind tab properties
    tab.textProperty().bind(codeEditorController.getModel().displayNameProperty());
    codeEditorController
        .getModel()
        .modifiedProperty()
        .addListener((obs, oldVal, newVal) -> view.updateTabIcon(tab, newVal));
    view.updateTabIcon(tab, codeEditorController.getModel().isModified());

    // Set up close handler
    tab.setOnCloseRequest(
        event -> {
          event.consume();
          handleCloseFile(tab);
        });

    // Bind execution shortcut (Ctrl+Enter)
    codeEditorController
        .getView()
        .setOnKeyPressed(
            event -> {
              if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)
                  && onExecutionRequest != null) {
                onExecutionRequest.run();
                event.consume();
              }
            });

    view.addNewEditorTab(tab);

    if (filePath != null) {
      codeEditorController.getModel().setFilePath(filePath);
      codeEditorController.getModel().markAsSaved();
    }

    return tab;
  }

  /**
   * Creates an execution button for a specific tab.
   *
   * @param tab The tab to create the button for
   * @return The created FloatingButton instance
   */
  private FloatingButton createExecutionButton(Tab tab) {
    FloatingButton runButton = new FloatingButton(executionButtonConfig);

    runButton.setOnAction(
        e -> {
          if (onExecutionRequest != null) {
            onExecutionRequest.run();
          }
        });

    CodeEditorController controller = model.getEditorControllerForTab(tab);
    if (controller != null) {
      BooleanBinding isEmpty =
          Bindings.createBooleanBinding(
              () -> {
                String c = controller.getModel().getContent();
                return c == null || c.trim().isEmpty();
              },
              controller.getModel().contentProperty());
      runButton.disableProperty().bind(isEmpty.or(runButton.loadingProperty()));
    }

    return runButton;
  }

  // ===============================================================================
  // Result Pane Management
  // ===============================================================================

  /**
   * Shows the result pane for the current tab with an animation.
   *
   * <p>Delegates to the view for the animation logic.
   */
  public void showResultPane() {
    Tab selectedTab = view.getSelectedTab();
    if (selectedTab != null) {
      ResultController resultController = model.getResultControllerForTab(selectedTab);
      if (resultController != null) {
        view.showResultPane(resultController.getView().getRoot());
      }
    }
  }

  /**
   * Hides the result pane for the current tab with an animation.
   *
   * <p>Delegates to the view for the animation logic.
   */
  public void hideResultPane() {
    view.hideResultPane();
  }

  // ===============================================================================
  // Execution Control
  // ===============================================================================

  /**
   * Sets the execution state for the current tab.
   *
   * <p>This controls the loading state of the floating execution button.
   *
   * @param loading true if execution is in progress (shows spinner), false otherwise
   */
  public void setExecutionState(boolean loading) {
    Tab selectedTab = view.getSelectedTab();
    if (selectedTab != null) {
      FloatingButton button = tabExecutionButtons.get(selectedTab);
      if (button != null) {
        button.setLoading(loading);
      }
    }
  }

  // ===============================================================================
  // Empty State Management
  // ===============================================================================

  /** Updates the visibility of the empty state view based on the number of open tabs. */
  private void updateEmptyStateVisibility() {
    if (emptyStateNode == null) {
      return;
    }

    boolean noTabsOpen = view.getTabs().isEmpty();
    emptyStateNode.setVisible(noTabsOpen);
    emptyStateNode.setManaged(noTabsOpen);
    view.setTabsVisible(!noTabsOpen);
  }

  // ===============================================================================
  // Utility Methods
  // ===============================================================================

  /**
   * Shows an error dialog with the specified message.
   *
   * @param content The error message to display
   */
  private void showError(String content) {
    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, content).showAndWait());
  }

  // ===============================================================================
  // Public API - View Access
  // ===============================================================================

  /**
   * Returns the root node of the view for integration into parent layouts.
   *
   * <p>This is the only view access point, respecting MVC encapsulation.
   *
   * @return The root Parent node
   */
  public Parent getViewRoot() {
    return view.getRoot();
  }

  // ===============================================================================
  // Public API - Tab Access
  // ===============================================================================

  /**
   * Returns the currently selected tab.
   *
   * @return The selected Tab, or null if none is selected
   */
  public Tab getSelectedTab() {
    return view.getSelectedTab();
  }

  /**
   * Returns the list of all tabs.
   *
   * @return The observable list of tabs
   */
  public javafx.collections.ObservableList<Tab> getTabs() {
    return view.getTabs();
  }

  /**
   * Selects the specified tab.
   *
   * @param tab The tab to select
   */
  public void selectTab(Tab tab) {
    view.selectTab(tab);
  }

  // ===============================================================================
  // Public API - Controller Access
  // ===============================================================================

  /**
   * Returns the ResultController for the currently selected tab.
   *
   * @return The ResultController, or null if none exists
   */
  public ResultController getCurrentResultController() {
    return model.getResultControllerForTab(view.getSelectedTab());
  }

  /**
   * Returns the CodeEditorController for the specified tab.
   *
   * @param tab The tab to get the controller for
   * @return The CodeEditorController, or null if not found
   */
  public CodeEditorController getControllerForTab(Tab tab) {
    return model.getEditorControllerForTab(tab);
  }

  /**
   * Retrieves the text content of the editor for a specific tab.
   *
   * @param tab The tab to get content from
   * @return The text content, or null if the tab is not valid
   */
  public String getEditorContent(Tab tab) {
    CodeEditorController controller = model.getEditorControllerForTab(tab);
    return controller != null ? controller.getView().getText() : null;
  }

  /**
   * Retrieves the file path associated with a specific tab.
   *
   * @param tab The tab to get the file path for
   * @return The file path, or null if the tab has no associated file
   */
  public String getFilePathForTab(Tab tab) {
    CodeEditorController controller = model.getEditorControllerForTab(tab);
    return controller != null ? controller.getModel().getFilePath() : null;
  }

  // ===============================================================================
  // Public API - Event Listeners
  // ===============================================================================

  /**
   * Adds a listener to the list of tabs.
   *
   * @param listener The listener to add
   */
  public void addTabListener(ListChangeListener<Tab> listener) {
    view.addTabListener(listener);
  }

  /**
   * Adds a listener to be notified when the selected tab changes.
   *
   * @param listener The listener to add
   */
  public void addSelectionListener(javafx.beans.value.ChangeListener<Tab> listener) {
    view.addSelectionListener(listener);
  }
}
