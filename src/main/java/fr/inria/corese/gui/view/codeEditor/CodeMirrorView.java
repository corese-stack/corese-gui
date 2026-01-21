package fr.inria.corese.gui.view.codeEditor;

import fr.inria.corese.gui.view.utils.AppTheme;
import fr.inria.corese.gui.view.utils.ThemeManager;
import java.net.URL;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
// JSException import removed to fix deprecation warning
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JavaFX wrapper around a simple web-based text editor using WebView.
 *
 * <p>This view provides a clean, minimal text editor with basic functionality for editing content
 * without external dependencies.
 *
 * <p><b>Base Features:</b>
 *
 * <ul>
 *   <li>Robust Resource Loading (works offline)
 *   <li>Bidirectional Binding (Java <-> JS)
 *   <li>Thread Safety (Platform.runLater)
 *   <li>Defensive JS Execution
 * </ul>
 */
public class CodeMirrorView extends VBox {
  private static final Logger logger = LoggerFactory.getLogger(CodeMirrorView.class);

  // Default path if none is provided
  public static final String DEFAULT_RESOURCE_PATH = "/fr/inria/corese/gui/web/editor.html";

  private final WebView webView;
  private final WebEngine webEngine;
  private final StringProperty contentProperty = new SimpleStringProperty("");
  private final JavaBridge bridge = new JavaBridge();

  // Customizable resource path
  private final String resourcePath;

  private boolean initialized = false;
  private boolean isInternalUpdate = false;
  private final boolean readOnly;

  // ==============================================================================================
  // Constructors
  // ==============================================================================================

  /** Default constructor using the default editor HTML path. */
  public CodeMirrorView() {
    this(DEFAULT_RESOURCE_PATH, false);
  }

  /**
   * Constructor with read-only option using the default editor HTML path.
   *
   * @param readOnly true to set the editor to read-only mode.
   */
  public CodeMirrorView(boolean readOnly) {
    this(DEFAULT_RESOURCE_PATH, readOnly);
  }

  /**
   * Fully customizable constructor.
   *
   * @param resourcePath The classpath location of the HTML editor file.
   * @param readOnly true to set the editor to read-only mode.
   */
  public CodeMirrorView(String resourcePath, boolean readOnly) {
    this.resourcePath = resourcePath;
    this.readOnly = readOnly;
    this.webView = new WebView();
    this.webEngine = webView.getEngine();

    initializeLayout();
    initializeListeners();

    // Load URL directly on the FX Thread
    Platform.runLater(this::loadEditorUrl);
  }

  // ==============================================================================================
  // Initialization Logic
  // ==============================================================================================

  private void initializeLayout() {
    // Disable default context menu (Reload, Print...)
    webView.setContextMenuEnabled(false);

    // Proper resizing logic
    webView.setPrefWidth(Region.USE_COMPUTED_SIZE);
    webView.setPrefHeight(Region.USE_COMPUTED_SIZE);
    webView.setMinHeight(0);
    webView.setMinWidth(0);

    setVgrow(webView, Priority.ALWAYS);
    getChildren().add(webView);
  }

  private void initializeListeners() {
    // Java -> JS Sync
    contentProperty.addListener(
        (obs, old, newValue) -> {
          if (initialized && newValue != null && !isInternalUpdate) {
            updateEditorContent(newValue);
          }
        });

    // Monitor loading state
    webEngine
        .getLoadWorker()
        .stateProperty()
        .addListener(
            (obs, oldState, newState) -> {
              if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
              } else if (newState == Worker.State.FAILED) {
                logger.error("Failed to load editor from: {}", resourcePath);
              }
            });

