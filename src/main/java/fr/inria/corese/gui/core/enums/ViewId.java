package fr.inria.corese.gui.core.enums;

import java.util.function.Supplier;

import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.feature.data.DataView;
import fr.inria.corese.gui.feature.data.DataViewController;
import fr.inria.corese.gui.feature.query.QueryView;
import fr.inria.corese.gui.feature.query.QueryViewController;
import fr.inria.corese.gui.feature.systemlog.SystemLogsController;
import fr.inria.corese.gui.feature.systemlog.SystemLogsView;
import fr.inria.corese.gui.feature.settings.SettingsController;
import fr.inria.corese.gui.feature.settings.SettingsModel;
import fr.inria.corese.gui.feature.settings.SettingsView;
import fr.inria.corese.gui.feature.validation.ValidationController;
import fr.inria.corese.gui.feature.validation.ValidationView;

/**
 * Enumeration of application view identifiers.
 */
public enum ViewId {

	// ====== Defined Application Views ==========================================

	/** View displaying dataset loading and management features. */
	DATA("data-view", () -> {
		DataView view = new DataView();
		new DataViewController(view);
		return view;
	}),

	/** View for writing and executing SPARQL queries. */
	QUERY("query-view", () -> {
		QueryView view = new QueryView();
		new QueryViewController(view);
		return view;
	}),

	/** View for validating RDF data (e.g., SHACL). */
	VALIDATION("validation-view", () -> {
		ValidationView view = new ValidationView();
		new ValidationController(view);
		return view;
	}),

	/** View listing graph-changing operations for traceability. */
	SYSTEM_LOGS("system-logs-view", () -> {
		SystemLogsView view = new SystemLogsView();
		new SystemLogsController(view);
		return view;
	}),

	/** Application configuration and preferences view. */
	SETTINGS("settings-view", () -> {
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

	/** Factory to create an instance of the view. */
	private final Supplier<AbstractView> factory;

	// ===========================================================================
	// Constructor
	// ===========================================================================

	ViewId(String id, Supplier<AbstractView> factory) {
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
	 * @return a newly created AbstractView instance, or {@code null} if no factory
	 *         exists
	 */
	public AbstractView createInstance() {
		return factory != null ? factory.get() : null;
	}

	@Override
	public String toString() {
		return id;
	}
}
