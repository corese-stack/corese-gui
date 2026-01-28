package fr.inria.corese.gui.core.manager;

import fr.inria.corese.gui.component.base.AbstractView;
import fr.inria.corese.gui.core.ViewId;






import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import javafx.scene.Node;

/**
 * Centralized manager responsible for loading and caching application views.
 *
 * <p>This class abstracts whether a view is loaded from an FXML file or instantiated directly as a
 * Java class. It also ensures each view is loaded only once and then cached.
 *
 * <p>Views are stored as {@link AbstractView} instances when available, falling back to raw {@link
 * Node} for FXML-loaded views that don't extend AbstractView.
 */
public final class ViewManager {

  // ===== Fields =====

  private static final Logger LOGGER = Logger.getLogger(ViewManager.class.getName());

  /** Cache mapping ViewId to their corresponding AbstractView instances. */
  private final Map<ViewId, AbstractView> cache = new EnumMap<>(ViewId.class);

  /**
   * Returns the view for the given {@link ViewId}.
   *
   * <p>Loads the view on first access (via FXML or factory) and caches it. Never returns {@code
   * null} — throws an exception if the view cannot be loaded.
   *
   * @param viewId the identifier of the view
   * @return the AbstractView instance associated with the view
   * @throws IllegalArgumentException if the viewId is {@code null} or invalid
   * @throws IllegalStateException if the view cannot be loaded
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
   * Preloads all views defined in {@link ViewId}.
   *
   * <p>Iterates through all enum constants and ensures their views are loaded and cached.
   * This is useful for avoiding initialization delays when switching views for the first time.
   */
  public void preloadAllViews() {
    for (ViewId viewId : ViewId.values()) {
      try {
        getView(viewId);
      } catch (Exception e) {
        LOGGER.warning("Failed to preload view: " + viewId + ". Error: " + e.getMessage());
      }
    }
  }

  /**
   * Adapter used by computeIfAbsent to load a view safely and convert checked exceptions into
   * unchecked ones with proper logging.
   */
  private AbstractView safeLoadView(ViewId viewId) {
    try {
      AbstractView view = loadView(viewId);
      LOGGER.fine(() -> "Loaded view: " + viewId);
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
