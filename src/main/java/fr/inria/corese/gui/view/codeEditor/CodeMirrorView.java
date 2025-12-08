package fr.inria.corese.gui.view.codeEditor;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.inria.corese.gui.view.utils.ThemeManager;
import atlantafx.base.theme.Theme;

public class CodeMirrorView extends VBox {
  private static final Logger logger = LoggerFactory.getLogger(CodeMirrorView.class);

  private final WebView webView;
  private final WebEngine webEngine;
  private final StringProperty contentProperty = new SimpleStringProperty("");
  private boolean initialized = false;
  private boolean isInternalUpdate = false;
  private boolean isDarkTheme = false;
  private final JavaBridge bridge = new JavaBridge();

  public CodeMirrorView() {
    webView = new WebView();
    webEngine = webView.getEngine();

    // Configuration du WebView
    webView.setContextMenuEnabled(false);

    // Permettre au WebView de se redimensionner
    webView.setPrefWidth(Region.USE_COMPUTED_SIZE);
    webView.setPrefHeight(Region.USE_COMPUTED_SIZE);
    webView.setMinHeight(0);
    webView.setMinWidth(0);

    // Configuration VBox
    setVgrow(webView, Priority.ALWAYS);
    setPrefHeight(Region.USE_COMPUTED_SIZE);
    setPrefWidth(Region.USE_COMPUTED_SIZE);
    setFillWidth(true);

    getChildren().add(webView);

    Platform.runLater(this::initializeEditor);

    contentProperty.addListener(
        (obs, old, newValue) -> {
          if (initialized && newValue != null && !isInternalUpdate) {
            updateEditorContent(newValue);
          }
        });

    // Ajouter un listener de redimensionnement
    heightProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (initialized) {
                Platform.runLater(
                    () -> {
                      webEngine.executeScript("editor.refresh();");
                    });
              }
            });

    // Initialize theme state
    Theme currentTheme = ThemeManager.getInstance().getTheme();
    if (currentTheme != null) {
        this.isDarkTheme = ThemeManager.getInstance().isDarkTheme(ThemeManager.getInstance().getCurrentThemeName());
    }

    // Listen to theme changes
    ThemeManager.getInstance().themeProperty().addListener((obs, oldTheme, newTheme) -> {
        if (newTheme != null) {
            boolean isDark = ThemeManager.getInstance().isDarkTheme(ThemeManager.getInstance().getCurrentThemeName());
            setTheme(isDark);
        }
    });
  }

  @SuppressWarnings("removal")
  private void initializeEditor() {
    try {
      String html = new String(getClass().getResourceAsStream("/fr/inria/corese/gui/codeMirror-editor.html").readAllBytes());
      webEngine.loadContent(html);
    } catch (Exception e) {
      logger.error("Failed to load CodeMirror HTML resource", e);
    }

    webEngine
        .getLoadWorker()
        .stateProperty()
        .addListener(
            (obs, old, newState) -> {
              switch (newState) {
                case SUCCEEDED:
                  try {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("bridge", bridge);
                    initialized = true;

                    String content = contentProperty.get();
                    if (content != null && !content.isEmpty()) {
                      updateEditorContent(content);
                    }
                    
                    // Apply saved theme
                    setTheme(isDarkTheme);
                    
                  } catch (Exception e) {
                    logger.error("Error during CodeMirror initialization", e);
                  }
                  break;
                case FAILED:
                  logger.error("Failed to load CodeMirror editor");
                  break;
                case READY, SCHEDULED, RUNNING, CANCELLED:
                  // Pas d'action nécessaire pour ces états
                  break;
              }
            });
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
  }

  private void updateEditorContent(String content) {
    if (initialized) {
      try {
        String escapedContent =
            content
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        webEngine.executeScript(String.format("window.setContent('%s');", escapedContent));
      } catch (Exception e) {
        logger.error("Error updating editor content", e);
      }
    }
  }

  public String getContent() {
    if (!initialized) return contentProperty.get();
    try {
      return (String) webEngine.executeScript("window.getContent()");
    } catch (Exception e) {
      logger.error("Error getting editor content", e);
      return "";
    }
  }

  public void setContent(String content) {
    contentProperty.set(content);
  }

  public StringProperty contentProperty() {
    return contentProperty;
  }

  public void setTheme(boolean isDark) {
    this.isDarkTheme = isDark;
    if (initialized) {
      String theme = isDark ? "dracula" : "eclipse";
      webEngine.executeScript("window.setTheme('" + theme + "')");
      
      ThemeColors colors = getThemeColors(isDark);
      System.out.println("Setting theme: isDark=" + isDark + ", bg=" + colors.backgroundColor + ", statusBg=" + colors.statusBarBg);
      
      webEngine.executeScript("document.body.style.backgroundColor = '" + colors.backgroundColor + "';");
      webEngine.executeScript(String.format("window.setStatusBarTheme('%s', '%s', '%s');", colors.statusBarBg, colors.statusBarText, colors.statusBarBorder));
    }
  }

  private ThemeColors getThemeColors(boolean isDark) {
      String backgroundColor = isDark ? "#2e3440" : "#ffffff";
      String statusBarBg = isDark ? "#3b4252" : "#f3f3f3";
      String statusBarText = isDark ? "#d8dee9" : "#666666";
      String statusBarBorder = isDark ? "#4c566a" : "#dddddd";

      String themeName = ThemeManager.getInstance().getCurrentThemeName();
      System.out.println("Current theme name: " + themeName);
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
