package fr.inria.corese.gui.feature.main;

import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.core.enums.ViewId;
import fr.inria.corese.gui.core.view.AbstractView;

/**
 * Centralized manager responsible for loading and caching application views.
 */
public final class ViewManager {

  // ===== Fields =====

  private static final Logger LOGGER = LoggerFactory.getLogger(ViewManager.class);

  /** Cache mapping ViewId to their corresponding AbstractView instances. */
  private final Map<ViewId, AbstractView> cache = new EnumMap<>(ViewId.class);

  /**
   * Returns the view for the given {@link ViewId}.
   *
   * <p>
   * Loads the view on first access and caches it (Lazy Loading). Never
   * returns {@code null} — throws an exception if the view cannot be loaded.
   *
   * @param viewId the identifier of the view
   * @return the AbstractView instance associated with the view
   * @throws IllegalArgumentException if the viewId is {@code null} or invalid
   * @throws IllegalStateException    if the view cannot be loaded
   */
  public AbstractView getView(ViewId viewId) {
    if (viewId == null) {
      throw new IllegalArgumentException("ViewId cannot be null.");
    }

    // Load once and cache; wrap loading exceptions with clear context
    return cache.computeIfAbsent(viewId, this::safeLoadView);
  }

  /** Clears all cached views (e.g., when refreshing the UI). */
  public void clearCache() {
    cache.clear();
  }

  /**
   * Adapter used by computeIfAbsent to load a view safely and convert checked
   * exceptions into
   * unchecked ones with proper logging.
   */
  private AbstractView safeLoadView(ViewId viewId) {
    try {
      AbstractView view = loadView(viewId);
      LOGGER.debug("Loaded view: {}", viewId);
      return view;
    } catch (RuntimeException e) {
      // Rethrow with contextual information (no logging to avoid double-reporting)
      throw new IllegalStateException("Failed to load view: " + viewId, e);
    }
  }

  /**
   * Loads a view via factory.
   */
  private AbstractView loadView(ViewId viewId) {
    AbstractView instance = viewId.createInstance();
    if (instance == null) {
      throw new IllegalStateException("Factory returned null for view: " + viewId);
    }
    return instance;
  }
}
