package fr.inria.corese.gui.feature.result.table.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TableResultPreferenceSupportTest {

	@Test
	void rowsPerPagePreference_isPersistedAndSanitized() {
		int previous = TableResultPreferenceSupport.loadRowsPerPage();
		try {
			TableResultPreferenceSupport.saveRowsPerPage(125);
			assertEquals(125, TableResultPreferenceSupport.loadRowsPerPage());

			TableResultPreferenceSupport.saveRowsPerPage(0);
			assertEquals(1, TableResultPreferenceSupport.loadRowsPerPage());
		} finally {
			TableResultPreferenceSupport.saveRowsPerPage(previous);
		}
	}
}
