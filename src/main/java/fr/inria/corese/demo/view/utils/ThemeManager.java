package fr.inria.corese.demo.view.utils;

import atlantafx.base.theme.Theme;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;

/**
 * Manages the global visual theme of the Corese-GUI application.
 *
 * <p>This class is a <b>singleton</b> implemented using the <em>Initialization-on-demand holder
 * idiom</em>, also known as the <em>Bill Pugh Singleton Pattern</em>. This ensures:
 *
 * <ul>
 *   <li>Lazy initialization — the instance is created only when first accessed.
 *   <li>Thread-safety — guaranteed by JVM class loading semantics.
 *   <li>No synchronization overhead — more efficient than classic synchronized patterns.
 * </ul>
 *
 * <p>Justification: JavaFX only allows one active user-agent stylesheet at a time, therefore the
 * application must have a single global theme manager instance.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ThemeManager.getInstance().applyTheme(new NordLight());
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is intentional and justified
public final class ThemeManager {

  // ===== Fields =====

  private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());

  /** The currently applied theme, or {@code null} if none is set. */
  private Theme currentTheme;

  // ===== Constructor =====

  /** Private constructor to prevent external instantiation. */
  private ThemeManager() {}

  // ===== Holder idiom =====

  /**
   * Inner static class responsible for holding the singleton instance. Loaded only when {@link
   * ThemeManager#getInstance()} is called, ensuring lazy, thread-safe initialization.
   */
  private static class Holder {
    private static final ThemeManager INSTANCE = new ThemeManager();
  }

  // ===== Accessors =====

  /**
   * Returns the singleton instance of {@link ThemeManager}. The instance is created lazily and
   * safely on first call.
   *
   * @return the global {@link ThemeManager} instance
   */
  public static ThemeManager getInstance() {
    return Holder.INSTANCE;
  }

  /**
   * Returns the currently applied theme, or {@code null} if none is active.
   *
   * @return the current {@link Theme}
   */
  public Theme getCurrentTheme() {
    return currentTheme;
  }

  // ===== Public API =====

  /**
   * Applies a new global theme to the application.
   *
   * <p>This updates the user-agent stylesheet used by JavaFX and stores the currently active theme
   * reference.
   *
   * @param theme the {@link Theme} to apply (must not be null)
   */
  public void applyTheme(Theme theme) {
    Objects.requireNonNull(theme, "Theme must not be null");

    Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
    currentTheme = theme;

    LOGGER.log(Level.INFO, "Applied theme: {0}", theme.getName());
  }

  /** Reapplies the currently active theme. Useful when scenes are recreated or reloaded. */
  public void reapplyTheme() {
    if (currentTheme != null) {
      Application.setUserAgentStylesheet(currentTheme.getUserAgentStylesheet());
      LOGGER.log(Level.FINE, "Reapplied theme: {0}", currentTheme.getName());
    }
  }
}
