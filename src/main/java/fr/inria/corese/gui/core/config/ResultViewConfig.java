package fr.inria.corese.gui.core.config;

import java.util.EnumSet;
import java.util.Set;

/**
 * Configuration for result view tabs.
 *
 * <p>
 * Defines which tabs (Text, Table, Graph) are available in the result view,
 * providing a declarative and type-safe way to configure the UI.
 *
 * <p>
 * This class uses the Builder pattern to construct configurations and is immutable
 * once created, ensuring thread-safety and predictable behavior.
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * // Query results: all tabs
 * ResultViewConfig config = ResultViewConfig.allTabs();
 * 
 * // Validation: text and table only
 * ResultViewConfig config = ResultViewConfig.builder()
 *     .withTextTab()
 *     .withTableTab()
 *     .build();
 * }</pre>
 */
public class ResultViewConfig {

    // ==============================================================================================
    // Inner Types
    // ==============================================================================================

    /** Available result tab types. */
    public enum TabType {
        TEXT,
        TABLE,
        GRAPH;

        /**
         * Returns the display label for this tab type.
         *
         * @return The human-readable label (e.g., "Text", "Table", "Graph")
         */
        public String getLabel() {
            String name = name();
            return name.charAt(0) + name.substring(1).toLowerCase();
        }
    }

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private final Set<TabType> enabledTabs;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    private ResultViewConfig(Set<TabType> enabledTabs) {
        this.enabledTabs = EnumSet.copyOf(enabledTabs);
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Checks if a specific tab type is enabled.
     *
     * @param tabType The tab type to check.
     * @return true if the tab is enabled, false otherwise.
     */
    public boolean hasTab(TabType tabType) {
        return enabledTabs.contains(tabType);
    }

    /**
     * Returns the set of enabled tabs.
     *
     * @return An unmodifiable view of enabled tabs.
     */
    public Set<TabType> getEnabledTabs() {
        return EnumSet.copyOf(enabledTabs);
    }

    /**
     * Creates a builder for constructing a configuration.
     *
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a configuration with all tabs enabled.
     *
     * @return Configuration with Text, Table, and Graph tabs.
     */
    public static ResultViewConfig allTabs() {
        return builder()
            .withTextTab()
            .withTableTab()
            .withGraphTab()
            .build();
    }

    // ==============================================================================================
    // Builder
    // ==============================================================================================

    /**
     * Builder for constructing ResultViewConfig instances.
     *
     * <p>
     * Provides a fluent API for selecting which tabs to enable.
     */
    public static class Builder {
        private final Set<TabType> tabs = EnumSet.noneOf(TabType.class);

        private Builder() {}

        /**
         * Enables the Text tab for displaying serialized results.
         *
         * @return This builder for method chaining.
         */
        public Builder withTextTab() {
            tabs.add(TabType.TEXT);
            return this;
        }

        /**
         * Enables the Table tab for displaying SPARQL query results.
         *
         * @return This builder for method chaining.
         */
        public Builder withTableTab() {
            tabs.add(TabType.TABLE);
            return this;
        }

        /**
         * Enables the Graph tab for visualizing RDF graphs.
         *
         * @return This builder for method chaining.
         */
        public Builder withGraphTab() {
            tabs.add(TabType.GRAPH);
            return this;
        }

        /**
         * Builds the immutable ResultViewConfig instance.
         *
         * @return The configured ResultViewConfig.
         * @throws IllegalStateException if no tabs are enabled.
         */
        public ResultViewConfig build() {
            if (tabs.isEmpty()) {
                throw new IllegalStateException("At least one tab must be enabled");
            }
            return new ResultViewConfig(tabs);
        }
    }
}
