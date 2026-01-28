package fr.inria.corese.gui.feature.codeeditor;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import fr.inria.corese.gui.core.factory.ButtonFactory;
import fr.inria.corese.gui.core.factory.popup.DocumentationPopup;
import fr.inria.corese.gui.core.factory.popup.ExportFormatPopup;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the code editor component. Manages the editor model, view, and the associated
 * toolbar.
 */
public class CodeEditorController {
  private static final Logger logger = LoggerFactory.getLogger(CodeEditorController.class);

  private final CodeEditorView view;
  private final CodeEditorModel model;
  private final List<String> allowedExtensions;

  public CodeEditorController(
      List<ButtonConfig> buttons, String initialContent, List<String> allowedExtensions) {
    this.allowedExtensions = allowedExtensions != null ? allowedExtensions : List.of();

    this.view = new CodeEditorView();
    this.model = new CodeEditorModel();

    // Enrich buttons with default actions and initialize toolbar
    if (buttons != null && !buttons.isEmpty()) {
      List<ButtonConfig> enrichedButtons =
          new ArrayList<>(enrichButtonsWithDefaultActions(buttons));

      // Always append Zoom controls
      enrichedButtons.add(ButtonFactory.zoomIn(this::zoomIn));
      enrichedButtons.add(ButtonFactory.zoomOut(this::zoomOut));

      view.getToolbarWidget().setButtons(enrichedButtons);
    } else {
      // Fallback if no buttons provided (default minimal toolbar)
      view.getToolbarWidget()
          .setButtons(
              List.of(ButtonFactory.zoomIn(this::zoomIn), ButtonFactory.zoomOut(this::zoomOut)));
    }

    // Bind the toolbar buttons state to the editor state
    bindToolbarToEditor();

    model.setContent(initialContent);
    Platform.runLater(this::initializeEditor);
  }

  // Constructor overloading for backward compatibility
  public CodeEditorController(List<ButtonConfig> buttons, String initialContent) {
    this(buttons, initialContent, List.of());
  }

  /**
   * Enriches the provided button configurations with default actions if they are missing.
   *
   * @param buttons The list of button configurations.
   * @return A new list of button configurations with actions populated.
   */
  private List<ButtonConfig> enrichButtonsWithDefaultActions(List<ButtonConfig> buttons) {
    return buttons.stream()
        .map(
            config -> {
              if (config.getAction() != null) {
                return config;
              }
              Runnable action = getDefaultAction(config.getIcon());
              if (action != null) {
                return new ButtonConfig(config.getIcon(), config.getTooltip(), action);
              }
              return config;
            })
        .toList();
  }

