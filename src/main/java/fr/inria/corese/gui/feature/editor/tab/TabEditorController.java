package fr.inria.corese.gui.feature.editor.tab;

import fr.inria.corese.gui.component.button.FloatingButtonWidget;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.feature.editor.code.CodeEditorController;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.core.io.ImportHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.application.Platform;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the tabbed editor interface.
 *
 * <p>
 * This controller manages multiple code editor tabs using a clean MVC
 * architecture with:
 *
 * <ul>
 * <li><b>Single source of truth:</b> TabContext stored in Tab.userData
 * <li><b>Builder pattern:</b> Configuration via TabEditorConfig
 * <li><b>Clean separation:</b> UI assembly vs business logic
 * <li><b>Async operations:</b> Non-blocking file I/O
 * </ul>
 *
 * <p>
 * <b>Usage example:</b>
 *
 */
public class TabEditorController {

  // ===============================================================================
  // Constants
  // ===============================================================================

  private static final String DEFAULT_TAB_TITLE = "untitled";
  private static final String DEFAULT_TAB_LABEL = "Untitled";
  private static final Logger LOGGER = LoggerFactory.getLogger(TabEditorController.class);

  // ===============================================================================
  // Fields
  // ===============================================================================

  /** The view component - handles UI presentation */
  private final TabEditorView view;

  /** Immutable configuration - all settings in one place */
  private final TabEditorConfig config;

  /** Factory for creating result controllers (lazy from config) */
  private final Supplier<ResultController> resultControllerFactory;

  /**
   * Preloaded tab that is ready for instant use.
   *
   * <p>
   * This tab is created during initialization if
   * {@link TabEditorConfig#shouldPreloadFirstTab()}
   * is enabled. It remains in memory but invisible until the user creates a new
   * empty tab, at which
   * point this preloaded instance is reused for instant display.
   *
   * <p>
   * After being used once, this field is set to null and all subsequent tabs are
   * created
   * on-demand.
   */
  private Tab preloadedTab;
  private int untitledCounter = 0;

  // ===============================================================================
  // Constructor
  // ===============================================================================

  /**
   * Constructs a TabEditorController with the given configuration.
   *
   * <p>
   * All configuration must be provided upfront via TabEditorConfig.builder().
   * This ensures the
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
    if (config.getEmptyStateWidget() != null) {
      view.setEmptyStateWidget(config.getEmptyStateWidget());
    }

    // Setup open-file action
    setupOpenFileAction();

    // Setup listeners
    view.subscribeToTabChanges(
        (ListChangeListener<Tab>) c -> Platform.runLater(this::updateEmptyStateVisibility));
    view.setOnNewTabAction(e -> createNewTab());

    // Preload first tab in background if configured
    // Tab is created but not added to the view, eliminating first-open delay
    if (config.shouldPreloadFirstTab()) {
      preloadedTab = createTabWithoutAdding(DEFAULT_TAB_TITLE, "", null);
    }

    // Initial state
    updateEmptyStateVisibility();
  }

  /** Configures the Open File button action from config. */
  private void setupOpenFileAction() {
    Runnable openFileAction = resolveOpenFileAction();
    if (openFileAction == null) {
      view.setOnOpenFileAction(null);
      return;
    }
    view.setOnOpenFileAction(e -> openFileAction.run());
  }

