package fr.inria.corese.gui.component.editor;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.AppPreferences;
import fr.inria.corese.gui.core.theme.ThemeManager;
import java.net.URL;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JavaFX wrapper around a simple web-based text editor using WebView.
 *
 * <p>
 * This widget provides a clean, minimal text editor powered by CodeMirror
 * inside a WebView. It handles bidirectional communication between Java and
 * JavaScript for content, mode, and theme.
 */
public class CodeMirrorWidget extends VBox implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeMirrorWidget.class);

	// Constants
	private static final String DEFAULT_EDITOR_HTML_PATH = "/editor/code-editor.html";
	private static final SerializationFormat DEFAULT_MODE = SerializationFormat.TEXT;

	// Zoom Constants
	private static final double MIN_ZOOM = 0.5;
	private static final double MAX_ZOOM = 3.0;
	private static final double ZOOM_STEP = 0.1;
	private static final double DEFAULT_ZOOM = 1.0;
	private static final String PREF_EDITOR_ZOOM = "editor.zoom";
	private static final AppPreferences.Node PREFS = AppPreferences.nodeForClass(CodeMirrorWidget.class);

	// Components
	private final WebView webView;
	private final WebEngine webEngine;
	private final ThemeManager themeManager;

	// Properties
	private final StringProperty contentProperty = new SimpleStringProperty("");
	private final ObjectProperty<SerializationFormat> modeProperty = new SimpleObjectProperty<>(DEFAULT_MODE);
	private final DoubleProperty zoomProperty = new SimpleDoubleProperty(loadSavedZoom());

	// Bridge
	private final JavaBridge bridge = new JavaBridge();

	// State
	private final String editorHtmlPath;
	private final boolean readOnly;
	private boolean disposed = false;
	private boolean autoFormat = false;
	private boolean initialized = false;
	private boolean isInternalUpdate = false;
	private boolean pendingFocusRequest = false;
	private final ChangeListener<Worker.State> loadStateListener;
	private final ChangeListener<Theme> themeChangeListener;
	private final ChangeListener<Color> accentColorChangeListener;
	private final EventHandler<ScrollEvent> zoomScrollFilter;

	// ==============================================================================================
	// Constructors
	// ==============================================================================================

	/** Creates a new read-write editor with the default resource path. */
	public CodeMirrorWidget() {
		this(DEFAULT_EDITOR_HTML_PATH, false);
	}

	/**
	 * Creates a new editor with the default resource path.
	 *
	 * @param readOnly
	 *            true for read-only mode, false for editable
	 */
	public CodeMirrorWidget(boolean readOnly) {
		this(DEFAULT_EDITOR_HTML_PATH, readOnly);
	}

	/**
	 * Creates a new editor with a custom resource path.
	 *
	 * @param editorHtmlPath
	 *            path to the HTML editor resource
	 * @param readOnly
	 *            true for read-only mode, false for editable
	 */
	public CodeMirrorWidget(String editorHtmlPath, boolean readOnly) {
		this.editorHtmlPath = editorHtmlPath;
		this.readOnly = readOnly;
		this.autoFormat = readOnly;
		this.themeManager = ThemeManager.getInstance();
		this.webView = new WebView();
		this.webEngine = webView.getEngine();
		this.loadStateListener = createLoadStateListener();
		this.themeChangeListener = createThemeChangeListener();
		this.accentColorChangeListener = createAccentColorChangeListener();
		this.zoomScrollFilter = createZoomScrollFilter();

		initializeLayout();
		initializeListeners();

		Platform.runLater(this::loadEditorUrl);
	}

	// ==============================================================================================
	// Initialization
	// ==============================================================================================

	private void initializeLayout() {
		webView.setContextMenuEnabled(false);
		// Allow WebView to shrink properly
		webView.setPrefWidth(Region.USE_COMPUTED_SIZE);
		webView.setPrefHeight(Region.USE_COMPUTED_SIZE);
		webView.setMinHeight(0);
		webView.setMinWidth(0);

		setVgrow(webView, Priority.ALWAYS);
		getChildren().add(webView);
	}

	private void initializeListeners() {
		// Content synchronization (Java -> JS)
		contentProperty.addListener((obs, old, newValue) -> {
			if (!disposed && initialized && newValue != null && !isInternalUpdate) {
				updateEditorContent(newValue);
			}
		});

		// Mode synchronization (Java -> JS)
		modeProperty.addListener((obs, old, newValue) -> {
			if (!disposed && initialized && newValue != null) {
				applyMode(newValue);
			}
		});

		// Zoom synchronization (editor text only, not the whole WebView)
		zoomProperty.addListener((obs, oldVal, newVal) -> applyEditorZoom(newVal.doubleValue()));

		// Loading status listener
		webEngine.getLoadWorker().stateProperty().addListener(loadStateListener);

		// Theme synchronization
		themeManager.themeProperty().addListener(themeChangeListener);
		themeManager.accentColorProperty().addListener(accentColorChangeListener);

		// Mouse Scroll Zoom (Ctrl + Scroll)
		// We attach the filter to 'this' (VBox) to intercept events before they reach
		// the WebView
		this.addEventFilter(ScrollEvent.SCROLL, zoomScrollFilter);
	}

	private ChangeListener<Worker.State> createLoadStateListener() {
		return (obs, oldState, newState) -> {
			if (disposed) {
				return;
			}
			if (newState == Worker.State.SUCCEEDED) {
				onPageLoaded();
				return;
			}
			if (newState == Worker.State.FAILED) {
				LOGGER.error("Failed to load editor from: {}", this.editorHtmlPath);
			}
		};
	}

	private ChangeListener<Theme> createThemeChangeListener() {
		return (obs, oldTheme, newTheme) -> scheduleThemeUpdateIfActive();
	}

	private ChangeListener<Color> createAccentColorChangeListener() {
		return (obs, oldColor, newColor) -> scheduleThemeUpdateIfActive();
	}

	private EventHandler<ScrollEvent> createZoomScrollFilter() {
		return event -> {
			if (disposed || event == null || !event.isControlDown()) {
				return;
			}
			double delta = event.getDeltaY();
			if (delta > 0) {
				zoomIn();
			} else if (delta < 0) {
				zoomOut();
			}
			// Always consume the event to prevent actual scrolling of the page content.
			event.consume();
		};
	}

	private void scheduleThemeUpdateIfActive() {
		if (!disposed) {
			Platform.runLater(this::updateTheme);
		}
	}

	private void loadEditorUrl() {
		if (disposed) {
			return;
		}
		URL url = getClass().getResource(editorHtmlPath);
		if (url == null) {
			LOGGER.error("Resource not found: {}", editorHtmlPath);
			return;
		}
		webEngine.load(url.toExternalForm());
	}

	@SuppressWarnings("removal")
	private void onPageLoaded() {
		if (disposed) {
			return;
		}
		try {
			// Inject Java Bridge into JavaScript
			// Using deprecated JSObject as there's no official alternative yet
			JSObject window = (JSObject) webEngine.executeScript("window");
			window.setMember("bridge", bridge);
			initialized = true;

			// Restore state
			String initialContent = contentProperty.get();
			if (initialContent != null && !initialContent.isEmpty()) {
				updateEditorContent(initialContent);
			}

			if (readOnly) {
				executeScriptSafe("if(window.setReadOnly) window.setReadOnly(true);");
			}

			applyMode(modeProperty.get());
			updateTheme();

			// Apply initial zoom (editor only)
			applyEditorZoom(zoomProperty.get());
			applyAutoFormatSetting();
			applyPendingFocusRequest();

		} catch (Exception e) {
			LOGGER.error("Error during editor initialization", e);
		}
	}

	public void setAutoFormat(boolean enabled) {
		if (disposed) {
			return;
		}
		this.autoFormat = enabled;
		applyAutoFormatSetting();
	}

	private void applyAutoFormatSetting() {
		if (!initialized) {
			return;
		}
		executeScriptSafe("if (window.setAutoFormat) { window.setAutoFormat(" + autoFormat + "); }");
	}

	private void applyEditorZoom(double zoom) {
		if (!initialized) {
			return;
		}
		executeScriptSafe("if (window.setEditorZoom) { window.setEditorZoom(" + zoom + "); }");
	}

	// ==============================================================================================
	// Interop Logic (Java -> JS)
	// ==============================================================================================

	private void updateTheme() {
		if (!initialized)
			return;

		ThemeManager.WebThemeInfo webTheme = themeManager.getWebThemeInfo();
		String script = String.format("if(window.setTheme) window.setTheme(%b, '%s', '%s');", webTheme.dark(),
				webTheme.accentHex(), webTheme.themeName());
		executeScriptSafe(script);
	}

	private void updateEditorContent(String content) {
		// Escape JS string safely for arbitrary content (queries, XML, etc.).
		String script = "if (typeof window.setContent === 'function') { window.setContent(" + toJsString(content)
				+ "); }";
		executeScriptSafe(script);
	}

	private static String toJsString(String value) {
		if (value == null) {
			return "''";
		}
		StringBuilder sb = new StringBuilder(value.length() + 2);
		sb.append('\'');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '\\' -> sb.append("\\\\");
				case '\'' -> sb.append("\\'");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				case '\b' -> sb.append("\\b");
				case '\f' -> sb.append("\\f");
				case '\u2028' -> sb.append("\\u2028");
				case '\u2029' -> sb.append("\\u2029");
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
				}
			}
		}
		sb.append('\'');
		return sb.toString();
	}

	private void applyMode(SerializationFormat format) {
		String mode = format != null ? format.getCodeMirrorMode() : "text/plain";
		String script = String.format("if(window.setMode) window.setMode('%s');", mode);
		executeScriptSafe(script);
	}

	private void executeScriptSafe(String script) {
		if (disposed || !initialized || webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
			return;
		}
		try {
			webEngine.executeScript(script);
		} catch (Exception e) {
			LOGGER.warn("JS Execution Warning: {}", e.getMessage());
		}
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Requests focus for the embedded editor surface.
	 *
	 * <p>
	 * If the WebView page is not ready yet, the focus request is deferred and
	 * applied as soon as initialization completes.
	 * </p>
	 */
	public void requestEditorFocus() {
		if (disposed) {
			return;
		}
		pendingFocusRequest = true;
		Platform.runLater(this::applyPendingFocusRequest);
	}

	@Override
	public void requestFocus() {
		requestEditorFocus();
	}

	private void applyPendingFocusRequest() {
		if (!pendingFocusRequest || disposed || !initialized) {
			return;
		}
		webView.requestFocus();
		executeScriptSafe("if (typeof window.focusEditor === 'function') { window.focusEditor(); }");
		pendingFocusRequest = false;
	}

	/**
	 * Sets the editor content.
	 *
	 * @param content
	 *            The new content to set
	 */
	public void setContent(String content) {
		if (disposed) {
			return;
		}
		contentProperty.set(content);
	}

	/**
	 * Gets the current editor content.
	 *
	 * @return The current content of the editor
	 */
	public String getContent() {
		if (!initialized || disposed)
			return contentProperty.get();
		try {
			Object result = webEngine
					.executeScript("typeof window.getContent === 'function' ? window.getContent() : ''");
			return result != null ? result.toString() : "";
		} catch (Exception e) {
			LOGGER.error("Error retrieving content", e);
			return contentProperty.get();
		}
	}

	/**
	 * Sets the editor mode based on the given serialization format.
	 *
	 * @param format
	 *            The serialization format to set the mode for
	 */
	public void setMode(SerializationFormat format) {
		if (disposed) {
			return;
		}
		modeProperty.set(format);
	}

	/**
	 * Gets the content property of the editor.
	 *
	 * @return The content property
	 */
	public StringProperty contentProperty() {
		return contentProperty;
	}

	// ==============================================================================================
	// Zoom API
	// ==============================================================================================

	public void zoomIn() {
		setZoom(zoomProperty.get() + ZOOM_STEP, true);
	}

	public void zoomOut() {
		setZoom(zoomProperty.get() - ZOOM_STEP, true);
	}

	/**
	 * Applies zoom only to this widget instance without updating the global
	 * persisted user preference.
	 */
	public void zoomInForCurrentEditorOnly() {
		setZoom(zoomProperty.get() + ZOOM_STEP, false);
	}

	private void setZoom(double value, boolean persistPreference) {
		if (disposed) {
			return;
		}
		double clamped = clampZoom(value);
		zoomProperty.set(clamped);
		if (persistPreference) {
			saveZoom(clamped);
		}
	}

	@Override
	public void close() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(this::close);
			return;
		}
		if (disposed) {
			return;
		}
		disposed = true;
		initialized = false;

		themeManager.themeProperty().removeListener(themeChangeListener);
		themeManager.accentColorProperty().removeListener(accentColorChangeListener);
		webEngine.getLoadWorker().stateProperty().removeListener(loadStateListener);
		removeEventFilter(ScrollEvent.SCROLL, zoomScrollFilter);

		try {
			webEngine.load("about:blank");
		} catch (Exception e) {
			LOGGER.debug("Unable to unload editor web view", e);
		}
	}

	private static double loadSavedZoom() {
		try {
			return clampZoom(PREFS.getDouble(PREF_EDITOR_ZOOM, DEFAULT_ZOOM));
		} catch (SecurityException e) {
			LOGGER.debug("Unable to read editor zoom preference", e);
			return DEFAULT_ZOOM;
		}
	}

	private static void saveZoom(double zoom) {
		try {
			PREFS.putDouble(PREF_EDITOR_ZOOM, clampZoom(zoom));
		} catch (SecurityException e) {
			LOGGER.debug("Unable to persist editor zoom preference", e);
		}
	}

	private static double clampZoom(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return DEFAULT_ZOOM;
		}
		return Math.clamp(value, MIN_ZOOM, MAX_ZOOM);
	}

	// ==============================================================================================
	// Java Bridge (JS -> Java)
	// ==============================================================================================

	/** Bridge class exposed to JavaScript for callbacks. */
	public class JavaBridge {
		/**
		 * Called by JavaScript when the content changes.
		 *
		 * @param newContent
		 *            The new content of the editor
		 */
		public void onContentChanged(String newContent) {
			Platform.runLater(() -> {
				isInternalUpdate = true;
				contentProperty.set(newContent);
				isInternalUpdate = false;
			});
		}

		/**
		 * Called by JavaScript to log messages to the Java logger.
		 *
		 * @param message
		 *            The message to log
		 */
		public void log(String message) {
			LOGGER.debug("[JS]: {}", message);
		}

		/**
		 * Copies the provided text to the system clipboard.
		 *
		 * <p>
		 * This provides a reliable clipboard fallback for environments where WebView
		 * does not correctly propagate native copy shortcuts.
		 *
		 * @param text
		 *            text to copy
		 * @return true if the request was accepted
		 */
		public boolean copyToClipboard(String text) {
			String safeText = text != null ? text : "";
			Platform.runLater(() -> {
				ClipboardContent clipboardContent = new ClipboardContent();
				clipboardContent.putString(safeText);
				Clipboard.getSystemClipboard().setContent(clipboardContent);
			});
			return true;
		}
	}
}
