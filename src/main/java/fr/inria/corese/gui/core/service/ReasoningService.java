package fr.inria.corese.gui.core.service;

import java.util.Map;

/**
 * Service managing built-in reasoning profile lifecycle.
 */
public interface ReasoningService {

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
	 * Returns whether at least one reasoning profile is enabled.
	 *
	 * @return true if any profile is enabled
	 */
	boolean hasAnyEnabledProfile();

	/**
	 * Recomputes all enabled profiles from asserted data.
	 */
	void recomputeEnabledProfiles();

	/**
	 * Disables all profiles and removes managed inference graphs.
	 */
	void resetAllProfiles();
}