  @SuppressWarnings("deprecation")
  private Runnable resolveOpenFileAction() {
    if (config.getOpenFileAction() != null) {
      return config.getOpenFileAction();
    }
    Optional<TabEditorConfig.MenuItem> fallback = config.getMenuItems().stream()
        .filter(item -> item.text() != null)
        .filter(item -> item.text().toLowerCase().contains("open"))
        .findFirst()
        .or(() -> config.getMenuItems().size() > 1
            ? Optional.of(config.getMenuItems().get(1))
            : Optional.empty());
    return fallback.map(TabEditorConfig.MenuItem::action).orElse(null);
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
   * <p>
   * If a preloaded tab is available and the parameters match (default title and
   * empty content),
   * the preloaded tab is reused for instant display. Otherwise, a new tab is
   * created.
   *
   * @param title   The tab title
   * @param content The initial content
   * @return The created Tab
   */
  public Tab createNewTab(String title, String content) {
    String effectiveTitle = resolveNewTabTitle(title);
    // Use preloaded tab if available and parameters match default empty tab
    if (preloadedTab != null && title.equals(DEFAULT_TAB_TITLE) && content.isEmpty()) {
      Tab tab = preloadedTab;
      preloadedTab = null; // Clear after use - only first tab is preloaded
      TabContext context = TabContext.get(tab);
      if (context != null) {
        context.getEditorController().getModel().setDisplayNameOverride(effectiveTitle);
      }
      view.addNewEditorTab(tab);
      return tab;
    }
    return createTabWithContext(effectiveTitle, content, null);
  }

  private String resolveNewTabTitle(String requestedTitle) {
    if (DEFAULT_TAB_TITLE.equals(requestedTitle)) {
      return nextUntitledTitle();
    }
    return ensureUniqueTitle(requestedTitle);
  }

  private String nextUntitledTitle() {
    java.util.Set<String> existingTitles = getExistingTabTitles();
    int next = untitledCounter + 1;
    String candidate = DEFAULT_TAB_LABEL + " " + next;
    while (existingTitles.contains(candidate)) {
      next++;
      candidate = DEFAULT_TAB_LABEL + " " + next;
    }
    untitledCounter = next;
    return candidate;
  }

  private String ensureUniqueTitle(String baseTitle) {
    if (baseTitle == null || baseTitle.isBlank()) {
      return nextUntitledTitle();
    }
    java.util.Set<String> existingTitles = getExistingTabTitles();
    if (!existingTitles.contains(baseTitle)) {
      return baseTitle;
    }
    int suffix = 2;
    String candidate = baseTitle + " (" + suffix + ")";
    while (existingTitles.contains(candidate)) {
      suffix++;
      candidate = baseTitle + " (" + suffix + ")";
    }
    return candidate;
  }

  private java.util.Set<String> getExistingTabTitles() {
    java.util.Set<String> titles = new java.util.HashSet<>();
    for (Tab tab : view.getTabs()) {
      String text = tab.getText();
      if (text != null && !text.isBlank()) {
        titles.add(text);
      }
    }
    return titles;
  }

  /**
   * Opens a file in a new tab asynchronously.
   *
   * <p>
   * If the file is already open, selects that tab instead. The file is loaded in
   * a background
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
   * <p>
   * Follows JavaFX naming convention for user-requested actions. If the tab has
   * unsaved changes,
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
            case ModalService.UnsavedChangesResult.SAVE:
              controller.saveFile();
              if (!controller.getModel().isModified()) {
                closeTabImmediately(tab);
              }
              break;
            case ModalService.UnsavedChangesResult.DONT_SAVE:
              closeTabImmediately(tab);
              break;
            case ModalService.UnsavedChangesResult.CANCEL:
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
      if (context != null) {
        context.executionRunningProperty().set(loading);
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
    return context != null ? context.getEditorController().getContent() : null;
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
  // Public API - Lifecycle
  // ===============================================================================

  /**
   * Shuts down this controller and releases all resources.
   *
   * <p>
   * This method should be called when the TabEditorController is no longer
   * needed, typically
   * when the application is closing.
   *
   * <p>
   * <b>Note:</b> File loading threads are daemon threads managed by
   * {@link ImportHelper} and will automatically terminate when the application exits.
   */
  public void shutdown() {
    LOGGER.debug("TabEditorController shutdown initiated");
  }

  // ===============================================================================
  // Internal - Tab Assembly (Core Logic)
  // ===============================================================================

  /**
   * Creates a tab with full context attached and adds it to the view.
   *
   * <p>
   * This method delegates to {@link #createTabWithoutAdding} and then adds the
   * tab to the view.
   *
   * @param title    Tab title
   * @param content  Initial content
   * @param filePath File path (null if not associated with a file)
   * @return The created and displayed Tab
   */
  private Tab createTabWithContext(String title, String content, String filePath) {
    Tab tab = createTabWithoutAdding(title, content, filePath);
    view.addNewEditorTab(tab);
    return tab;
  }

  /**
   * Creates a tab with full context attached but does not add it to the view.
   *
   * <p>
   * This is the core tab assembly method that creates all components and wires
   * them together:
   *
   * <ul>
   * <li>Creates editor and result controllers
   * <li>Assembles the UI layout
   * <li>Creates the tab with its content
   * <li>Attaches the TabContext
   * <li>Binds properties and file associations
   * </ul>
   *
   * <p>
   * This method is used for preloading tabs without displaying them, allowing
   * instant display
   * when needed later.
   *
   * @param title    Tab title
   * @param content  Initial content
   * @param filePath File path (null if not associated with a file)
   * @return The fully configured Tab (not yet added to view)
   */
  private Tab createTabWithoutAdding(String title, String content, String filePath) {
    // 1. Create controllers
    CodeEditorController editorController = new CodeEditorController(config.getEditorButtons(), content,
        config.getAllowedExtensions());
    ResultController resultController = createResultControllerIfConfigured();
    if (filePath == null) {
      editorController.getModel().setDisplayNameOverride(title);
    }

    // 2. Assemble UI
    StackPane editorWrapper = new StackPane(editorController.getViewRoot());
    Node tabContent = layoutTabContent(editorWrapper, resultController);

    // 3. Create tab
    Tab tab = view.createEditorTab(title, tabContent);

    // 4. Attach context (single source of truth)
    FloatingButtonWidget executionButton = setupExecutionButton(editorWrapper);
    attachContext(tab, editorController, resultController, executionButton);

    // 5. Final setup
    bindTabProperties(tab, editorController);
    if (filePath != null) {
      bindFileToEditor(editorController, filePath);
    }

    return tab;
  }

  /**
   * Attaches TabContext to tab's userData.
   *
   * <p>
   * This is the single source of truth for all tab-related components.
   *
   * @param tab              The tab
   * @param editorController The editor controller
   * @param resultController The result controller (nullable)
   * @param executionButton  The execution button (nullable)
   */
  private void attachContext(
      Tab tab,
      CodeEditorController editorController,
      ResultController resultController,
      FloatingButtonWidget executionButton) {
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

  private FloatingButtonWidget setupExecutionButton(
      StackPane editorWrapper) {
    if (!config.hasExecution()) {
      return null;
    }

    FloatingButtonWidget runButton = createExecutionButton();
    StackPane.setAlignment(runButton, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(runButton, TabEditorView.getExecutionButtonMargin());
    editorWrapper.getChildren().add(runButton);

    return runButton;
  }

  private FloatingButtonWidget createExecutionButton() {
    FloatingButtonWidget runButton = new FloatingButtonWidget(config.getExecutionButton());

    // Set action
    runButton.setOnAction(
        e -> {
          if (config.getExecutionAction() != null) {
            config.getExecutionAction().run();
          }
        });

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

    // Bind execution button properties
    TabContext context = TabContext.get(tab);
    if (context != null && context.hasExecutionButton()) {
      BooleanBinding isEmpty = Bindings.createBooleanBinding(
          () -> {
            String c = editorController.getModel().getContent();
            return c == null || c.trim().isEmpty();
          },
          editorController.getModel().contentProperty());

      // Bind loading state (visual animation)
      context.getExecutionButton().loadingProperty().bind(context.executionRunningProperty());

      // Bind disabled state (interaction)
      context
          .getExecutionButton()
          .disableProperty()
          .bind(isEmpty.or(context.executionRunningProperty()));
    }
  }

  private void bindFileToEditor(CodeEditorController editorController, String filePath) {
    editorController.getModel().setFilePath(filePath);
    editorController.getModel().markAsSaved();
  }

  // ===============================================================================
  // Internal - File Loading (Async)
  // ===============================================================================

  private void startFileLoadingTask(Tab tab, File file) {
    ImportHelper.loadFileAsync(
        file,
        content -> {
          unlockTabUI(tab);
          updateTabContent(tab, content);
          LOGGER.info("File loaded: {}", file.getAbsolutePath());
        },
        ex -> {
          // Log full exception for debugging
          LOGGER.error("Failed to load file: {}", file.getAbsolutePath(), ex);

          // Show user-friendly error message
          String errorMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
          view.showError("File Error", "Could not read file: " + errorMsg);
          Platform.runLater(() -> closeTabImmediately(tab));
        });
  }

  private void lockTabUI(Tab tab) {
    TabContext context = TabContext.get(tab);
    if (context != null) {
      context.getEditorController().setDisable(true);
    }
  }

  private void unlockTabUI(Tab tab) {
    TabContext context = TabContext.get(tab);
    if (context != null) {
      context.getEditorController().setDisable(false);
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

  /**
   * Closes a tab immediately without confirmation.
   *
   * <p>
   * Performs thorough cleanup to prevent memory leaks:
   *
   * <ul>
   * <li>Unbinds all JavaFX property bindings
   * <li>Calls dispose() on TabContext, which in turn disposes
   * CodeEditorController (unbinding
   * CodeMirror content and cleaning up listeners)
   * <li>Clears all references
   * <li>Removes tab from view
   * </ul>
   *
   * @param tab The tab to close
   */
  private void closeTabImmediately(Tab tab) {
    if (tab == null) {
      return;
    }

    // Get context before clearing
    TabContext context = TabContext.get(tab);

    // Unbind to prevent memory leaks
    tab.textProperty().unbind();

    // Dispose resources (CodeEditorController.dispose() unbinds bidirectional
    // content binding)
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
