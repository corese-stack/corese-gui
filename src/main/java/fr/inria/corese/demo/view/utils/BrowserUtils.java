package fr.inria.corese.demo.view.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for opening URLs in the system's default browser.
 * Provides cross-platform support including fallbacks for Linux/KDE.
 */
public final class BrowserUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserUtils.class);

    private BrowserUtils() {
        // Utility class
    }

    /**
     * Opens the specified URL in the default browser.
     *
     * @param url The URL to open.
     */
    public static void openUrl(String url) {
        CompletableFuture.runAsync(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    openUrlFallback(url);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to open URL with Desktop API, trying fallback...", e);
                openUrlFallback(url);
            }
        });
    }

    private static void openUrlFallback(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime runtime = Runtime.getRuntime();
        try {
            if (os.contains("win")) {
                runtime.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                runtime.exec(new String[]{"open", url});
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux - try xdg-open
                runtime.exec(new String[]{"xdg-open", url});
            } else {
                LOGGER.error("Cannot open URL: OS not supported or no browser found.");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to open URL with fallback command", e);
        }
    }
}
