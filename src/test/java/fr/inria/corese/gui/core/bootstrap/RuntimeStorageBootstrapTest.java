package fr.inria.corese.gui.core.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeStorageBootstrapTest {

	@Test
	void resolveLayoutUsesWindowsAppDataConventions() {
		Path userHome = Path.of("/users/tester");
		Map<String, String> env = Map.of("APPDATA", "/env/roaming", "LOCALAPPDATA", "/env/local");

		RuntimeStorageBootstrap.StorageLayout layout = RuntimeStorageBootstrap.resolveLayout("Windows 11", userHome,
				env, Map.of());

		assertEquals(Path.of("/env/local/Corese GUI/logs").toAbsolutePath().normalize(), layout.logDirectory());
		assertEquals(Path.of("/env/roaming/Corese GUI/preferences").toAbsolutePath().normalize(),
				layout.preferencesDirectory());
		assertEquals(Path.of("/env/roaming/Corese GUI/preferences/preferences.properties").toAbsolutePath().normalize(),
				layout.preferencesFile());
	}

	@Test
	void resolveLayoutUsesLinuxXdgFallbacksWhenEnvIsMissing() {
		Path userHome = Path.of("/home/tester");

		RuntimeStorageBootstrap.StorageLayout layout = RuntimeStorageBootstrap.resolveLayout("Linux", userHome,
				Map.of(), Map.of());

		assertEquals(Path.of("/home/tester/.local/state/corese-gui/logs").toAbsolutePath().normalize(),
				layout.logDirectory());
		assertEquals(Path.of("/home/tester/.config/corese-gui/preferences").toAbsolutePath().normalize(),
				layout.preferencesDirectory());
		assertEquals(Path.of("/home/tester/.config/corese-gui/preferences/preferences.properties").toAbsolutePath()
				.normalize(), layout.preferencesFile());
	}

	@Test
	void resolveLayoutIgnoresInvalidRelativeLinuxXdgPaths() {
		Path userHome = Path.of("/home/tester");
		Map<String, String> env = Map.of("XDG_STATE_HOME", "$XDG_STATE_HOME/corese-gui", "XDG_CONFIG_HOME",
				"relative/config");

		RuntimeStorageBootstrap.StorageLayout layout = RuntimeStorageBootstrap.resolveLayout("Linux", userHome, env,
				Map.of());

		assertEquals(Path.of("/home/tester/.local/state/corese-gui/logs").toAbsolutePath().normalize(),
				layout.logDirectory());
		assertEquals(Path.of("/home/tester/.config/corese-gui/preferences").toAbsolutePath().normalize(),
				layout.preferencesDirectory());
	}

	@Test
	void resolveLayoutUsesMacConventions() {
		Path userHome = Path.of("/Users/tester");

		RuntimeStorageBootstrap.StorageLayout layout = RuntimeStorageBootstrap.resolveLayout("Mac OS X", userHome,
				Map.of(), Map.of());

		assertEquals(Path.of("/Users/tester/Library/Logs/Corese GUI").toAbsolutePath().normalize(),
				layout.logDirectory());
		assertEquals(Path.of("/Users/tester/Library/Application Support/Corese GUI/preferences").toAbsolutePath()
				.normalize(), layout.preferencesDirectory());
		assertEquals(Path.of("/Users/tester/Library/Application Support/Corese GUI/preferences/preferences.properties")
				.toAbsolutePath().normalize(), layout.preferencesFile());
	}

	@Test
	void resolveLayoutHonorsSystemPropertyOverrides() {
		Path userHome = Path.of("/home/tester");
		Map<String, String> env = Map.of();
		Map<String, String> properties = Map.of(RuntimeStorageBootstrap.LOG_DIR_PROPERTY, "/tmp/corese/logs",
				RuntimeStorageBootstrap.PREFERENCES_FILE_PROPERTY, "/tmp/corese/prefs/custom.properties");

		RuntimeStorageBootstrap.StorageLayout layout = RuntimeStorageBootstrap.resolveLayout("Linux", userHome, env,
				properties);

		assertEquals(Path.of("/tmp/corese/logs").toAbsolutePath().normalize(), layout.logDirectory());
		assertEquals(Path.of("/tmp/corese/prefs").toAbsolutePath().normalize(), layout.preferencesDirectory());
		assertEquals(Path.of("/tmp/corese/prefs/custom.properties").toAbsolutePath().normalize(),
				layout.preferencesFile());
	}
}
