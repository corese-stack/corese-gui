package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.core.DialogHelper;
import fr.inria.corese.gui.core.TabContext;
import fr.inria.corese.gui.core.TabEditorConfig;
import fr.inria.corese.gui.manager.FileLoaderService;
import fr.inria.corese.gui.view.FloatingButton;
import fr.inria.corese.gui.view.TabEditorView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
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
 * <p>This controller manages multiple code editor tabs using a clean MVC architecture with:
 *
 * <ul>
 *   <li><b>Single source of truth:</b> TabContext stored in Tab.userData
 *   <li><b>Builder pattern:</b> Configuration via TabEditorConfig
 *   <li><b>Clean separation:</b> UI assembly vs business logic
 *   <li><b>Async operations:</b> Non-blocking file I/O
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * // 1. Build configuration
 * TabEditorConfig config = TabEditorConfig.builder()
 *     .withEditorButtons(List.of(
 *         new ButtonConfig(IconButtonType.SAVE, "Save"),
 *         new ButtonConfig(IconButtonType.UNDO, "Undo")
 *     ))
 *     .withExecution(
 *         new ButtonConfig(IconButtonType.PLAY, "Run"),
 *         this::executeQuery
 *     )
 *     .withResultView(
 *         List.of(new ButtonConfig(IconButtonType.COPY, "Copy")),
 *         ResultViewConfig.builder().withTextTab().build()
 *     )
 *     .build();
 *
 * // 2. Create controller (all configuration done!)
 * TabEditorController controller = new TabEditorController(config);
 *
 * // 3. Use it
 * controller.openFile(myFile);
 * Parent view = controller.getViewRoot();
 * }</pre>
 */
public class TabEditorController {

  // ===============================================================================
  // Constants
  // ===============================================================================

  private static final String DEFAULT_TAB_TITLE = "untitled";

  // ===============================================================================
  // Fields
  // ===============================================================================

  /** The view component - handles UI presentation */
  private final TabEditorView view;

  /** Immutable configuration - all settings in one place */
  private final TabEditorConfig config;

  /** Factory for creating result controllers (lazy from config) */
  private final Supplier<ResultController> resultControllerFactory;

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /**
   * Constructs a TabEditorController with the given configuration.
   *
   * <p>All configuration must be provided upfront via TabEditorConfig.builder(). This ensures the
   * controller is always in a valid state and prevents incomplete initialization.
   *
   * @param config The configuration for this controller (must not be null)
   * @throws NullPointerException if config is null
   */
  public TabEditorController(TabEditorConfig config) {
    if (config == null) {
      throw new NullPointerException("TabEditorConfig cannot be null");
    }

    this.config = config;
    this.view = new TabEditorView();
    this.resultControllerFactory = config.createResultControllerFactory();

    initialize();
  }

  // ===============================================================================
  // Initialization
  // ===============================================================================

  /** Initializes the view with configuration and listeners */
  private void initialize() {
    // Setup empty state
    if (config.getEmptyStateView() != null) {
      view.setEmptyStateView(config.getEmptyStateView());
    }

    // Setup menu items
    setupMenuItems();

    // Setup listeners
    view.subscribeToTabChanges(
        (ListChangeListener<Tab>) c -> Platform.runLater(this::updateEmptyStateVisibility));
    view.setOnAddTabAction(e -> createNewTab());

    // Initial state
    updateEmptyStateVisibility();
  }

  /** Configures menu items from config */
  private void setupMenuItems() {
    view.clearMenuItems();
    if (config.getMenuItems().isEmpty()) {
      // Default: just "New File"
      view.addMenuItem("New File", e -> createNewTab());
    } else {
      for (TabEditorConfig.MenuItem item : config.getMenuItems()) {
        view.addMenuItem(item.text(), e -> item.action().run());
      }
    }
  }

  // ===============================================================================
  // Public API - Tab Creation
  // ===============================================================================

