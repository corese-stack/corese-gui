package fr.inria.corese.gui.feature.data.model;

/**
 * Immutable rule-file view model for the Data reasoning panel.
 *
 * @param id
 *            stable rule file id
 * @param label
 *            display label
 * @param sourcePath
 *            full source file path
 * @param enabled
 *            current toggle state
 */
public record DataRuleFileItem(String id, String label, String sourcePath, boolean enabled) {
}
