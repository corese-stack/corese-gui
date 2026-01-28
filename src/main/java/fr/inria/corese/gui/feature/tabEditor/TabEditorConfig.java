package fr.inria.corese.gui.feature.tabEditor;

import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.config.ResultViewConfig;

import fr.inria.corese.gui.component.toolbar.ButtonIcon;
import fr.inria.corese.gui.feature.textResult.ResultController;







import javafx.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Configuration object for TabEditorController using the Builder pattern.
 *
 * <p>This class consolidates all configuration parameters for the TabEditor, replacing multiple
 * configure() methods with a single immutable configuration object. This approach:
 *
 * <ul>
 *   <li>Prevents incomplete initialization (compile-time safety)
 *   <li>Makes the controller simpler and more testable
 *   <li>Allows configuration reuse across multiple instances
 *   <li>Provides clear API via fluent builder
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * TabEditorConfig config = TabEditorConfig.builder()
 *     .withEditorButtons(List.of(
 *         new ButtonConfig(ButtonIcon.SAVE, "Save"),
 *         new ButtonConfig(ButtonIcon.UNDO, "Undo")
 *     ))
 *     .withExecution(
 *         new ButtonConfig(ButtonIcon.PLAY, "Run"),
 *         this::executeQuery
 *     )
 *     .withResultView(
 *         List.of(new ButtonConfig(ButtonIcon.COPY, "Copy")),
 *         ResultViewConfig.builder().withTextTab().build()
 *     )
 *     .withEmptyState(emptyStateNode)
 *     .build();
 *
 * TabEditorController controller = new TabEditorController(config);
 * }</pre>
 */
public class TabEditorConfig {

  // ===============================================================================
  // Fields
  // ===============================================================================

  private final List<ButtonConfig> editorButtons;
  private final ButtonConfig executionButton;
  private final Runnable executionAction;
  private final List<ButtonConfig> resultViewButtons;
  private final ResultViewConfig resultViewConfig;
  private final Node emptyStateWidget;
  private final List<MenuItem> menuItems;
  private final boolean preloadFirstTab;
  private final List<String> allowedExtensions;

  // ===============================================================================
  // Constructor (Private - use Builder)
  // ===============================================================================

  private TabEditorConfig(Builder builder) {
    this.editorButtons = builder.editorButtons != null ? List.copyOf(builder.editorButtons) : List.of();
    this.executionButton = builder.executionButton;
    this.executionAction = builder.executionAction;
    this.resultViewButtons = builder.resultViewButtons != null ? List.copyOf(builder.resultViewButtons) : null;
    this.resultViewConfig = builder.resultViewConfig;
    this.emptyStateWidget = builder.emptyStateWidget;
    this.menuItems = builder.menuItems != null ? List.copyOf(builder.menuItems) : new ArrayList<>();
    this.preloadFirstTab = builder.preloadFirstTab;
    this.allowedExtensions = builder.allowedExtensions != null ? List.copyOf(builder.allowedExtensions) : List.of();
  }

  // ===============================================================================
  // Getters
  // ===============================================================================

  public List<ButtonConfig> getEditorButtons() {
    return editorButtons;
  }

  public List<String> getAllowedExtensions() {
    return allowedExtensions;
  }

  public ButtonConfig getExecutionButton() {
    return executionButton;
  }

  public Runnable getExecutionAction() {
    return executionAction;
  }

  public boolean hasExecution() {
    return executionButton != null && executionAction != null;
  }

  public List<ButtonConfig> getResultViewButtons() {
    return resultViewButtons;
  }

  public ResultViewConfig getResultViewConfig() {
    return resultViewConfig;
  }

  public boolean hasResultView() {
    return resultViewConfig != null;
  }

  public Node getEmptyStateWidget() {
    return emptyStateWidget;
  }

  public List<MenuItem> getMenuItems() {
    return menuItems;
  }

  /**
   * Indicates whether the first tab should be preloaded during initialization.
   *
   * <p>When enabled, an empty tab is created in the background during controller initialization,
   * eliminating the delay when the user creates their first tab. The preloaded tab remains
   * invisible until explicitly requested.
   *
   * @return true if the first tab should be preloaded, false otherwise
   */
  public boolean shouldPreloadFirstTab() {
    return preloadFirstTab;
  }

