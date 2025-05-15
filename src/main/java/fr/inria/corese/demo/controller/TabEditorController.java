package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.factory.popup.DocumentationPopup;
import fr.inria.corese.demo.model.TabEditorModel;
import fr.inria.corese.demo.view.TabEditorView;
import fr.inria.corese.demo.factory.popup.PopupFactory;
import fr.inria.corese.demo.factory.popup.SaveConfirmationPopup;
import fr.inria.corese.demo.view.TopBar;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


public class TabEditorController {
    private final TabEditorView view;
    private final TabEditorModel model;
    private final IconButtonBarType type;
    private TopBar topBar;


    public TabEditorController(IconButtonBarType type) {
        this.view = new TabEditorView();
        this.model = new TabEditorModel();
        this.type = type;
        
        initializeTabPane();
        initializeKeyboardShortcuts();
    }

    private void initializeTabPane() {
        // S'assurer que l'onglet "+" reste toujours à la fin
        view.getTabPane().getTabs().addListener((ListChangeListener<Tab>) change -> {
            if (view.getTabPane().getTabs().isEmpty()) {
                view.getTabPane().getTabs().add(view.getAddTab());
            } else if (view.getTabPane().getTabs().get(view.getTabPane().getTabs().size() - 1) != view.getAddTab()) {
                view.getTabPane().getTabs().add(view.getAddTab());
            }
        });

        // Configurer le bouton "+" pour créer un nouvel onglet
        view.getAddTabButton().setOnAction(e -> {
            addNewTab("Untitled");
        });
    }

    private void addNewTabHelper(String title, String content, String filePath) {
        CodeEditorController codeEditorController = new CodeEditorController(type, content);
        Tab tab = view.createEditorTab(title, codeEditorController.getView());
        model.addTabModel(tab, codeEditorController);
    
        int addTabIndex = view.getTabPane().getTabs().size() - 1;
        if (addTabIndex < 0) addTabIndex = 0;
        view.getTabPane().getTabs().add(addTabIndex, tab);
        view.getTabPane().getSelectionModel().select(tab);
    
        if (filePath != null) {
            codeEditorController.getModel().setCurrentFile(filePath);
        }
    
        codeEditorController.getModel().modifiedProperty().addListener((obs, oldVal, newVal) -> {
            view.updateTabIcon(tab, newVal);
        });
    
        tab.setOnCloseRequest(event -> {
            event.consume();
            if (handleCloseFile(tab)) {
                view.getTabPane().getTabs().remove(tab);
            }
        });
    
        Platform.runLater(() -> {
            codeEditorController.getView().displayRunButton();
        });
    }

    public void addNewTab(String title) {
        addNewTabHelper(title, "", null);
    }

    private void initializeKeyboardShortcuts(){
        view.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    boolean isShortcutDown = event.isControlDown() || event.isMetaDown();
                    if (isShortcutDown && event.getCode() == KeyCode.S) {
                        handleSaveShortcut();
                        event.consume();
                    }
                    if (isShortcutDown && event.getCode() == KeyCode.W) {
                        Tab tab = view.getTabPane().getSelectionModel().getSelectedItem();
                        if (handleCloseFile(tab)) {
                            view.getTabPane().getTabs().remove(tab);
                        }
                        event.consume();
                    }
                    if (isShortcutDown && (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.E)) {
                        // shortcut to execute the code
                        event.consume();
                    }
                });
            }
        });
    }
    
    private void onOpenFilesButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("RDF and SPARQL Files", "*.ttl", "*.rdf", "*.n3", "*.rq"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                addNewTab(file);
            } catch (Exception e) {
                showError("Error Opening File", "Could not open the file: " + e.getMessage());
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleSaveShortcut() {
        Tab selectedTab = view.getTabPane().getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab != view.getAddTab()) {
            CodeEditorController activeController = model.getControllerForTab(selectedTab);
            if (activeController != null) {
                activeController.saveFile();
            }
        }
    }

    private boolean handleCloseFile(Tab tab) {
        CodeEditorController controller = model.getControllerForTab(tab);
        if (controller != null && controller.getModel().isModified()) {
            SaveConfirmationPopup saveConfirmationPopup = (SaveConfirmationPopup) PopupFactory.getInstance().createPopup("saveFileConfirmation");
            saveConfirmationPopup.setOnSaveCallback(() -> {
                controller.saveFile();
            });

            String result = saveConfirmationPopup.getResult();
            switch (result) {
                case "save":
                    model.getTabControllers().remove(tab);
                    return true;
                case "close":
                    model.getTabControllers().remove(tab);
                    return true;
                case "cancel":
                    return false;
                default:
                    return false;
            }
        } else {
            model.getTabControllers().remove(tab);
            return true;
        }
    }

    public void addNewTab(File file) {
        try {
            String content = Files.readString(file.toPath());
            addNewTabHelper(file.getName(), content, file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void openFile(File file) {
        for (Tab tab : view.getTabPane().getTabs()) {
            if (tab != view.getAddTab() && file.getName().equals(tab.getText())) {
                view.getTabPane().getSelectionModel().select(tab);
                return;
            }
        }
        addNewTab(file);
    }

    public TabEditorView getView() {
        return view;
    }

    public TabEditorModel getModel() {
        return model;
    }

    public IconButtonBarType getType() {
        return type;
    }
}
