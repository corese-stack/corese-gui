package fr.inria.corese.gui.component.button.factory;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;

/**
 * Factory for creating standardized button configurations.
 *
 * <p>
 * This utility class provides factory methods for creating {@link ButtonConfig}
 * instances with consistent icons and tooltips across the application. Using
 * this factory instead of creating ButtonConfig instances manually ensures:
 *
 * <ul>
 * <li><b>Consistency:</b> Same tooltip wording for the same action everywhere
 * <li><b>Centralization:</b> Single place to update tooltip text
 * <li><b>Type safety:</b> Clear method names for each button type
 * </ul>
 *
 * <p>
 * <b>Design pattern:</b> This follows the Factory pattern for creating
 * pre-configured objects.
 *
 * <p>
 * <b>Usage example:</b>
 *
 * <pre>{@code
 * List<ButtonConfig> buttons = List.of(AppButtons.save(() -> saveFile()), AppButtons.undo(() -> undo()),
 * 		AppButtons.redo(() -> redo()));
 *
 * // Or with null action to be wired later by controller
 * ButtonConfig config = AppButtons.save(null);
 * }</pre>
 *
 * @see ButtonConfig
 * @see ButtonIcon
 */
public final class ButtonFactory {

	// ===============================================================================
	// Constructor
	// ===============================================================================

	/** Private constructor to prevent instantiation of this utility class. */
	private ButtonFactory() {
		throw new AssertionError("Utility class - do not instantiate");
	}

	// ===============================================================================
	// Custom Configuration
	// ===============================================================================

	/**
	 * Creates a custom button configuration with non-standard icon/tooltip
	 * combination.
	 *
	 * <p>
	 * Use this method when you need a button that doesn't fit the standard factory
	 * methods (e.g., context-specific tooltips like "Copy SVG" instead of "Copy to
	 * Clipboard").
	 *
	 * <p>
	 * <b>Note:</b> Prefer using standard factory methods when possible to maintain
	 * consistency.
	 *
	 * @param icon
	 *            The icon to display
	 * @param tooltip
	 *            The custom tooltip text
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with custom settings
	 */
	public static ButtonConfig custom(ButtonIcon icon, String tooltip, Runnable action) {
		return new ButtonConfig(icon, tooltip, action);
	}

	/** Creates a custom button configuration with no action bound yet. */
	public static ButtonConfig custom(ButtonIcon icon, String tooltip) {
		return custom(icon, tooltip, null);
	}

	// ===============================================================================
	// File Operations
	// ===============================================================================

	/**
	 * Creates a standardized Save button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with save icon and tooltip
	 */
	public static ButtonConfig save(Runnable action) {
		return new ButtonConfig(ButtonIcon.SAVE, "Save", action);
	}

	/** Creates a Save button configuration with no action bound yet. */
	public static ButtonConfig save() {
		return save(null);
	}

	/**
	 * Creates a standardized Export button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with export icon and tooltip
	 */
	public static ButtonConfig export(Runnable action) {
		return new ButtonConfig(ButtonIcon.EXPORT, "Export to File", action);
	}

	/** Creates an Export button configuration with no action bound yet. */
	public static ButtonConfig export() {
		return export(null);
	}

	/**
	 * Creates a standardized RDF data export button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with data-export icon and tooltip
	 */
	public static ButtonConfig exportData(Runnable action) {
		return new ButtonConfig(ButtonIcon.EXPORT_DATA, "Export RDF Data", action);
	}

	/** Creates an RDF data export button configuration with no action bound yet. */
	public static ButtonConfig exportData() {
		return exportData(null);
	}

	/**
	 * Creates a standardized visual graph export button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig for visual graph exports
	 */
	public static ButtonConfig exportGraph(Runnable action) {
		return new ButtonConfig(ButtonIcon.EXPORT, "Export Graph (SVG/PNG/PDF)", action);
	}

	/**
	 * Creates a visual graph export button configuration with no action bound yet.
	 */
	public static ButtonConfig exportGraph() {
		return exportGraph(null);
	}

	/**
	 * Creates an Export SVG button configuration.
	 *
	 * <p>
	 * Specialized variant for exporting SVG content specifically.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with export icon and SVG-specific tooltip
	 */
	public static ButtonConfig exportSvg(Runnable action) {
		return new ButtonConfig(ButtonIcon.EXPORT, "Export SVG", action);
	}

	/**
	 * Creates a standardized Open File button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with open file icon and tooltip
	 */
	public static ButtonConfig openFile(Runnable action) {
		return new ButtonConfig(ButtonIcon.OPEN_FILE, "Open File", action);
	}

	/** Creates an Open File button configuration with no action bound yet. */
	public static ButtonConfig openFile() {
		return openFile(null);
	}