  /**
   * Creates a ResultController factory if result view is configured.
   *
   * @return A supplier that creates new ResultController instances, or null if not configured
   */
  public Supplier<ResultController> createResultControllerFactory() {
    if (!hasResultView()) {
      return null;
    }
    return () -> new ResultController(resultViewButtons, resultViewConfig);
  }

  // ===============================================================================
  // Builder
  // ===============================================================================

  /**
   * Creates a new builder for TabEditorConfig.
   *
   * @return A new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for TabEditorConfig using fluent API.
   */
  public static class Builder {
    private List<ButtonConfig> editorButtons;
    private ButtonConfig executionButton;
    private Runnable executionAction;
    private List<ButtonConfig> resultViewButtons;
    private ResultViewConfig resultViewConfig;
    private Node emptyStateWidget;
    private List<MenuItem> menuItems;
    private boolean preloadFirstTab = false;
    private List<String> allowedExtensions;

    private Builder() {}

    /**
     * Configures editor toolbar buttons (Save, Clear, Undo, Redo).
     *
     * @param buttons The button configurations
     * @return This builder for chaining
     */
    public Builder withEditorButtons(List<ButtonConfig> buttons) {
      this.editorButtons = buttons;
      return this;
    }

    /**
     * Restricts the allowed file extensions for opening and saving files.
     *
     * @param extensions List of allowed extensions (e.g., ".ttl", ".rq")
     * @return This builder for chaining
     */
    public Builder withAllowedExtensions(List<String> extensions) {
      this.allowedExtensions = extensions;
      return this;
    }

    /**
     * Configures the floating execution button and its action.
     *
     * @param button The button configuration
     * @param action The action to execute when clicked
     * @return This builder for chaining
     */
    public Builder withExecution(ButtonConfig button, Runnable action) {
      this.executionButton = button;
      this.executionAction = action;
      return this;
    }

    /**
     * Configures the result view with buttons and tabs.
     *
     * @param buttons The result view toolbar buttons
     * @param config The result view tab configuration
     * @return This builder for chaining
     */
    public Builder withResultView(List<ButtonConfig> buttons, ResultViewConfig config) {
      this.resultViewButtons = buttons;
      this.resultViewConfig = config;
      return this;
    }

    /**
     * Configures the empty state view.
     *
     * @param emptyState The node to display when no tabs are open
     * @return This builder for chaining
     */
    public Builder withEmptyState(Node emptyState) {
      this.emptyStateWidget = emptyState;
      return this;
    }

    /**
     * Configures custom menu items for the "+" button.
     *
     * @param items The menu items
     * @return This builder for chaining
     */
    public Builder withMenuItems(List<MenuItem> items) {
      this.menuItems = items;
      return this;
    }

    /**
     * Enables preloading of the first tab to eliminate initial creation delay.
     *
     * <p>When enabled, the controller creates an empty tab in the background during
     * initialization. This tab remains invisible until the user explicitly creates a new tab,
     * at which point the preloaded tab is instantly displayed, providing a seamless experience.
     *
     * <p><b>Benefits:</b>
     * <ul>
     *   <li>Eliminates the delay when opening the first editor tab</li>
     *   <li>Improves perceived performance and user experience</li>
     *   <li>Invisible until needed - doesn't force a tab to display on startup</li>
     * </ul>
     *
     * <p><b>Note:</b> Only the first empty tab is preloaded. Subsequent tabs or tabs with
     * specific content are created on-demand as usual.
     *
     * @return This builder for chaining
     */
    public Builder withPreloadFirstTab() {
      this.preloadFirstTab = true;
      return this;
    }

    /**
     * Builds the immutable TabEditorConfig instance.
     *
     * @return A new TabEditorConfig
     */
    public TabEditorConfig build() {
      return new TabEditorConfig(this);
    }
  }

  // ===============================================================================
  // MenuItem Record
  // ===============================================================================

  /**
   * Configuration for a menu item in the "+" button dropdown.
   *
   * @param text The display text
   * @param action The action to execute
   */
  public record MenuItem(String text, Runnable action) {}
}
