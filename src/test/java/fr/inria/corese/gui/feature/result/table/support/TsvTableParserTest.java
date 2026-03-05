package fr.inria.corese.gui.feature.result.table.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TsvTableParserTest {

	@Test
	void parse_withNullOrEmptyInput_returnsEmptyRows() {
		assertTrue(TsvTableParser.parse(null).isEmpty());
		assertTrue(TsvTableParser.parse("").isEmpty());
	}

	@Test
	void parse_supportsRdfTermValuesWithLanguageAndDatatype() {
		String tsv = "?text\t?num\n\"Bla bla\"@fr\t\"12\"^^<http://www.w3.org/2001/XMLSchema#int>";

		List<String[]> rows = TsvTableParser.parse(tsv);

		assertEquals(2, rows.size());
		assertArrayEquals(new String[]{"?text", "?num"}, rows.get(0));
		assertArrayEquals(new String[]{"\"Bla bla\"@fr", "\"12\"^^<http://www.w3.org/2001/XMLSchema#int>"},
				rows.get(1));
	}

	@Test
	void parse_keepsEmptyCellsForUnboundVariables() {
		String tsv = "?a\t?b\nvalue\t\n\tother";

		List<String[]> rows = TsvTableParser.parse(tsv);

		assertEquals(3, rows.size());
		assertArrayEquals(new String[]{"?a", "?b"}, rows.get(0));
		assertArrayEquals(new String[]{"value", ""}, rows.get(1));
		assertArrayEquals(new String[]{"", "other"}, rows.get(2));
	}

	@Test
	void parse_doesNotCreateExtraRowWhenContentEndsWithNewline() {
		String tsv = "?a\t?b\n1\t2\n";

		List<String[]> rows = TsvTableParser.parse(tsv);

		assertEquals(2, rows.size());
		assertArrayEquals(new String[]{"?a", "?b"}, rows.get(0));
		assertArrayEquals(new String[]{"1", "2"}, rows.get(1));
	}
}
