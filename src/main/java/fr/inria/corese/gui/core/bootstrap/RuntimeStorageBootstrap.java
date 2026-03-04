package fr.inria.corese.gui.core.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves and initializes runtime storage locations (logs + preferences).
 *
 * <p>
 * This bootstrap is executed before application startup so logging and
 * preferences always target stable, writable locations on every OS.
 */
public final class RuntimeStorageBootstrap {

	public static final String LOG_DIR_PROPERTY = "corese.log.dir";
	public static final String PREFERENCES_DIR_PROPERTY = "corese.preferences.dir";
	public static final String PREFERENCES_FILE_PROPERTY = "corese.preferences.file";

	private static final String APP_DISPLAY_NAME = "Corese GUI";
	private static final String APP_UNIX_NAME = "corese-gui";
	private static final String FALLBACK_ROOT_NAME = ".corese-gui";
	private static final String PREFERENCES_FILE_NAME = "preferences.properties";
	private static final String USER_HOME_PROPERTY = "user.home";

	private static volatile boolean initialized;

	private RuntimeStorageBootstrap() {
		throw new AssertionError("Utility class - do not instantiate");
	}

	/**
	 * Initializes runtime directories and exports their absolute paths via system
	 * properties.
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}
		synchronized (RuntimeStorageBootstrap.class) {
			if (initialized) {
				return;
			}

			Path userHome = resolveUserHome();
			String osName = System.getProperty("os.name");
			StorageLayout requestedLayout = resolveLayout(osName, userHome, System.getenv(), System.getProperties());
			StorageLayout fallbackLayout = resolveLayout(osName, userHome, Map.of(), Map.of());
			StorageLayout effectiveLayout = ensureWritableLayout(requestedLayout, fallbackLayout);

			System.setProperty(LOG_DIR_PROPERTY, effectiveLayout.logDirectory().toString());
			System.setProperty(PREFERENCES_DIR_PROPERTY, effectiveLayout.preferencesDirectory().toString());
			System.setProperty(PREFERENCES_FILE_PROPERTY, effectiveLayout.preferencesFile().toString());

			initialized = true;
		}
	}

	static StorageLayout resolveLayout(String osName, Path userHome, Map<String, String> env,
			Map<?, ?> systemProperties) {
		Path safeUserHome = userHome == null ? Path.of(".").toAbsolutePath().normalize() : userHome;

		Path logDirOverride = parseOptionalPath(readProperty(systemProperties, LOG_DIR_PROPERTY));
		Path preferencesDirOverride = parseOptionalPath(readProperty(systemProperties, PREFERENCES_DIR_PROPERTY));
		Path preferencesFileOverride = parseOptionalPath(readProperty(systemProperties, PREFERENCES_FILE_PROPERTY));

		OsFamily osFamily = classifyOs(osName);
		Path defaultLogDirectory = switch (osFamily) {
			case WINDOWS -> resolveWindowsLocalBase(env, safeUserHome).resolve(APP_DISPLAY_NAME).resolve("logs");
			case MACOS -> safeUserHome.resolve("Library").resolve("Logs").resolve(APP_DISPLAY_NAME);
			case LINUX -> resolveLinuxStateBase(env, safeUserHome).resolve(APP_UNIX_NAME).resolve("logs");
			case OTHER -> safeUserHome.resolve(FALLBACK_ROOT_NAME).resolve("logs");
		};

		Path defaultPreferencesDirectory = switch (osFamily) {
			case WINDOWS ->
				resolveWindowsRoamingBase(env, safeUserHome).resolve(APP_DISPLAY_NAME).resolve("preferences");
			case MACOS -> safeUserHome.resolve("Library").resolve("Application Support").resolve(APP_DISPLAY_NAME)
					.resolve("preferences");
			case LINUX -> resolveLinuxConfigBase(env, safeUserHome).resolve(APP_UNIX_NAME).resolve("preferences");
			case OTHER -> safeUserHome.resolve(FALLBACK_ROOT_NAME).resolve("preferences");
		};

		Path resolvedLogDirectory = normalizePath(logDirOverride != null ? logDirOverride : defaultLogDirectory);
		Path resolvedPreferencesDirectory = normalizePath(
				preferencesDirOverride != null ? preferencesDirOverride : defaultPreferencesDirectory);
		Path resolvedPreferencesFile = normalizePath(preferencesFileOverride != null
				? preferencesFileOverride
				: resolvedPreferencesDirectory.resolve(PREFERENCES_FILE_NAME));

		Path preferencesDirectoryFromFile = resolvedPreferencesFile.getParent();
		if (preferencesDirectoryFromFile != null) {
			resolvedPreferencesDirectory = preferencesDirectoryFromFile;
		}

		return new StorageLayout(resolvedLogDirectory, resolvedPreferencesDirectory, resolvedPreferencesFile);
	}

	private static StorageLayout ensureWritableLayout(StorageLayout requestedLayout, StorageLayout fallbackLayout) {
		Path fallbackLogDirectory = fallbackLayout == null
				? requestedLayout.logDirectory()
				: fallbackLayout.logDirectory();
		Path fallbackPreferencesDirectory = fallbackLayout == null
				? requestedLayout.preferencesDirectory()
				: fallbackLayout.preferencesDirectory();
		Path fallbackPreferencesFile = fallbackLayout == null
				? requestedLayout.preferencesFile()
				: fallbackLayout.preferencesFile();

		Path logDirectory = ensureDirectory(requestedLayout.logDirectory(), fallbackLogDirectory);
		Path preferencesDirectory = ensureDirectory(requestedLayout.preferencesDirectory(),
				fallbackPreferencesDirectory);
		Path preferencesFile = requestedLayout.preferencesFile();
		if (preferencesFile == null || preferencesFile.getParent() == null) {
			preferencesFile = fallbackPreferencesFile;
		} else {
			Path ensuredParent = ensureDirectory(preferencesFile.getParent(), fallbackPreferencesDirectory);
			preferencesFile = ensuredParent.resolve(preferencesFile.getFileName());
		}
		return new StorageLayout(logDirectory, preferencesDirectory, preferencesFile);
	}

	private static Path ensureDirectory(Path preferredPath, Path fallbackPath) {
		Path safePreferred = normalizePath(preferredPath);
		if (tryCreateDirectory(safePreferred)) {
			return safePreferred;
		}
		Path safeFallback = normalizePath(fallbackPath);
		if (tryCreateDirectory(safeFallback)) {
			return safeFallback;
		}
		return safePreferred;
	}

	private static boolean tryCreateDirectory(Path path) {
		if (path == null) {
			return false;
		}
		try {
			Files.createDirectories(path);
			return true;
		} catch (Exception _) {
			return false;
		}
	}

	private static Path resolveWindowsRoamingBase(Map<String, String> env, Path userHome) {
		Path fromEnv = parseAbsolutePathFromEnv(readEnv(env, "APPDATA"), userHome);
		if (fromEnv != null) {
			return fromEnv;
		}
		return userHome.resolve("AppData").resolve("Roaming");
	}

	private static Path resolveWindowsLocalBase(Map<String, String> env, Path userHome) {
		Path fromEnv = parseAbsolutePathFromEnv(readEnv(env, "LOCALAPPDATA"), userHome);
		if (fromEnv != null) {
			return fromEnv;
		}
		return userHome.resolve("AppData").resolve("Local");
	}

	private static Path resolveLinuxConfigBase(Map<String, String> env, Path userHome) {
		Path fromEnv = parseAbsolutePathFromEnv(readEnv(env, "XDG_CONFIG_HOME"), userHome);
		if (fromEnv != null) {
			return fromEnv;
		}
		return userHome.resolve(".config");
	}

	private static Path resolveLinuxStateBase(Map<String, String> env, Path userHome) {
		Path fromEnv = parseAbsolutePathFromEnv(readEnv(env, "XDG_STATE_HOME"), userHome);
		if (fromEnv != null) {
			return fromEnv;
		}
		return userHome.resolve(".local").resolve("state");
	}

	private static Path parseAbsolutePathFromEnv(String value, Path userHome) {
		String normalized = normalizeToken(value);
		if (normalized.isBlank()) {
			return null;
		}

		String expanded = expandUserHomeToken(normalized, userHome);
		Path path = parseOptionalPath(expanded);
		if (path == null || !path.isAbsolute()) {
			return null;
		}
		return normalizePath(path);
	}

	private static String expandUserHomeToken(String value, Path userHome) {
		if (value == null || value.isBlank() || userHome == null) {
			return normalizeToken(value);
		}
		String userHomeString = userHome.toString();
		if ("~".equals(value) || "$HOME".equals(value) || "${HOME}".equals(value)) {
			return userHomeString;
		}
		if (value.startsWith("~/")) {
			return userHome.resolve(value.substring(2)).toString();
		}
		if (value.startsWith("$HOME/")) {
			return userHome.resolve(value.substring("$HOME/".length())).toString();
		}
		if (value.startsWith("${HOME}/")) {
			return userHome.resolve(value.substring("${HOME}/".length())).toString();
		}
		return value;
	}

	private static Path resolveUserHome() {
		String rawUserHome = normalizeToken(System.getProperty(USER_HOME_PROPERTY));
		if (rawUserHome.isBlank()) {
			return Path.of(".").toAbsolutePath().normalize();
		}
		return normalizePath(Path.of(rawUserHome));
	}

	private static OsFamily classifyOs(String osName) {
		String token = normalizeToken(osName).toLowerCase(Locale.ROOT);
		if (token.contains("win")) {
			return OsFamily.WINDOWS;
		}
		if (token.contains("mac")) {
			return OsFamily.MACOS;
		}
		if (token.contains("nux") || token.contains("nix") || token.contains("aix") || token.contains("linux")) {
			return OsFamily.LINUX;
		}
		return OsFamily.OTHER;
	}

	private static String readEnv(Map<String, String> env, String key) {
		if (env == null || key == null || key.isBlank()) {
			return "";
		}
		String value = env.get(key);
		return normalizeToken(value);
	}

	private static String readProperty(Map<?, ?> properties, String key) {
		if (properties == null || key == null || key.isBlank()) {
			return "";
		}
		Object value = properties.get(key);
		return normalizeToken(value == null ? null : String.valueOf(value));
	}

	private static Path parseOptionalPath(String value) {
		String normalized = normalizeToken(value);
		if (normalized.isBlank()) {
			return null;
		}
		try {
			return Path.of(normalized);
		} catch (Exception _) {
			return null;
		}
	}

	private static Path normalizePath(Path path) {
		if (path == null) {
			return null;
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

	enum OsFamily {
		WINDOWS, MACOS, LINUX, OTHER
	}

	record StorageLayout(Path logDirectory, Path preferencesDirectory, Path preferencesFile) {
	}
}