  /**
   * Creates a new empty tab with default title.
   *
   * @return The created Tab
   */
  public Tab createNewTab() {
    return createNewTab(DEFAULT_TAB_TITLE, "");
  }

  /**
   * Creates a new tab with specific title and content.
   *
   * @param title The tab title
   * @param content The initial content
   * @return The created Tab
   */
  public Tab createNewTab(String title, String content) {
    return createTabWithContext(title, content, null);
  }

  /**
   * Opens a file in a new tab asynchronously.
   *
   * <p>If the file is already open, selects that tab instead. The file is loaded in a background
   * thread to keep the UI responsive.
   *
   * @param file The file to open
   * @return The created or existing Tab
   */
  public Tab openFile(File file) {
    // Check if already open
    Tab existingTab = findTabByFile(file);
    if (existingTab != null) {
      view.selectTab(existingTab);
      return existingTab;
    }

    // Create placeholder tab immediately
    Tab tab = createTabWithContext(file.getName(), "Loading...", file.getPath());
    lockTabUI(tab);

    // Load file asynchronously
    startFileLoadingTask(tab, file);

    return tab;
  }

  /**
   * Requests to close a tab, showing confirmation if needed.
   *
   * <p>Follows JavaFX naming convention for user-requested actions. If the tab has unsaved changes,
   * shows a confirmation dialog.
   *
   * @param tab The tab to close
   */
  public void requestCloseTab(Tab tab) {
    if (tab == null) {
      return;
    }

    TabContext context = TabContext.get(tab);
    if (context == null || !context.getEditorController().getModel().isModified()) {
      closeTabImmediately(tab);
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
                closeTabImmediately(tab);
              }
              break;
            case DialogHelper.UnsavedChangesResult.DONT_SAVE:
              closeTabImmediately(tab);
              break;
            case DialogHelper.UnsavedChangesResult.CANCEL:
              // Do nothing
              break;
          }
        });
  }

  // ===============================================================================
  // Public API - Result Pane
  // ===============================================================================

  /** Shows the result pane for the current tab */
  public void showResultPane() {
    Tab selectedTab = view.getSelectedTab();
    if (selectedTab != null) {
      TabContext context = TabContext.get(selectedTab);
      if (context != null && context.hasResultController()) {
        view.showResultPane(context.getResultController().getViewRoot());
      }
    }
  }

  /** Hides the result pane for the current tab */
  public void hideResultPane() {
    view.hideResultPane();
  }

  // ===============================================================================
  // Public API - Execution Control
  // ===============================================================================

  /**
   * Sets the execution state (loading spinner on run button).
   *
   * @param loading true to show loading spinner
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
  // Public API - View Access
  // ===============================================================================

  /**
   * Returns the root view node for integration.
   *
   * @return The root Parent node
   */
  public Parent getViewRoot() {
    return view.getRoot();
  }

  // ===============================================================================
  // Public API - Tab Access
  // ===============================================================================

  public Tab getSelectedTab() {
    return view.getSelectedTab();
  }

  public javafx.collections.ObservableList<Tab> getTabs() {
    return view.getTabs();
  }

  public void selectTab(Tab tab) {
    view.selectTab(tab);
  }

  // ===============================================================================
  // Public API - Controller Access
  // ===============================================================================

  public ResultController getCurrentResultController() {
    Tab selectedTab = view.getSelectedTab();
    TabContext context = TabContext.get(selectedTab);
    return context != null ? context.getResultController() : null;
  }

  public CodeEditorController getEditorControllerForTab(Tab tab) {
    TabContext context = TabContext.get(tab);
    return context != null ? context.getEditorController() : null;
  }

  public String getEditorContent(Tab tab) {
    TabContext context = TabContext.get(tab);
    return context != null ? context.getEditorController().getView().getText() : null;
  }

  public String getFilePathForTab(Tab tab) {
    TabContext context = TabContext.get(tab);
    return context != null ? context.getEditorController().getModel().getFilePath() : null;
  }

  // ===============================================================================
  // Public API - Event Listeners
  // ===============================================================================

  public void addTabListener(ListChangeListener<Tab> listener) {
    view.subscribeToTabChanges(listener);
  }

  public void addSelectionListener(javafx.beans.value.ChangeListener<Tab> listener) {
    view.addSelectionListener(listener);
  }

  // ===============================================================================
  // Public API - Dialogs
  // ===============================================================================

  public void showError(String title, String message) {
    view.showError(title, message);
  }

  public void showError(String title, String message, String details) {
    view.showError(title, message, details);
  }

  // ===============================================================================
  // Internal - Tab Assembly (Core Logic)
  // ===============================================================================

  /**
   * Creates a tab with full context attached.
   *
   * <p>This is the core assembly method that creates all components and wires them together.
   *
   * @param title Tab title
   * @param content Initial content
   * @param filePath File path (null if not associated with a file)
   * @return The created Tab with attached TabContext
   */
  private Tab createTabWithContext(String title, String content, String filePath) {
    // 1. Create controllers
    CodeEditorController editorController =
        new CodeEditorController(config.getEditorButtons(), content);
    ResultController resultController = createResultControllerIfConfigured();

    // 2. Assemble UI
    StackPane editorWrapper = new StackPane(editorController.getViewRoot());
    Node tabContent = layoutTabContent(editorWrapper, resultController);

    // 3. Create tab
    Tab tab = view.createEditorTab(title, tabContent);

    // 4. Attach context (single source of truth)
    FloatingButton executionButton = setupExecutionButton(editorWrapper, editorController);
    attachContext(tab, editorController, resultController, executionButton);

    // 5. Final setup
    bindTabProperties(tab, editorController);
    if (filePath != null) {
      bindFileToEditor(editorController, filePath);
    }

    view.addNewEditorTab(tab);
    return tab;
  }

  /**
   * Attaches TabContext to tab's userData.
   *
   * <p>This is the single source of truth for all tab-related components.
   *
   * @param tab The tab
   * @param editorController The editor controller
   * @param resultController The result controller (nullable)
   * @param executionButton The execution button (nullable)
   */
  private void attachContext(
      Tab tab,
      CodeEditorController editorController,
      ResultController resultController,
      FloatingButton executionButton) {
    TabContext context = new TabContext(editorController, resultController, executionButton);
    tab.setUserData(context);
  }

  // ===============================================================================
  // Internal - UI Layout
  // ===============================================================================

  private ResultController createResultControllerIfConfigured() {
    return resultControllerFactory != null ? resultControllerFactory.get() : null;
  }

  private Node layoutTabContent(StackPane editorWrapper, ResultController resultController) {
    if (resultController == null) {
      return editorWrapper;
    }

    SplitPane splitPane = new SplitPane();
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.getItems().add(editorWrapper);
    return splitPane;
  }

  private FloatingButton setupExecutionButton(
      StackPane editorWrapper, CodeEditorController editorController) {
    if (!config.hasExecution()) {
      return null;
    }

    FloatingButton runButton = createExecutionButton(editorController);
    StackPane.setAlignment(runButton, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(runButton, TabEditorView.getExecutionButtonMargin());
    editorWrapper.getChildren().add(runButton);

    return runButton;
  }

  private FloatingButton createExecutionButton(CodeEditorController editorController) {
    FloatingButton runButton = new FloatingButton(config.getExecutionButton());

    // Set action
    runButton.setOnAction(
        e -> {
          if (config.getExecutionAction() != null) {
            config.getExecutionAction().run();
          }
        });

    // Bind disabled state
    BooleanBinding isEmpty =
        Bindings.createBooleanBinding(
            () -> {
              String c = editorController.getModel().getContent();
              return c == null || c.trim().isEmpty();
            },
            editorController.getModel().contentProperty());
    runButton.disableProperty().bind(isEmpty.or(runButton.loadingProperty()));

    return runButton;
  }

  private void bindTabProperties(Tab tab, CodeEditorController editorController) {
    // Bind title
    tab.textProperty().bind(editorController.getModel().displayNameProperty());

    // Update icon on modified state change
    editorController
        .getModel()
        .modifiedProperty()
        .addListener((obs, oldVal, newVal) -> view.updateTabIcon(tab, newVal));
    view.updateTabIcon(tab, editorController.getModel().isModified());

    // Handle close request
    tab.setOnCloseRequest(
        event -> {
          event.consume();
          requestCloseTab(tab);
        });
  }

  private void bindFileToEditor(CodeEditorController editorController, String filePath) {
    editorController.getModel().setFilePath(filePath);
    editorController.getModel().markAsSaved();
  }

  // ===============================================================================
  // Internal - File Loading (Async)
  // ===============================================================================

  private void startFileLoadingTask(Tab tab, File file) {
    Task<String> task = FileLoaderService.loadFileAsync(file);

    task.setOnSucceeded(
        event -> {
          unlockTabUI(tab);
          updateTabContent(tab, task.getValue());
        });

    task.setOnFailed(
        event -> {
          Throwable ex = task.getException();
          String errorMsg = ex != null ? ex.getMessage() : "Unknown error";
          view.showError("File Error", "Could not read file: " + errorMsg);
          Platform.runLater(() -> closeTabImmediately(tab));
        });

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private void lockTabUI(Tab tab) {
    TabContext context = TabContext.get(tab);
    if (context != null) {
      context.getEditorController().getView().getRoot().setDisable(true);
    }
  }

  private void unlockTabUI(Tab tab) {
    TabContext context = TabContext.get(tab);
    if (context != null) {
      context.getEditorController().getView().getRoot().setDisable(false);
    }
  }

  private void updateTabContent(Tab tab, String content) {
    TabContext context = TabContext.get(tab);
    if (context != null) {
      CodeEditorController editor = context.getEditorController();
      editor.getModel().setContent(content);
      editor.getModel().setModified(false);
    }
  }

  // ===============================================================================
  // Internal - Tab Lifecycle
  // ===============================================================================

  private void closeTabImmediately(Tab tab) {
    if (tab == null) {
      return;
    }

    // Get context before clearing
    TabContext context = TabContext.get(tab);

    // Unbind to prevent memory leaks
    tab.textProperty().unbind();

    // Dispose resources
    if (context != null) {
      context.dispose();
    }

    // Clear reference
    tab.setUserData(null);

    // Remove from view
    view.getTabs().remove(tab);
  }

  private Tab findTabByFile(File file) {
    Path normalizedPath = normalizePathSafely(file);
    String absolutePath = file.getAbsolutePath();

    for (Tab tab : view.getTabs()) {
      String tabFilePath = getFilePathForTab(tab);
      if (tabFilePath != null && isMatchingFile(normalizedPath, absolutePath, tabFilePath)) {
        return tab;
      }
    }
    return null;
  }

  private Path normalizePathSafely(File file) {
    try {
      return file.toPath().toRealPath();
    } catch (IOException _) {
      return null;
    }
  }

  private boolean isMatchingFile(
      Path normalizedSearchPath, String absoluteSearchPath, String tabFilePath) {
    if (normalizedSearchPath != null) {
      return matchesNormalizedPath(normalizedSearchPath, tabFilePath);
    }
    return absoluteSearchPath.equals(tabFilePath);
  }

  private boolean matchesNormalizedPath(Path normalizedPath, String tabFilePath) {
    try {
      Path tabPath = Path.of(tabFilePath).toRealPath();
      return normalizedPath.equals(tabPath);
    } catch (IOException _) {
      return false;
    }
  }

  // ===============================================================================
  // Internal - Empty State
  // ===============================================================================

  private void updateEmptyStateVisibility() {
    boolean noTabsOpen = view.getTabs().isEmpty();
    view.updateEmptyStateVisibility(noTabsOpen);
    view.setTabsVisible(!noTabsOpen);
  }
}
