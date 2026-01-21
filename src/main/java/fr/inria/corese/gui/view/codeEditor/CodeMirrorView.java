package fr.inria.corese.gui.view.codeEditor;

import atlantafx.base.theme.Theme;
import fr.inria.corese.gui.view.utils.ThemeManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JavaFX wrapper around the CodeMirror JavaScript editor using WebView.
 *
 * <p>This view handles the bridge between Java and JavaScript, ensuring thread safety and robust
 * error handling during the initialization phase (loading of external JS libraries).
 */
public class CodeMirrorView extends VBox {
  private static final Logger logger = LoggerFactory.getLogger(CodeMirrorView.class);
  private static final String EDITOR_RESOURCE_PATH = "/fr/inria/corese/gui/codeMirror-editor.html";

  private final WebView webView;
  private final WebEngine webEngine;
  private final StringProperty contentProperty = new SimpleStringProperty("");
  private final JavaBridge bridge = new JavaBridge();

  private boolean initialized = false;
  private boolean isInternalUpdate = false;
  private boolean isDarkTheme = false;
  private final boolean readOnly;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  public CodeMirrorView() {
    this(false);
  }

  public CodeMirrorView(boolean readOnly) {
    this.readOnly = readOnly;
    this.webView = new WebView();
    this.webEngine = webView.getEngine();

    initializeLayout();
    initializeListeners();

    // Load the HTML content asynchronously
    Platform.runLater(this::loadEditorHtml);
  }

  // ==============================================================================================
  // Initialization Logic
  // ==============================================================================================

  private void initializeLayout() {
    // WebView configuration
    webView.setContextMenuEnabled(false);
    webView.setPrefWidth(Region.USE_COMPUTED_SIZE);
    webView.setPrefHeight(Region.USE_COMPUTED_SIZE);
    webView.setMinHeight(0);
    webView.setMinWidth(0);

    // VBox configuration
    setVgrow(webView, Priority.ALWAYS);
    setPrefHeight(Region.USE_COMPUTED_SIZE);
    setPrefWidth(Region.USE_COMPUTED_SIZE);
    setFillWidth(true);

    getChildren().add(webView);
  }

