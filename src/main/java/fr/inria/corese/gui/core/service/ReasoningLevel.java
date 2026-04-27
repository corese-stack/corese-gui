package fr.inria.corese.gui.core.service;

import java.util.EnumSet;
import java.util.Map;

/**
 * Discrete reasoning levels exposed by the Data page.
 */
public enum ReasoningLevel {
	NONE("None", false, EnumSet.noneOf(ReasoningProfile.class)),
	RDFS_SUBSET("RDFS Subset", true, EnumSet.noneOf(ReasoningProfile.class)),
	RDFS_RL("RDFS RL", true, EnumSet.of(ReasoningProfile.RDFS)),
	OWL_RL_LITE("OWL RL Lite", true, EnumSet.of(ReasoningProfile.RDFS, ReasoningProfile.OWL_RL_LITE)),
	OWL_RL("OWL RL", true, EnumSet.of(ReasoningProfile.RDFS, ReasoningProfile.OWL_RL)),
	OWL_RL_EXT("OWL RL Ext", true,
			EnumSet.of(ReasoningProfile.RDFS, ReasoningProfile.OWL_RL, ReasoningProfile.OWL_RL_EXT));

	private final String label;
	private final boolean rdfsSubsetEnabled;
	private final EnumSet<ReasoningProfile> enabledProfiles;

	ReasoningLevel(String label, boolean rdfsSubsetEnabled, EnumSet<ReasoningProfile> enabledProfiles) {
		this.label = label;
		this.rdfsSubsetEnabled = rdfsSubsetEnabled;
		this.enabledProfiles = EnumSet.copyOf(enabledProfiles);
	}

	/**
	 * Returns display label for the level.
	 *
	 * @return level label
	 */
	public String label() {
		return label;
	}

	/**
	 * Returns whether the native RDFS subset is active at this level.
	 *
	 * @return true if RDFS subset is active
	 */
	public boolean isRdfsSubsetEnabled() {
		return rdfsSubsetEnabled;
	}

	/**
	 * Returns whether one built-in profile is active at this level.
	 *
	 * @param profile
	 *            built-in profile
	 * @return true if profile is active
	 */
	public boolean isProfileEnabled(ReasoningProfile profile) {
		return profile != null && enabledProfiles.contains(profile);
	}

	/**
	 * Returns whether this level is at or above another level in the hierarchy.
	 *
	 * @param other
	 *            level to compare with
	 * @return true if this level includes or exceeds the other level
	 */
	public boolean isAtLeast(ReasoningLevel other) {
		ReasoningLevel target = other == null ? NONE : other;
		return this.ordinal() >= target.ordinal();
	}

	/**
	 * Resolves a level from current reasoning states.
	 *
	 * @param rdfsSubsetEnabled
	 *            native RDFS subset state
	 * @param states
	 *            built-in profile states
	 * @return resolved reasoning level
	 */
	public static ReasoningLevel fromStates(boolean rdfsSubsetEnabled, Map<ReasoningProfile, Boolean> states) {
		Map<ReasoningProfile, Boolean> safeStates = states == null ? Map.of() : states;
		for (ReasoningLevel level : ReasoningLevel.values()) {
			if (matchesExactly(level, rdfsSubsetEnabled, safeStates)) {
				return level;
			}
		}
		if (isEnabled(safeStates, ReasoningProfile.OWL_RL_EXT)) {
			return OWL_RL_EXT;
		}
		if (isEnabled(safeStates, ReasoningProfile.OWL_RL)) {
			return OWL_RL;
		}
		if (isEnabled(safeStates, ReasoningProfile.OWL_RL_LITE)) {
			return OWL_RL_LITE;
		}
		if (isEnabled(safeStates, ReasoningProfile.RDFS)) {
			return RDFS_RL;
		}
		return rdfsSubsetEnabled ? RDFS_SUBSET : NONE;
	}

	private static boolean matchesExactly(ReasoningLevel level, boolean rdfsSubsetEnabled,
			Map<ReasoningProfile, Boolean> states) {
		if (level.rdfsSubsetEnabled != rdfsSubsetEnabled) {
			return false;
		}
		for (ReasoningProfile profile : ReasoningProfile.values()) {
			boolean expectedEnabled = level.enabledProfiles.contains(profile);
			if (expectedEnabled != isEnabled(states, profile)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEnabled(Map<ReasoningProfile, Boolean> states, ReasoningProfile profile) {
		return Boolean.TRUE.equals(states.get(profile));
	}

	@Override
	public String toString() {
		return label;
	}
}
