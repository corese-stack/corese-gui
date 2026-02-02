package fr.inria.corese.gui.feature.codeeditor;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.gui.component.notification.NotificationManager;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.DialogHelper;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import fr.inria.corese.gui.core.factory.ButtonFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Code Editor component.
 *
 * <p>Manages:
 *
 * <ul>
 *   <li>The editor life-cycle and state (via {@link CodeEditorModel}).
 *   <li>Toolbar actions (Open, Save, Undo, Redo, etc.).
 *   <li>File I/O operations.
 *   <li>Syntax highlighting detection.
 * </ul>
 */
public class CodeEditorController {
  private static final Logger logger = LoggerFactory.getLogger(CodeEditorController.class);

  private final CodeEditorView view;
  private final CodeEditorModel model;
  private final List<String> allowedExtensions;

  /**
   * Creates a new CodeEditorController.
   *
   * @param buttons Configuration for toolbar buttons.
   * @param initialContent Initial text content.
   * @param allowedExtensions List of allowed file extensions (e.g. ".ttl", ".rq"). If empty, all
   *     supported types are allowed.
   */
  public CodeEditorController(
      List<ButtonConfig> buttons, String initialContent, List<String> allowedExtensions) {
    this.allowedExtensions = allowedExtensions != null ? allowedExtensions : List.of();
    this.view = new CodeEditorView();
    this.model = new CodeEditorModel();

    initializeToolbar(buttons);
    bindToolbarToModel();

    model.setContent(initialContent);
    Platform.runLater(this::initializeEditorBehavior);
  }

  public CodeEditorController(List<ButtonConfig> buttons, String initialContent) {
    this(buttons, initialContent, List.of());
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private void initializeToolbar(List<ButtonConfig> buttons) {
    List<ButtonConfig> config = new ArrayList<>();

    // 1. Process provided buttons (enrich with default actions if missing)
    if (buttons != null) {
      for (ButtonConfig btn : buttons) {
        if (btn.getAction() == null) {
          Runnable defaultAction = getDefaultAction(btn.getIcon());
          if (defaultAction != null) {
            config.add(new ButtonConfig(btn.getIcon(), btn.getTooltip(), defaultAction));
          } else {
            config.add(btn);
          }
        } else {
          config.add(btn);
        }
      }
    }

    // 2. Add Zoom controls if not present (logic could be refined, but safe to append)
    // Check if zoom buttons are already added to avoid duplicates if user provided them?
    // For simplicity, we append them as standard tools.
    config.add(ButtonFactory.zoomIn(this::zoomIn));
    config.add(ButtonFactory.zoomOut(this::zoomOut));

    view.getToolbarWidget().setButtons(config);
  }

  private void initializeEditorBehavior() {
    // Two-way binding for content
    view.getCodeMirrorView().contentProperty().bindBidirectional(model.contentProperty());

    // Mode detection triggers
    model.filePathProperty().addListener((obs, oldVal, newVal) -> detectAndSetMode());
    model
        .contentProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (model.getFilePath() == null) {
                detectAndSetMode();
              }
            });

