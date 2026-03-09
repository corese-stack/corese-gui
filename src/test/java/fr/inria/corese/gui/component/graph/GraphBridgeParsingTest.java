package fr.inria.corese.gui.component.graph;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphBridgeParsingTest {

	@Test
	void parseNonNegativeInt_supportsNumbersAndStrings() {
		assertEquals(0, GraphBridgeParsing.parseNonNegativeInt((Object) null));
		assertEquals(12, GraphBridgeParsing.parseNonNegativeInt((Object) Integer.valueOf(12)));
		assertEquals(0, GraphBridgeParsing.parseNonNegativeInt((Object) Integer.valueOf(-3)));
		assertEquals(7, GraphBridgeParsing.parseNonNegativeInt((Object) "7"));
		assertEquals(4, GraphBridgeParsing.parseNonNegativeInt((Object) "4.9"));
		assertEquals(0, GraphBridgeParsing.parseNonNegativeInt((Object) "-2"));
		assertEquals(0, GraphBridgeParsing.parseNonNegativeInt((Object) "not-a-number"));
	}

	@Test
	void parseNonNegativeInt_stringOverload_handlesEdgeCases() {
		assertEquals(0, GraphBridgeParsing.parseNonNegativeInt((String) null));
		assertEquals(0, GraphBridgeParsing.parseNonNegativeInt("  "));
		assertEquals(15, GraphBridgeParsing.parseNonNegativeInt(" 15 "));
		assertEquals(0, GraphBridgeParsing.parseNonNegativeInt("-15"));
		assertEquals(2, GraphBridgeParsing.parseNonNegativeInt("2.99"));
	}

	@Test
	void isCurrentRenderRequest_isDeterministic() {
		assertTrue(GraphBridgeParsing.isCurrentRenderRequest("42", 42L));
		assertFalse(GraphBridgeParsing.isCurrentRenderRequest("43", 42L));
		assertFalse(GraphBridgeParsing.isCurrentRenderRequest("", 42L));
		assertFalse(GraphBridgeParsing.isCurrentRenderRequest("abc", 42L));
		assertFalse(GraphBridgeParsing.isCurrentRenderRequest(null, 42L));
	}

	@Test
	void parseBoolean_supportsCommonTruthyValues() {
		assertTrue(GraphBridgeParsing.parseBoolean(Boolean.TRUE));
		assertTrue(GraphBridgeParsing.parseBoolean("true"));
		assertTrue(GraphBridgeParsing.parseBoolean("1"));
		assertTrue(GraphBridgeParsing.parseBoolean("yes"));
		assertTrue(GraphBridgeParsing.parseBoolean("on"));
		assertFalse(GraphBridgeParsing.parseBoolean(Boolean.FALSE));
		assertFalse(GraphBridgeParsing.parseBoolean("0"));
		assertFalse(GraphBridgeParsing.parseBoolean("off"));
		assertFalse(GraphBridgeParsing.parseBoolean(null));
	}

	@Test
	void parseStringList_supportsMultilinePayload() {
		List<String> lines = GraphBridgeParsing.parseStringList("first line\nsecond line\r\n third line ");

		assertEquals(List.of("first line", "second line", "third line"), lines);
	}

	@Test
	void parseStringList_keepsSingleLinePayload() {
		List<String> lines = GraphBridgeParsing.parseStringList("single line");

		assertEquals(List.of("single line"), lines);
	}
}
