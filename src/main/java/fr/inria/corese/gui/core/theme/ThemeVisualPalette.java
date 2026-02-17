package fr.inria.corese.gui.core.theme;

import javafx.scene.paint.Color;

/**
 * Theme-dependent visual tokens shared across UI components.
 */
final class ThemeVisualPalette {

	private static final String EDITOR_BG_LIGHT = "#FFFFFF";
	private static final String EDITOR_BG_CUPERTINO_DARK = "#1E1E1E";
	private static final String EDITOR_BG_PRIMER_DARK = "#010409";
	private static final String EDITOR_BG_NORD_DARK = "#242933";

	private static final Palette LIGHT_PALETTE = new Palette(Color.rgb(0, 0, 0, 0.25), Color.rgb(0, 0, 0, 0.16),
			Color.rgb(15, 23, 42, 0.14), Color.rgb(0, 0, 0, 0.1), Color.rgb(0, 0, 0, 0.05), Color.rgb(0, 0, 0, 0.1),
			Color.rgb(0, 0, 0, 0.2));

	private static final Palette DARK_PALETTE = new Palette(Color.rgb(255, 255, 255, 0.25),
			Color.rgb(255, 255, 255, 0.12), Color.rgb(255, 255, 255, 0.1), Color.rgb(0, 0, 0, 0.32),
			Color.rgb(255, 255, 255, 0.06), Color.rgb(255, 255, 255, 0.11), Color.rgb(255, 255, 255, 0.12));

	record Palette(Color logoShadow, Color tabOverflowShadow, Color sidebarSeparator, Color sidebarShadow,
			Color sidebarHover, Color sidebarPressed, Color notificationShadow) {
	}

	private ThemeVisualPalette() {
		// Utility class
	}

	static Palette forDarkMode(boolean dark) {
		return dark ? DARK_PALETTE : LIGHT_PALETTE;
	}

	static String resolveEditorBackgroundHex(AppThemeRegistry appTheme) {
		if (appTheme == null || !appTheme.isDark()) {
			return EDITOR_BG_LIGHT;
		}
		return switch (appTheme.getBaseName()) {
			case "Primer" -> EDITOR_BG_PRIMER_DARK;
			case "Nord" -> EDITOR_BG_NORD_DARK;
			default -> EDITOR_BG_CUPERTINO_DARK;
		};
	}
}
