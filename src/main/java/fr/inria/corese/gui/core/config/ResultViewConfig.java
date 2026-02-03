package fr.inria.corese.gui.core.config;


import java.util.EnumSet;
import java.util.Set;

/**
 * Configuration for which tabs should be displayed in the ResultView.
 *
 * <p>This class provides a declarative way to specify which result tabs are available, making
 * ResultController truly generic and context-agnostic.
 *
 * <p><b>Usage examples:</b>
 *
 * <pre>{@code
 * // Query context: Text + Table + Graph
 * ResultViewConfig config = ResultViewConfig.builder()
 *     .withTextTab()
 *     .withTableTab()
 *     .withGraphTab()
 *     .build();
 *
 * // Validation context: Text + Table
 * ResultViewConfig config = ResultViewConfig.builder()
 *     .withTextTab()
 *     .withTableTab()
 *     .build();
 *
 * // All tabs
 * ResultViewConfig config = ResultViewConfig.allTabs();
 *
 * // Default (Text only)
 * ResultViewConfig config = ResultViewConfig.defaultConfig();
 * }</pre>
 */
public class ResultViewConfig {

  /** Available result tab types. */
  public enum TabType {
    TEXT,
    TABLE,
    GRAPH;

    /**
     * Returns the display label for this tab type.
     *
     * <p>Formats the enum name with title case (e.g., TEXT → "Text").
     *
     * @return The human-readable tab label
     */
    public String getLabel() {
      String name = name();
      return name.charAt(0) + name.substring(1).toLowerCase();
    }
  }

  private final Set<TabType> enabledTabs;

  private ResultViewConfig(Set<TabType> enabledTabs) {
    this.enabledTabs = EnumSet.copyOf(enabledTabs);
  }

  /**
   * Checks if a specific tab type is enabled.
   *
   * @param tabType The tab type to check
   * @return true if the tab is enabled, false otherwise
   */
  public boolean hasTab(TabType tabType) {
    return enabledTabs.contains(tabType);
  }

  /**
   * Returns the set of enabled tab types.
   *
   * @return An immutable copy of enabled tabs
   */
  public Set<TabType> getEnabledTabs() {
    return EnumSet.copyOf(enabledTabs);
  }

  /**
   * Creates a builder for constructing a ResultViewConfig.
   *
   * @return A new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration with only the Text tab.
   *
   * @return Default configuration
   */
  public static ResultViewConfig defaultConfig() {
    return builder().withTextTab().build();
  }

  /**
   * Creates a configuration with all tabs enabled.
   *
   * @return Configuration with all tabs
   */
  public static ResultViewConfig allTabs() {
    return builder().withTextTab().withTableTab().withGraphTab().build();
  }

  /** Builder for ResultViewConfig. */
  public static class Builder {
    private final Set<TabType> tabs = EnumSet.noneOf(TabType.class);

    private Builder() {}

    /**
     * Enables the Text tab.
     *
     * @return This builder for chaining
     */
    public Builder withTextTab() {
      tabs.add(TabType.TEXT);
      return this;
    }

    /**
     * Enables the Table tab (SPARQL results).
     *
     * @return This builder for chaining
     */
    public Builder withTableTab() {
      tabs.add(TabType.TABLE);
      return this;
    }

    /**
     * Enables the Graph tab (visualization).
     *
     * @return This builder for chaining
     */
    public Builder withGraphTab() {
      tabs.add(TabType.GRAPH);
      return this;
    }

    /**
     * Builds the ResultViewConfig.
     *
     * @return The configured ResultViewConfig instance
     * @throws IllegalStateException if no tabs are enabled
     */
    public ResultViewConfig build() {
      if (tabs.isEmpty()) {
        throw new IllegalStateException("At least one tab must be enabled");
      }
      return new ResultViewConfig(tabs);
    }
  }
}
