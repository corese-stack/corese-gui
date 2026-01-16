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

  public CodeEditorController(List<ButtonConfig> buttons, String initialContent) {
    // Extract IconButtonType from ButtonConfig for IconButtonBarController
    List<IconButtonType> iconButtons = buttons != null 
        ? buttons.stream().map(ButtonConfig::getIcon).toList()
        : List.of();
    
    this.view = new CodeEditorView();
    this.model = new CodeEditorModel();
    
    // Use the existing IconButtonBarView from the view instead of creating a new one via Factory
    this.iconButtonBarController = new IconButtonBarController(
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

  private void initializeEditor() {
    view.getCodeMirrorView().contentProperty().bindBidirectional(model.contentProperty());
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
    FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files", "*.*");

    fileChooser.getExtensionFilters().addAll(ttlFilter, rqFilter, rdfFilter, allFilter);

    fileChooser.setSelectedExtensionFilter(ttlFilter);

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