  /** Returns the default action for a given button type. */
  private Runnable getDefaultAction(ButtonIcon type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case SAVE -> this::saveFile;
      case OPEN_FILE -> this::onOpenFilesButtonClick;
      case EXPORT -> this::onExportButtonClick;
      case IMPORT -> this::onImportButtonClick;
      case CLEAR -> this::onClearButtonClick;
      case UNDO -> this::onUndoButtonClick;
      case REDO -> this::onRedoButtonClick;
      case DOCUMENTATION -> this::onDocumentationButtonClick;
      case ZOOM_IN -> this::zoomIn;
      case ZOOM_OUT -> this::zoomOut;
      default -> null;
    };
  }

  // ... (Existing methods like bindToolbarToEditor, initializeEditor, etc. remain unchanged)

  /** Binds the toolbar buttons' enabled/disabled state to the code editor's state. */
  private void bindToolbarToEditor() {
    ToolbarWidget toolbar = view.getToolbarWidget();

    // Define when the editor is considered empty
    BooleanBinding isEmpty =
        Bindings.createBooleanBinding(
            () -> {
              String content = model.getContent();
              return content == null || content.trim().isEmpty();
            },
            model.contentProperty());

    // Bind SAVE: Disabled if NOT (modified AND not empty)
    Button saveButton = toolbar.getButton(ButtonIcon.SAVE);
    if (saveButton != null) {
      saveButton.disableProperty().bind(model.modifiedProperty().not().or(isEmpty));
    }

    // Bind CLEAR: Disabled if empty
    Button clearButton = toolbar.getButton(ButtonIcon.CLEAR);
    if (clearButton != null) {
      clearButton.disableProperty().bind(isEmpty);
    }

    // Bind EXPORT: Disabled if empty
    Button exportButton = toolbar.getButton(ButtonIcon.EXPORT);
    if (exportButton != null) {
      exportButton.disableProperty().bind(isEmpty);
    }

    // Bind UNDO/REDO: Listen to changes
    model.contentProperty().addListener((obs, old, newV) -> updateUndoRedoState());
    updateUndoRedoState();
  }

  private void updateUndoRedoState() {
    ToolbarWidget toolbar = view.getToolbarWidget();
    toolbar.setButtonDisabled(ButtonIcon.UNDO, !model.canUndo());
    toolbar.setButtonDisabled(ButtonIcon.REDO, !model.canRedo());
  }

  private void initializeEditor() {
    view.getCodeMirrorView().contentProperty().bindBidirectional(model.contentProperty());

    model.filePathProperty().addListener((obs, oldVal, newVal) -> detectAndSetMode());

    model
        .contentProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (model.getFilePath() == null) {
                detectAndSetMode();
              }
            });

    Platform.runLater(this::detectAndSetMode);
  }

  private void detectAndSetMode() {
    String path = model.getFilePath();
    String content = model.getContent();
    String mode = "text/plain";

    if (path != null) {
      String lowerPath = path.toLowerCase();
      if (lowerPath.endsWith(".ttl") || lowerPath.endsWith(".n3") || lowerPath.endsWith(".nt")) {
        mode = "turtle";
      } else if (lowerPath.endsWith(".rq") || lowerPath.endsWith(".sparql")) {
        mode = "sparql";
      } else if (lowerPath.endsWith(".rdf")
          || lowerPath.endsWith(".owl")
          || lowerPath.endsWith(".xml")) {
        mode = "xml";
      } else if (lowerPath.endsWith(".json") || lowerPath.endsWith(".jsonld")) {
        mode = "json";
      } else if (lowerPath.endsWith(".trig")) {
        mode = "trig";
      } else if (lowerPath.endsWith(".js")) {
        mode = "javascript";
      }
    } else if (content != null) {
      String trimmed = content.trim();
      String lower = content.toLowerCase();

      if (isModeAllowed("sparql")
          && (lower.contains("select ")
              || lower.contains("construct ")
              || lower.contains("ask ")
              || lower.contains("describe ")
              || lower.startsWith("prefix ")
              || lower.contains("\nprefix "))) {
        mode = "sparql";
      } else if ((isModeAllowed("turtle") || isModeAllowed("trig"))
          && (lower.contains("@prefix")
              || lower.contains("@base")
              || lower.contains(" a ")
              || trimmed.endsWith("."))) {
        mode = isModeAllowed("trig") ? "trig" : "turtle";
      } else if (isModeAllowed("json") && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
        mode = "json";
      } else if (isModeAllowed("xml") && trimmed.startsWith("<")) {
        mode = "xml";
      }
    }

    view.getCodeMirrorView().setMode(mode);
  }

  private boolean isModeAllowed(String mode) {
    if (allowedExtensions.isEmpty()) return true;

    for (String ext : allowedExtensions) {
      switch (mode) {
        case "sparql":
          if (ext.equals(".rq") || ext.equals(".sparql")) return true;
          break;
        case "turtle":
          if (ext.equals(".ttl") || ext.equals(".n3") || ext.equals(".nt")) return true;
          break;
        case "trig":
          if (ext.equals(".trig")) return true;
          break;
        case "xml":
          if (ext.equals(".xml") || ext.equals(".rdf") || ext.equals(".owl")) return true;
          break;
        case "json":
          if (ext.equals(".json") || ext.equals(".jsonld")) return true;
          break;
        case "javascript":
          if (ext.equals(".js")) return true;
          break;
      }
    }
    return false;
  }

  // --- Button Actions ---

  private void onOpenFilesButtonClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open File");
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter(
                "RDF and SPARQL Files", "*.ttl", "*.rdf", "*.n3", "*.rq"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));

    File file = fileChooser.showOpenDialog(view.getRoot().getScene().getWindow());
    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        model.setContent(content);
        model.setFilePath(file.getAbsolutePath());
        model.markAsSaved();
      } catch (Exception e) {
        showError("Error Opening File", "Could not open the file: " + e.getMessage());
      }
    }
  }

  private void onImportButtonClick() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Import File");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

    File file = fileChooser.showOpenDialog(view.getRoot().getScene().getWindow());
    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        model.setContent(content);
      } catch (Exception e) {
        showError("Error Importing File", "Could not import the file: " + e.getMessage());
      }
    }
  }

  private void onExportButtonClick() {
    String content = model.getContent();
    if (content == null || content.trim().isEmpty()) {
      showError("Export Error", "The editor is empty. There is nothing to export.");
      return;
    }

    Graph graphToExport = Graph.create();
    try {
      Load.create(graphToExport)
          .parse(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
              Loader.format.TURTLE_FORMAT);
    } catch (Exception e) {
      showError(
          "Parsing Error",
          "Could not parse the content. Please ensure it is valid RDF syntax.\n\n"
              + e.getMessage());
      return;
    }

    ExportFormatPopup.show((Stage) view.getRoot().getScene().getWindow(), graphToExport);
  }

  private void onClearButtonClick() {
    model.setContent("");
  }

  private void onUndoButtonClick() {
    if (model.canUndo()) {
      model.undo();
    }
  }

  private void onRedoButtonClick() {
    if (model.canRedo()) {
      model.redo();
    }
  }

  private void onDocumentationButtonClick() {
    new DocumentationPopup().displayPopup();
  }

  private void zoomIn() {
    view.zoomIn();
  }

  private void zoomOut() {
    view.zoomOut();
  }

  private void showError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  // --- End Button Actions ---

  public void saveFile() {
    String path = model.getFilePath();
    if (path == null) {
      saveFileAs();
    } else {
      File file = new File(path);
      if (!file.exists()) {
        File parentDir = file.getParentFile();
        if (parentDir == null || !parentDir.exists()) {
          logger.warn("Cannot save file: parent directory no longer exists: {}", path);
          saveFileAs();
          return;
        }
      }
      writeToFile(file);
    }
  }

  public void saveFileAs() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save File As");

    FileChooser.ExtensionFilter ttlFilter =
        new FileChooser.ExtensionFilter("Turtle files (*.ttl)", "*.ttl");
    FileChooser.ExtensionFilter rqFilter =
        new FileChooser.ExtensionFilter("SPARQL Query files (*.rq)", "*.rq");
    FileChooser.ExtensionFilter rdfFilter =
        new FileChooser.ExtensionFilter("RDF/XML files (*.rdf)", "*.rdf");
    FileChooser.ExtensionFilter trigFilter =
        new FileChooser.ExtensionFilter("TriG files (*.trig)", "*.trig");
    FileChooser.ExtensionFilter jsonldFilter =
        new FileChooser.ExtensionFilter("JSON-LD files (*.jsonld)", "*.jsonld");
    FileChooser.ExtensionFilter shaclFilter =
        new FileChooser.ExtensionFilter("SHACL files (*.shacl)", "*.shacl");
    FileChooser.ExtensionFilter allFilter =
        new FileChooser.ExtensionFilter("All Files", "*.*", "*.*");

    if (allowedExtensions.isEmpty()) {
      fileChooser
          .getExtensionFilters()
          .addAll(ttlFilter, rqFilter, rdfFilter, trigFilter, jsonldFilter, shaclFilter, allFilter);
      fileChooser.setSelectedExtensionFilter(ttlFilter);
    } else {
      boolean added = false;
      if (isAllowed(".ttl") || isAllowed(".n3") || isAllowed(".nt")) {
        fileChooser.getExtensionFilters().add(ttlFilter);
        added = true;
      }
      if (isAllowed(".rq") || isAllowed(".sparql")) {
        fileChooser.getExtensionFilters().add(rqFilter);
        added = true;
      }
      if (isAllowed(".rdf") || isAllowed(".owl") || isAllowed(".xml")) {
        fileChooser.getExtensionFilters().add(rdfFilter);
        added = true;
      }
      if (isAllowed(".trig")) {
        fileChooser.getExtensionFilters().add(trigFilter);
        added = true;
      }
      if (isAllowed(".jsonld") || isAllowed(".json")) {
        fileChooser.getExtensionFilters().add(jsonldFilter);
        added = true;
      }
      if (isAllowed(".shacl")) {
        fileChooser.getExtensionFilters().add(shaclFilter);
        added = true;
      }

      if (!added) {
        fileChooser.getExtensionFilters().add(allFilter);
      }
    }

    File file = fileChooser.showSaveDialog(view.getRoot().getScene().getWindow());

    if (file != null) {
      FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
      if (selectedFilter != null && selectedFilter != allFilter) {
        String extension = selectedFilter.getExtensions().get(0).substring(1);
        if (!file.getName().toLowerCase().endsWith(extension)) {
          file = new File(file.getAbsolutePath() + extension);
        }
      }
      writeToFile(file);
    }
  }

  private boolean isAllowed(String ext) {
    if (allowedExtensions.isEmpty()) return true;
    for (String allowed : allowedExtensions) {
      if (allowed.equalsIgnoreCase(ext)) return true;
    }
    return false;
  }

  private void writeToFile(File file) {
    File parentDir = file.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      logger.error(
          "Cannot save file: parent directory does not exist: {}", parentDir.getAbsolutePath());
      return;
    }

    try (FileWriter writer = new FileWriter(file)) {
      writer.write(model.getContent());
      model.setFilePath(file.getAbsolutePath());
      model.markAsSaved();
    } catch (IOException e) {
      logger.error("Error saving file: {}", file.getAbsolutePath(), e);
    }
  }

  public void dispose() {
    view.getCodeMirrorView().contentProperty().unbindBidirectional(model.contentProperty());
  }

  public CodeEditorModel getModel() {
    return model;
  }

  public javafx.scene.Node getViewRoot() {
    return view.getRoot();
  }

  public CodeEditorView getView() {
    return view;
  }
}
