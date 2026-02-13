package fr.inria.corese.gui.feature.query.support;

import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.QueryType;
import java.util.prefs.Preferences;

/**
 * Stores and restores preferred result tabs per query family.
 */
public final class QueryResultTabPreferenceSupport {

	private static final Preferences PREFS = Preferences.userNodeForPackage(QueryResultTabPreferenceSupport.class);
	private static final Preferences LEGACY_PREFS = Preferences.userRoot().node("/fr/inria/corese/gui/feature/query");
	private static final String PREF_LAST_TABLE_TAB = "results.lastTab.table";
	private static final String PREF_LAST_GRAPH_TAB = "results.lastTab.graph";

	private ResultViewConfig.TabType lastTableTab = loadTabPreference(PREF_LAST_TABLE_TAB);
	private ResultViewConfig.TabType lastGraphTab = loadTabPreference(PREF_LAST_GRAPH_TAB);

	public QueryResultTabPreferenceSupport() {
	}

	public ResultViewConfig.TabType preferredTab(QueryType queryType) {
		if (queryType == QueryType.SELECT || queryType == QueryType.ASK) {
			return isTableTab(lastTableTab) ? lastTableTab : null;
		}
		if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
			return isGraphTab(lastGraphTab) ? lastGraphTab : null;
		}
		return null;
	}

	public void rememberPreferredTab(QueryType queryType, ResultViewConfig.TabType tabType) {
		if (queryType == null || tabType == null) {
			return;
		}
		if (queryType == QueryType.SELECT || queryType == QueryType.ASK) {
			if (!isTableTab(tabType)) {
				return;
			}
			lastTableTab = tabType;
			PREFS.put(PREF_LAST_TABLE_TAB, tabType.name());
			LEGACY_PREFS.put(PREF_LAST_TABLE_TAB, tabType.name());
			return;
		}
		if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
			if (!isGraphTab(tabType)) {
				return;
			}
			lastGraphTab = tabType;
			PREFS.put(PREF_LAST_GRAPH_TAB, tabType.name());
			LEGACY_PREFS.put(PREF_LAST_GRAPH_TAB, tabType.name());
		}
	}

	private static ResultViewConfig.TabType loadTabPreference(String key) {
		String value = PREFS.get(key, null);
		if (value == null || value.isBlank()) {
			value = LEGACY_PREFS.get(key, null);
			if (value != null && !value.isBlank()) {
				PREFS.put(key, value);
			}
		}
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return ResultViewConfig.TabType.valueOf(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static boolean isTableTab(ResultViewConfig.TabType tabType) {
		return tabType == ResultViewConfig.TabType.TABLE || tabType == ResultViewConfig.TabType.TEXT;
	}

	private static boolean isGraphTab(ResultViewConfig.TabType tabType) {
		return tabType == ResultViewConfig.TabType.GRAPH || tabType == ResultViewConfig.TabType.TEXT;
	}
}
