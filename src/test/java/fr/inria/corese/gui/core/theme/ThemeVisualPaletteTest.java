package fr.inria.corese.gui.core.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThemeVisualPaletteTest {

	@Test
	void resolveEditorBackgroundHex_mapsKnownDarkThemes() {
		assertEquals("#010409", ThemeVisualPalette.resolveEditorBackgroundHex(AppThemeRegistry.PRIMER_DARK));
		assertEquals("#242933", ThemeVisualPalette.resolveEditorBackgroundHex(AppThemeRegistry.NORD_DARK));
		assertEquals("#1E1E1E", ThemeVisualPalette.resolveEditorBackgroundHex(AppThemeRegistry.CUPERTINO_DARK));
	}

	@Test
	void resolveEditorBackgroundHex_defaultsToLightBackground() {
		assertEquals("#FFFFFF", ThemeVisualPalette.resolveEditorBackgroundHex(null));
		assertEquals("#FFFFFF", ThemeVisualPalette.resolveEditorBackgroundHex(AppThemeRegistry.PRIMER_LIGHT));
	}

	@Test
	void forDarkMode_returnsDistinctPalette() {
		ThemeVisualPalette.Palette light = ThemeVisualPalette.forDarkMode(false);
		ThemeVisualPalette.Palette dark = ThemeVisualPalette.forDarkMode(true);

		assertEquals(0.25, light.logoShadow().getOpacity(), 0.0001);
		assertEquals(0.25, dark.logoShadow().getOpacity(), 0.0001);
		assertEquals(0.0, light.logoShadow().getRed(), 0.0001);
		assertEquals(1.0, dark.logoShadow().getRed(), 0.0001);
	}
}
