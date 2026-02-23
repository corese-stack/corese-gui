package fr.inria.corese.gui.core.theme;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Robust system theme and accent color detector for JavaFX.
 *
 * <p>
 * This utility provides cross-platform detection for:
 *
 * <ul>
 * <li><b>Windows:</b> Registry checks for Dark Mode and DWM Accent Color.
 * <li><b>macOS:</b> 'defaults' command for Interface Style and Accent Color.
 * <li><b>Linux:</b> Support for GNOME (gsettings), KDE Plasma (kdeglobals), and
 * generic XDG Desktop Portal (Flatpak/Snap).
 * </ul>
 */
public final class SystemThemeDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemThemeDetector.class);
	private static final long COMMAND_TIMEOUT_SECONDS = 2;

	// ===== OS Detection =====
	private static final String OS = System.getProperty("os.name").toLowerCase();
	private static final boolean IS_WINDOWS = OS.contains("windows");
	private static final boolean IS_MACOS = OS.contains("mac");
	private static final boolean IS_LINUX = OS.contains("linux");

	// ===== Private Constructor =====
	private SystemThemeDetector() {
		throw new UnsupportedOperationException("Utility class");
	}

	// ===== Public API =====

	/**
	 * Detects if the system is currently using a Dark Theme.
	 *
	 * @return true if dark mode is active, false otherwise.
	 */
	public static boolean isSystemDarkTheme() {
		try {
			if (IS_WINDOWS)
				return isWindowsDark();
			if (IS_MACOS)
				return isMacOsDark();
			if (IS_LINUX)
				return isLinuxDark();
		} catch (Exception e) {
			LOGGER.warn("Failed to detect system theme", e);
		}
		return false; // Default to Light
	}

	/**
	 * Retrieves the system's current Accent Color.
	 *
	 * @return The detected Color, or the application's default accent color if
	 *         detection fails.
	 */
	public static Color getSystemAccentColor() {
		try {
			if (IS_WINDOWS)
				return getWindowsAccentColor();
			if (IS_MACOS)
				return getMacOsAccentColor();
			if (IS_LINUX)
				return getLinuxAccentColor();
		} catch (Exception e) {
			LOGGER.warn("Failed to detect system accent color", e);
		}
		return ThemeManager.getDefaultAccentColor();
	}

	/**
	 * Returns a recommended AtlantaFX Theme based on the OS and detected mode.
	 *
	 * @return A Theme instance (Cupertino for Mac, Primer for Windows, Nord for
	 *         Linux).
	 */
	public static Theme getSystemTheme() {
		boolean isDark = isSystemDarkTheme();

		if (IS_MACOS) {
			return isDark ? new CupertinoDark() : new CupertinoLight();
		} else {
			// Windows, Linux (GNOME, KDE, etc.) -> Primer
			return isDark ? new PrimerDark() : new PrimerLight();
		}
	}

	// ==========================================
	// WINDOWS
	// ==========================================

	private static boolean isWindowsDark() {
		// 0 = Dark, 1 = Light
		String result = queryWindowsRegistry("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
				"AppsUseLightTheme");
		return result != null && result.contains("0x0");
	}

	private static Color getWindowsAccentColor() {
		// Returns color in 0xAABBGGRR format (ABGR)
		String result = queryWindowsRegistry("HKCU\\Software\\Microsoft\\Windows\\DWM", "AccentColor");

		if (result != null && result.contains("0x")) {
			try {
				String hex = result.substring(result.indexOf("0x") + 2).trim();
				long value = Long.parseLong(hex, 16);
				// Extract RGB from ABGR
				int r = (int) (value & 0xFF);
				int g = (int) ((value >> 8) & 0xFF);
				int b = (int) ((value >> 16) & 0xFF);
				return Color.rgb(r, g, b);
			} catch (NumberFormatException _) {
				LOGGER.warn("Failed to parse Windows accent color: {}", result);
			}
		}
		return ThemeManager.getDefaultAccentColor();
	}

	private static String queryWindowsRegistry(String path, String key) {
		return executeCommand("reg", "query", path, "/v", key);
	}

	// ==========================================
	// MACOS
	// ==========================================

	private static boolean isMacOsDark() {
		// Returns "Dark" if dark mode is on, otherwise usually empty or "does not
		// exist"
		String result = executeCommand("defaults", "read", "-g", "AppleInterfaceStyle");
		return result != null && result.trim().equalsIgnoreCase("Dark");
	}

	private static Color getMacOsAccentColor() {
		String result = executeCommand("defaults", "read", "-g", "AppleAccentColor");
		if (result != null) {
			try {
				int index = Integer.parseInt(result.trim());
				return switch (index) {
					case -1 -> Color.web("#808080"); // Graphite (Standard Gray)
					case 0 -> Color.web("#C4262C"); // Red
					case 1 -> Color.web("#D96B09"); // Orange
					case 2 -> Color.web("#E2AC15"); // Yellow
					case 3 -> Color.web("#4DA033"); // Green
					case 4 -> Color.web("#0164E1"); // Blue
					case 5 -> Color.web("#7E2A7F"); // Purple
					case 6 -> Color.web("#DA3B87"); // Pink
					default -> Color.web("#0164E1");
				};
			} catch (NumberFormatException _) {
				LOGGER.warn("Failed to parse macOS accent color index: {}", result);
			}
		}
		return Color.web("#007AFF");
	}

	// ==========================================
	// LINUX
	// ==========================================

	private static boolean isLinuxDark() {
		// 1. Try XDG Desktop Portal (Universal for Flatpak/Snap/Modern DEs)
		if (isXdgPortalDark())
			return true;

		// 2. Try GNOME / GTK settings
		if (isGnomeDark())
			return true;

		// 3. Try KDE Plasma settings
		return isKdeDark();
	}

	private static Color getLinuxAccentColor() {
		// 1. Try KDE Plasma (kdeglobals) - often has precise RGB
		Color kdeColor = getKdeAccentColor();
		if (kdeColor != null)
			return kdeColor;

		// 2. Try GNOME (gsettings)
		Color gnomeColor = getGnomeAccentColor();
		if (gnomeColor != null)
			return gnomeColor;

		return ThemeManager.getDefaultAccentColor();
	}

	// --- Linux Helpers ---

	private static boolean isXdgPortalDark() {
		String result = executeCommand("gdbus", "call", "--session", "--dest=org.freedesktop.portal.Desktop",
				"--object-path=/org/freedesktop/portal/desktop", "--method=org.freedesktop.portal.Settings.Read",
				"org.freedesktop.appearance", "color-scheme");

		// 1 = Dark, 2 = Light, 0 = No preference
		return result != null && result.contains("uint32 1");
	}

	private static boolean isGnomeDark() {
		// Modern GNOME (42+)
		String scheme = executeCommand("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
		if (scheme != null && (scheme.contains("prefer-dark") || scheme.contains("dark")))
			return true;

		// Older GNOME / GTK
		String theme = executeCommand("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
		return theme != null && theme.toLowerCase().contains("dark");
	}

	private static boolean isKdeDark() {
		try {
			Path config = Paths.get(System.getProperty("user.home"), ".config", "kdeglobals");
			if (Files.exists(config)) {
				String content = Files.readString(config, StandardCharsets.UTF_8);
				// Look for ColorScheme=...Dark...
				return content.contains("ColorScheme=Breeze Dark") || content.contains("ColorScheme=BreezeDark")
						|| content.toLowerCase().contains("dark");
			}
		} catch (Exception _) {
			// Ignore
		}
		return false;
	}

	private static Color getGnomeAccentColor() {
		String result = executeCommand("gsettings", "get", "org.gnome.desktop.interface", "accent-color");
		if (result != null) {
			String name = result.replaceAll("['\"]", "").trim();
			return switch (name) {
				case "blue" -> Color.web("#3584e4");
				case "teal" -> Color.web("#2190a4");
				case "green" -> Color.web("#3a944c");
				case "yellow" -> Color.web("#e5a50a");
				case "orange" -> Color.web("#e66100");
				case "red" -> Color.web("#c01c28");
				case "pink" -> Color.web("#d56199");
				case "purple" -> Color.web("#9141ac");
				case "slate" -> Color.web("#6f8396");
				default -> null;
			};
		}
		return null;
	}

	private static Color getKdeAccentColor() {
		try {
			Path config = Paths.get(System.getProperty("user.home"), ".config", "kdeglobals");
			if (!Files.exists(config))
				return null;

			return parseKdeGlobals(Files.readAllLines(config, StandardCharsets.UTF_8));
		} catch (Exception e) {
			LOGGER.warn("Failed to parse KDE globals", e);
		}
		return null;
	}

	private static Color parseKdeGlobals(java.util.List<String> lines) {
		KdeColorParser parser = new KdeColorParser();
		for (String line : lines) {
			if (parser.processLine(line)) {
				return parser.getFoundAccentColor();
			}
		}
		return parser.getFallbackColor();
	}

	private static class KdeColorParser {
		private String currentSection = "";
		private Color selectionColor = null;
		private Color headerColor = null;
		private Color foundAccentColor = null;

		public boolean processLine(String line) {
			line = line.trim();
			if (isSectionHeader(line)) {
				currentSection = line;
				return false;
			}
			if (line.contains("=")) {
				Color color = checkKdeLine(currentSection, line);
				if (color != null) {
					if ("[General]".equals(currentSection)) {
						foundAccentColor = color;
						return true;
					}
					if ("[Colors:Selection]".equals(currentSection))
						selectionColor = color;
					else if ("[Colors:Header]".equals(currentSection))
						headerColor = color;
				}
			}
			return false;
		}

		public Color getFoundAccentColor() {
			return foundAccentColor;
		}
		public Color getFallbackColor() {
			return selectionColor != null ? selectionColor : headerColor;
		}

		private boolean isSectionHeader(String line) {
			return line.startsWith("[") && line.endsWith("]");
		}

		private Color checkKdeLine(String section, String line) {
			String[] parts = line.split("=", 2);
			String key = parts[0].trim();
			String value = parts[1].trim();

			if ("[General]".equals(section) && "AccentColor".equals(key)) {
				return parseKdeRgb(value);
			}
			if ("[Colors:Selection]".equals(section) && "BackgroundNormal".equals(key)) {
				return parseKdeRgb(value);
			}
			if ("[Colors:Header]".equals(section) && "BackgroundNormal".equals(key)) {
				return parseKdeRgb(value);
			}
			return null;
		}

		private Color parseKdeRgb(String value) {
			try {
				String[] components = value.split(",");
				if (components.length >= 3) {
					int r = Integer.parseInt(components[0].trim());
					int g = Integer.parseInt(components[1].trim());
					int b = Integer.parseInt(components[2].trim());
					return Color.rgb(r, g, b);
				}
			} catch (Exception _) {
				// ignore
			}
			return null;
		}
	}

	// ==========================================
	// COMMON UTILS
	// ==========================================

	private static String executeCommand(String... command) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			// Read output
			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line);
				}
			}

			boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Command timed out: {}", String.join(" ", command));
				}
				return null;
			}
			String result = output.toString().trim();
			return result.isEmpty() ? null : result;
		} catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception _) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Command execution failed: {}", String.join(" ", command));
			}
			return null;
		}
	}
}
