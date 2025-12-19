package fr.inria.corese.gui.view;

import java.util.function.Supplier;

import fr.inria.corese.gui.controller.DataViewController;
import fr.inria.corese.gui.controller.QueryViewController;
import fr.inria.corese.gui.controller.ValidationController;
import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.controller.SettingsController;
import fr.inria.corese.gui.model.SettingsModel;

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

  QUERY("query-view", null, () -> {
    QueryView view = new QueryView();
    new QueryViewController(view);
    return view;
  }),

  /** View for validating RDF data (e.g., SHACL). */
  VALIDATION("validation-view", null, () -> {
    ValidationView view = new ValidationView();
    new ValidationController(view);
    return view;
  }),

  /** Application configuration and preferences view. */
  SETTINGS("settings-view", null, () -> {
    SettingsModel model = new SettingsModel();
    SettingsView view = new SettingsView();
    new SettingsController(model, view);
    return view;
  });

  // ===========================================================================
  // Fields
  // ===========================================================================

  /** Unique string identifier for this view. */
  private final String id;

  /** Optional factory function that instantiates the view directly in Java (no FXML). */
  private final Supplier<AbstractView> factory;

  // ===========================================================================
  // Constructor
  // ===========================================================================

  ViewId(String id, String fxmlPath, Supplier<AbstractView> factory) {
    this.id = id;
    this.factory = factory;
  }

  // ===========================================================================
  // Accessors
  // ===========================================================================

  /** Returns the unique string identifier of this view (e.g., "query-view"). */
  public String getId() {
    return id;
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
