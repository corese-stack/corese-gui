package fr.inria.corese.gui.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.model.codeEditor.CodeEditorModel;
import fr.inria.corese.gui.view.codeEditor.CodeEditorView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.stage.FileChooser;

public class CodeEditorController {
  private static final Logger logger = LoggerFactory.getLogger(CodeEditorController.class);
  
  private final CodeEditorView view;
  private final CodeEditorModel model;
  private final IconButtonBarController iconButtonBarController;
  private final List<String> allowedExtensions;

  public CodeEditorController(List<ButtonConfig> buttons, String initialContent, List<String> allowedExtensions) {
    this.allowedExtensions = allowedExtensions != null ? allowedExtensions : List.of();
    
    // Extract IconButtonType from ButtonConfig for IconButtonBarController
    List<IconButtonType> iconButtons = buttons != null 
        ? buttons.stream().map(ButtonConfig::getIcon).toList()
        : List.of();
    
    this.view = new CodeEditorView();
    this.model = new CodeEditorModel();
    
    // Use the existing IconButtonBarView from the view instead of creating a new one via Factory
    this.iconButtonBarController = new IconButtonBarController(
        buttons != null ? buttons : List.of(),
        new fr.inria.corese.gui.model.IconButtonBarModel(iconButtons), 
        view.getIconButtonBarView(), 
        this
    );

    this.iconButtonBarController.getModel().setCodeEditorModel(this.model);
    this.iconButtonBarController.bindToModel();
    // No need to add child, as we are using the view's component directly

    model.setContent(initialContent);
    Platform.runLater(this::initializeEditor);
  }

  // Constructor overloading for backward compatibility or when no restriction is needed
  public CodeEditorController(List<ButtonConfig> buttons, String initialContent) {
    this(buttons, initialContent, List.of());
  }

  private void initializeEditor() {
    view.getCodeMirrorView().contentProperty().bindBidirectional(model.contentProperty());

    // Listen for file path changes to update syntax highlighting
    model.filePathProperty().addListener((obs, oldVal, newVal) -> detectAndSetMode());

    // Listen for content changes to update mode if file is untitled (auto-detect)
    model.contentProperty().addListener((obs, oldVal, newVal) -> {
        if (model.getFilePath() == null) {
            detectAndSetMode();
        }
    });

    // Initial mode check
    Platform.runLater(this::detectAndSetMode);
  }

  /**
   * Detects the appropriate syntax highlighting mode based on file extension
   * or content heuristics, and updates the view.
   */
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
      } else if (lowerPath.endsWith(".rdf") || lowerPath.endsWith(".owl") || lowerPath.endsWith(".xml")) {
        mode = "xml";
      } else if (lowerPath.endsWith(".json") || lowerPath.endsWith(".jsonld")) {
        mode = "json";
      } else if (lowerPath.endsWith(".trig")) {
        mode = "trig";
      } else if (lowerPath.endsWith(".js")) {
        mode = "javascript";
      }
    } else if (content != null) {
      // Heuristic analysis for untitled files to determine syntax
      String trimmed = content.trim();
      String lower = content.toLowerCase();

      // Check SPARQL keywords
      if (isModeAllowed("sparql") && (
          lower.contains("select ") || 
          lower.contains("construct ") || 
          lower.contains("ask ") || 
          lower.contains("describe ") || 
          lower.startsWith("prefix ") || 
          lower.contains("\nprefix "))) {
          mode = "sparql";
      }
      // Check Turtle / TriG patterns
      else if ((isModeAllowed("turtle") || isModeAllowed("trig")) && (
          lower.contains("@prefix") || 
          lower.contains("@base") || 
          lower.contains(" a ") || 
          trimmed.endsWith("."))) {
          mode = isModeAllowed("trig") ? "trig" : "turtle";
      }
      // Check JSON-LD structure
      else if (isModeAllowed("json") && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
          mode = "json";
      }
      // Check XML/RDF structure
      else if (isModeAllowed("xml") && trimmed.startsWith("<")) {
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

  public void saveFile() {
    String path = model.getFilePath();
    if (path == null) {
      saveFileAs();
    } else {
      File file = new File(path);
      // Check if file or parent directory still exists
      if (!file.exists()) {
        File parentDir = file.getParentFile();
        if (parentDir == null || !parentDir.exists()) {
          logger.warn("Cannot save file: parent directory no longer exists: {}", path);
          // Show dialog to user suggesting to save as a new file
          // For now, fall back to Save As
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
    FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files", "*.*");

    if (allowedExtensions.isEmpty()) {
        fileChooser.getExtensionFilters().addAll(ttlFilter, rqFilter, rdfFilter, trigFilter, jsonldFilter, shaclFilter, allFilter);
        fileChooser.setSelectedExtensionFilter(ttlFilter);
    } else {
        boolean added = false;
        // Logic to add filters based on allowedExtensions
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
    // Check if parent directory exists, if not it might have been deleted
    File parentDir = file.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      logger.error("Cannot save file: parent directory does not exist: {}", parentDir.getAbsolutePath());
      // The caller should handle the error - we could throw an exception here
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

  /**
   * Disposes of resources to prevent memory leaks.
   * 
   * <p>This method should be called when the editor is no longer needed,
   * typically when the associated tab is closed. It unbinds all properties
   * to allow proper garbage collection.
   */
  public void dispose() {
    // Unbind bidirectional binding to prevent memory leaks
    view.getCodeMirrorView().contentProperty().unbindBidirectional(model.contentProperty());
    
    // Dispose icon button bar controller if it has resources
    if (iconButtonBarController != null) {
      // IconButtonBarController cleanup if needed
    }
  }

  public CodeEditorModel getModel() {
    return model;
  }

  /**
   * Returns the root node of the view for integration into parent layouts.
   * 
   * @return The root node
   */
  public javafx.scene.Node getViewRoot() {
    return view.getRoot();
  }

  public CodeEditorView getView() {
    return view;
  }

  public IconButtonBarController getIconButtonBarController() {
    return iconButtonBarController;
  }
}
