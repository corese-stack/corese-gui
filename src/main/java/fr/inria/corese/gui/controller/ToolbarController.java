package fr.inria.corese.gui.controller;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.core.ButtonFactory;
import fr.inria.corese.gui.enums.icon.ButtonIcon;
import fr.inria.corese.gui.factory.popup.DocumentationPopup;
import fr.inria.corese.gui.factory.popup.ExportFormatPopup;
import fr.inria.corese.gui.model.ToolbarModel;
import fr.inria.corese.gui.model.codeEditor.CodeEditorModel;
import fr.inria.corese.gui.view.icon.ToolbarView;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller for managing the toolbar (actions and binding).
 */
public class ToolbarController {

  private final ToolbarView view;
  private final ToolbarModel model;
  private final CodeEditorController parentController;
  private CodeEditorModel editorModel;

  public ToolbarController(
      List<ButtonConfig> buttons,
      ToolbarModel model,
      ToolbarView view,
      CodeEditorController parentController) {
    this.model = model;
    this.view = view;
    this.parentController = parentController;

    List<ButtonConfig> configs = (buttons == null || buttons.isEmpty())
        ? createConfigsFromModel()
        : buttons;
    
    initializeButtons(configs);
  }

  private List<ButtonConfig> createConfigsFromModel() {
    return model.getAvailableButtons().stream()
        .map(this::createConfigFromFactory)
        .toList();
  }

  private ButtonConfig createConfigFromFactory(ButtonIcon icon) {
    return switch (icon) {
      case SAVE -> ButtonFactory.save(null);
      case OPEN_FILE -> ButtonFactory.openFile(null);
      case EXPORT -> ButtonFactory.export(null);
      case IMPORT -> ButtonFactory.importFile(null);
      case CLEAR -> ButtonFactory.clear(null);
      case UNDO -> ButtonFactory.undo(null);
      case REDO -> ButtonFactory.redo(null);
      case DOCUMENTATION -> ButtonFactory.documentation(null);
      case DELETE -> ButtonFactory.delete(null);
      case COPY -> ButtonFactory.copy(null);
      case PLAY -> ButtonFactory.play(null);
      case RELOAD -> ButtonFactory.reload(null);
      case LOGS -> ButtonFactory.logs(null);
      default -> new ButtonConfig(icon, null, null);
    };
  }

  private void initializeButtons(List<ButtonConfig> buttons) {
    if (buttons == null) {
      return;
    }

    List<ButtonConfig> enhancedConfigs = 
        buttons.stream()
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

    view.setButtons(enhancedConfigs);
  }

  private Runnable getDefaultAction(ButtonIcon type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case SAVE -> () -> {
        if (parentController != null) {
          parentController.saveFile();
        }
      };
      case OPEN_FILE -> this::onOpenFilesButtonClick;
      case EXPORT -> this::onExportButtonClick;
      case IMPORT -> this::onImportButtonClick;
      case CLEAR -> this::onClearButtonClick;
      case UNDO -> this::onUndoButtonClick;
      case REDO -> this::onRedoButtonClick;
      case DOCUMENTATION -> this::onDocumentationButtonClick;
      default -> null;
    };
  }

  public void setEditorModel(CodeEditorModel editorModel) {
    this.editorModel = editorModel;
  }

  public void bindToModel() {
    if (editorModel == null) {
      return;
    }

    for (ButtonIcon icon : model.getAvailableButtons()) {
      Button button = view.getButton(icon);
      if (button != null) {
        button.disableProperty().bind(model.enabledProperty(icon).not());
      }
    }

    BooleanBinding isEmpty = Bindings.createBooleanBinding(
            () -> {
              String content = editorModel.getContent();
              return content == null || content.trim().isEmpty();
            },
            editorModel.contentProperty()
    );

    if (model.getAvailableButtons().contains(ButtonIcon.SAVE)) {
      model.enabledProperty(ButtonIcon.SAVE).bind(
          editorModel.modifiedProperty().and(isEmpty.not()) 
      );
    }

    if (model.getAvailableButtons().contains(ButtonIcon.CLEAR)) {
      model.enabledProperty(ButtonIcon.CLEAR).bind(isEmpty.not());
    }

    if (model.getAvailableButtons().contains(ButtonIcon.EXPORT)) {
      model.enabledProperty(ButtonIcon.EXPORT).bind(isEmpty.not());
    }

    editorModel.contentProperty().addListener((obs, old, newV) -> updateUndoRedoState());
    updateUndoRedoState();
  }

  private void updateUndoRedoState() {
    if (model.getAvailableButtons().contains(ButtonIcon.UNDO)) {
      model.setButtonEnabled(ButtonIcon.UNDO, editorModel.canUndo());
    }
    if (model.getAvailableButtons().contains(ButtonIcon.REDO)) {
      model.setButtonEnabled(ButtonIcon.REDO, editorModel.canRedo());
    }
  }

  private void onOpenFilesButtonClick() {
    if (editorModel == null) {
      return;
    }

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
        editorModel.setContent(content);
        editorModel.setFilePath(file.getAbsolutePath());
        editorModel.markAsSaved();
      } catch (Exception e) {
        showError("Error Opening File", "Could not open the file: " + e.getMessage());
      }
    }
  }

  private void onImportButtonClick() {
    if (editorModel == null) {
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Import File");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

    File file = fileChooser.showOpenDialog(view.getRoot().getScene().getWindow());
    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        editorModel.setContent(content);
      } catch (Exception e) {
        showError("Error Importing File", "Could not import the file: " + e.getMessage());
      }
    }
  }

  private void onExportButtonClick() {
    if (editorModel == null) {
      return;
    }

    String content = editorModel.getContent();
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
    if (editorModel != null) {
      editorModel.setContent("");
    }
  }

  private void onUndoButtonClick() {
    if (editorModel != null && editorModel.canUndo()) {
      editorModel.undo();
    }
  }

  private void onRedoButtonClick() {
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

  public ToolbarView getView() {
    return view;
  }

  public ToolbarModel getModel() {
    return model;
  }
}