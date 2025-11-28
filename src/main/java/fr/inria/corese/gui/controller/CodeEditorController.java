package fr.inria.corese.gui.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.enums.icon.IconButtonBarType;
import fr.inria.corese.gui.factory.icon.IconButtonBarFactory;
import fr.inria.corese.gui.model.codeEditor.CodeEditorModel;
import fr.inria.corese.gui.view.codeEditor.CodeEditorView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javafx.application.Platform;
import javafx.stage.FileChooser;

public class CodeEditorController {
  private static final Logger logger = LoggerFactory.getLogger(CodeEditorController.class);
  
  private final CodeEditorView view;
  private final CodeEditorModel model;
  private final IconButtonBarController iconButtonBarController;

  public CodeEditorController(IconButtonBarType type, String initialContent) {
    this.view = new CodeEditorView();
    this.model = new CodeEditorModel();
    this.iconButtonBarController = IconButtonBarFactory.create(type, this);

    this.iconButtonBarController.getModel().setCodeEditorModel(this.model);
    view.getIconButtonBarView().getChildren().add(iconButtonBarController.getView());

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
      writeToFile(new File(path));
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

    File file = fileChooser.showSaveDialog(view.getScene().getWindow());

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
    try (FileWriter writer = new FileWriter(file)) {
      writer.write(model.getContent());
      model.setFilePath(file.getAbsolutePath());
      model.markAsSaved();
    } catch (IOException e) {
      logger.error("Error saving file: {}", file.getAbsolutePath(), e);
    }
  }

  public CodeEditorModel getModel() {
    return model;
  }

  public CodeEditorView getView() {
    return view;
  }

  public IconButtonBarController getIconButtonBarController() {
    return iconButtonBarController;
  }
}
