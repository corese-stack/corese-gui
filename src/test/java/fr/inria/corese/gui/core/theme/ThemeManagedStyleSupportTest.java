package fr.inria.corese.gui.core.theme;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeManagedStyleSupportTest {

	@Test
	void toCssColor_returnsDefaultWhenNull() {
		assertEquals("#123456", ThemeManagedStyleSupport.toCssColor(null, "#123456"));
	}

	@Test
	void toCssColor_formatsRgbHex() {
		assertEquals("#112233", ThemeManagedStyleSupport.toCssColor(Color.rgb(17, 34, 51), "#000000"));
	}

	@Test
	void toCssRgbaColor_formatsAlphaChannel() {
		String css = ThemeManagedStyleSupport.toCssRgbaColor(Color.rgb(255, 0, 0, 0.5), Color.BLACK);
		assertEquals("rgba(255, 0, 0, 0.500)", css);
	}

	@Test
	void stripAndMergeStyle_keepBaseStyleClean() {
		String base = ThemeManagedStyleSupport.stripManagedStyle("a:b; -x:y; c:d;", "-x:y;");
		assertEquals("a:b; c:d", base.replaceAll("\\s+", " ").trim());
		assertEquals("a:b; c:d; -x:y;",
				ThemeManagedStyleSupport.mergeStyle(base, "-x:y;").replaceAll("\\s+", " ").trim());
	}

	@Test
	void buildManagedAccentStyle_containsExpectedTokens() {
		ThemeVisualPalette.Palette palette = ThemeVisualPalette.forDarkMode(true);
		String style = ThemeManagedStyleSupport.buildManagedAccentStyle(Color.web("#336699"), "#0078D4",
				Color.web("#0078D4"), palette);

		assertTrue(style.contains("-color-accent-fg: #336699;"));
		assertTrue(style.contains("-color-tab-overflow-shadow-transparent: rgba("));
		assertTrue(style.contains("-color-notification-shadow: rgba("));
	}
}