  private void initializeListeners() {
    // 1. Sync Java content -> JavaScript
    contentProperty.addListener(
        (obs, old, newValue) -> {
          if (initialized && newValue != null && !isInternalUpdate) {
            updateEditorContent(newValue);
          }
        });

    // 2. Handle Window Resize
    heightProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (initialized) {
                // Debounce could be added here if performance is an issue
                executeScriptSafe("if (window.editor) { editor.refresh(); }");
              }
            });

    // 3. Handle Theme Changes
    ThemeManager.getInstance()
        .themeProperty()
        .addListener(
            (obs, oldTheme, newTheme) -> {
              if (newTheme != null) {
                boolean isDark =
                    ThemeManager.getInstance()
                        .isDarkTheme(ThemeManager.getInstance().getCurrentThemeName());
                setTheme(isDark);
              }
            });

    // Initialize initial theme state
    Theme currentTheme = ThemeManager.getInstance().getTheme();
    if (currentTheme != null) {
      this.isDarkTheme =
          ThemeManager.getInstance().isDarkTheme(ThemeManager.getInstance().getCurrentThemeName());
    }
  }

  private void loadEditorHtml() {
    try {
      var resource = getClass().getResourceAsStream(EDITOR_RESOURCE_PATH);
      if (resource == null) {
        logger.error("CodeMirror HTML resource not found at: {}", EDITOR_RESOURCE_PATH);
        return;
      }
      String html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
      webEngine.loadContent(html);
    } catch (IOException e) {
      logger.error("Failed to read CodeMirror HTML resource", e);
    }

    // Monitor loading state
    webEngine
        .getLoadWorker()
        .stateProperty()
        .addListener(
            (obs, oldState, newState) -> {
              if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
              } else if (newState == Worker.State.FAILED) {
                logger.error("Failed to load CodeMirror web engine content.");
              }
            });
  }

  /**
   * Called when the WebView has finished loading the DOM. Note: External scripts (CDN) might still
   * be processing or have failed.
   */
  private void onPageLoaded() {
    try {
      // 1. Inject Java Bridge (Critical)
      JSObject window = (JSObject) webEngine.executeScript("window");
      window.setMember("bridge", bridge);
      initialized = true;

      // 2. Set Initial Content
      String initialContent = contentProperty.get();
      if (initialContent != null && !initialContent.isEmpty()) {
        updateEditorContent(initialContent);
      }

      // 3. Set ReadOnly Mode if needed
      if (readOnly) {
        executeScriptSafe("if (window.editor) { editor.setOption('readOnly', true); }");
      }

      // 4. Apply Theme (Safely)
      safeSetTheme(isDarkTheme);

    } catch (Exception e) {
      logger.error("Error during CodeMirror JS initialization", e);
    }
  }

  // ==============================================================================================
  // Javascript Interaction Methods (Defensive Programming)
  // ==============================================================================================

  /**
   * Executes JavaScript safely, catching specific JS exceptions to prevent Java crashes. This
   * solves the "undefined is not a function" crash.
   */
  private void executeScriptSafe(String script) {
    if (!initialized || webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
      return;
    }
    try {
      webEngine.executeScript(script);
    } catch (JSException e) {
      // Log warning but do not crash the application
      logger.warn("JavaScript Execution Warning: {}", e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected error executing JavaScript", e);
    }
  }

  private void updateEditorContent(String content) {
    String escapedContent =
        content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");

    // Check if setContent exists before calling it
    String script =
        "if (typeof window.setContent === 'function') { window.setContent('"
            + escapedContent
            + "'); }";
    executeScriptSafe(script);
  }

  private void safeSetTheme(boolean isDark) {
    String theme = isDark ? "dracula" : "eclipse";
    ThemeColors colors = getThemeColors(isDark);

    // Construct a robust JS script that checks function existence
    String script =
        """
            if (typeof window.setTheme === 'function') {
                window.setTheme('%s');
            }
            if (document && document.body) {
                document.body.style.backgroundColor = '%s';
            }
            if (typeof window.setStatusBarTheme === 'function') {
                window.setStatusBarTheme('%s', '%s', '%s');
            }
        """
            .formatted(
                theme,
                colors.backgroundColor,
                colors.statusBarBg,
                colors.statusBarText,
                colors.statusBarBorder);

    executeScriptSafe(script);
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  public void setTheme(boolean isDark) {
    this.isDarkTheme = isDark;
    if (initialized) {
      safeSetTheme(isDark);
    }
  }

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
      logger.error("Error getting editor content", e);
      return contentProperty.get(); // Fallback to last known Java state
    }
  }

  public StringProperty contentProperty() {
    return contentProperty;
  }

  // ==============================================================================================
  // Internal Helper Classes
  // ==============================================================================================

  /** Bridge class to allow JavaScript to call Java methods. */
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
      logger.debug("[JS Log]: {}", message);
    }
  }

  private ThemeColors getThemeColors(boolean isDark) {
    String backgroundColor = isDark ? "#2e3440" : "#ffffff";
    String statusBarBg = isDark ? "#3b4252" : "#f3f3f3";
    String statusBarText = isDark ? "#d8dee9" : "#666666";
    String statusBarBorder = isDark ? "#4c566a" : "#dddddd";

    String themeName = ThemeManager.getInstance().getCurrentThemeName();

    if (themeName != null) {
      if (themeName.contains("PRIMER")) {
        backgroundColor = isDark ? "#0d1117" : "#ffffff";
        statusBarBg = isDark ? "#161b22" : "#f6f8fa";
        statusBarText = isDark ? "#8b949e" : "#57606a";
        statusBarBorder = isDark ? "#30363d" : "#d0d7de";
      } else if (themeName.contains("NORD")) {
        backgroundColor = isDark ? "#2e3440" : "#eceff4";
        statusBarBg = isDark ? "#3b4252" : "#e5e9f0";
        statusBarText = isDark ? "#d8dee9" : "#4c566a";
        statusBarBorder = isDark ? "#4c566a" : "#d8dee9";
      } else if (themeName.contains("CUPERTINO")) {
        backgroundColor = isDark ? "#1c1c1e" : "#f2f2f7";
        statusBarBg = isDark ? "#2c2c2e" : "#e5e5ea";
        statusBarText = isDark ? "#aeaeb2" : "#8e8e93";
        statusBarBorder = isDark ? "#3a3a3c" : "#c6c6c8";
      } else if (themeName.contains("DRACULA")) {
        backgroundColor = "#282a36";
        statusBarBg = "#44475a";
        statusBarText = "#f8f8f2";
        statusBarBorder = "#6272a4";
      }
    }
    return new ThemeColors(backgroundColor, statusBarBg, statusBarText, statusBarBorder);
  }

  private static class ThemeColors {
    final String backgroundColor;
    final String statusBarBg;
    final String statusBarText;
    final String statusBarBorder;

    ThemeColors(String bg, String sbBg, String sbText, String sbBorder) {
      this.backgroundColor = bg;
      this.statusBarBg = sbBg;
      this.statusBarText = sbText;
      this.statusBarBorder = sbBorder;
    }
  }
}
