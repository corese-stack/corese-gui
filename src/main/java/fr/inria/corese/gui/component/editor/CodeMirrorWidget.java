package fr.inria.corese.gui.component.editor;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.theme.AppThemeRegistry;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.theme.ThemeManager;
import java.net.URL;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
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
 * inside a WebView. It
 * handles bidirectional communication between Java and JavaScript for content,
 * mode, and theme.
 */
public class CodeMirrorWidget extends VBox {
  private static final Logger logger = LoggerFactory.getLogger(CodeMirrorWidget.class);

  // Constants
  private static final String DEFAULT_EDITOR_HTML_PATH = "/editor/code-editor.html";
  private static final SerializationFormat DEFAULT_MODE = SerializationFormat.TEXT;

  // Zoom Constants
  private static final double MIN_ZOOM = 0.5;
  private static final double MAX_ZOOM = 3.0;
  private static final double ZOOM_STEP = 0.1;
  private static final double DEFAULT_ZOOM = 1.0;

  // Components
  private final WebView webView;
  private final WebEngine webEngine;

  // Properties
  private final StringProperty contentProperty = new SimpleStringProperty("");
  private final ObjectProperty<SerializationFormat> modeProperty = new SimpleObjectProperty<>(DEFAULT_MODE);
  private final DoubleProperty zoomProperty = new SimpleDoubleProperty(DEFAULT_ZOOM);

  // Bridge
  private final JavaBridge bridge = new JavaBridge();

  // State
  private final String editorHtmlPath;
  private final boolean readOnly;
  private boolean initialized = false;
  private boolean isInternalUpdate = false;

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
   * @param readOnly true for read-only mode, false for editable
   */
  public CodeMirrorWidget(boolean readOnly) {
    this(DEFAULT_EDITOR_HTML_PATH, readOnly);
  }

  /**
   * Creates a new editor with a custom resource path.
   *
   * @param editorHtmlPath path to the HTML editor resource
   * @param readOnly       true for read-only mode, false for editable
   */
  public CodeMirrorWidget(String editorHtmlPath, boolean readOnly) {
    this.editorHtmlPath = editorHtmlPath;
    this.readOnly = readOnly;
    this.webView = new WebView();
    this.webEngine = webView.getEngine();

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
    contentProperty.addListener(
        (obs, old, newValue) -> {
          if (initialized && newValue != null && !isInternalUpdate) {
            updateEditorContent(newValue);
          }
        });

    // Mode synchronization (Java -> JS)
    modeProperty.addListener(
        (obs, old, newValue) -> {
          if (initialized && newValue != null) {
            applyMode(newValue);
          }
        });

    // Zoom synchronization
    zoomProperty.addListener((obs, oldVal, newVal) -> webView.setZoom(newVal.doubleValue()));

    // Loading status listener
    webEngine
        .getLoadWorker()
        .stateProperty()
        .addListener(
            (obs, oldState, newState) -> {
              if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
              } else if (newState == Worker.State.FAILED) {
                logger.error("Failed to load editor from: {}", editorHtmlPath);
              }
            });

    // Theme synchronization
    ThemeManager.getInstance()
        .themeProperty()
        .addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));
    ThemeManager.getInstance()
        .accentColorProperty()
        .addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));

    // Mouse Scroll Zoom (Ctrl + Scroll)
    // We attach the filter to 'this' (VBox) to intercept events before they reach
    // the WebView
    this.addEventFilter(
        ScrollEvent.SCROLL,
        event -> {
          if (event.isControlDown()) {
            // Adjust zoom based on scroll direction
            // DeltaY is usually positive for scrolling up (zoom in) and negative for down
            // (zoom
            // out)
            double delta = event.getDeltaY();
            if (delta > 0) {
              zoomIn();
            } else if (delta < 0) {
              zoomOut();
            }
            // Always consume the event to prevent actual scrolling of the page content
            event.consume();
          }
        });
  }

  private void loadEditorUrl() {
    URL url = getClass().getResource(editorHtmlPath);
    if (url == null) {
      logger.error("Resource not found: {}", editorHtmlPath);
      return;
    }
    webEngine.load(url.toExternalForm());
  }

  @SuppressWarnings("removal")
  private void onPageLoaded() {
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

      // Apply initial zoom
      webView.setZoom(zoomProperty.get());

    } catch (Exception e) {
      logger.error("Error during editor initialization", e);
    }
  }

  // ==============================================================================================
  // Interop Logic (Java -> JS)
  // ==============================================================================================

  private void updateTheme() {
    if (!initialized)
      return;

    ThemeManager tm = ThemeManager.getInstance();
    boolean isDark = false;
    String themeName = "default";

    if (tm.getTheme() != null) {
      AppThemeRegistry appTheme = AppThemeRegistry.fromTheme(tm.getTheme());
      if (appTheme != null) {
        isDark = appTheme.isDark();
        themeName = appTheme.getBaseName().toLowerCase();
      }
    }

    Color accent = tm.getAccentColor();
    String hexAccent = CssUtils.toHex(accent);

    String script = String.format(
        "if(window.setTheme) window.setTheme(%b, '%s', '%s');", isDark, hexAccent, themeName);
    executeScriptSafe(script);
  }

  private void updateEditorContent(String content) {
    // Escape JS string safely for arbitrary content (queries, XML, etc.).
    String script = "if (typeof window.setContent === 'function') { window.setContent("
        + toJsString(content)
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
    if (!initialized || webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
      return;
    }
    try {
      webEngine.executeScript(script);
    } catch (Exception e) {
      logger.warn("JS Execution Warning: {}", e.getMessage());
    }
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Sets the editor content.
   * 
   * @param content The new content to set
   */
  public void setContent(String content) {
    contentProperty.set(content);
  }

  /**
   * Gets the current editor content.
   * 
   * @return The current content of the editor
   */
  public String getContent() {
    if (!initialized)
      return contentProperty.get();
    try {
      Object result = webEngine.executeScript(
          "typeof window.getContent === 'function' ? window.getContent() : ''");
      return result != null ? result.toString() : "";
    } catch (Exception e) {
      logger.error("Error retrieving content", e);
      return contentProperty.get();
    }
  }

  /**
   * Sets the editor mode based on the given serialization format.
   * 
   * @param format The serialization format to set the mode for
   */
  public void setMode(SerializationFormat format) {
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
    setZoom(zoomProperty.get() + ZOOM_STEP);
  }

  public void zoomOut() {
    setZoom(zoomProperty.get() - ZOOM_STEP);
  }

  private void setZoom(double value) {
    double clamped = Math.clamp(value, MIN_ZOOM, MAX_ZOOM);
    zoomProperty.set(clamped);
  }

  // ==============================================================================================
  // Java Bridge (JS -> Java)
  // ==============================================================================================

  /** Bridge class exposed to JavaScript for callbacks. */
  public class JavaBridge {
    /**
     * Called by JavaScript when the content changes.
     *
     * @param newContent The new content of the editor
     */
    public void onContentChanged(String newContent) {
      Platform.runLater(
          () -> {
            isInternalUpdate = true;
            contentProperty.set(newContent);
            isInternalUpdate = false;
          });
    }

    /**
     * Called by JavaScript to log messages to the Java logger.
     *
     * @param message The message to log
     */
    public void log(String message) {
      logger.debug("[JS]: {}", message);
    }
  }
}