	/**
	 * Creates a standardized "Load from URI" button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with URI icon and tooltip
	 */
	public static ButtonConfig openUri(Runnable action) {
		return new ButtonConfig(ButtonIcon.OPEN_URI, "Load from URI", action);
	}

	/** Creates a "Load from URI" button configuration with no action bound yet. */
	public static ButtonConfig openUri() {
		return openUri(null);
	}

	/**
	 * Creates a standardized New Tab button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with new tab icon and tooltip
	 */
	public static ButtonConfig newTab(Runnable action) {
		return new ButtonConfig(ButtonIcon.NEW_TAB, "New Tab", action);
	}

	/** Creates a New Tab button configuration with no action bound yet. */
	public static ButtonConfig newTab() {
		return newTab(null);
	}

	/**
	 * Creates a standardized Query Template button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with template icon and tooltip
	 */
	public static ButtonConfig template(Runnable action) {
		return new ButtonConfig(ButtonIcon.TEMPLATE, "Query Templates", action);
	}

	/** Creates a Query Template button configuration with no action bound yet. */
	public static ButtonConfig template() {
		return template(null);
	}

	// ===============================================================================
	// Editor Operations
	// ===============================================================================

	/**
	 * Creates a standardized Undo button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with undo icon and tooltip
	 */
	public static ButtonConfig undo(Runnable action) {
		return new ButtonConfig(ButtonIcon.UNDO, "Undo", action);
	}

	/** Creates an Undo button configuration with no action bound yet. */
	public static ButtonConfig undo() {
		return undo(null);
	}

	/**
	 * Creates a standardized Redo button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with redo icon and tooltip
	 */
	public static ButtonConfig redo(Runnable action) {
		return new ButtonConfig(ButtonIcon.REDO, "Redo", action);
	}

	/** Creates a Redo button configuration with no action bound yet. */
	public static ButtonConfig redo() {
		return redo(null);
	}

	/**
	 * Creates a standardized Copy to Clipboard button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with copy icon and tooltip
	 */
	public static ButtonConfig copy(Runnable action) {
		return new ButtonConfig(ButtonIcon.COPY, "Copy to Clipboard", action);
	}

	/** Creates a Copy button configuration with no action bound yet. */
	public static ButtonConfig copy() {
		return copy(null);
	}

	/**
	 * Creates a standardized Copy Selection button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with copy-selection icon and tooltip
	 */
	public static ButtonConfig copySelection(Runnable action) {
		return new ButtonConfig(ButtonIcon.COPY_SELECTION, "Copy Selection", action);
	}

	/**
	 * Creates a Copy SVG button configuration.
	 *
	 * <p>
	 * Specialized variant for copying SVG content specifically.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with copy icon and SVG-specific tooltip
	 */
	public static ButtonConfig copySvg(Runnable action) {
		return new ButtonConfig(ButtonIcon.COPY, "Copy SVG", action);
	}

	/**
	 * Creates a standardized Zoom In button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with zoom in icon and tooltip
	 */
	public static ButtonConfig zoomIn(Runnable action) {
		return new ButtonConfig(ButtonIcon.ZOOM_IN, "Zoom In", action);
	}

	/**
	 * Creates a standardized Zoom Out button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with zoom out icon and tooltip
	 */
	public static ButtonConfig zoomOut(Runnable action) {
		return new ButtonConfig(ButtonIcon.ZOOM_OUT, "Zoom Out", action);
	}

	/**
	 * Creates a Reset Layout button configuration.
	 *
	 * <p>
	 * Specialized variant for resetting graph layouts.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with reload icon and layout-specific
	 *         tooltip
	 */
	public static ButtonConfig resetLayout(Runnable action) {
		return new ButtonConfig(ButtonIcon.LAYOUT_FORCE, "Re-energize Layout", action);
	}

	/**
	 * Creates a standardized Reload button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with reload icon and tooltip
	 */
	public static ButtonConfig reload(Runnable action) {
		return new ButtonConfig(ButtonIcon.RELOAD, "Reload Sources", action);
	}

	/** Creates a Reload button configuration with no action bound yet. */
	public static ButtonConfig reload() {
		return reload(null);
	}

	/**
	 * Creates a standardized Clear Graph button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with clear icon and tooltip
	 */
	public static ButtonConfig clearGraph(Runnable action) {
		return new ButtonConfig(ButtonIcon.CLEAR, "Clear Graph", action);
	}

	/** Creates a Clear Graph button configuration with no action bound yet. */
	public static ButtonConfig clearGraph() {
		return clearGraph(null);
	}

	// ===============================================================================
	// Execution Operations
	// ===============================================================================

	/**
	 * Creates a standardized Play/Run button configuration.
	 *
	 * @param action
	 *            The action to execute when clicked, or null if wired later
	 * @return A configured ButtonConfig with play icon and tooltip
	 */
	public static ButtonConfig play(Runnable action) {
		return new ButtonConfig(ButtonIcon.PLAY, "Run", action);
	}

}
