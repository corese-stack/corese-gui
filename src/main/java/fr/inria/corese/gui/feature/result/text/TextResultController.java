package fr.inria.corese.gui.feature.result.text;

import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.ExportHelper;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for the text-based result view.
 *
 * <p>
 * This controller manages:
 * <ul>
 * <li>The data flow of text results to the view.
 * <li>Format selection and corresponding syntax highlighting updates.
 * <li>User actions: Copy to Clipboard, Export to File, Zoom.
 * </ul>
 */
public class TextResultController implements AutoCloseable {

	private final TextResultView view;
	private Consumer<SerializationFormat> onFormatChanged;
	private static final Preferences PREFS = Preferences.userNodeForPackage(TextResultController.class);
	private static final String PREF_LAST_SPARQL_FORMAT = "results.lastSparqlFormat";
	private static final String PREF_LAST_RDF_FORMAT = "results.lastRdfFormat";
	private static SerializationFormat lastSparqlFormat;
	private static SerializationFormat lastRdfFormat;

	static {
		lastSparqlFormat = loadFormat(PREF_LAST_SPARQL_FORMAT);
		lastRdfFormat = loadFormat(PREF_LAST_RDF_FORMAT);
	}

	/**
	 * Creates a new TextResultController.
	 */
	public TextResultController() {
		this.view = new TextResultView();
		initialize();
	}

	private void initialize() {
		// 1. Setup Toolbar Actions
		view.setToolbarActions(List.of(ButtonFactory.copy(this::copyContent), ButtonFactory.export(this::exportContent),
				ButtonFactory.zoomIn(view::zoomIn), ButtonFactory.zoomOut(view::zoomOut)));

		// 2. Configure Format Selector (Default to Turtle or last used)
		applyAvailableFormats(SerializationFormat.rdfFormats(), SerializationFormat.TURTLE);

		// 3. Setup Listeners
		// When the user (or code) changes the format in the view, we update
		// highlighting and notify listeners.
		view.setOnFormatChanged((obs, oldVal, newVal) -> {
			if (newVal != null) {
				handleFormatChange(newVal);
			}
		});

		// 4. Initial State
		updateSyntaxHighlighting(view.getFormat());
	}

	// ==============================================================================================
	// Logic - Events & Updates
	// ==============================================================================================

	private void handleFormatChange(SerializationFormat newVal) {
		updateSyntaxHighlighting(newVal);
		rememberLastFormat(newVal);
		if (onFormatChanged != null) {
			onFormatChanged.accept(newVal);
		}
	}

	private void updateSyntaxHighlighting(SerializationFormat format) {
		if (format == null)
			return;
		view.setMode(format);
	}

	// ==============================================================================================
	// Actions
	// ==============================================================================================

	private void copyContent() {
		String text = view.getContent();
		if (text == null || text.isEmpty())
			return;

		ClipboardContent content = new ClipboardContent();
		content.putString(text);
		Clipboard.getSystemClipboard().setContent(content);
		NotificationWidget.getInstance().showSuccess("Result copied to clipboard");
	}

	private void exportContent() {
		ExportHelper.exportText(view.getRoot().getScene().getWindow(), view.getContent(), view.getFormat());
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Sets the content of the text editor. Thread-safe: can be called from any
	 * thread.
	 *
	 * @param content
	 *            The text content to display.
	 */
	public void setContent(String content) {
		Platform.runLater(() -> {
			view.setContent(Objects.requireNonNullElse(content, ""));
			// Re-apply highlighting for the current format to ensure the new content is
			// styled
			updateSyntaxHighlighting(view.getFormat());
		});
	}

	/**
	 * Clears the editor content. Thread-safe.
	 */
	public void clearContent() {
		Platform.runLater(() -> view.setContent(""));
	}

	/**
	 * Sets a listener to be notified when the serialization format changes.
	 *
	 * @param listener
	 *            The listener consuming the new format.
	 */
	public void setOnFormatChanged(Consumer<SerializationFormat> listener) {
		this.onFormatChanged = listener;
	}

	/**
	 * Gets the root view node.
	 *
	 * @return The JavaFX node for this controller's view.
	 */
	public Node getView() {
		return view.getRoot();
	}

	@Override
	public void close() {
		onFormatChanged = null;
		view.close();
	}

	/**
	 * Updates the list of available formats and sets the default selection.
	 * Thread-safe.
	 *
	 * @param formats
	 *            The available formats.
	 * @param defaultFormat
	 *            The format to select.
	 */
	public void setAvailableFormats(SerializationFormat[] formats, SerializationFormat defaultFormat) {
		Platform.runLater(() -> applyAvailableFormats(formats, defaultFormat));
	}

	public SerializationFormat getPreferredFormat(SerializationFormat[] formats, SerializationFormat defaultFormat) {
		return pickRememberedFormat(formats, defaultFormat);
	}

	private void applyAvailableFormats(SerializationFormat[] formats, SerializationFormat defaultFormat) {
		SerializationFormat effectiveDefault = pickRememberedFormat(formats, defaultFormat);
		view.configureFormatSelector(formats, effectiveDefault);
		rememberLastFormat(effectiveDefault);
		// If the format didn't change (same as old value), the listener won't fire.
		// We force an update to ensure consistency with the requested default.
		if (view.getFormat() == effectiveDefault) {
			updateSyntaxHighlighting(effectiveDefault);
		}
	}

	private static SerializationFormat pickRememberedFormat(SerializationFormat[] formats,
			SerializationFormat fallback) {
		if (containsFormat(formats, lastSparqlFormat)) {
			return lastSparqlFormat;
		}
		if (containsFormat(formats, lastRdfFormat)) {
			return lastRdfFormat;
		}
		return fallback;
	}

	private static void rememberLastFormat(SerializationFormat format) {
		if (format == null) {
			return;
		}
		if (isSparqlFormat(format)) {
			lastSparqlFormat = format;
			PREFS.put(PREF_LAST_SPARQL_FORMAT, format.name());
		} else if (isRdfFormat(format)) {
			lastRdfFormat = format;
			PREFS.put(PREF_LAST_RDF_FORMAT, format.name());
		}
	}

	private static boolean isSparqlFormat(SerializationFormat format) {
		return containsFormat(SerializationFormat.sparqlResultFormats(), format);
	}

	private static boolean isRdfFormat(SerializationFormat format) {
		return containsFormat(SerializationFormat.rdfFormats(), format);
	}

	private static boolean containsFormat(SerializationFormat[] formats, SerializationFormat target) {
		if (target == null || formats == null) {
			return false;
		}
		for (SerializationFormat format : formats) {
			if (format == target) {
				return true;
			}
		}
		return false;
	}

	private static SerializationFormat loadFormat(String key) {
		String value = PREFS.get(key, null);
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return SerializationFormat.valueOf(value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
