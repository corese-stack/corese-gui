package fr.inria.corese.gui.core.shortcut;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardShortcutRegistryTest {

	@Test
	void shortcuts_defineUniqueKeyCombinations() {
		List<KeyboardShortcutRegistry.Shortcut> shortcuts = KeyboardShortcutRegistry.shortcuts();
		assertFalse(shortcuts.isEmpty(), "At least one keyboard shortcut should be registered.");

		Set<String> seenCombinations = new HashSet<>();
		for (KeyboardShortcutRegistry.Shortcut shortcut : shortcuts) {
			String combinationKey = shortcut.combination().toString();
			assertTrue(seenCombinations.add(combinationKey), () -> "Duplicate key combination detected: "
					+ shortcut.category() + " / " + shortcut.description());
		}
	}

	@Test
	void displayKeyTokens_resolvesPlatformModifierToken() {
		KeyboardShortcutRegistry.Shortcut navigationShortcut = KeyboardShortcutRegistry.shortcuts().stream()
				.filter(shortcut -> shortcut.action() == KeyboardShortcutRegistry.Action.OPEN_QUERY_VIEW).findFirst()
				.orElseThrow();

		List<String> tokens = KeyboardShortcutRegistry.displayKeyTokens(navigationShortcut);
		assertFalse(tokens.isEmpty(), "Resolved shortcut tokens should not be empty.");
		assertFalse(tokens.contains("MOD"), "Internal MOD token should be resolved before display.");
		assertTrue(tokens.contains("Ctrl") || tokens.contains("Cmd"),
				"Resolved tokens should expose Ctrl on non-macOS or Cmd on macOS.");
	}

	@Test
	void primaryDisplayLabel_returnsResolvedShortcutLabel() {
		String label = KeyboardShortcutRegistry
				.primaryDisplayLabel(KeyboardShortcutRegistry.Action.GRAPH_REENERGIZE_LAYOUT);
		assertFalse(label.isBlank(), "Primary shortcut label should be available.");
		assertNotEquals("MOD+Shift+L", label, "Primary shortcut label must resolve platform modifier placeholder.");
	}
}
