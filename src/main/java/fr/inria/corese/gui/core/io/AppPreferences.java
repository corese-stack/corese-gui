package fr.inria.corese.gui.core.io;

import fr.inria.corese.gui.core.bootstrap.RuntimeStorageBootstrap;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * File-backed preference store used by Corese GUI.
 *
 * <p>
 * Values are persisted in a single properties file under a per-OS runtime
 * directory resolved at bootstrap. Legacy {@link Preferences} values are read
 * on-demand and migrated transparently.
 */
public final class AppPreferences {

	private static final String PREFERENCES_DIR_PROPERTY = "corese.preferences.dir";
	private static final String PREFERENCES_FILE_PROPERTY = "corese.preferences.file";
	private static final String DEFAULT_PREFERENCES_FILE_NAME = "preferences.properties";
	private static final String FILE_HEADER = "Corese GUI preferences";

	private static final Object LOCK = new Object();
	private static final Properties VALUES = new Properties();
	private static Path preferencesFile;
	private static boolean loaded;

	private AppPreferences() {
		throw new AssertionError("Utility class - do not instantiate");
	}

	/**
	 * Returns a namespaced preference node for the given class.
	 */
	public static Node nodeForClass(Class<?> ownerClass) {
		Objects.requireNonNull(ownerClass, "ownerClass");
		Preferences legacyNode = null;
		try {
			legacyNode = Preferences.userNodeForPackage(ownerClass);
		} catch (RuntimeException _) {
			legacyNode = null;
		}
		return new Node(ownerClass.getName(), legacyNode);
	}

	/**
	 * Returns a namespaced preference node.
	 */
	public static Node node(String namespace) {
		return new Node(namespace, null);
	}

	/**
	 * Returns a namespaced preference node with an explicit legacy fallback node.
	 */
	public static Node node(String namespace, Preferences legacyNode) {
		return new Node(namespace, legacyNode);
	}

	/**
	 * Returns the absolute preference file path used by the current process.
	 */
	public static Path preferencesFilePath() {
		synchronized (LOCK) {
			ensureLoadedLocked();
			return preferencesFile;
		}
	}

	public static final class Node {

		private final String namespace;
		private final Preferences legacyNode;

		private Node(String namespace, Preferences legacyNode) {
			this.namespace = sanitizeNamespace(namespace);
			this.legacyNode = legacyNode;
		}

		public String get(String key, String defaultValue) {
			String normalizedKey = sanitizeKey(key);
			String namespacedKey = namespacedKey(normalizedKey);
			String value = readValue(namespacedKey);
			if (value != null) {
				return value;
			}
			String legacyValue = readLegacyValue(normalizedKey);
			if (legacyValue != null) {
				writeValue(namespacedKey, legacyValue);
				return legacyValue;
			}
			return defaultValue;
		}

		public boolean getBoolean(String key, boolean defaultValue) {
			String value = get(key, null);
			if (value == null || value.isBlank()) {
				return defaultValue;
			}
			if ("true".equalsIgnoreCase(value.trim())) {
				return true;
			}
			if ("false".equalsIgnoreCase(value.trim())) {
				return false;
			}
			return defaultValue;
		}

		public int getInt(String key, int defaultValue) {
			String value = get(key, null);
			if (value == null || value.isBlank()) {
				return defaultValue;
			}
			try {
				return Integer.parseInt(value.trim());
			} catch (NumberFormatException _) {
				return defaultValue;
			}
		}

		public double getDouble(String key, double defaultValue) {
			String value = get(key, null);
			if (value == null || value.isBlank()) {
				return defaultValue;
			}
			try {
				return Double.parseDouble(value.trim());
			} catch (NumberFormatException _) {
				return defaultValue;
			}
		}

		public void put(String key, String value) {
			String normalizedKey = sanitizeKey(key);
			if (value == null) {
				remove(normalizedKey);
				return;
			}
			writeValue(namespacedKey(normalizedKey), value);
		}

		public void putBoolean(String key, boolean value) {
			put(key, Boolean.toString(value));
		}

		public void putInt(String key, int value) {
			put(key, Integer.toString(value));
		}

		public void putDouble(String key, double value) {
			put(key, Double.toString(value));
		}

		public void remove(String key) {
			String normalizedKey = sanitizeKey(key);
			removeValue(namespacedKey(normalizedKey));
		}

		private String namespacedKey(String key) {
			return namespace + "." + key;
		}

		private String readLegacyValue(String key) {
			if (legacyNode == null) {
				return null;
			}
			try {
				return legacyNode.get(key, null);
			} catch (RuntimeException _) {
				return null;
			}
		}
	}

	private static String readValue(String key) {
		synchronized (LOCK) {
			ensureLoadedLocked();
			return VALUES.getProperty(key);
		}
	}

