package fr.inria.corese.gui.feature.result.table.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTableParserTest {

	@Test
	void parse_withNullOrEmptyInput_returnsEmptyRows() {
		assertTrue(CsvTableParser.parse(null).isEmpty());
		assertTrue(CsvTableParser.parse("").isEmpty());
	}

	@Test
	void parse_supportsQuotedCommaAndEscapedQuote() {
		String csv = "text,comment\n\"shoe, size\",\"He said \"\"ok\"\"\"";

		List<String[]> rows = CsvTableParser.parse(csv);

		assertEquals(2, rows.size());
		assertArrayEquals(new String[]{"text", "comment"}, rows.get(0));
		assertArrayEquals(new String[]{"shoe, size", "He said \"ok\""}, rows.get(1));
	}

	@Test
	void parse_supportsEmbeddedNewlineInQuotedField() {
		String csv = "text\nshoe size\npointure\n\"taille\nde chaussure\"";

		List<String[]> rows = CsvTableParser.parse(csv);

		assertEquals(4, rows.size());
		assertArrayEquals(new String[]{"text"}, rows.get(0));
		assertArrayEquals(new String[]{"shoe size"}, rows.get(1));
		assertArrayEquals(new String[]{"pointure"}, rows.get(2));
		assertArrayEquals(new String[]{"taille\nde chaussure"}, rows.get(3));
	}

	@Test
	void parse_doesNotCreateExtraRowWhenContentEndsWithNewline() {
		String csv = "a,b\n1,2\n";

		List<String[]> rows = CsvTableParser.parse(csv);

		assertEquals(2, rows.size());
		assertArrayEquals(new String[]{"a", "b"}, rows.get(0));
		assertArrayEquals(new String[]{"1", "2"}, rows.get(1));
	}
}
