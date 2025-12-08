package fr.inria.corese.gui.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.factory.popup.DocumentationPopup;
import fr.inria.corese.gui.factory.popup.ExportFormatPopup;
import fr.inria.corese.gui.model.IconButtonBarModel;
import fr.inria.corese.gui.model.codeEditor.CodeEditorModel;
import fr.inria.corese.gui.view.icon.IconButtonBarView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class IconButtonBarController {
  private final IconButtonBarView view;
  private final IconButtonBarModel model;
  private final CodeEditorController parentController;

  public IconButtonBarController(
      IconButtonBarModel model, IconButtonBarView view, CodeEditorController parentController) {
    this.model = model;
    this.view = view;
    this.parentController = parentController;

    view.initializeButtons(model.getAvailableButtons());
    initializeButtonHandlers();
  }

  /**
   * Binds the controller to the CodeEditorModel.
   * Must be called after the CodeEditorModel is set in the IconButtonBarModel.
   */
  public void bindToModel() {
      if (model.getCodeEditorModel() != null) {
          // Undo/Redo listeners
          model.getCodeEditorModel().contentProperty().addListener((obs, o, n) -> updateUndoRedoButtons());
          updateUndoRedoButtons();
          
          // Save Button state
          Button saveButton = view.getButton(IconButtonType.SAVE);
          if (saveButton != null) {
              BooleanBinding isEmpty = Bindings.createBooleanBinding(
                  () -> {
                      String c = model.getCodeEditorModel().getContent();
                      return c == null || c.trim().isEmpty();
                  },
                  model.getCodeEditorModel().contentProperty()
              );
              
              saveButton.disableProperty().bind(
                  model.getCodeEditorModel().modifiedProperty().not()
                  .or(isEmpty)
              );
          }
      }
  }

  private void initializeButtonHandlers() {
    model.getAvailableButtons().forEach(this::initializeHandler);
  }

  private void initializeHandler(IconButtonType type) {
    Button button = view.getButton(type);
    if (button == null) return;

    switch (type) {
      case SAVE ->
          button.setOnAction(
              e -> {
                if (parentController != null) {
                  parentController.saveFile();
                }
              });
      case OPEN_FILE ->
          button.setOnAction(
              e -> onOpenFilesButtonClick());
      case EXPORT -> button.setOnAction(e -> onExportButtonClick());
      case IMPORT -> button.setOnAction(e -> onImportButtonClick());
      case CLEAR -> button.setOnAction(e -> onClearButtonClick());
      case UNDO -> button.setOnAction(e -> onUndoButtonClick());
      case REDO -> button.setOnAction(e -> onRedoButtonClick());
      case DOCUMENTATION -> button.setOnAction(e -> onDocumentationButtonClick());
      // Cases non implémentées - pas d'action pour l'instant
      case NEW_FILE,
          NEW_FOLDER,
          FULL_SCREEN,
          LOGS,
          CLOSE_FILE_EXPLORER,
          COPY,
          DELETE,
          ZOOM_IN,
          RELOAD,
          OPEN_FOLDER,
          TEMPLATE,
          SPLIT,
          PASTE,
          ZOOM_OUT -> {
        // Placeholder pour futures implémentations
      }
    }
  }

  private void onOpenFilesButtonClick() {
    if (model.getCodeEditorModel() == null) return;
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open File");
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter(
                "RDF and SPARQL Files", "*.ttl", "*.rdf", "*.n3", "*.rq"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));

    File file = fileChooser.showOpenDialog(null);
    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        model.getCodeEditorModel().setContent(content);
        model.getCodeEditorModel().setFilePath(file.getAbsolutePath());
        model.getCodeEditorModel().markAsSaved();
      } catch (Exception e) {
        showError("Error Opening File", "Could not open the file: " + e.getMessage());
      }
    }
  }

  private void onImportButtonClick() {
    if (model.getCodeEditorModel() == null) return;
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Import File");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

    File file = fileChooser.showOpenDialog(view.getScene().getWindow());
    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        model.getCodeEditorModel().setContent(content);
      } catch (Exception e) {
        showError("Error Importing File", "Could not import the file: " + e.getMessage());
      }
    }
  }

  private void onExportButtonClick() {
    if (model.getCodeEditorModel() == null) {
      return;
    }

    String content = model.getCodeEditorModel().getContent();
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

    ExportFormatPopup.show((Stage) view.getScene().getWindow(), graphToExport);
  }

  private void onClearButtonClick() {
    if (model.getCodeEditorModel() != null) {
      model.getCodeEditorModel().setContent("");
    }
  }

  private void updateUndoRedoButtons() {
    CodeEditorModel editorModel = model.getCodeEditorModel();
    if (editorModel != null) {
      if (view.getButton(IconButtonType.UNDO) != null) {
        view.getButton(IconButtonType.UNDO).setDisable(!editorModel.canUndo());
      }
      if (view.getButton(IconButtonType.REDO) != null) {
        view.getButton(IconButtonType.REDO).setDisable(!editorModel.canRedo());
      }
    }
  }

  private void onUndoButtonClick() {
    CodeEditorModel editorModel = model.getCodeEditorModel();
    if (editorModel != null && editorModel.canUndo()) {
      editorModel.undo();
    }
  }

  private void onRedoButtonClick() {
    CodeEditorModel editorModel = model.getCodeEditorModel();
    if (editorModel != null && editorModel.canRedo()) {
      editorModel.redo();
    }
  }

  private void onDocumentationButtonClick() {
    new DocumentationPopup().displayPopup();
  }

  private void showError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  public IconButtonBarView getView() {
    return view;
  }

  public IconButtonBarModel getModel() {
    return model;
  }
}
