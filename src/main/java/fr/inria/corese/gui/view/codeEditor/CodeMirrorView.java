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

public class CodeMirrorView extends VBox {
  private static final Logger logger = LoggerFactory.getLogger(CodeMirrorView.class);

  private final WebView webView;
  private final WebEngine webEngine;
  private final StringProperty contentProperty = new SimpleStringProperty("");
  private boolean initialized = false;
  private boolean isInternalUpdate = false;

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
  }

  private void initializeEditor() {
    String html =
        """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body, html {
                            height: 100%;
                            margin: 0;
                            padding: 0;
                            overflow: hidden;
                        }
                        .editor-container {
                            display: flex;
                            flex-direction: column;
                            height: 100vh; /* Utilisez viewport height au lieu de 100% */
                            position: relative;
                            overflow: hidden;
                        }

                        .CodeMirror {
                            flex: 1;
                            height: 100% !important; /* Force la hauteur à 100% */
                            width: 100%;
                            position: relative;
                            display: flex;
                            flex-direction: column;
                        }

                        .CodeMirror-scroll {
                            flex: 1;
                            height: auto !important;
                            max-height: none !important;
                        }

                        #editor {
                            width: 100%;
                            height: 100%;
                        }
                        .status-bar {
                            height: 25px;
                            background-color: #f3f3f3;
                            border-top: 1px solid #ddd;
                            display: flex;
                            align-items: center;
                            padding: 0 10px;
                            font-size: 12px;
                            font-family: sans-serif;
                            color: #666;
                        }
                        .status-bar-right {
                            margin-left: auto;
                            display: flex;
                            gap: 15px;
                        }
                        .status-item {
                            margin-left: 15px;
                        }

                        .error-indicators {
                            position: absolute;
                            bottom: 50px;
                            right: 50px;
                            display: flex;
                            gap: 10px;
                            z-index: 1000;
                        }

                        .error-count, .warning-count {
                            background-color: white;
                            border-radius: 4px;
                            padding: 2px 8px;
                            font-size: 12px;
                            display: flex;
                            align-items: center;
                            gap: 4px;
                            box-shadow: 0 1px 3px rgba(0,0,0,0.2);
                        }

                        .error-count {
                            color: #d32f2f;
                            border: 1px solid #ffcdd2;
                        }

                        .warning-count {
                            color: #f57c00;
                            border: 1px solid #ffe0b2;
                        }

                        .CodeMirror-line-error {
                            background-color: rgba(255, 0, 0, 0.1);
                        }

                        .CodeMirror-line-warning {
                            background-color: rgba(255, 152, 0, 0.1);
                        }

                        .error-icon, .warning-icon {
                            display: inline-flex;
                            align-items: center;
                            justify-content: center;
                            width: 16px;
                            height: 16px;
                        }

                        .error-icon svg {
                            fill: #d32f2f;
                        }

                        .warning-icon svg {
                            fill: #f57c00;
                        }

                        .CodeMirror-linenumbers {
                            padding: 0;
                            min-width: 2px;
                            max-width: 2px;
                        }

                        .CodeMirror-lint-tooltip {
                            background-color: white;
                            border: 1px solid #ddd;
                            border-radius: 4px;
                            font-family: monospace;
                            font-size: 12px;
                            padding: 4px 8px;
                            transition: opacity .4s;
                            white-space: pre-wrap;
                            z-index: 100;
                            box-shadow: 0 2px 8px rgba(0,0,0,.15);
                        }

                        .CodeMirror-lint-mark {
                            display: none;
                            background-position: left bottom;
                            background-repeat: repeat-x;
                        }

                    </style>
                    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.css">
                    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/theme/eclipse.min.css">
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/turtle/turtle.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/edit/closebrackets.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/edit/matchbrackets.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/scroll/simplescrollbars.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/selection/active-line.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/fold/foldcode.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/fold/foldgutter.min.js"></script>
                    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/fold/foldgutter.min.css">
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/lint/lint.js"></script>
                    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/lint/lint.css">
                    <script src="https://cdn.jsdelivr.net/npm/n3@1.16.2/browser/n3.min.js"></script>
                    <script src="https://cdn.jsdelivr.net/npm/@frogcat/rdf-validate@1.0.0/rdf-validate.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/selection/undo.js"></script>
                        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
                </head>
                <body>
                    <div class="editor-container">
                        <div class="error-indicators">
                            <div class="error-count" id="error-count" style="display:none">
                                <span class="error-icon">
                                    <svg viewBox="0 0 24 24" width="16" height="16">
                                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                                    </svg>
                                </span>
                                <span class="count">0</span> errors
                            </div>
                            <div class="warning-count" id="warning-count" style="display:none">
                                <span class="warning-icon">
                                    <svg viewBox="0 0 24 24" width="16" height="16">
                                        <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
                                    </svg>
                                </span>
                                <span class="count">0</span> warnings
                            </div>
                        </div>
                        <textarea id="editor"></textarea>
                        <div class="status-bar">
                            <div class="status-bar-right">
                                <span id="cursor-position">Ln 1, Col 1</span>
                                <span class="status-item" id="selection-count"></span>
                                <span class="status-item" id="file-info">UTF-8</span>
                                <span class="status-item" id="editor-mode">Turtle</span>
                            </div>
                        </div>
                    </div>

                    <script>
                        window.onerror = function(message, source, lineno, colno, error) {
                            if (window.bridge) {
                                window.bridge.log("JS Error: " + message + " at " + source + ":" + lineno + ":" + colno);
                            }
                        };

                        console.log = function(message) {
                            if (window.bridge) {
                                window.bridge.log(message);
                            }
                        };

                        console.error = function(message) {
                            if (window.bridge) {
                                window.bridge.log("Error: " + message);
                            }
                        };

                        function getRealPosition(text, globalColumn) {
                            let lines = text.split("\\n");
                            let charCount = 0;

                            for (let i = 0; i < lines.length; i++) {
                                if (charCount + lines[i].length >= globalColumn) {
                                    return { realLine: i, realColumn: globalColumn - charCount };
                                }
                                charCount += lines[i].length + 1; // +1 pour le "\\n"
                            }

                            // Si on dépasse la longueur du texte, on retourne la dernière position connue
                            return { realLine: lines.length - 1, realColumn: lines[lines.length - 1].length };
                        }

                        function extractLineNumber(message) {
                            const lineMatch = message.match(/on line (\\d+)/);
                                if (lineMatch) {
                                    return parseInt(lineMatch[1]) - 1; // -1 car CodeMirror commence à 0
                                }
                            return null;
                        }

                        function updateErrorCounts(issues) {
                            const errorCount = issues.filter(i => i.severity === 'error').length;
                            const warningCount = issues.filter(i => i.severity === 'warning').length;

                            const errorCounter = document.getElementById('error-count');
                            const warningCounter = document.getElementById('warning-count');

                            if (errorCounter) {
                                errorCounter.style.display = errorCount > 0 ? 'flex' : 'none';
                                const countElement = errorCounter.querySelector('.count');
                                if (countElement) {
                                    countElement.textContent = errorCount;
                                }
                            }

                            if (warningCounter) {
                                warningCounter.style.display = warningCount > 0 ? 'flex' : 'none';
                                const countElement = warningCounter.querySelector('.count');
                                if (countElement) {
                                    countElement.textContent = warningCount;
                                }
                            }
                        }

                        function turtleLinter(text, callback) {
                            console.log("Running turtleLinter!");

                            console.log("Before N3.Parser initialization");
                            const parser = new N3.Parser({ format: 'Turtle' });
                            console.log("After N3.Parser initialization");

                            var issues = [];
                            const lines = text.split('\\n');
                            console.log(lines);
                            let currentText = '';

                            if (window.errorLines) {

                                window.errorLines.forEach(lineNumber => {
                                    console.log("line "+lineNumber);
                                    editor.removeLineClass(lineNumber, 'background', 'CodeMirror-line-error');
                                });
                            }
                            window.errorLines = [];

                            new Promise((resolve) => {
                                parser.parse(text, (error, quad, prefixes) => {
                                    if (error) {
                                        console.error("Parsing error detected:");
                                        console.error(`Message: ${error.message}`);
                                        console.error(`Line: ${error.line}, Column: ${error.column}`);

                                        let { realLine, realColumn } = getRealPosition(text, error.column);
                                        console.log(`Recalculated position -> Line: ${realLine}, Column: ${realColumn}`);

                                        let line = extractLineNumber(error.message);
                                        console.log("line : "+line);

                                        issues.push({
                                            message: error.message,
                                            severity: 'error',
                                            from: CodeMirror.Pos(line, realColumn),
                                            to: CodeMirror.Pos(line, realColumn + 1)
                                        });

                                        editor.addLineClass(line, 'background', 'CodeMirror-line-error');
                                        window.errorLines.push(line);
                                    }
                                    updateErrorCounts(issues);
                                    resolve();
                                });
                            }).then(() => {
                                updateErrorCounts(issues);
                                callback(issues);
                            }).catch((e) => {
                                console.error("Catch block error:", e);
                                issues.push({
                                    message: e.message,
                                    severity: 'error',
                                    from: CodeMirror.Pos(0, 0),
                                    to: CodeMirror.Pos(0, 1)
                                });
                                updateErrorCounts(issues);
                                callback(issues);
                            });
                        }

                        CodeMirror.registerHelper("lint", "turtle", turtleLinter);

                        var editor = CodeMirror.fromTextArea(document.getElementById('editor'), {
                            mode: 'turtle',
                            theme: 'eclipse',
                            lineNumbers: true,
                            matchBrackets: true,
                            autoCloseBrackets: true,
                            lineWrapping: true,
                            tabSize: 2,
                            autofocus: true,
                            styleActiveLine: true,
                            scrollbarStyle: 'overlay',
                            foldGutter: true,
                            historyEventDelay: 350,
                            undoDepth: 200,
                            lint: {
                                getAnnotations: turtleLinter,
                                async: true
                            },
                            gutters: ["CodeMirror-linenumbers", "CodeMirror-lint-markers", "CodeMirror-foldgutter"],
                            extraKeys: {
                                "Ctrl-S": function(cm) {
                                    if (window.bridge) {
                                        window.bridge.saveFile();
                                    }
                                },
                                "Ctrl-F": "findPersistent",
                                "Ctrl-/": "toggleComment",
                                "Ctrl-Space": "autocomplete",
                                "Ctrl-Z": function(cm) { cm.undo(); },
                                "Ctrl-Y": function(cm) { cm.redo(); },
                            },
                            viewportMargin: Infinity,
                            scrollbarStyle: 'native'
                        });

                        (function setupZooming() {
                            let currentFontSize = 12;

                        function setFontSize(size) {
                            currentFontSize = Math.max(8, Math.min(40, size));
                            editor.getWrapperElement().style.fontSize = currentFontSize + 'px';
                            editor.refresh();
                        }

                editor.getWrapperElement().addEventListener('wheel', function(e) {
                    if (e.ctrlKey) {
                        e.preventDefault();
                        if (e.deltaY < 0) {
                            setFontSize(currentFontSize + 1); // Zoom in
                        } else {
                            setFontSize(currentFontSize - 1); // Zoom out
                        }
                    }
            });
        })();


                        setTimeout(() => {
                            editor.refresh();
                        }, 100);

                        // Mise à jour de la position du curseur
                        editor.on('cursorActivity', function() {
                            var pos = editor.getCursor();
                            var sel = editor.getSelection();
                            document.getElementById('cursor-position').textContent =
                                'Ln ' + (pos.line + 1) + ', Col ' + (pos.ch + 1);

                            // Afficher le nombre de caractères sélectionnés
                            if (sel && sel.length > 0) {
                                document.getElementById('selection-count').textContent =
                                    sel.length + ' selected';
                            } else {
                                document.getElementById('selection-count').textContent = '';
                            }
                        });

                        editor.on('change', function(cm, change) {
                            editor.refresh();
                            if (!change.origin || change.origin !== 'setValue') {
                                if (window.bridge) {
                                    window.bridge.onContentChanged(editor.getValue());
                                    turtleLinter(editor.getValue, '');
                                }
                            }
                        });

                        window.setContent = function(content) {
                            var cursor = editor.getCursor();
                            var scrollInfo = editor.getScrollInfo();
                            editor.setValue(content || '');
                            editor.setCursor(cursor);
                            editor.scrollTo(scrollInfo.left, scrollInfo.top);
                            editor.refresh();
                        };

                        window.getContent = function() {
                            return editor.getValue();
                        };

                        window.addEventListener('resize', () => {
                            editor.refresh();
                        });
                    </script>
                </body>
                </html>
        """;

    webEngine.loadContent(html);

    webEngine
        .getLoadWorker()
        .stateProperty()
        .addListener(
            (obs, old, newState) -> {
              switch (newState) {
                case SUCCEEDED:
                  try {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("bridge", new JavaBridge());
                    initialized = true;

                    String content = contentProperty.get();
                    if (content != null && !content.isEmpty()) {
                      updateEditorContent(content);
                    }
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
}
