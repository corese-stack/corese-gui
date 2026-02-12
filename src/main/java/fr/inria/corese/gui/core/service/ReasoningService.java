package fr.inria.corese.gui.core.service;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service managing built-in reasoning profile lifecycle and rule files.
 */
public interface ReasoningService {

	/**
	 * Immutable rule-file descriptor exposed to the UI layer.
	 *
	 * @param id
	 *            stable rule id
	 * @param label
	 *            display label
	 * @param sourcePath
	 *            absolute rule file path
	 * @param namedGraphUri
	 *            target named graph used to store inferred triples
	 * @param enabled
	 *            current enablement state
	 */
	record RuleFileState(String id, String label, String sourcePath, String namedGraphUri, boolean enabled) {
	}

	/**
	 * Enables/disables one reasoning profile and refreshes managed inferences.
	 *
	 * @param profile
	 *            profile to update
	 * @param enabled
	 *            target state
	 */
	void setEnabled(ReasoningProfile profile, boolean enabled);

	/**
	 * Returns whether a reasoning profile is currently enabled.
	 *
	 * @param profile
	 *            profile to check
	 * @return true if enabled
	 */
	boolean isEnabled(ReasoningProfile profile);

	/**
	 * Returns current enablement states for all built-in profiles.
	 *
	 * @return immutable state map
	 */
	Map<ReasoningProfile, Boolean> snapshotStates();

	/**
	 * Returns whether at least one reasoning profile or rule file is enabled.
	 *
	 * @return true if any built-in profile or rule file is enabled
	 */
	boolean hasAnyEnabledProfile();

	/**
	 * Registers one {@code .rul} rule file and enables it.
	 *
	 * @param ruleFile
	 *            local rule file to add
	 */
	void addRuleFile(File ruleFile);

	/**
	 * Removes one rule file by id.
	 *
	 * @param ruleId
	 *            rule file identifier
	 */
	void removeRuleFile(String ruleId);

	/**
	 * Removes all rule files.
	 */
	void removeAllRuleFiles();

	/**
	 * Enables/disables one rule file.
	 *
	 * @param ruleId
	 *            rule file identifier
	 * @param enabled
	 *            target state
	 */
	void setRuleFileEnabled(String ruleId, boolean enabled);

	/**
	 * Returns current rule-file states.
	 *
	 * @return immutable rule-file list
	 */
	List<RuleFileState> snapshotRuleFiles();

	/**
	 * Applies rule-file enablement from a selected id set and recomputes reasoning
	 * once.
	 *
	 * <p>
	 * Every loaded rule file whose id is in {@code enabledRuleIds} becomes enabled,
	 * all others become disabled.
	 *
	 * @param enabledRuleIds
	 *            selected rule-file ids to enable
	 */
	void applyRuleFileSelection(Collection<String> enabledRuleIds);

	/**
	 * Recomputes all enabled profiles from asserted data.
	 */
	void recomputeEnabledProfiles();

	/**
	 * Disables all built-in profiles and rule files, and removes managed inference
	 * graphs.
	 */
	void resetAllProfiles();
}
