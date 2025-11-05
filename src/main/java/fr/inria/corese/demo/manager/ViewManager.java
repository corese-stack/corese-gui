package fr.inria.corese.demo.manager;

import fr.inria.corese.demo.view.ViewId;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

/**
 * Centralized manager responsible for loading and caching application views.
 *
 * <p>This class abstracts whether a view is loaded from an FXML file or instantiated directly as a
 * Java class. It also ensures each view is loaded only once and then cached.
 */
public final class ViewManager {

  // ===== Fields =====

  private static final Logger LOGGER = Logger.getLogger(ViewManager.class.getName());

  /** Cache mapping ViewId to their corresponding JavaFX Node instances. */
  private final Map<ViewId, Node> cache = new EnumMap<>(ViewId.class);

  /**
   * Returns the JavaFX node for the given {@link ViewId}.
   *
   * <p>Loads the view on first access (via FXML or factory) and caches it. Never returns {@code
   * null} — throws an exception if the view cannot be loaded.
   *
   * @param viewId the identifier of the view
   * @return the JavaFX node associated with the view
   * @throws IllegalArgumentException if the viewId is {@code null} or invalid
   * @throws IllegalStateException if the view cannot be loaded
   */
  public Node getView(ViewId viewId) {
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
   * Adapter used by computeIfAbsent to load a view safely and convert checked exceptions into
   * unchecked ones with proper logging.
   */
  private Node safeLoadView(ViewId viewId) {
    try {
      Node content = loadView(viewId);
      LOGGER.fine(() -> "Loaded view: " + viewId);
      return content;
    } catch (IOException | RuntimeException e) {
      // Rethrow with contextual information (no logging to avoid double-reporting)
      throw new IllegalStateException("Failed to load view: " + viewId, e);
    }
  }

  /**
   * Loads a view either via factory or from an FXML resource.
   *
   * @throws IOException when FXML loading fails
   */
  private Node loadView(ViewId viewId) throws IOException {
    if (viewId.hasFactory()) {
      Node instance = viewId.createInstance();
      if (instance == null) {
        throw new IllegalStateException("Factory returned null for view: " + viewId);
      }
      return instance;
    }

    // FXML-based view
    String fxmlPath = viewId.getFxmlPath();
    if (fxmlPath == null || fxmlPath.isEmpty()) {
      throw new IllegalArgumentException("No FXML path defined for view: " + viewId);
    }

    URL resource = ViewManager.class.getResource(fxmlPath);
    if (resource == null) {
      throw new IllegalArgumentException(
          "FXML resource not found on classpath: " + fxmlPath + " for view: " + viewId);
    }

    FXMLLoader loader = new FXMLLoader(resource);
    return loader.load();
  }
}