    // Theme Management Integration
    ThemeManager.getInstance()
        .themeProperty()
        .addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));
    ThemeManager.getInstance()
        .accentColorProperty()
        .addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));
  }

  /**
   * Pushes the current application theme configuration to the web editor.
   */
  private void updateTheme() {
    if (!initialized) return;

    ThemeManager tm = ThemeManager.getInstance();
    boolean isDark = false;
    String themeName = "default";

    // Determine if dark mode is active and get theme name
    if (tm.getTheme() != null) {
      AppTheme appTheme = AppTheme.fromTheme(tm.getTheme());
      if (appTheme != null) {
        isDark = appTheme.isDark();
        themeName = appTheme.getBaseName().toLowerCase();
      }
    }

    // Format accent color
    Color accent = tm.getAccentColor();
    String hexAccent = toHex(accent);

    // Call JS: setTheme(isDark, accentColor, themeName)
    String script = String.format("if(window.setTheme) window.setTheme(%b, '%s', '%s');", isDark, hexAccent, themeName);
    executeScriptSafe(script);
  }

  private String toHex(Color color) {
    if (color == null) return "#000000";
    return String.format(
        "#%02X%02X%02X",
        (int) (color.getRed() * 255),
        (int) (color.getGreen() * 255),
        (int) (color.getBlue() * 255));
  }

  /**
   * Loads the HTML file using its URL. This is crucial for relative paths (js/..., css/...) to
   * work.
   */
  private void loadEditorUrl() {
    URL url = getClass().getResource(resourcePath);
    if (url == null) {
      logger.error("Resource not found: {}", resourcePath);
      return;
    }
    webEngine.load(url.toExternalForm());
  }

  /** Called when the WebView DOM is ready. */
  private void onPageLoaded() {
    try {
      // 1. Inject Java Bridge
      JSObject window = (JSObject) webEngine.executeScript("window");
      window.setMember("bridge", bridge);
      initialized = true;

      // 2. Set Initial Content
      String initialContent = contentProperty.get();
      if (initialContent != null && !initialContent.isEmpty()) {
        updateEditorContent(initialContent);
      }

      // 3. Set ReadOnly if needed
      if (readOnly) {
        executeScriptSafe("if(window.setReadOnly) window.setReadOnly(true);");
      }

      // 4. Apply Theme
      updateTheme();

    } catch (Exception e) {
      logger.error("Error during editor initialization", e);
    }
  }

  // ==============================================================================================
  // Javascript Interaction Methods
  // ==============================================================================================

  /** * Executes JavaScript safely without crashing the Java thread. */
  private void executeScriptSafe(String script) {
    if (!initialized || webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
      return;
    }
    try {
      webEngine.executeScript(script);
    } catch (Exception e) {
      // JSException is a RuntimeException, so catching Exception covers it.
      // We removed the explicit JSException catch block to avoid using the deprecated class.
      logger.warn("JS Execution Warning: {}", e.getMessage());
    }
  }

  private void updateEditorContent(String content) {
    // Basic escaping strategy (No external dependencies)
    String escapedContent =
        content
            .replace("\\", "\\\\") // Escape backslashes first!
            .replace("'", "\\'") // Escape single quotes
            .replace("\n", "\\n") // Escape newlines
            .replace("\r", "\\r"); // Escape carriage returns

    // Call the JS function defined in editor.html
    String script =
        "if (typeof window.setContent === 'function') { window.setContent('"
            + escapedContent
            + "'); }";
    executeScriptSafe(script);
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  public void setContent(String content) {
    contentProperty.set(content);
  }

  public String getContent() {
    if (!initialized) return contentProperty.get();
    try {
      Object result =
          webEngine.executeScript(
              "typeof window.getContent === 'function' ? window.getContent() : ''");
      return result != null ? result.toString() : "";
    } catch (Exception e) {
      logger.error("Error retrieving content", e);
      return contentProperty.get();
    }
  }

  public StringProperty contentProperty() {
    return contentProperty;
  }

  // ==============================================================================================
  // Bridge Class
  // ==============================================================================================

  /** Bridge to allow JavaScript to call Java. Note: Methods must be public. */
  public class JavaBridge {
    public void onContentChanged(String newContent) {
      Platform.runLater(
          () -> {
            isInternalUpdate = true;
            contentProperty.set(newContent);
            isInternalUpdate = false;
          });
    }

    public void log(String message) {
      logger.debug("[JS]: {}", message);
    }
  }
}
