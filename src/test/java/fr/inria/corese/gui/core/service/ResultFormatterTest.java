package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.inria.corese.gui.core.enums.SerializationFormat;

class ResultFormatterTest {

	private final ResultFormatter formatter = ResultFormatter.getInstance();

	@Test
	void formatGraphOrThrow_withNullGraph_throwsResultFormattingException() {
		ResultFormatter.ResultFormattingException exception = assertThrows(
				ResultFormatter.ResultFormattingException.class,
				() -> formatter.formatGraphOrThrow(null, SerializationFormat.TURTLE));

		assertTrue(exception.getMessage().contains("No graph data available."),
				"Null graph should fail with an explicit formatting exception.");
	}

	@Test
	void formatGraphOrThrow_withUnsupportedFormat_throwsResultFormattingException() {
		ResultFormatter.ResultFormattingException exception = assertThrows(
				ResultFormatter.ResultFormattingException.class, () -> formatter
						.formatGraphOrThrow(fr.inria.corese.core.Graph.create(), SerializationFormat.SPARQL_QUERY));

		assertTrue(exception.getMessage().contains("Unsupported format"),
				"Unsupported graph format should fail with an explicit formatting exception.");
	}

	@Test
	void formatGraph_withNullGraph_keepsLegacyErrorStringContract() {
		String formatted = formatter.formatGraph(null, SerializationFormat.TURTLE);
		assertTrue(formatted.startsWith("Error:"),
				"Legacy API should continue returning a user-displayable error string.");
	}

	@Test
	void formatMappingsOrThrow_withNullMappings_throwsResultFormattingException() {
		ResultFormatter.ResultFormattingException exception = assertThrows(
				ResultFormatter.ResultFormattingException.class,
				() -> formatter.formatMappingsOrThrow(null, SerializationFormat.JSON));

		assertTrue(exception.getMessage().contains("No query results available."),
				"Null mappings should fail with an explicit formatting exception.");
	}
}
