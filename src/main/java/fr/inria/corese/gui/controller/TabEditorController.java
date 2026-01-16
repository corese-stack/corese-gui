package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.core.DialogHelper;
import fr.inria.corese.gui.core.ResultViewConfig;
import fr.inria.corese.gui.core.TabContext;
import fr.inria.corese.gui.manager.FileLoaderService;
import fr.inria.corese.gui.view.FloatingButton;
import fr.inria.corese.gui.view.TabEditorView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
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
 *         new ButtonConfig(IconButtonType.SAVE, "Save File"),
 *         new ButtonConfig(IconButtonType.CLEAR, "Clear Content"),
 *         new ButtonConfig(IconButtonType.UNDO, "Undo"),
 *         new ButtonConfig(IconButtonType.REDO, "Redo")
 *     )
 * );
 *
 * // 3. Configure execution (Run button + action) - Optional
 * controller.configureExecution(
 *     new ButtonConfig(IconButtonType.PLAY, "Run Query"),
 *     this::executeQuery
 * );
 *
 * // 4. Configure result view - Optional
 * controller.configureResultView(
 *     List.of(
 *         new ButtonConfig(IconButtonType.COPY, "Copy to Clipboard"),
 *         new ButtonConfig(IconButtonType.EXPORT, "Export Results")
 *     ),
 *     ResultViewConfig.builder()
 *         .withTextTab()
 *         .withTableTab()
 *         .build()
 * );
 *
 * // 5. Configure empty state - Optional
 * controller.configureEmptyState(emptyStateNode);
 *
 * // 6. Configure menu items - Optional
 * controller.configureMenuItems(
 *     List.of(
 *         new TabEditorController.MenuItem("New File", this::createNewFile),
 *         new TabEditorController.MenuItem("Open File", this::openFile)
 *     )
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
 *   <li><b>Execution:</b> Floating "Play" button at bottom-right
 *   <li><b>Result view:</b> Splits each tab vertically with editor on top and result view on bottom
 *   <li><b>Empty state:</b> Custom view shown when no tabs are open
 *   <li><b>Menu items:</b> Dropdown menu on the "+" button to add new tabs
 * </ul>
 */
public class TabEditorController {

  // ===============================================================================
  // Constants
  // ===============================================================================

  private static final String DEFAULT_TAB_TITLE = "untitled";

  // ===============================================================================
  // Fields - MVC Components
  // ===============================================================================

  /** The view component (MVC) - handles UI presentation and user interaction. */
  private final TabEditorView view;

  // ===============================================================================
  // Fields - Configuration (set via configure* methods)
  // ===============================================================================

  /** Configuration for editor toolbar buttons (Save, Clear, Undo, Redo, etc.). */
  private List<ButtonConfig> editorToolbarButtons;

  /** Configuration for the floating execution button (Run/Play button). */
  private ButtonConfig executionButtonConfig;

  /** Action to execute when the Run button is clicked. */
  private Runnable onExecutionRequest;

  /** Factory for creating ResultController instances with custom configuration. */
  private Supplier<ResultController> resultControllerFactory;

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
   *   <li>{@link #configureExecution(ButtonConfig, Runnable)} - Configure execution button and
   *       action
   *   <li>{@link #configureResultView(List, ResultViewConfig)} - Configure result view tabs
   *   <li>{@link #configureEmptyState(Node)} - Configure empty state
   *   <li>{@link #configureMenuItems(List)} - Configure menu items
   * </ul>
   */
  public TabEditorController() {
    this.view = new TabEditorView();
    initializeTabPane();
  }

  // ===============================================================================
  // Initialization Methods
  // ===============================================================================

  /** Initializes the tab pane with listeners. */
  private void initializeTabPane() {

    // Update empty state visibility on tab changes (add/remove)
    view.subscribeToTabChanges(
        (ListChangeListener<Tab>) c -> Platform.runLater(this::updateEmptyStateVisibility));

    // Link add + button to addNewTab action
    view.setOnAddTabAction(e -> addNewTab());

    // Configure default menu items (can be overridden with configureMenuItems)
    configureMenuItems(List.of(new MenuItem("New File", this::addNewTab)));
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
   * controller.configureEditor(
   *     List.of(
   *         new ButtonConfig(IconButtonType.SAVE, "Save File"),
   *         new ButtonConfig(IconButtonType.CLEAR, "Clear Content"),
   *         new ButtonConfig(IconButtonType.UNDO, "Undo"),
   *         new ButtonConfig(IconButtonType.REDO, "Redo")
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
   * Configures the execution system: adds a floating "Run" button.
   *
   * <p>This is a convenience method that combines:
   *
   * <ul>
   *   <li>Adding a floating "Play" button at the bottom-right of each tab
   *   <li>Auto-disabling the button when editor is empty or execution is in progress
   * </ul>
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * // With custom icon
   * controller.configureExecution(
   *     new ButtonConfig(IconButtonType.PLAY, "Run Query"),
   *     this::executeQuery
   * );
   *
   * // Without icon (uses default Play icon)
   * controller.configureExecution(
   *     new ButtonConfig(null, "Run Query"),
   *     this::executeQuery
   * );
   * }</pre>
   *
   * @param buttonConfig The button configuration (icon, tooltip)
   * @param executionAction The action to execute when button is clicked
   */
  public void configureExecution(ButtonConfig buttonConfig, Runnable executionAction) {
    this.executionButtonConfig = buttonConfig;
    this.onExecutionRequest = executionAction;
  }

  /**
   * Configures the result view for each tab with specific tabs enabled.
   *
   * <p>When configured, each tab will be split vertically with:
   *
   * <ul>
   *   <li>Code editor on top
   *   <li>Result view on bottom (animated slide up/down)
   * </ul>
   *
   * <p>Use {@link ResultViewConfig} to declaratively specify which result tabs should be displayed:
   *
   * <p><b>Usage:</b>
   *
   * <pre>{@code
   * // Query context: Text + Table + Graph
   * controller.configureResultView(
   *     List.of(
   *         new ButtonConfig(IconButtonType.COPY, "Copy to Clipboard"),
   *         new ButtonConfig(IconButtonType.EXPORT, "Export Results")
   *     ),
   *     ResultViewConfig.builder()
   *         .withTextTab()
   *         .withTableTab()
   *         .withGraphTab()
   *         .build()
   * );
   *
   * // Validation context: Text + Visual
   * controller.configureResultView(
   *     List.of(...),
   *     ResultViewConfig.builder()
   *         .withTextTab()
   *         .withVisualTab()
   *         .build()
   * );
   * }</pre>
   *
   * @param toolbarButtons The list of button configurations for the result view toolbar
   * @param config Configuration specifying which tabs to display (Text, Visual, Table, Graph)
   */
  public void configureResultView(List<ButtonConfig> toolbarButtons, ResultViewConfig config) {
    this.resultControllerFactory = () -> new ResultController(toolbarButtons, config);
  }

  /**
   * Configures the empty state view displayed when no tabs are open.
   *
   * <p>The empty state is automatically shown/hidden based on tab count.
   *
   * @param emptyStateNode The node to display when no tabs are open
   */
  public void configureEmptyState(Node emptyStateNode) {
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
   *     List.of(
   *         new TabEditorController.MenuItem("New File", this::createNewFile),
   *         new TabEditorController.MenuItem("Open File", this::openFile),
   *         new TabEditorController.MenuItem("Templates", this::showTemplates)
   *     )
   * );
   * }</pre>
   *
   * @param items The list of menu items to display
   */
  public void configureMenuItems(List<MenuItem> items) {
    view.clearMenuItems();
    for (MenuItem item : items) {
      view.addMenuItem(item.text, e -> item.action.run());
    }
  }

  /**
   * Configuration for a menu item in the "+" button dropdown.
   *
   * @param text The display text for the menu item
   * @param action The action to execute when the menu item is clicked
   */
  public record MenuItem(String text, Runnable action) {}

  // ===============================================================================
  // Tab Management
  // ===============================================================================

  /**
   * Creates and adds a new empty tab with default title.
   *
   * <p>This is a convenience method for creating an empty tab. For tabs with initial content (e.g.,
   * from templates), use {@link #addNewTab(String, String)}.
   *
   * @return The created Tab instance
   */
  public Tab addNewTab() {
    return addNewTab(DEFAULT_TAB_TITLE, "");
  }

  /**
   * Creates and adds a new tab with the specified title and content.
   *
   * <p>Use this method when you need to create a tab with specific title or initial content (e.g.,
   * from templates or snippets).
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
   * <p>The file is read asynchronously in a background thread to avoid blocking the UI.
   * A placeholder tab is created immediately with a loading message.
   *
   * @param file The file to open
   * @return The created or selected Tab instance, or null if the file is already being loaded
   */
  public Tab addNewTab(File file) {
    // Check if file is already open
    Tab existingTab = findTabByFile(file);
    if (existingTab != null) {
      view.selectTab(existingTab);
      return existingTab;
    }

    // Create a placeholder tab immediately with loading message
    Tab placeholderTab = addNewTabHelper(file.getName(), "Loading...", file.getPath());
    
    // Disable the tab's editor during loading
    TabContext context = TabContext.get(placeholderTab);
    if (context != null) {
      context.getEditorController().getView().getRoot().setDisable(true);
    }

    // Read file content in background thread using FileLoaderService
    Task<String> fileReadTask = FileLoaderService.loadFileAsync(file);

    // Handle successful file read
    fileReadTask.setOnSucceeded(event -> {
      String content = fileReadTask.getValue();
      TabContext ctx = TabContext.get(placeholderTab);
      if (ctx != null) {
        CodeEditorController editor = ctx.getEditorController();
        editor.getView().getRoot().setDisable(false);
        editor.getModel().setContent(content);
        editor.getModel().setModified(false);
      }
    });

    // Handle file read failure
    fileReadTask.setOnFailed(event -> {
      Throwable exception = fileReadTask.getException();
      String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
      view.showError("File Error", "Could not read file: " + errorMsg);
      // Close the placeholder tab on error
      Platform.runLater(() -> closeTab(placeholderTab));
    });

    // Start the background task
    Thread thread = new Thread(fileReadTask);
    thread.setDaemon(true);
    thread.start();

    return placeholderTab;
  }

  /**
   * Handles closing a tab with unsaved changes confirmation.
   *
   * <p>If the file has unsaved changes, shows a confirmation dialog using ModalPane. Otherwise,
   * closes the tab immediately.
   *
   * @param tab The tab to close
   */
  public void handleCloseFile(Tab tab) {
    if (tab == null) {
      return;
    }

    TabContext context = TabContext.get(tab);
    if (context == null || !context.getEditorController().getModel().isModified()) {
      closeTab(tab);
      return;
    }

    CodeEditorController controller = context.getEditorController();
    view.showUnsavedChangesDialog(
        controller.getModel().getDisplayName(),
        result -> {
          switch (result) {
            case DialogHelper.UnsavedChangesResult.SAVE:
              controller.saveFile();
              if (!controller.getModel().isModified()) {
                closeTab(tab);
              }
              break;
            case DialogHelper.UnsavedChangesResult.DONT_SAVE:
              closeTab(tab);
              break;
            case DialogHelper.UnsavedChangesResult.CANCEL:
              // Do nothing
              break;
          }
        });
  }

  /**
   * Closes a tab without confirmation.
   *
   * <p>This method performs a complete cleanup to prevent memory leaks:
   * <ul>
   *   <li>Unbinds all property bindings
   *   <li>Disposes controllers via TabContext
   *   <li>Clears userData reference
   *   <li>Removes tab from view
   * </ul>
   *
   * @param tab The tab to close
   */
  private void closeTab(Tab tab) {
    if (tab == null) {
      return;
    }

    // Get context before clearing
    TabContext context = TabContext.get(tab);
    
    // Unbind title property to prevent memory leak
    tab.textProperty().unbind();
    
    // Dispose all resources held by the context
    if (context != null) {
      context.dispose();
    }
    
    // Clear userData to break the reference
    tab.setUserData(null);
    
    // Remove from view
    view.getTabs().remove(tab);
  }

  /**
   * Finds a tab that has the given file open.
   *
   * <p>Uses normalized paths to properly detect duplicates even with symbolic links, relative
   * paths, or different path separators.
   *
   * @param file The file to search for
   * @return The tab with the file open, or null if not found
   */
  private Tab findTabByFile(File file) {
    Path normalizedPath = normalizePathSafely(file);
    String absolutePath = file.getAbsolutePath();

    for (Tab tab : view.getTabs()) {
      String tabFilePath = getTabFilePath(tab);
      if (tabFilePath != null && isMatchingFile(normalizedPath, absolutePath, tabFilePath)) {
        return tab;
      }
    }
    return null;
  }

  /**
   * Normalizes a file path, returning null if the file doesn't exist.
   *
   * @param file The file to normalize
   * @return The normalized path, or null if normalization fails
   */
  private Path normalizePathSafely(File file) {
    try {
      return file.toPath().toRealPath();
    } catch (IOException _) {
      return null;
    }
  }

  /**
   * Gets the file path associated with a tab.
   *
   * @param tab The tab to get the file path from
   * @return The file path, or null if not found
   */
  private String getTabFilePath(Tab tab) {
    TabContext context = TabContext.get(tab);
    return context != null ? context.getEditorController().getModel().getFilePath() : null;
  }

  /**
   * Checks if a tab's file path matches the search file.
   *
   * @param normalizedSearchPath The normalized path of the search file (may be null)
   * @param absoluteSearchPath The absolute path of the search file
   * @param tabFilePath The file path from the tab
   * @return true if the paths match, false otherwise
   */
  private boolean isMatchingFile(
      Path normalizedSearchPath, String absoluteSearchPath, String tabFilePath) {
    if (normalizedSearchPath != null) {
      return matchesNormalizedPath(normalizedSearchPath, tabFilePath);
    }
    return absoluteSearchPath.equals(tabFilePath);
  }

  /**
   * Checks if a tab's file path matches a normalized path.
   *
   * @param normalizedPath The normalized path to compare
   * @param tabFilePath The file path from the tab
   * @return true if the paths match, false otherwise
   */
  private boolean matchesNormalizedPath(Path normalizedPath, String tabFilePath) {
    try {
      Path tabPath = Path.of(tabFilePath).toRealPath();
      return normalizedPath.equals(tabPath);
    } catch (IOException _) {
      return false;
    }
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
    CodeEditorController editorController = new CodeEditorController(editorToolbarButtons, content);
    ResultController resultController = createResultControllerIfConfigured();
    
    StackPane editorWrapper = new StackPane(editorController.getViewRoot());
    Node tabContent = createTabContent(editorWrapper, resultController);
    
    Tab tab = view.createEditorTab(title, tabContent);
    registerControllers(tab, editorController, resultController);
    
    addExecutionButtonIfConfigured(tab, editorWrapper);
    setupTabProperties(tab, editorController);
    initializeTabFile(editorController, filePath);
    
    view.addNewEditorTab(tab);
    return tab;
  }

  /**
   * Creates a ResultController if the factory is configured.
   *
   * @return The ResultController instance, or null if not configured
   */
  private ResultController createResultControllerIfConfigured() {
    return resultControllerFactory != null ? resultControllerFactory.get() : null;
  }

  /**
   * Creates the content node for a tab, potentially wrapping it in a SplitPane.
   *
   * @param editorWrapper The editor wrapper StackPane
   * @param resultController The result controller (may be null)
   * @return The complete tab content node
   */
  private Node createTabContent(StackPane editorWrapper, ResultController resultController) {
    if (resultController == null) {
      return editorWrapper;
    }
    
    SplitPane splitPane = new SplitPane();
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.getItems().add(editorWrapper);
    return splitPane;
  }

  /**
   * Registers controllers by creating a TabContext and storing it in the tab's userData.
   * 
   * <p>This is the single source of truth for tab-controller associations.
   * All tab components are accessible via {@link TabContext#get(Tab)}.
   *
   * @param tab The tab to register controllers for
   * @param editorController The code editor controller
   * @param resultController The result controller (may be null)
   */
  private void registerControllers(
      Tab tab, CodeEditorController editorController, ResultController resultController) {
    // Store context in tab's userData
    TabContext context = new TabContext(editorController, resultController, null);
    tab.setUserData(context);
  }

  /**
   * Adds the execution button to the editor if configured.
   *
   * @param tab The tab to add the button for
   * @param editorWrapper The editor wrapper to add the button to
   */
  private void addExecutionButtonIfConfigured(Tab tab, StackPane editorWrapper) {
    if (executionButtonConfig == null) {
      return;
    }
    
    FloatingButton runButton = createExecutionButton(tab);
    
    // Update TabContext with the execution button
    TabContext oldContext = TabContext.get(tab);
    if (oldContext != null) {
      TabContext newContext = new TabContext(
          oldContext.getEditorController(),
          oldContext.getResultController(),
          runButton);
      tab.setUserData(newContext);
    }
    
    StackPane.setAlignment(runButton, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(runButton, TabEditorView.getExecutionButtonMargin());
    editorWrapper.getChildren().add(runButton);
  }

  /**
   * Sets up tab properties including title binding, icon updates, and close handler.
   *
   * <p>This method creates bindings and listeners that need to be cleaned up when the tab is closed
   * to prevent memory leaks.
   *
   * @param tab The tab to configure
   * @param editorController The code editor controller
   */
  private void setupTabProperties(Tab tab, CodeEditorController editorController) {
    // Bind tab title to editor display name
    tab.textProperty().bind(editorController.getModel().displayNameProperty());
    
    // Update tab icon when modified state changes
    // Note: This listener will be garbage collected with the tab since it only references
    // the tab (which is being closed) and the view (which is a singleton)
    editorController
        .getModel()
        .modifiedProperty()
        .addListener((obs, oldVal, newVal) -> view.updateTabIcon(tab, newVal));
    view.updateTabIcon(tab, editorController.getModel().isModified());
    
    // Handle close requests with confirmation
    tab.setOnCloseRequest(
        event -> {
          event.consume();
          handleCloseFile(tab);
        });
  }

  /**
   * Initializes the file association for a tab.
   *
   * @param editorController The code editor controller
   * @param filePath The file path (null if not associated with a file)
   */
  private void initializeTabFile(CodeEditorController editorController, String filePath) {
    if (filePath != null) {
      editorController.getModel().setFilePath(filePath);
      editorController.getModel().markAsSaved();
    }
  }

  /**
   * Creates an execution button for a specific tab.
   *
   * @param tab The tab to create the button for
   * @return The created FloatingButton instance
   */
  private FloatingButton createExecutionButton(Tab tab) {
    FloatingButton runButton = new FloatingButton(executionButtonConfig);

    // Set button action
    runButton.setOnAction(
        e -> {
          if (onExecutionRequest != null) {
            onExecutionRequest.run();
          }
        });

    // Bind button disabled state based on editor content and loading state
    TabContext context = TabContext.get(tab);
    if (context != null) {
      CodeEditorController controller = context.getEditorController();
      BooleanBinding isEmpty =
          Bindings.createBooleanBinding(
              () -> {
                // Check if editor content is empty
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
      TabContext context = TabContext.get(selectedTab);
      if (context != null && context.hasResultController()) {
        view.showResultPane(context.getResultController().getViewRoot());
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
      TabContext context = TabContext.get(selectedTab);
      if (context != null && context.hasExecutionButton()) {
        context.getExecutionButton().setLoading(loading);
      }
    }
  }

  // ===============================================================================
  // Empty State Management
  // ===============================================================================

  /** Updates the visibility of the empty state view based on the number of open tabs. */
  private void updateEmptyStateVisibility() {
    boolean noTabsOpen = view.getTabs().isEmpty();
    view.updateEmptyStateVisibility(noTabsOpen);
    view.setTabsVisible(!noTabsOpen);
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
    Tab selectedTab = view.getSelectedTab();
    TabContext context = TabContext.get(selectedTab);
    return context != null ? context.getResultController() : null;
  }

  /**
   * Returns the CodeEditorController for the specified tab.
   *
   * @param tab The tab to get the controller for
   * @return The CodeEditorController, or null if not found
   */
  public CodeEditorController getControllerForTab(Tab tab) {
    TabContext context = TabContext.get(tab);
    return context != null ? context.getEditorController() : null;
  }

  /**
   * Retrieves the text content of the editor for a specific tab.
   *
   * @param tab The tab to get content from
   * @return The text content, or null if the tab is not valid
   */
  public String getEditorContent(Tab tab) {
    TabContext context = TabContext.get(tab);
    if (context != null) {
      return context.getEditorController().getView().getText();
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
    TabContext context = TabContext.get(tab);
    if (context != null) {
      return context.getEditorController().getModel().getFilePath();
    }
    return null;
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
    view.subscribeToTabChanges(listener);
  }

  /**
   * Adds a listener to be notified when the selected tab changes.
   *
   * @param listener The listener to add
   */
  public void addSelectionListener(javafx.beans.value.ChangeListener<Tab> listener) {
    view.addSelectionListener(listener);
  }

  // ==============================================================================================
  // Public API - Error Dialogs
  // ==============================================================================================

  /**
   * Shows an error dialog.
   *
   * @param title The dialog title
   * @param message The error message
   */
  public void showError(String title, String message) {
    view.showError(title, message);
  }

  /**
   * Shows an error dialog with detailed information.
   *
   * @param title The dialog title
   * @param message The error message
   * @param details The detailed error message (e.g., stack trace)
   */
  public void showError(String title, String message, String details) {
    view.showError(title, message, details);
  }
}