	private static void writeValue(String key, String value) {
		synchronized (LOCK) {
			ensureLoadedLocked();
			String previous = VALUES.getProperty(key);
			if (Objects.equals(previous, value)) {
				return;
			}
			VALUES.setProperty(key, value);
			persistLocked();
		}
	}

	private static void removeValue(String key) {
		synchronized (LOCK) {
			ensureLoadedLocked();
			Object removed = VALUES.remove(key);
			if (removed != null) {
				persistLocked();
			}
		}
	}

	private static void ensureLoadedLocked() {
		if (loaded) {
			return;
		}
		preferencesFile = resolvePreferencesFile();
		Path parent = preferencesFile.getParent();
		if (parent != null) {
			try {
				Files.createDirectories(parent);
			} catch (Exception _) {
				// Keep in-memory preferences when directory creation fails.
			}
		}
		if (Files.isRegularFile(preferencesFile)) {
			try (InputStream input = Files.newInputStream(preferencesFile, StandardOpenOption.READ)) {
				VALUES.load(input);
			} catch (Exception _) {
				VALUES.clear();
			}
		}
		loaded = true;
	}

	private static Path resolvePreferencesFile() {
		String fileProperty = normalizeToken(System.getProperty(PREFERENCES_FILE_PROPERTY));
		if (!fileProperty.isBlank()) {
			return normalizePath(Path.of(fileProperty));
		}

		String directoryProperty = normalizeToken(System.getProperty(PREFERENCES_DIR_PROPERTY));
		if (!directoryProperty.isBlank()) {
			return normalizePath(Path.of(directoryProperty).resolve(DEFAULT_PREFERENCES_FILE_NAME));
		}

		// Ensure runtime storage properties are available even if AppPreferences is
		// accessed before Launcher.main() bootstrap.
		RuntimeStorageBootstrap.initialize();

		fileProperty = normalizeToken(System.getProperty(PREFERENCES_FILE_PROPERTY));
		if (!fileProperty.isBlank()) {
			return normalizePath(Path.of(fileProperty));
		}

		directoryProperty = normalizeToken(System.getProperty(PREFERENCES_DIR_PROPERTY));
		if (!directoryProperty.isBlank()) {
			return normalizePath(Path.of(directoryProperty).resolve(DEFAULT_PREFERENCES_FILE_NAME));
		}

		String userHome = normalizeToken(System.getProperty("user.home"));
		Path fallbackRoot = userHome.isBlank() ? Path.of(".") : Path.of(userHome);
		String osName = normalizeToken(System.getProperty("os.name")).toLowerCase(Locale.ROOT);
		if (osName.contains("linux") || osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
			return normalizePath(fallbackRoot.resolve(".config").resolve("corese-gui").resolve("preferences")
					.resolve(DEFAULT_PREFERENCES_FILE_NAME));
		}
		if (osName.contains("mac")) {
			return normalizePath(fallbackRoot.resolve("Library").resolve("Application Support").resolve("Corese GUI")
					.resolve("preferences").resolve(DEFAULT_PREFERENCES_FILE_NAME));
		}
		if (osName.contains("win")) {
			return normalizePath(fallbackRoot.resolve("AppData").resolve("Roaming").resolve("Corese GUI")
					.resolve("preferences").resolve(DEFAULT_PREFERENCES_FILE_NAME));
		}
		return normalizePath(
				fallbackRoot.resolve(".corese-gui").resolve("preferences").resolve(DEFAULT_PREFERENCES_FILE_NAME));
	}

	private static void persistLocked() {
		if (preferencesFile == null) {
			return;
		}
		Path parent = preferencesFile.getParent();
		if (parent == null) {
			return;
		}
		Path tempFile = parent.resolve(preferencesFile.getFileName().toString() + ".tmp");
		try {
			Files.createDirectories(parent);
			try (OutputStream output = Files.newOutputStream(tempFile, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				VALUES.store(output, FILE_HEADER);
			}
			try {
				Files.move(tempFile, preferencesFile, StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (Exception _) {
				Files.move(tempFile, preferencesFile, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception _) {
			// Keep runtime values in memory if persistence is unavailable.
			try {
				Files.deleteIfExists(tempFile);
			} catch (Exception _) {
				// No-op.
			}
		}
	}

	private static String sanitizeNamespace(String namespace) {
		String value = normalizeToken(namespace).replace('/', '.').replace('\\', '.');
		if (value.isBlank()) {
			throw new IllegalArgumentException("Preference namespace must not be blank");
		}
		return value;
	}

	private static String sanitizeKey(String key) {
		String value = normalizeToken(key);
		if (value.isBlank()) {
			throw new IllegalArgumentException("Preference key must not be blank");
		}
		return value;
	}

	private static Path normalizePath(Path path) {
		if (path == null) {
			return Path.of(".").toAbsolutePath().normalize();
		}
		try {
			return path.toAbsolutePath().normalize();
		} catch (Exception _) {
			return path;
		}
	}

	private static String normalizeToken(String value) {
		return value == null ? "" : value.trim();
	}
}
