package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.inria.corese.gui.core.enums.SerializationFormat;

class RdfConversionServiceTest {

	private final RdfConversionService conversionService = RdfConversionService.getInstance();

	@Test
	void convertGraphContent_turtleToNTriples_returnsSerializedOutput() {
		String turtle = """
				@prefix ex: <http://example.org/> .
				ex:alice ex:knows ex:bob .
				""";

		String converted = conversionService.convertGraphContent(turtle, SerializationFormat.TURTLE,
				SerializationFormat.N_TRIPLES);

		assertTrue(converted.contains("http://example.org/alice"),
				"Converted graph should contain the expected subject URI.");
		assertTrue(converted.contains("http://example.org/knows"),
				"Converted graph should contain the expected predicate URI.");
	}

	@Test
	void convertGraphContent_withNullContent_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> conversionService.convertGraphContent(null,
				SerializationFormat.TURTLE, SerializationFormat.N_TRIPLES));
	}

	@Test
	void convertGraphContent_withUnsupportedSourceFormat_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> conversionService.convertGraphContent("irrelevant",
				SerializationFormat.CSV, SerializationFormat.N_TRIPLES));
	}

	@Test
	void convertGraphContent_withUnsupportedTargetFormat_throwsIllegalStateException() {
		String turtle = """
				@prefix ex: <http://example.org/> .
				ex:alice ex:knows ex:bob .
				""";

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> conversionService
				.convertGraphContent(turtle, SerializationFormat.TURTLE, SerializationFormat.SPARQL_QUERY));

		assertTrue(exception.getMessage().contains("Failed to format graph"),
				"Unsupported target format should fail during formatting.");
	}

	@Test
	void convertGraphContent_withInvalidSourceContent_throwsRdfConversionException() {
		String invalidTurtle = """
				@prefix ex: <http://example.org/> .
				ex:alice ex:knows ? .
				""";

		assertThrows(RdfConversionException.class, () -> conversionService.convertGraphContent(invalidTurtle,
				SerializationFormat.TURTLE, SerializationFormat.N_TRIPLES));
	}
}
