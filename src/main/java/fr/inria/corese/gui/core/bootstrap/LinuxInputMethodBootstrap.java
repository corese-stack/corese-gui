package fr.inria.corese.gui.core.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
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
 * <li>No-op if already bootstrapped.
 * <li>No-op if {@code CORESE_IM_BOOTSTRAP_DISABLE=1} is set.
 * <li>Relaunches with {@code XMODIFIERS=@im=none}, {@code GTK_IM_MODULE=xim},
 * {@code QT_IM_MODULE=xim}.
 * </ul>
 */
public final class LinuxInputMethodBootstrap {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxInputMethodBootstrap.class);

	private static final String MARKER_ENV = "CORESE_IM_BOOTSTRAPPED";
	private static final String DISABLE_ENV = "CORESE_IM_BOOTSTRAP_DISABLE";

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
		if ("1".equals(System.getenv(DISABLE_ENV))) {
			return;
		}
		if (alreadyConfiguredForXim()) {
			return;
		}

		List<String> command = buildRelaunchCommand(args);
		ProcessBuilder pb = new ProcessBuilder(command);
		Map<String, String> env = pb.environment();
		env.put(MARKER_ENV, "1");
		env.put("XMODIFIERS", "@im=none");
		env.put("GTK_IM_MODULE", "xim");
		env.put("QT_IM_MODULE", "xim");
		pb.inheritIO();

		try {
			Process child = pb.start();
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
		return "xim".equalsIgnoreCase(System.getenv("GTK_IM_MODULE")) && "@im=none".equals(System.getenv("XMODIFIERS"));
	}

	private static List<String> buildRelaunchCommand(String[] args) {
		List<String> command = new ArrayList<>();
		String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		command.add(javaBin);
		command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());

		String entry = extractEntryPointFromSunCommand();
		if (entry != null && entry.toLowerCase(Locale.ROOT).endsWith(".jar")) {
			command.add("-jar");
			command.add(entry);
		} else {
			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			command.add("fr.inria.corese.gui.App");
		}

		if (args != null) {
			for (String arg : args) {
				command.add(arg);
			}
		}
		return command;
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
}
