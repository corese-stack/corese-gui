package fr.inria.corese.demo.view.utils;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Efficiently monitors system theme and accent color changes.
 *
 * <p>This class uses a background polling mechanism to detect changes in the OS
 * appearance settings (Dark/Light mode and Accent Color).
 *
 * <p><b>Optimization:</b>
 * <ul>
 *   <li>Uses {@code scheduleWithFixedDelay} to prevent task overlapping.</li>
 *   <li>Polls every 2 seconds to balance responsiveness and CPU usage (process spawning).</li>
 *   <li>Updates UI on the JavaFX Application Thread.</li>
 * </ul>
 */
public final class SystemThemeMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemThemeMonitor.class);
    private static final long POLL_INTERVAL_MS = 20;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    // State tracking
    private boolean lastDarkMode;
    private Color lastAccentColor;

    // Listeners
    private Consumer<Boolean> themeChangeListener;
    private Consumer<Color> accentColorChangeListener;

    /**
     * Starts the monitoring service.
     * If already running, this method does nothing.
     */
    public void start() {
        if (running.getAndSet(true)) {
            return; // Already running
        }

        // Initialize state to avoid immediate false-positive triggers
        this.lastDarkMode = SystemThemeDetector.isSystemDarkTheme();
        this.lastAccentColor = SystemThemeDetector.getSystemAccentColor();

        LOGGER.info("Starting SystemThemeMonitor (Poll Interval: {}ms)", POLL_INTERVAL_MS);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SystemThemeMonitor-Thread");
            t.setDaemon(true); // Allow JVM to exit even if this thread is running
            return t;
        });

        // Use fixed delay to ensure previous execution is finished before starting next wait
        scheduler.scheduleWithFixedDelay(
            this::pollSystemSettings,
            POLL_INTERVAL_MS,
            POLL_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the monitoring service and releases resources.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        LOGGER.info("SystemThemeMonitor stopped.");
    }

    /**
     * Sets the callback for Dark/Light mode changes.
     * @param listener Consumer accepting true (Dark) or false (Light).
     */
    public void setThemeChangeListener(Consumer<Boolean> listener) {
        this.themeChangeListener = listener;
    }

    /**
     * Sets the callback for Accent Color changes.
     * @param listener Consumer accepting the new Color.
     */
    public void setAccentColorChangeListener(Consumer<Color> listener) {
        this.accentColorChangeListener = listener;
    }

    /**
     * The polling task. Checks for changes and notifies listeners on the UI thread.
     */
    private void pollSystemSettings() {
        if (!running.get()) return;

        try {
            // 1. Check Dark Mode
            boolean currentDarkMode = SystemThemeDetector.isSystemDarkTheme();
            if (currentDarkMode != lastDarkMode) {
                lastDarkMode = currentDarkMode;
                LOGGER.info("Detected Theme Change: {}", currentDarkMode ? "Dark" : "Light");
                notifyListener(themeChangeListener, currentDarkMode);
            }

            // 2. Check Accent Color
            Color currentAccentColor = SystemThemeDetector.getSystemAccentColor();
            if (!isSameColor(currentAccentColor, lastAccentColor)) {
                lastAccentColor = currentAccentColor;
                LOGGER.info("Detected Accent Color Change: {}", currentAccentColor);
                notifyListener(accentColorChangeListener, currentAccentColor);
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to poll system theme settings", e);
        }
    }

    private <T> void notifyListener(Consumer<T> listener, T value) {
        if (listener != null) {
            Platform.runLater(() -> listener.accept(value));
        }
    }

    private boolean isSameColor(Color c1, Color c2) {
        if (c1 == null || c2 == null) return c1 == c2;
        // Use strict equality or small epsilon if needed. 
        // Since we create new Color objects from parsing, exact equality is safer with epsilon
        // but Color.equals() in JavaFX is usually robust enough for this.
        // Let's use a tiny epsilon to be safe against float precision issues.
        return Math.abs(c1.getRed() - c2.getRed()) < 0.001 &&
               Math.abs(c1.getGreen() - c2.getGreen()) < 0.001 &&
               Math.abs(c1.getBlue() - c2.getBlue()) < 0.001;
    }
}
