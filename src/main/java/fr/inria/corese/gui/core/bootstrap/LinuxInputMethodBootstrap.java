package fr.inria.corese.gui.core.bootstrap;

import java.awt.SplashScreen;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux input-method bootstrap for JavaFX/WebView.
 *
 * <p>
 * On some Linux environments (notably GTK + IBus), dead-key composition can
 * break inside JavaFX WebView editors. This bootstrap relaunches the current
 * JVM once with XIM-related environment variables <em>before</em> JavaFX
 * initializes, which is when GTK input method choice is locked in.
 *
 * <p>
 * Behavior:
 *
 * <ul>
 * <li>No-op on non-Linux systems.
 * <li>Enabled by default on Linux to preserve dead-key composition in
 * JavaFX/WebView editors.
 * <li>No-op if explicitly disabled with {@code CORESE_IM_BOOTSTRAP_MODE=off} or
 * {@code CORESE_IM_BOOTSTRAP_DISABLE=1}.
 * <li>No-op if already bootstrapped.
 * <li>Relaunches with {@code XMODIFIERS=@im=none}, {@code GTK_IM_MODULE=xim},
 * {@code QT_IM_MODULE=xim}.
 * </ul>
 */
public final class LinuxInputMethodBootstrap {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxInputMethodBootstrap.class);

	private static final String MARKER_ENV = "CORESE_IM_BOOTSTRAPPED";
	private static final String DISABLE_ENV = "CORESE_IM_BOOTSTRAP_DISABLE";
	private static final String MODE_ENV = "CORESE_IM_BOOTSTRAP_MODE";
	private static final String MODE_OFF = "off";
	private static final String MODE_XIM = "xim";
	private static final String XMODIFIERS_ENV = "XMODIFIERS";
	private static final String XMODIFIERS_XIM_VALUE = "@im=none";
	private static final String GTK_IM_MODULE_ENV = "GTK_IM_MODULE";
	private static final String QT_IM_MODULE_ENV = "QT_IM_MODULE";
	private static final String SPLASH_OPTION_PREFIX = "-splash:";

	private LinuxInputMethodBootstrap() {
		throw new AssertionError("Utility class - do not instantiate");
	}

	/**
	 * Relaunches the current process once with XIM environment variables when
	 * needed.
	 *
	 * @param args
	 *            original command-line arguments
	 */
	public static void relaunchWithXimIfNeeded(String[] args) {
		if (!isLinux()) {
			return;
		}
		if ("1".equals(System.getenv(MARKER_ENV))) {
			return;
		}
		if (isBootstrapDisabledByConfig()) {
			return;
		}
		if (alreadyConfiguredForXim()) {
			return;
		}

		List<String> command = buildRelaunchCommand(args);
		ProcessBuilder pb = new ProcessBuilder(command);
		Map<String, String> env = pb.environment();
		env.put(MARKER_ENV, "1");
		env.put(XMODIFIERS_ENV, XMODIFIERS_XIM_VALUE);
		env.put(GTK_IM_MODULE_ENV, MODE_XIM);
		env.put(QT_IM_MODULE_ENV, MODE_XIM);
		pb.inheritIO();

		try {
			Process child = pb.start();
			closeNativeSplashIfPresent();
			int exitCode = child.waitFor();
			System.exit(exitCode);
		} catch (IOException e) {
			LOGGER.warn("Failed to relaunch with Linux IM compatibility; continuing current process", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Interrupted while waiting Linux IM compatibility relaunch; continuing current process", e);
		}
	}

	private static boolean isLinux() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
	}

	private static boolean alreadyConfiguredForXim() {
		return MODE_XIM.equalsIgnoreCase(System.getenv(GTK_IM_MODULE_ENV))
				&& XMODIFIERS_XIM_VALUE.equals(System.getenv(XMODIFIERS_ENV));
	}

	private static boolean isBootstrapDisabledByConfig() {
		if ("1".equals(System.getenv(DISABLE_ENV))) {
			return true;
		}
		String mode = System.getenv(MODE_ENV);
		if (mode == null || mode.isBlank()) {
			return false;
		}
		if (MODE_OFF.equalsIgnoreCase(mode)) {
			return true;
		}
		if (MODE_XIM.equalsIgnoreCase(mode)) {
			return false;
		}
		LOGGER.warn("Unknown {}='{}'; expected '{}' or '{}'. Falling back to default '{}'.", MODE_ENV, mode, MODE_OFF,
				MODE_XIM, MODE_XIM);
		return false;
	}

	private static List<String> buildRelaunchCommand(String[] args) {
		List<String> command = new ArrayList<>();
		String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		command.add(javaBin);
		List<String> inputArguments = new ArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
		command.addAll(inputArguments);
		appendSplashOptionIfMissing(command, inputArguments);

		String entry = extractEntryPointFromSunCommand();
		if (entry != null && entry.toLowerCase(Locale.ROOT).endsWith(".jar")) {
			command.add("-jar");
			command.add(entry);
		} else {
			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			// Keep the plain launcher entrypoint to avoid JDK JavaFX launcher checks
			// in classpath-based packaged runtimes (jpackage app-image).
			command.add("fr.inria.corese.gui.Launcher");
		}

		if (args != null) {
			Collections.addAll(command, args);
		}
		return command;
	}

	private static void appendSplashOptionIfMissing(List<String> command, List<String> inputArguments) {
		boolean splashAlreadyProvided = inputArguments.stream().anyMatch(arg -> arg.startsWith(SPLASH_OPTION_PREFIX));
		if (splashAlreadyProvided) {
			return;
		}

		String splashOption = resolveSplashOptionFromProcCmdline();
		if (splashOption == null) {
			splashOption = resolveCurrentSplashOption();
		}
		if (splashOption != null) {
			command.add(splashOption);
		}
	}

	private static String resolveSplashOptionFromProcCmdline() {
		try {
			byte[] cmdline = Files.readAllBytes(Path.of("/proc/self/cmdline"));
			int start = 0;
			for (int i = 0; i <= cmdline.length; i++) {
				boolean isSeparator = i == cmdline.length || cmdline[i] == 0;
				if (!isSeparator) {
					continue;
				}
				if (i > start) {
					String arg = new String(cmdline, start, i - start, StandardCharsets.UTF_8);
					if (arg.startsWith(SPLASH_OPTION_PREFIX)) {
						return arg;
					}
				}
				start = i + 1;
			}
		} catch (IOException _) {
			// Best effort only; fallback to SplashScreen API.
		}
		return null;
	}

	private static String resolveCurrentSplashOption() {
		try {
			SplashScreen splash = SplashScreen.getSplashScreen();
			if (splash == null || splash.getImageURL() == null) {
				return null;
			}
			return SPLASH_OPTION_PREFIX + splash.getImageURL().toExternalForm();
		} catch (UnsupportedOperationException _) {
			// No native splash active.
			return null;
		}
	}

	private static String extractEntryPointFromSunCommand() {
		String sunCommand = System.getProperty("sun.java.command", "");
		if (sunCommand == null) {
			return null;
		}
		String trimmed = sunCommand.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		char first = trimmed.charAt(0);
		if (first == '"' || first == '\'') {
			int end = trimmed.indexOf(first, 1);
			if (end > 1) {
				return trimmed.substring(1, end);
			}
		}

		int spaceIdx = trimmed.indexOf(' ');
		return spaceIdx < 0 ? trimmed : trimmed.substring(0, spaceIdx);
	}

	private static void closeNativeSplashIfPresent() {
		try {
			SplashScreen splash = SplashScreen.getSplashScreen();
			if (splash != null) {
				splash.close();
			}
		} catch (UnsupportedOperationException _) {
			// No native splash active.
		}
	}
}
