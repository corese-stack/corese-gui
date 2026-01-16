package fr.inria.corese.gui.core;

import fr.inria.corese.gui.controller.ResultController;
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
  private final Node emptyStateView;
  private final List<MenuItem> menuItems;

  // ===============================================================================
  // Constructor (Private - use Builder)
  // ===============================================================================

  private TabEditorConfig(Builder builder) {
    this.editorButtons = builder.editorButtons != null ? List.copyOf(builder.editorButtons) : List.of();
    this.executionButton = builder.executionButton;
    this.executionAction = builder.executionAction;
    this.resultViewButtons = builder.resultViewButtons != null ? List.copyOf(builder.resultViewButtons) : null;
    this.resultViewConfig = builder.resultViewConfig;
    this.emptyStateView = builder.emptyStateView;
    this.menuItems = builder.menuItems != null ? List.copyOf(builder.menuItems) : new ArrayList<>();
  }

  // ===============================================================================
  // Getters
  // ===============================================================================

  public List<ButtonConfig> getEditorButtons() {
    return editorButtons;
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

  public Node getEmptyStateView() {
    return emptyStateView;
  }

  public List<MenuItem> getMenuItems() {
    return menuItems;
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
    private Node emptyStateView;
    private List<MenuItem> menuItems;

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
      this.emptyStateView = emptyState;
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
