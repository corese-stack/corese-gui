package fr.inria.corese.gui.core.shortcut;

import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Central registry for keyboard shortcuts used by the application.
 *
 * <p>
 * Shortcuts are defined once here and reused by:
 * <ul>
 * <li>the global shortcut dispatcher in {@code MainController}</li>
 * <li>the shortcuts list displayed in the Settings page</li>
 * </ul>
 */
public final class KeyboardShortcutRegistry {

	/** Shortcut scope level. */
	public enum Scope {
		GLOBAL("Global"), CONTEXTUAL("Contextual");

		private final String label;

		Scope(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	/** Internal action identifiers dispatched by the main controller. */
	public enum Action {
		OPEN_DATA_VIEW, OPEN_QUERY_VIEW, OPEN_VALIDATION_VIEW, OPEN_SYSTEM_LOGS_VIEW, OPEN_SETTINGS_VIEW, TOGGLE_NAVIGATION_BAR, GLOBAL_ZOOM_IN, GLOBAL_ZOOM_OUT, GLOBAL_ZOOM_RESET, OPEN_FILE, SAVE_EDITOR, EXPORT_CONTEXT, EXPORT_GRAPH, OPEN_TEMPLATE, CREATE_TAB, CLOSE_TAB, NEXT_TAB, PREVIOUS_TAB, EXECUTE_PRIMARY_ACTION, FOCUS_EDITOR, DATA_LOAD_URI, DATA_RELOAD_SOURCES, DATA_CLEAR_GRAPH, GRAPH_REENERGIZE_LAYOUT, GRAPH_CENTER_VIEW
	}

	/**
	 * Shortcut metadata and key combination binding.
	 *
	 * @param category
	 *            display category used in Settings
	 * @param keyTokens
	 *            key tokens (example: MOD, Shift, T)
	 * @param description
	 *            user-facing action description
	 * @param availability
	 *            where shortcut applies (Global / Data / Query...)
	 * @param combination
	 *            JavaFX key combination used for matching
	 * @param scope
	 *            scope level (global vs contextual)
	 * @param action
	 *            action identifier handled by main controller
	 */
	public record Shortcut(String category, List<String> keyTokens, String description, String availability,
			KeyCombination combination, Scope scope, Action action) {

		public Shortcut {
			category = requireNonBlank(category, "category");
			keyTokens = keyTokens == null
					? List.of()
					: keyTokens.stream().filter(token -> token != null && !token.isBlank()).map(String::trim).toList();
			if (keyTokens.isEmpty()) {
				throw new IllegalArgumentException("keyTokens must not be empty");
			}
			description = requireNonBlank(description, "description");
			availability = requireNonBlank(availability, "availability");
			Objects.requireNonNull(combination, "combination");
			Objects.requireNonNull(scope, "scope");
			Objects.requireNonNull(action, "action");
		}
	}

	private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
	private static final String TOKEN_MOD = "MOD";

	private static final List<Shortcut> SHORTCUTS = List.of(
			// Global navigation
			global("Navigation", keys(TOKEN_MOD, "1"), "Open Data page",
					new KeyCodeCombination(KeyCode.DIGIT1, SHORTCUT_DOWN), Action.OPEN_DATA_VIEW),
			global("Navigation", keys(TOKEN_MOD, "1"), "Open Data page",
					new KeyCharacterCombination("1", SHORTCUT_DOWN), Action.OPEN_DATA_VIEW),
			global("Navigation", keys(TOKEN_MOD, "2"), "Open Query page",
					new KeyCodeCombination(KeyCode.DIGIT2, SHORTCUT_DOWN), Action.OPEN_QUERY_VIEW),
			global("Navigation", keys(TOKEN_MOD, "2"), "Open Query page",
					new KeyCharacterCombination("2", SHORTCUT_DOWN), Action.OPEN_QUERY_VIEW),
			global("Navigation", keys(TOKEN_MOD, "3"), "Open Validation page",
					new KeyCodeCombination(KeyCode.DIGIT3, SHORTCUT_DOWN), Action.OPEN_VALIDATION_VIEW),
			global("Navigation", keys(TOKEN_MOD, "3"), "Open Validation page",
					new KeyCharacterCombination("3", SHORTCUT_DOWN), Action.OPEN_VALIDATION_VIEW),
			global("Navigation", keys(TOKEN_MOD, "4"), "Open System Logs page",
					new KeyCodeCombination(KeyCode.DIGIT4, SHORTCUT_DOWN), Action.OPEN_SYSTEM_LOGS_VIEW),
			global("Navigation", keys(TOKEN_MOD, "4"), "Open System Logs page",
					new KeyCharacterCombination("4", SHORTCUT_DOWN), Action.OPEN_SYSTEM_LOGS_VIEW),
			global("Navigation", keys(TOKEN_MOD, "5"), "Open Settings page",
					new KeyCodeCombination(KeyCode.DIGIT5, SHORTCUT_DOWN), Action.OPEN_SETTINGS_VIEW),
			global("Navigation", keys(TOKEN_MOD, "5"), "Open Settings page",
					new KeyCharacterCombination("5", SHORTCUT_DOWN), Action.OPEN_SETTINGS_VIEW),
			global("Navigation", keys(TOKEN_MOD, ","), "Open Settings page",
					new KeyCharacterCombination(",", SHORTCUT_DOWN), Action.OPEN_SETTINGS_VIEW),
			global("Navigation", keys(TOKEN_MOD, "B"), "Toggle navigation bar",
					new KeyCodeCombination(KeyCode.B, SHORTCUT_DOWN), Action.TOGGLE_NAVIGATION_BAR),
			global("Navigation", keys(TOKEN_MOD, "\\"), "Toggle navigation bar",
					new KeyCodeCombination(KeyCode.BACK_SLASH, SHORTCUT_DOWN), Action.TOGGLE_NAVIGATION_BAR),

			// Global zoom
			global("Zoom", keys(TOKEN_MOD, "="), "Zoom in the whole application",
					new KeyCodeCombination(KeyCode.EQUALS, SHORTCUT_DOWN), Action.GLOBAL_ZOOM_IN),
			global("Zoom", keys(TOKEN_MOD, "+"), "Zoom in the whole application",
					new KeyCharacterCombination("+", SHORTCUT_DOWN), Action.GLOBAL_ZOOM_IN),
			global("Zoom", keys(TOKEN_MOD, "-"), "Zoom out the whole application",
					new KeyCodeCombination(KeyCode.MINUS, SHORTCUT_DOWN), Action.GLOBAL_ZOOM_OUT),
			global("Zoom", keys(TOKEN_MOD, "-"), "Zoom out the whole application",
					new KeyCharacterCombination("-", SHORTCUT_DOWN), Action.GLOBAL_ZOOM_OUT),
			global("Zoom", keys(TOKEN_MOD, "0"), "Reset application zoom",
					new KeyCodeCombination(KeyCode.DIGIT0, SHORTCUT_DOWN), Action.GLOBAL_ZOOM_RESET),
			global("Zoom", keys(TOKEN_MOD, "0"), "Reset application zoom",
					new KeyCharacterCombination("0", SHORTCUT_DOWN), Action.GLOBAL_ZOOM_RESET),

			// File loading shortcuts
			contextual("Files", keys(TOKEN_MOD, "O"), "Open file(s)", "Data / Query / Validation",
					new KeyCodeCombination(KeyCode.O, SHORTCUT_DOWN), Action.OPEN_FILE),
			contextual("Files", keys(TOKEN_MOD, "S"), "Save current editor file", "Query / Validation",
					new KeyCodeCombination(KeyCode.S, SHORTCUT_DOWN), Action.SAVE_EDITOR),
			contextual("Files", keys(TOKEN_MOD, "Shift", "S"), "Export current content/result",
					"Data / Query / Validation", new KeyCodeCombination(KeyCode.S, SHORTCUT_DOWN, SHIFT_DOWN),
					Action.EXPORT_CONTEXT),
			contextual("Files", keys(TOKEN_MOD, "Shift", "G"), "Export current graph view", "Data / Query Results",
					new KeyCodeCombination(KeyCode.G, SHORTCUT_DOWN, SHIFT_DOWN), Action.EXPORT_GRAPH),

			// Query / Validation shortcuts
			contextual("Editor Tabs", keys(TOKEN_MOD, "T"), "Create a new editor tab", "Query / Validation",
					new KeyCodeCombination(KeyCode.T, SHORTCUT_DOWN), Action.CREATE_TAB),
			contextual("Editor Tabs", keys(TOKEN_MOD, "W"), "Close current editor tab", "Query / Validation",
					new KeyCodeCombination(KeyCode.W, SHORTCUT_DOWN), Action.CLOSE_TAB),
			contextual("Editor Tabs", keys(TOKEN_MOD, "Tab"), "Select next tab", "Query / Validation",
					new KeyCodeCombination(KeyCode.TAB, SHORTCUT_DOWN), Action.NEXT_TAB),
			contextual("Editor Tabs", keys(TOKEN_MOD, "Shift", "Tab"), "Select previous tab", "Query / Validation",
					new KeyCodeCombination(KeyCode.TAB, SHORTCUT_DOWN, SHIFT_DOWN), Action.PREVIOUS_TAB),
			contextual("Editor", keys(TOKEN_MOD, "Enter"), "Run query or validation", "Query / Validation",
					new KeyCodeCombination(KeyCode.ENTER, SHORTCUT_DOWN), Action.EXECUTE_PRIMARY_ACTION),
			contextual("Editor", keys(TOKEN_MOD, "E"), "Focus editor", "Query / Validation",
					new KeyCodeCombination(KeyCode.E, SHORTCUT_DOWN), Action.FOCUS_EDITOR),
			contextual("Templates", keys(TOKEN_MOD, "Shift", "T"), "Open templates", "Query / Validation",
					new KeyCodeCombination(KeyCode.T, SHORTCUT_DOWN, SHIFT_DOWN), Action.OPEN_TEMPLATE),

			// Data and graph shortcuts
			contextual("Data Graph", keys(TOKEN_MOD, "U"), "Load RDF from URI(s)", "Data",
					new KeyCodeCombination(KeyCode.U, SHORTCUT_DOWN), Action.DATA_LOAD_URI),
			contextual("Data Graph", keys(TOKEN_MOD, "Shift", "R"), "Reload tracked data sources", "Data",
					new KeyCodeCombination(KeyCode.R, SHORTCUT_DOWN, SHIFT_DOWN), Action.DATA_RELOAD_SOURCES),
			contextual("Data Graph", keys(TOKEN_MOD, "Shift", "Delete"), "Clear graph", "Data",
					new KeyCodeCombination(KeyCode.DELETE, SHORTCUT_DOWN, SHIFT_DOWN), Action.DATA_CLEAR_GRAPH),
			contextual("Graph Views", keys(TOKEN_MOD, "Shift", "L"), "Re-energize graph layout", "Data / Query Results",
					new KeyCodeCombination(KeyCode.L, SHORTCUT_DOWN, SHIFT_DOWN), Action.GRAPH_REENERGIZE_LAYOUT),
			contextual("Graph Views", keys(TOKEN_MOD, "Shift", "C"), "Center graph view", "Data / Query Results",
					new KeyCodeCombination(KeyCode.C, SHORTCUT_DOWN, SHIFT_DOWN), Action.GRAPH_CENTER_VIEW));

	private KeyboardShortcutRegistry() {
		// Utility class
	}

	public static List<Shortcut> shortcuts() {
		return SHORTCUTS;
	}

	public static List<String> displayKeyTokens(Shortcut shortcut) {
		if (shortcut == null) {
			return List.of();
		}
		List<String> parsedDisplayTokens = tokensFromCombination(shortcut.combination());
		int expectedTokenCount = shortcut.keyTokens() == null ? 0 : shortcut.keyTokens().size();
		if (!parsedDisplayTokens.isEmpty() && (parsedDisplayTokens.size() >= 2 || expectedTokenCount <= 1)) {
			return parsedDisplayTokens;
		}
		if (shortcut.keyTokens() == null || shortcut.keyTokens().isEmpty()) {
			return parsedDisplayTokens;
		}
		return shortcut.keyTokens().stream().map(KeyboardShortcutRegistry::resolveTokenPlaceholder).toList();
	}

	public static String primaryDisplayLabel(Action action) {
		return displayLabels(action).stream().findFirst().orElse("");
	}

	public static List<String> displayLabels(Action action) {
		if (action == null) {
			return List.of();
		}
		LinkedHashSet<String> labels = new LinkedHashSet<>();
		for (Shortcut shortcut : SHORTCUTS) {
			if (shortcut.action() != action) {
				continue;
			}
			String label = String.join("+", displayKeyTokens(shortcut));
			if (!label.isBlank()) {
				labels.add(label);
			}
		}
		return List.copyOf(labels);
	}

	private static List<String> tokensFromCombination(KeyCombination combination) {
		if (combination == null) {
			return List.of();
		}
		String displayText = combination.getDisplayText();
		if (displayText == null || displayText.isBlank()) {
			return List.of();
		}
		String normalized = displayText.replace("\u2318", "Cmd+").replace("\u21E7", "Shift+").replace("\u2325", "Alt+")
				.replace("\u2303", "Ctrl+").replace("Shortcut", IS_MAC ? "Cmd" : "Ctrl").replace("Control", "Ctrl")
				.replace("Meta", "Cmd").replace("Command", "Cmd");
		return java.util.Arrays.stream(normalized.split("\\s*\\+\\s*")).map(String::trim)
				.filter(token -> !token.isBlank()).toList();
	}

	private static String resolveTokenPlaceholder(String token) {
		if (token == null || token.isBlank()) {
			return "";
		}
		return switch (token) {
			case TOKEN_MOD -> IS_MAC ? "Cmd" : "Ctrl";
			default -> token;
		};
	}

	private static Shortcut global(String category, List<String> keyTokens, String description,
			KeyCombination combination, Action action) {
		return new Shortcut(category, keyTokens, description, Scope.GLOBAL.label(), combination, Scope.GLOBAL, action);
	}

	private static Shortcut contextual(String category, List<String> keyTokens, String description, String availability,
			KeyCombination combination, Action action) {
		return new Shortcut(category, keyTokens, description, availability, combination, Scope.CONTEXTUAL, action);
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}

	private static List<String> keys(String... tokens) {
		return tokens == null ? List.of() : List.of(tokens);
	}
}
