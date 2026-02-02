package fr.inria.corese.gui.feature.tabeditor;

import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.feature.result.ResultController;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Node;

/**
 * Configuration object for TabEditorController using the Builder pattern.
 *
 * <p>This class consolidates all configuration parameters for the TabEditor, replacing multiple
 * configure() methods with a single immutable configuration object. This approach:
 *
 * <ul>
 *   <li>Preventing incomplete initialization (compile-time safety)
 *   <li>Simplifying the controller and making it more testable
 *   <li>Allowing configuration reuse across multiple instances
 *   <li>Providing a clear API via a fluent builder
 * </ul>
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
    return () -> new ResultController(resultViewConfig);
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

    public Builder withEditorButtons(List<ButtonConfig> buttons) {
      this.editorButtons = buttons;
      return this;
    }

    public Builder withAllowedExtensions(List<String> extensions) {
      this.allowedExtensions = extensions;
      return this;
    }

    public Builder withExecution(ButtonConfig button, Runnable action) {
      this.executionButton = button;
      this.executionAction = action;
      return this;
    }

    public Builder withResultView(List<ButtonConfig> buttons, ResultViewConfig config) {
      this.resultViewButtons = buttons;
      this.resultViewConfig = config;
      return this;
    }

    public Builder withEmptyState(Node emptyState) {
      this.emptyStateWidget = emptyState;
      return this;
    }

    public Builder withMenuItems(List<MenuItem> items) {
      this.menuItems = items;
      return this;
    }

    public Builder withPreloadFirstTab() {
      this.preloadFirstTab = true;
      return this;
    }

    public TabEditorConfig build() {
      return new TabEditorConfig(this);
    }
  }

  // ===============================================================================
  // MenuItem Record
  // ===============================================================================

  public record MenuItem(String text, Runnable action) {}
}
