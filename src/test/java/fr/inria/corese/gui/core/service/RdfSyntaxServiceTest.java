package fr.inria.corese.gui.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RdfSyntaxServiceTest {

	private final RdfSyntaxService syntaxService = RdfSyntaxService.getInstance();

	@Test
	void checkTurtle_withValidContent_returnsValidResult() {
		String turtle = """
				@prefix ex: <http://example.org/> .
				ex:alice ex:knows ex:bob .
				""";

		RdfSyntaxService.CheckResult result = syntaxService.checkTurtle(turtle);

		assertTrue(result.valid(), "Valid Turtle should pass syntax validation.");
		assertNull(result.message(), "Valid Turtle should not produce an error message.");
	}

	@Test
	void checkTurtle_withInvalidContent_returnsSyntaxError() {
		String invalidTurtle = """
				@prefix ex: <http://example.org/> .
				ex:alice ex:knows ? .
				""";

		RdfSyntaxService.CheckResult result = syntaxService.checkTurtle(invalidTurtle);

		assertFalse(result.valid(), "Invalid Turtle should fail syntax validation.");
		assertTrue(result.message() != null && result.message().contains("syntax error"),
				"Invalid Turtle should return a user-facing syntax error message.");
	}

	@Test
	void checkTurtle_withBlankContent_returnsEmptyContentError() {
		RdfSyntaxService.CheckResult result = syntaxService.checkTurtle("   ");

		assertFalse(result.valid(), "Blank content should fail syntax validation.");
		assertTrue(result.message() != null && result.message().contains("Content is empty"),
				"Blank content should produce an explicit empty-content message.");
	}
}
