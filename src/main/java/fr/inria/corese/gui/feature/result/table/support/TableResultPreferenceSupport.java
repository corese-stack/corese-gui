package fr.inria.corese.gui.feature.result.table.support;

import java.util.prefs.Preferences;

/**
 * Preference helpers for table result settings.
 */
public final class TableResultPreferenceSupport {

	private static final Preferences PREFS = Preferences.userNodeForPackage(TableResultPreferenceSupport.class);
	private static final String PREF_TABLE_ROWS_PER_PAGE = "results.table.rowsPerPage";

	private static final int DEFAULT_ROWS_PER_PAGE = 50;
	private static final int MIN_ROWS_PER_PAGE = 1;

	private TableResultPreferenceSupport() {
		throw new AssertionError("Utility class");
	}

	public static int loadRowsPerPage() {
		try {
			int stored = PREFS.getInt(PREF_TABLE_ROWS_PER_PAGE, DEFAULT_ROWS_PER_PAGE);
			return sanitizeRowsPerPage(stored);
		} catch (RuntimeException _) {
			return DEFAULT_ROWS_PER_PAGE;
		}
	}

	public static void saveRowsPerPage(int rowsPerPage) {
		try {
			PREFS.putInt(PREF_TABLE_ROWS_PER_PAGE, sanitizeRowsPerPage(rowsPerPage));
		} catch (RuntimeException _) {
			// Ignore preference persistence failures.
		}
	}

	public static int defaultRowsPerPage() {
		return DEFAULT_ROWS_PER_PAGE;
	}

	private static int sanitizeRowsPerPage(int rowsPerPage) {
		return Math.max(MIN_ROWS_PER_PAGE, rowsPerPage);
	}
}