    detectAndSetMode();
  }

  private void bindToolbarToModel() {
    ToolbarWidget toolbar = view.getToolbarWidget();

    BooleanBinding isEmpty =
        Bindings.createBooleanBinding(
            () -> {
              String s = model.getContent();
              return s == null || s.trim().isEmpty();
            },
            model.contentProperty());

    // Bind Save
    Button saveBtn = toolbar.getButton(ButtonIcon.SAVE);
    if (saveBtn != null) {
      // Enable save if modified OR if file is new (never saved) and has content?
      // Logic: Disable if NOT (modified AND not empty) -> Enable if modified AND not empty.
      // Simplified: Enable if modified.
      saveBtn.disableProperty().bind(model.modifiedProperty().not().or(isEmpty));
    }

    // Bind Clear/Export
    toolbar.setButtonDisabled(ButtonIcon.CLEAR, isEmpty.get());
    toolbar.setButtonDisabled(ButtonIcon.EXPORT, isEmpty.get());

    // Listen to isEmpty for dynamic updates
    isEmpty.addListener(
        (obs, old, empty) -> {
          toolbar.setButtonDisabled(ButtonIcon.CLEAR, empty);
          toolbar.setButtonDisabled(ButtonIcon.EXPORT, empty);
        });

    // Bind Undo/Redo
    model.contentProperty().addListener((obs, o, n) -> updateUndoRedoState());
    updateUndoRedoState();
  }

  private void updateUndoRedoState() {
    ToolbarWidget toolbar = view.getToolbarWidget();
    toolbar.setButtonDisabled(ButtonIcon.UNDO, !model.canUndo());
    toolbar.setButtonDisabled(ButtonIcon.REDO, !model.canRedo());
  }

  // ==============================================================================================
  // Mode Detection
  // ==============================================================================================

  private void detectAndSetMode() {
    String mode = "text/plain";
    String path = model.getFilePath();
    String content = model.getContent();

    if (path != null) {
      mode = detectModeFromExtension(path);
    } else if (content != null) {
      mode = detectModeFromContent(content);
    }

    view.getCodeMirrorView().setMode(mode);
  }

  private String detectModeFromExtension(String path) {
    String lower = path.toLowerCase();
    if (endsWithAny(lower, ".ttl", ".n3", ".nt")) return "turtle";
    if (endsWithAny(lower, ".rq", ".sparql")) return "sparql";
    if (endsWithAny(lower, ".rdf", ".owl", ".xml")) return "xml";
    if (endsWithAny(lower, ".json", ".jsonld")) return "json";
    if (endsWithAny(lower, ".trig")) return "trig";
    if (endsWithAny(lower, ".js")) return "javascript";
    return "text/plain";
  }

  private String detectModeFromContent(String content) {
    String lower = content.toLowerCase();
    String trimmed = content.trim();

    // SPARQL
    if (isModeAllowed("sparql")
        && (lower.contains("select ")
            || lower.contains("construct ")
            || lower.contains("ask ")
            || lower.contains("describe ")
            || lower.startsWith("prefix ")
            || lower.contains("\nprefix "))) {
      return "sparql";
    }

    // Turtle / Trig
    if ((isModeAllowed("turtle") || isModeAllowed("trig"))
        && (lower.contains("@prefix")
            || lower.contains("@base")
            || lower.contains(" a ")
            || trimmed.endsWith("."))) {
      return isModeAllowed("trig") ? "trig" : "turtle";
    }

    // JSON
    if (isModeAllowed("json") && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
      return "json";
    }

    // XML
    if (isModeAllowed("xml") && trimmed.startsWith("<")) {
      return "xml";
    }

    return "text/plain";
  }

  private boolean endsWithAny(String str, String... suffixes) {
    for (String s : suffixes) {
      if (str.endsWith(s)) return true;
    }
    return false;
  }

  private boolean isModeAllowed(String mode) {
    // If no restriction, everything is allowed
    if (allowedExtensions.isEmpty()) return true;

    // Simplified mapping logic
    return switch (mode) {
      case "sparql" -> containsAnyAllowed(".rq", ".sparql");
      case "turtle" -> containsAnyAllowed(".ttl", ".n3", ".nt");
      case "trig" -> containsAnyAllowed(".trig");
      case "xml" -> containsAnyAllowed(".xml", ".rdf", ".owl");
      case "json" -> containsAnyAllowed(".json", ".jsonld");
      case "javascript" -> containsAnyAllowed(".js");
      default -> true;
    };
  }

  private boolean containsAnyAllowed(String... exts) {
    for (String ext : exts) {
      if (allowedExtensions.contains(ext)) return true;
    }
    return false;
  }

  // ==============================================================================================
  // Actions
  // ==============================================================================================

  private Runnable getDefaultAction(ButtonIcon type) {
    if (type == null) return null;
    return switch (type) {
      case SAVE -> this::saveFile;
      case OPEN_FILE -> this::openFile;
      case EXPORT -> this::exportContent;
      case IMPORT -> this::importFile;
      case CLEAR -> this::clearContent;
      case UNDO -> this::undo;
      case REDO -> this::redo;
      case ZOOM_IN -> this::zoomIn;
      case ZOOM_OUT -> this::zoomOut;
      default -> null; // Other actions (like Documentation) handled externally or not implemented
    };
  }

  private void openFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Open File");

    // Add filters
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "RDF and SPARQL",
                "*.ttl",
                "*.rdf",
                "*.n3",
                "*.rq",
                "*.sparql",
                "*.jsonld",
                "*.trig"));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

    File file = chooser.showOpenDialog(view.getRoot().getScene().getWindow());
    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        model.setContent(content);
        model.setFilePath(file.getAbsolutePath());
        model.markAsSaved();
      } catch (IOException e) {
        logger.error("Failed to open file", e);
        DialogHelper.showError("Error Opening File", e.getMessage());
      }
    }
  }

  private void importFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Import File");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md", "*.*"));

    File file = chooser.showOpenDialog(view.getRoot().getScene().getWindow());
    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        // Append or Replace? Standard import usually replaces content in editors unless "Insert"
        model.setContent(content);
        NotificationManager.getInstance().showSuccess("Imported: " + file.getName());
      } catch (IOException e) {
        logger.error("Failed to import file", e);
        NotificationManager.getInstance().showError("Import failed: " + e.getMessage());
      }
    }
  }

  public void saveFile() {
    if (model.getFilePath() == null) {
      saveFileAs();
    } else {
      File file = new File(model.getFilePath());
      writeToFile(file);
    }
  }

  public void saveFileAs() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Save File As");

    // Common Filters
    FileChooser.ExtensionFilter ttl = new FileChooser.ExtensionFilter("Turtle (*.ttl)", "*.ttl");
    FileChooser.ExtensionFilter rq = new FileChooser.ExtensionFilter("SPARQL (*.rq)", "*.rq");
    FileChooser.ExtensionFilter rdf = new FileChooser.ExtensionFilter("RDF/XML (*.rdf)", "*.rdf");
    FileChooser.ExtensionFilter jsonld =
        new FileChooser.ExtensionFilter("JSON-LD (*.jsonld)", "*.jsonld");
    FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("All Files", "*.*");

    chooser.getExtensionFilters().addAll(ttl, rq, rdf, jsonld, all);

    // Select default filter based on content mode if possible
    // (Simple heuristic: default to TTL)
    chooser.setSelectedExtensionFilter(ttl);

    File file = chooser.showSaveDialog(view.getRoot().getScene().getWindow());
    if (file != null) {
      // Enforce extension if selected filter implies one
      file = enforceExtension(file, chooser.getSelectedExtensionFilter());
      writeToFile(file);
    }
  }

  private File enforceExtension(File file, FileChooser.ExtensionFilter filter) {
    if (filter != null && filter.getExtensions().size() > 0) {
      String ext = filter.getExtensions().get(0).replace("*", ""); // e.g. ".ttl"
      if (!file.getName().toLowerCase().endsWith(ext)) {
        return new File(file.getAbsolutePath() + ext);
      }
    }
    return file;
  }

  private void writeToFile(File file) {
    try (FileWriter writer = new FileWriter(file)) {
      writer.write(model.getContent());
      model.setFilePath(file.getAbsolutePath());
      model.markAsSaved();
      NotificationManager.getInstance().showSuccess("Saved: " + file.getName());
    } catch (IOException e) {
      logger.error("Save failed", e);
      DialogHelper.showError("Save Error", "Could not save file: " + e.getMessage());
    }
  }

  private void exportContent() {
    String content = model.getContent();
    if (content == null || content.isBlank()) {
      NotificationManager.getInstance().showWarning("Nothing to export");
      return;
    }

    // Validate before export logic (simulating validation check)
    try {
      Graph g = Graph.create();
      Load.create(g)
          .parse(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
              Loader.format.TURTLE_FORMAT);
      // If parse succeeds, proceed to export (stub for now)
      DialogHelper.showInformation(
          "Export", "Content is valid RDF. Export implementation pending.");
    } catch (Exception e) {
      DialogHelper.showError(
          "Validation Error", "Content is not valid RDF/Turtle:\n" + e.getMessage());
    }
  }

  private void clearContent() {
    model.setContent("");
  }

  private void undo() {
    model.undo();
  }

  private void redo() {
    model.redo();
  }

  private void zoomIn() {
    view.zoomIn();
  }

  private void zoomOut() {
    view.zoomOut();
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Gets the editor model. Note: Exposed to allow data binding and state management by parent
   * controllers.
   *
   * @return The model.
   */
  public CodeEditorModel getModel() {
    return model;
  }

  /**
   * Gets the current text content.
   *
   * @return The text content.
   */
  public String getContent() {
    return model.getContent();
  }

  /**
   * Sets the current text content.
   *
   * @param content The new content.
   */
  public void setContent(String content) {
    model.setContent(content);
  }

  /**
   * Disables or enables the editor view.
   *
   * @param disable True to disable, false to enable.
   */
  public void setDisable(boolean disable) {
    view.getRoot().setDisable(disable);
  }

  /** Cleans up resources. */
  public void dispose() {
    view.getCodeMirrorView().contentProperty().unbindBidirectional(model.contentProperty());
  }

  public javafx.scene.Node getViewRoot() {
    return view.getRoot();
  }
}
