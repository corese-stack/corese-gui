package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.model.codeEditor.CodeEditorModel;
import fr.inria.corese.demo.model.IconButtonBarModel;
import fr.inria.corese.demo.view.icon.IconButtonBarView;
import fr.inria.corese.demo.factory.popup.DocumentationPopup;
import fr.inria.corese.demo.factory.popup.IPopup;
import fr.inria.corese.demo.factory.popup.PopupFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;

public class IconButtonBarController {
    private final IconButtonBarView view;
    private final IconButtonBarModel model;
    private final PopupFactory popupFactory;

    public IconButtonBarController(IconButtonBarModel model, IconButtonBarView view) {
        this.model = model;
        this.view = view;
        this.popupFactory = new PopupFactory();

        view.initializeButtons(model.getAvailableButtons());
        initializeButtonHandlers();
    }

    private void initializeButtonHandlers() {
        model.getAvailableButtons().forEach(this::initializeHandler);
    }

    private void initializeHandler(IconButtonType type) {
        Button button = view.getButton(type);
        switch (type) {
            case SAVE -> button.setOnAction(e -> onSaveButtonClick());
            case OPEN_FILE -> button.setOnAction(e -> onOpenFilesButtonClick());
            case EXPORT -> button.setOnAction(e -> onExportButtonClick());
            case IMPORT -> button.setOnAction(e -> onImportButtonClick());
            case CLEAR -> button.setOnAction(e -> onClearButtonClick());
            case UNDO -> button.setOnAction(e -> onUndoButtonClick());
            case REDO -> button.setOnAction(e -> onRedoButtonClick());
            case DOCUMENTATION -> button.setOnAction(e -> onDocumentationButtonClick());
            case ZOOM_IN -> button.setOnAction(e -> onZoomInButtonClick());
            case ZOOM_OUT -> button.setOnAction(e -> onZoomOutButtonClick());
            case FULL_SCREEN -> button.setOnAction(e -> onFullScreenButtonClick());
        }

    }

    public IconButtonBarView getView() {
        return view;
    }

    public IconButtonBarModel getModel() {
        return model;
    }

    private void onSaveButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        FileChooser.ExtensionFilter rqFilter = new FileChooser.ExtensionFilter("SPARQL Query Files (*.rq)", "*.rq");
        FileChooser.ExtensionFilter ttlFilter = new FileChooser.ExtensionFilter("Turtle Files (*.ttl)", "*.ttl");
        FileChooser.ExtensionFilter rdfFilter = new FileChooser.ExtensionFilter("RDF/XML Files (*.rdf)", "*.rdf");
        FileChooser.ExtensionFilter n3Filter = new FileChooser.ExtensionFilter("N3 Files (*.n3)", "*.n3");
        fileChooser.getExtensionFilters().addAll(rqFilter, ttlFilter, rdfFilter, n3Filter);
        fileChooser.setSelectedExtensionFilter(rqFilter);

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            String selectedExt = fileChooser.getSelectedExtensionFilter().getExtensions().get(0).replace("*", "");
            if (!file.getName().toLowerCase().endsWith(selectedExt.replace(".", ""))) {
                file = new File(file.getAbsolutePath() + selectedExt);
            }
            try {
                Files.writeString(file.toPath(), model.getCodeEditorModel().getContent());
                CodeEditorModel editorModel = model.getCodeEditorModel();
                editorModel.setFilePath(file.getAbsolutePath());
                editorModel.markSaved();
                IPopup successPopup = PopupFactory.getInstance().createPopup(PopupFactory.TOAST_NOTIFICATION);
                successPopup.setMessage("File has been saved successfully!");
                successPopup.displayPopup();
            } catch (Exception e) {
                showError("Error Saving File", "Could not save the file: " + e.getMessage());
            }
        }
    }

    private void onOpenFilesButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("RDF and SPARQL Files", "*.ttl", "*.rdf", "*.n3", "*.rq"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                model.getCodeEditorModel().setContent(content);
            } catch (Exception e) {
                showError("Error Opening File", "Could not open the file: " + e.getMessage());
            }
        }
    }

    private void onImportButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(view.getScene().getWindow());
        if (file != null) {
            // Ensure the file has the correct extension
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            try {
                Files.writeString(file.toPath(), model.getCodeEditorModel().getContent());
            } catch (Exception e) {
                showError("Error Exporting File", "Could not export the file: " + e.getMessage());
            }
        }
    }

    private void onClearButtonClick() {
        model.getCodeEditorModel().setContent("");
    }

    private void updateUndoRedoButtons() {
        CodeEditorModel editorModel = model.getCodeEditorModel();
        view.getButton(IconButtonType.UNDO).setDisable(!editorModel.canUndo());
        view.getButton(IconButtonType.REDO).setDisable(!editorModel.canRedo());
    }

    private void onUndoButtonClick() {
        CodeEditorModel editorModel = model.getCodeEditorModel();
        if (editorModel.canUndo()) {
            editorModel.undo();
            updateUndoRedoButtons();
        }

    }

    private void onRedoButtonClick() {
        CodeEditorModel editorModel = model.getCodeEditorModel();
        if (editorModel.canRedo()) {
            editorModel.redo();
            updateUndoRedoButtons();
        }

    }

    private void onDocumentationButtonClick() {
        DocumentationPopup documentationPopup = new DocumentationPopup();
        documentationPopup.displayPopup();
    }

    private void onZoomInButtonClick() {
        // TODO
    }

    private void onZoomOutButtonClick() {
        // TODO
    }

    private void onFullScreenButtonClick() {
        // TODO
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
