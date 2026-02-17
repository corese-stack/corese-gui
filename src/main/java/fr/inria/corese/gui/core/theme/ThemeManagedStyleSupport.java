package fr.inria.corese.gui.core.theme;

import java.util.Locale;
import javafx.scene.paint.Color;

/**
 * Utilities to generate and merge managed inline CSS blocks.
 */
final class ThemeManagedStyleSupport {

	private ThemeManagedStyleSupport() {
		// Utility class
	}

	static String buildManagedAccentStyle(Color accentColor, String defaultAccentHex, Color defaultAccentColor,
			ThemeVisualPalette.Palette palette) {
		Color safeAccent = accentColor == null ? defaultAccentColor : accentColor;
		String cssColor = toCssColor(safeAccent, defaultAccentHex);

		return String.format(
				"-color-accent-emphasis: %s; " + "-color-accent-fg: %s; " + "-color-accent-subtle: %s; "
						+ "-color-accent-muted: %s; " + "-color-logo-shadow: %s; " + "-color-tab-overflow-shadow: %s; "
						+ "-color-tab-overflow-shadow-transparent: %s; " + "-color-sidebar-separator: %s; "
						+ "-color-sidebar-shadow: %s; " + "-color-sidebar-hover: %s; " + "-color-sidebar-pressed: %s; "
						+ "-color-notification-shadow: %s;",
				cssColor, cssColor, toCssColor(safeAccent.deriveColor(0, 0.3, 1.0, 0.3), defaultAccentHex),
				toCssColor(safeAccent.deriveColor(0, 0.5, 1.0, 0.5), defaultAccentHex),
				toCssRgbaColor(palette.logoShadow(), defaultAccentColor),
				toCssRgbaColor(palette.tabOverflowShadow(), defaultAccentColor),
				toCssRgbaColor(withOpacity(palette.tabOverflowShadow(), 0.0), defaultAccentColor),
				toCssRgbaColor(palette.sidebarSeparator(), defaultAccentColor),
				toCssRgbaColor(palette.sidebarShadow(), defaultAccentColor),
				toCssRgbaColor(palette.sidebarHover(), defaultAccentColor),
				toCssRgbaColor(palette.sidebarPressed(), defaultAccentColor),
				toCssRgbaColor(palette.notificationShadow(), defaultAccentColor));
	}

	static String toCssColor(Color color, String defaultHex) {
		if (color == null) {
			return defaultHex;
		}
		return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
				(int) (color.getBlue() * 255));
	}

	static String toCssRgbaColor(Color color, Color fallbackColor) {
		Color safeColor = color == null ? fallbackColor : color;
		if (safeColor == null) {
			safeColor = Color.TRANSPARENT;
		}
		int red = (int) Math.round(safeColor.getRed() * 255);
		int green = (int) Math.round(safeColor.getGreen() * 255);
		int blue = (int) Math.round(safeColor.getBlue() * 255);
		return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.3f)", red, green, blue, safeColor.getOpacity());
	}

	static Color withOpacity(Color color, double opacity) {
		if (color == null) {
			return Color.TRANSPARENT;
		}
		double clampedOpacity = Math.max(0.0, Math.min(1.0, opacity));
		return Color.color(color.getRed(), color.getGreen(), color.getBlue(), clampedOpacity);
	}

	static String stripManagedStyle(String currentStyle, String managedStyle) {
		String style = currentStyle == null ? "" : currentStyle.trim();
		if (managedStyle == null || managedStyle.isBlank()) {
			return style;
		}
		String cleaned = style.replace(managedStyle, "").trim();
		while (cleaned.contains(";;")) {
			cleaned = cleaned.replace(";;", ";");
		}
		if (cleaned.startsWith(";")) {
			cleaned = cleaned.substring(1).trim();
		}
		if (cleaned.endsWith(";")) {
			cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
		}
		return cleaned;
	}

	static String mergeStyle(String baseStyle, String managedStyle) {
		if (baseStyle == null || baseStyle.isBlank()) {
			return managedStyle;
		}
		String normalizedBase = baseStyle.trim();
		if (!normalizedBase.endsWith(";")) {
			normalizedBase += ";";
		}
		return normalizedBase + " " + managedStyle;
	}
}
