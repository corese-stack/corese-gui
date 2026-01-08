package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.enums.icon.IconButtonType;
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
import javafx.stage.FileChooser;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

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
 *   <li>File operations (open, save)
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * TabEditorController controller = new TabEditorController(buttons);
 * controller.setResultControllerFactory(tab -> new ResultController());
 * controller.setOnExecutionRequest(() -> executeQuery());
 * controller.configureMenuItems();
 * controller.addTemplatesMenuItem(); // Optional, for Query context
 * }</pre>
 */
public class TabEditorController {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  private static final Insets EXECUTION_BUTTON_MARGIN = new Insets(0, 60, 40, 0);

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  private final TabEditorView view;
  private final TabEditorModel model;
  private final List<IconButtonType> buttons;
  private final Map<Tab, FloatingButton> tabExecutionButtons;

  private Runnable onExecutionRequest;
  private Node emptyStateNode;
  private Function<Tab, ResultController> resultControllerFactory;
  private String executionButtonTooltip;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new TabEditorController.
   *
   * @param buttons The list of icon buttons to display in the code editor toolbar
   */
  public TabEditorController(List<IconButtonType> buttons) {
    this.view = new TabEditorView();
    this.model = new TabEditorModel();
    this.buttons = buttons;
    this.tabExecutionButtons = new HashMap<>();

    initializeTabPane();
    initializeKeyboardShortcuts();
  }

  // ==============================================================================================
  // Initialization Methods
  // ==============================================================================================

  /**
   * Initializes the tab pane with listeners and default menu configuration.
   */
  private void initializeTabPane() {
    view.addTabListener(
        (ListChangeListener<Tab>) c -> Platform.runLater(this::updateEmptyStateVisibility));

    view.setOnAddTabAction(e -> addNewTab("Untitled", ""));
    configureMenuItems();
  }

  /**
   * Initializes keyboard shortcuts for the editor.
   */
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

  /**
   * Handles the Ctrl+S save shortcut.
   */
  private void handleSaveShortcut() {
    Tab selectedTab = view.getSelectedTab();
    if (selectedTab != null) {
      CodeEditorController activeController = model.getEditorControllerForTab(selectedTab);
      if (activeController != null) {
        activeController.saveFile();
      }
    }
  }

  // ==============================================================================================
  // Configuration Methods
  // ==============================================================================================

  /**
   * Configures the menu items for the add tab button.
   *
   * <p>This method should be called by subclasses or external configurators to set up
   * context-specific menu items (e.g., Query editor has "Templates", Validate editor doesn't).
   */
  public void configureMenuItems() {
    view.addMenuItem("New File", e -> addNewTab("Untitled", ""));
    view.addMenuItem("Open File", e -> openFile());
  }

  /**
   * Adds a "Templates" menu item to the add tab button.
   *
   * <p>This can be called by contexts that support templates (e.g., Query editor).
   */
  public void addTemplatesMenuItem() {
    view.addMenuItem("Templates", e -> openTemplates());
  }

  /**
   * Clears all menu items from the add tab button.
   *
   * <p>Useful for reconfiguring menus dynamically based on context.
   */
  public void clearMenuItems() {
    view.clearMenuItems();
  }

  /**
   * Adds a custom menu item to the add tab button.
   *
   * @param text The menu item text
   * @param action The action to execute when clicked
   */
  public void addMenuItem(String text, Runnable action) {
    view.addMenuItem(text, e -> action.run());
  }

  /**
   * Sets the action to be executed when the user triggers the execution command (e.g., pressing
   * Ctrl+Enter in the editor).
   *
   * @param action The action to execute
   */
  public void setOnExecutionRequest(Runnable action) {
    this.onExecutionRequest = action;
  }

  /**
   * Sets the factory to create a ResultController for each new tab. If set, each tab will contain
   * a SplitPane with the editor and the result view.
   *
   * @param factory The factory function that creates a ResultController for a given tab
   */
  public void setResultControllerFactory(Function<Tab, ResultController> factory) {
    this.resultControllerFactory = factory;
  }

  /**
   * Sets the empty state view to display when no tabs are open. Automatically manages the
   * visibility of the tab pane and the empty state.
   *
   * @param emptyStateNode The node to display in empty state
   */
  public void setEmptyState(Node emptyStateNode) {
    this.emptyStateNode = emptyStateNode;
    view.setEmptyStateView(emptyStateNode);
    updateEmptyStateVisibility();
  }

