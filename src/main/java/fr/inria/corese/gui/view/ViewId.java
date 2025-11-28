package fr.inria.corese.gui.view;

import java.util.function.Supplier;

import fr.inria.corese.gui.controller.DataViewController;
import fr.inria.corese.gui.controller.ValidationController;
import fr.inria.corese.gui.view.base.AbstractView;

/**
 * Enumerates all available views in the Corese-GUI application.
 *
 * <p>Each {@code ViewId} represents a distinct section of the UI (e.g., "Data", "Query",
 * "Settings") and defines how that view should be loaded — either from an FXML file or via a
 * Java-based factory.
 *
 * <p>This enum provides a uniform way to refer to views across controllers, ensuring type safety
 * and avoiding string-based identifiers.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * ViewId view = ViewId.DATA;
 * AbstractView viewInstance = view.hasFactory()
 *     ? view.createInstance()
 *     : loadFromFxml(view.getFxmlPath());
 * }</pre>
 *
 * <p>Each entry can define:
 *
 * <ul>
 *   <li>A unique {@code id} used for logging and navigation.
 *   <li>An optional FXML path (if the view is defined in FXML).
 *   <li>An optional Java {@code Supplier<AbstractView>} factory (if the view is built
 *       programmatically).
 * </ul>
 */
public enum ViewId {

    // ====== Defined Application Views ==========================================

  /** View displaying dataset loading and management features. */
  DATA("data-view", null, () -> {
    DataView view = new DataView();
    new DataViewController(view);
    return view;
  }),

  /** View for writing and executing SPARQL queries. */

  QUERY("query-view", "/fr/inria/corese/gui/query-view.fxml", null),

  /** View for validating RDF data (e.g., SHACL). */
  VALIDATION("validation-view", null, () -> {
    ValidationView view = new ValidationView();
    new ValidationController(view);
    return view;
  }),

  /** View providing a text editor for RDF graph manipulation. */
  RDF_EDITOR("rdf-editor-view", "/fr/inria/corese/gui/rdf-editor-view.fxml", null),

  /** Application configuration and preferences view. */
  SETTINGS("settings-view", null, SettingsView::new);

  // ===========================================================================
  // Fields
  // ===========================================================================

  /** Unique string identifier for this view. */
  private final String id;

  /** Path to the FXML file defining this view (may be {@code null} if factory-based). */
  private final String fxmlPath;

  /** Optional factory function that instantiates the view directly in Java (no FXML). */
  private final Supplier<AbstractView> factory;

  // ===========================================================================
  // Constructor
  // ===========================================================================

  ViewId(String id, String fxmlPath, Supplier<AbstractView> factory) {
    this.id = id;
    this.fxmlPath = fxmlPath;
    this.factory = factory;
  }

  // ===========================================================================
  // Accessors
  // ===========================================================================

  /** Returns the unique string identifier of this view (e.g., "query-view"). */
  public String getId() {
    return id;
  }

  /** Returns the FXML path used to load this view (or {@code null} if none). */
  public String getFxmlPath() {
    return fxmlPath;
  }

  /** Returns {@code true} if this view uses a Java-based factory instead of an FXML file. */
  public boolean hasFactory() {
    return factory != null;
  }

  /**
   * Instantiates the view using its Java-based factory, if defined.
   *
   * @return a newly created AbstractView instance, or {@code null} if no factory exists
   */
  public AbstractView createInstance() {
    return factory != null ? factory.get() : null;
  }

  @Override
  public String toString() {
    return id;
  }
}
