package fr.inria.corese.gui.feature.validation.support;

import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.io.AppPreferences;

/**
 * Stores and restores the preferred validation result tab.
 */
public final class ValidationResultTabPreferenceSupport {

	private static final AppPreferences.Node PREFS = AppPreferences
			.nodeForClass(ValidationResultTabPreferenceSupport.class);
	private static final String PREF_LAST_VALIDATION_TAB = "results.lastTab.validation";

	private ResultViewConfig.TabType lastPreferredTab = loadTabPreference();

	public ResultViewConfig.TabType preferredTab() {
		return isValidationTab(lastPreferredTab) ? lastPreferredTab : null;
	}

	public void rememberPreferredTab(ResultViewConfig.TabType tabType) {
		if (!isValidationTab(tabType)) {
			return;
		}
		lastPreferredTab = tabType;
		PREFS.put(PREF_LAST_VALIDATION_TAB, tabType.name());
	}

	private static ResultViewConfig.TabType loadTabPreference() {
		String value = PREFS.get(PREF_LAST_VALIDATION_TAB, null);
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return ResultViewConfig.TabType.valueOf(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static boolean isValidationTab(ResultViewConfig.TabType tabType) {
		return tabType == ResultViewConfig.TabType.TEXT || tabType == ResultViewConfig.TabType.GRAPH;
	}
}
