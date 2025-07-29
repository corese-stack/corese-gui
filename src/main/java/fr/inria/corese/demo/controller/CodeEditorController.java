package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.factory.icon.IconButtonBarFactory;
import fr.inria.corese.demo.model.codeEditor.CodeEditorModel;
import fr.inria.corese.demo.view.codeEditor.CodeEditorView;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CodeEditorController {
    private final CodeEditorView view;
    private final CodeEditorModel model;
    private final IconButtonBarController iconButtonBarController;

    public CodeEditorController(IconButtonBarType type, String initialContent) {
        this.view = new CodeEditorView();
        this.model = new CodeEditorModel();
        this.iconButtonBarController = IconButtonBarFactory.create(type, this);

        this.iconButtonBarController.getModel().setCodeEditorModel(this.model);
        view.getIconButtonBarView().getChildren().add(iconButtonBarController.getView());

        if (type.equals(IconButtonBarType.VALIDATION) || type.equals(IconButtonBarType.QUERY)) {
            view.displayRunButton();
        }

        Platform.runLater(() -> initializeEditor(initialContent));
    }

    private void initializeEditor(String initialContent) {
        model.setContent(initialContent);

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
        FileChooser.ExtensionFilter ttlFilter = new FileChooser.ExtensionFilter("Turtle files (*.ttl)", "*.ttl");
        FileChooser.ExtensionFilter rqFilter = new FileChooser.ExtensionFilter("SPARQL Query files (*.rq)", "*.rq");
        FileChooser.ExtensionFilter rdfFilter = new FileChooser.ExtensionFilter("RDF/XML files (*.rdf)", "*.rdf");
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
            System.err.println("Error saving file: " + e.getMessage());
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