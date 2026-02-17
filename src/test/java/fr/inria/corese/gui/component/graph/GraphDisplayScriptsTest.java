package fr.inria.corese.gui.component.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.inria.corese.gui.core.theme.ThemeManager;
import org.junit.jupiter.api.Test;

class GraphDisplayScriptsTest {

	@Test
	void escapeForJsSingleQuoted_escapesBackslashesAndQuotes() {
		assertEquals("", GraphDisplayScripts.escapeForJsSingleQuoted(null));
		assertEquals("", GraphDisplayScripts.escapeForJsSingleQuoted(""));
		assertEquals("abc", GraphDisplayScripts.escapeForJsSingleQuoted("abc"));
		assertEquals("a\\'b\\\\c", GraphDisplayScripts.escapeForJsSingleQuoted("a'b\\c"));
	}

	@Test
	void buildThemeScript_escapesThemePayload() {
		ThemeManager.WebThemeInfo info = new ThemeManager.WebThemeInfo(true, "#12'34", "nord\\dark");
		String script = GraphDisplayScripts.buildThemeScript(info);

		assertTrue(script.contains("window.setTheme(true"),
				"Theme script should encode dark mode flag.");
		assertTrue(script.contains("#12\\'34"),
				"Theme script should escape single quotes in accent payload.");
		assertTrue(script.contains("nord\\\\dark"),
				"Theme script should escape backslashes in theme name.");
	}

	@Test
	void buildGraphCommandScript_embedsTargetElementAndCommand() {
		String script = GraphDisplayScripts.buildGraphCommandScript("my'Graph", "el.recenter();");
		assertTrue(script.contains("document.getElementById('my\\'Graph')"));
		assertTrue(script.contains("el.recenter();"));
	}
}
