package fr.inria.corese.gui.component.editor;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.utils.AppTheme;
import fr.inria.corese.gui.utils.ThemeManager;
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
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JavaFX wrapper around a simple web-based text editor using WebView. This widget provides a
 * clean, minimal text editor.
 */
@SuppressWarnings("removal")
public class CodeMirrorWidget extends VBox {
  private static final Logger logger = LoggerFactory.getLogger(CodeMirrorWidget.class);

  public static final String DEFAULT_RESOURCE_PATH = "/fr/inria/corese/gui/web/editor.html";

  private final WebView webView;
  private final WebEngine webEngine;
  private final StringProperty contentProperty = new SimpleStringProperty("");
  private final StringProperty modeProperty = new SimpleStringProperty("text/plain");
  private final JavaBridge bridge = new JavaBridge();

  private final String resourcePath;

  private boolean initialized = false;
  private boolean isInternalUpdate = false;
  private final boolean readOnly;

  public CodeMirrorWidget() {
    this(DEFAULT_RESOURCE_PATH, false);
  }

  public CodeMirrorWidget(boolean readOnly) {
    this(DEFAULT_RESOURCE_PATH, readOnly);
  }

  public CodeMirrorWidget(String resourcePath, boolean readOnly) {
    this.resourcePath = resourcePath;
    this.readOnly = readOnly;
    this.webView = new WebView();
    this.webEngine = webView.getEngine();

    initializeLayout();
    initializeListeners();

    Platform.runLater(this::loadEditorUrl);
  }

  private void initializeLayout() {
    webView.setContextMenuEnabled(false);
    webView.setPrefWidth(Region.USE_COMPUTED_SIZE);
    webView.setPrefHeight(Region.USE_COMPUTED_SIZE);
    webView.setMinHeight(0);
    webView.setMinWidth(0);

    setVgrow(webView, Priority.ALWAYS);
    getChildren().add(webView);
  }

  private void initializeListeners() {
    contentProperty.addListener(
        (obs, old, newValue) -> {
          if (initialized && newValue != null && !isInternalUpdate) {
            updateEditorContent(newValue);
          }
        });

    modeProperty.addListener(
        (obs, old, newValue) -> {
          if (initialized && newValue != null) {
            applyMode(newValue);
          }
        });

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

    ThemeManager.getInstance()
        .themeProperty()
        .addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));
    ThemeManager.getInstance()
        .accentColorProperty()
        .addListener((obs, old, newVal) -> Platform.runLater(this::updateTheme));
  }

  private void updateTheme() {
    if (!initialized) return;

    ThemeManager tm = ThemeManager.getInstance();
    boolean isDark = false;
    String themeName = "default";

    if (tm.getTheme() != null) {
      AppTheme appTheme = AppTheme.fromTheme(tm.getTheme());
      if (appTheme != null) {
        isDark = appTheme.isDark();
        themeName = appTheme.getBaseName().toLowerCase();
      }
    }

    Color accent = tm.getAccentColor();
    String hexAccent = toHex(accent);

    String script =
        String.format(
            "if(window.setTheme) window.setTheme(%b, '%s', '%s');", isDark, hexAccent, themeName);
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

  private void loadEditorUrl() {
    URL url = getClass().getResource(resourcePath);
    if (url == null) {
      logger.error("Resource not found: {}", resourcePath);
      return;
    }
    webEngine.load(url.toExternalForm());
  }

  private void onPageLoaded() {
    try {
      JSObject window = (JSObject) webEngine.executeScript("window");
      window.setMember("bridge", bridge);
      initialized = true;

      String initialContent = contentProperty.get();
      if (initialContent != null && !initialContent.isEmpty()) {
        updateEditorContent(initialContent);
      }

      if (readOnly) {
        executeScriptSafe("if(window.setReadOnly) window.setReadOnly(true);");
      }

      applyMode(modeProperty.get());
      updateTheme();

    } catch (Exception e) {
      logger.error("Error during editor initialization", e);
    }
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

  private void applyMode(String mode) {
    String script = String.format("if(window.setMode) window.setMode('%s');", mode);
    executeScriptSafe(script);
  }

  public void setContent(String content) {
    contentProperty.set(content);
  }

  public void setMode(SerializationFormat format) {
    if (format != null) {
      setMode(format.getCodeMirrorMode());
    }
  }

  public void setMode(String mode) {
    if (mode == null || mode.isEmpty()) return;
    Platform.runLater(() -> modeProperty.set(mode));
  }

  public StringProperty modeProperty() {
    return modeProperty;
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
