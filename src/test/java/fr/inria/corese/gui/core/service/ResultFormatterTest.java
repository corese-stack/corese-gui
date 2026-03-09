package fr.inria.corese.gui.core.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		Graph graph = Graph.create();
		ResultFormatter.ResultFormattingException exception = assertThrows(
				ResultFormatter.ResultFormattingException.class,
				() -> formatter.formatGraphOrThrow(graph, SerializationFormat.SPARQL_QUERY));

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