  /**
   * Adds a standard "Run/Execute" floating button to the editor. The button is automatically bound
   * to the execution request action and enabled/disabled based on the tab selection.
   *
   * @param tooltipText The text to display in the tooltip
   */
  public void addExecutionButton(String tooltipText) {
    this.executionButtonTooltip = tooltipText;
  }

  /**
   * Adds a floating action node (e.g., a button) to the editor view.
   *
   * @param node The node to add
   * @param position The position alignment within the container
   * @param margin The margin around the node
   */
  public void addFloatingNode(Node node, Pos position, Insets margin) {
    view.addFloatingNode(node, position, margin);
  }

  // ==============================================================================================
  // Tab Management
  // ==============================================================================================

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
    for (Tab tab : view.getTabs()) {
      CodeEditorController controller = model.getEditorControllerForTab(tab);
      if (controller != null
          && file.getAbsolutePath().equals(getFilePathForTab(tab))) {
        view.selectTab(tab);
        return tab;
      }
    }

    try {
      String content = Files.readString(file.toPath());
      return addNewTabHelper(file.getName(), content, file.getPath());
    } catch (IOException e) {
      showError("Could not read file: " + e.getMessage());
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
    alert.setTitle("Unsaved Changes");
    alert.setHeaderText("Save changes to " + controller.getModel().getDisplayName() + "?");

    ButtonType save = new ButtonType("Save");
    ButtonType dontSave = new ButtonType("Don't Save");
    ButtonType cancel = new ButtonType("Cancel");
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
    CodeEditorController codeEditorController = new CodeEditorController(buttons, content);

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
    if (executionButtonTooltip != null) {
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
    tab.setOnCloseRequest(event -> {
      event.consume();
      handleCloseFile(tab);
    });

    // Bind execution shortcut (Ctrl+Enter)
    codeEditorController.getView().setOnKeyPressed(event -> {
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
    FloatingButton runButton = new FloatingButton(MaterialDesignP.PLAY, executionButtonTooltip);

    runButton.setOnAction(e -> {
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

  // ==============================================================================================
  // File Operations
  // ==============================================================================================

  /**
   * Opens a file chooser dialog to select and open a file.
   */
  private void openFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open File");
    File file =
        fileChooser.showOpenDialog(
            view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null);
    if (file != null) {
      addNewTab(file);
    }
  }

  /**
   * Shows a placeholder dialog for the templates feature.
   */
  private void openTemplates() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Templates");
    alert.setHeaderText("Templates not implemented yet");
    alert.setContentText("This feature will be available soon.");
    alert.showAndWait();
  }

  // ==============================================================================================
  // Result Pane Management
  // ==============================================================================================

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

  // ==============================================================================================
  // Execution State Management
  // ==============================================================================================

  /**
   * Sets the execution state for the current tab.
   *
   * @param loading true if execution is in progress, false otherwise
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

  // ==============================================================================================
  // Empty State Management
  // ==============================================================================================

  /**
   * Updates the visibility of the empty state view based on the number of open tabs.
   */
  private void updateEmptyStateVisibility() {
    if (emptyStateNode == null) {
      return;
    }

    boolean noTabsOpen = view.getTabs().isEmpty();
    emptyStateNode.setVisible(noTabsOpen);
    emptyStateNode.setManaged(noTabsOpen);
    view.setTabsVisible(!noTabsOpen);
  }

  // ==============================================================================================
  // Utility Methods
  // ==============================================================================================

  /**
   * Shows an error dialog with the specified message.
   *
   * @param content The error message to display
   */
  private void showError(String content) {
    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, content).showAndWait());
  }

  // ==============================================================================================
  // Public Getters
  // ==============================================================================================

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
   * <p>Note: This returns the observable list for monitoring, but modifications should go through
   * controller methods.
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

  /**
   * Returns the ResultController for the currently selected tab.
   *
   * @return The ResultController, or null if none exists
   */
  public ResultController getCurrentResultController() {
    Tab selectedTab = view.getSelectedTab();
    return model.getResultControllerForTab(selectedTab);
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
    if (controller != null) {
      return controller.getView().getText();
    }
    return null;
  }

  /**
   * Retrieves the file path associated with a specific tab.
   *
   * @param tab The tab to get the file path for
   * @return The file path, or null if the tab has no associated file
   */
  public String getFilePathForTab(Tab tab) {
    CodeEditorController controller = model.getEditorControllerForTab(tab);
    if (controller != null) {
      return controller.getModel().getFilePath();
    }
    return null;
  }

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
