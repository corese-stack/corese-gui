package fr.inria.corese.gui.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import fr.inria.corese.gui.enums.icon.IconButtonBarType;
import fr.inria.corese.gui.model.TabEditorModel;
import fr.inria.corese.gui.view.TabEditorView;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

public class TabEditorController {
  private final TabEditorView view;
  private final TabEditorModel model;
  private final IconButtonBarType type;
  private CodeEditorController preloadedController;

  public TabEditorController(IconButtonBarType type) {
    this.view = new TabEditorView();
    this.model = new TabEditorModel();
    this.type = type;
    initializeTabPane();
    initializeKeyboardShortcuts();
    Platform.runLater(this::preloadNextEditor);
  }

  private void preloadNextEditor() {
    if (preloadedController == null) {
      preloadedController = new CodeEditorController(type, "");
    }
  }

  private void initializeTabPane() {
    view.getTabPane()
        .getTabs()
        .addListener(
            (ListChangeListener<Tab>)
                c -> {
                  while (c.next()) {
                    if (c.wasRemoved() && view.getTabPane().getTabs().isEmpty()) {
                      Platform.runLater(() -> view.getTabPane().getTabs().add(view.getAddTab()));
                    } else if (!view.getTabPane().getTabs().contains(view.getAddTab())) {
                      view.getTabPane().getTabs().add(view.getAddTab());
                    }
                  }
                });
    
    // Handle selection of the "Add Tab"
    view.getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
        if (newTab == view.getAddTab()) {
            // If the user clicked the add tab, create a new tab
            Platform.runLater(() -> {
                addNewTab("Untitled", "");
                // The addNewTab method selects the new tab, so we don't need to do anything else
                // But if for some reason it failed, we should go back to the old tab
                if (view.getTabPane().getSelectionModel().getSelectedItem() == view.getAddTab()) {
                     if (oldTab != null && view.getTabPane().getTabs().contains(oldTab)) {
                         view.getTabPane().getSelectionModel().select(oldTab);
                     } else if (view.getTabPane().getTabs().size() > 1) {
                         view.getTabPane().getSelectionModel().select(0);
                     }
                }
            });
        }
    });
    
    // Handle SplitMenuButton actions
    view.getAddTabButton().setOnAction(e -> addNewTab("Untitled", ""));
    view.getNewFileItem().setOnAction(e -> addNewTab("Untitled", ""));
    view.getOpenFileItem().setOnAction(e -> openFile());
    view.getTemplatesItem().setOnAction(e -> openTemplates());
  }

  private void openFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open File");
    File file = fileChooser.showOpenDialog(view.getScene().getWindow());
    if (file != null) {
      addNewTab(file);
    }
  }

  private void openTemplates() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Templates");
    alert.setHeaderText("Templates not implemented yet");
    alert.setContentText("This feature will be available soon.");
    alert.showAndWait();
  }

  private Tab addNewTabHelper(String title, String content, String filePath) {
    CodeEditorController codeEditorController;
    if (preloadedController != null) {
      codeEditorController = preloadedController;
      preloadedController = null;
      codeEditorController.getModel().setContent(content);
      Platform.runLater(this::preloadNextEditor);
    } else {
      codeEditorController = new CodeEditorController(type, content);
    }

    Tab tab = view.createEditorTab(title, codeEditorController.getView());
    model.addTabModel(tab, codeEditorController);

    tab.textProperty().bind(codeEditorController.getModel().displayNameProperty());
    
    // Bind modified property to tab icon
    codeEditorController.getModel().modifiedProperty().addListener((obs, oldVal, newVal) -> 
        view.updateTabIcon(tab, newVal)
    );

    int addTabIndex = view.getTabPane().getTabs().size() - 1;
    view.getTabPane().getTabs().add(Math.max(0, addTabIndex), tab);
    view.getTabPane().getSelectionModel().select(tab);

    if (filePath != null) {
      codeEditorController.getModel().setFilePath(filePath);
      codeEditorController.getModel().markAsSaved();
    }
    tab.setOnCloseRequest(
        event -> {
          event.consume();
          handleCloseFile(tab);
        });

    return tab;
  }

  public Tab addNewTab(String title, String content) {
    return addNewTabHelper(title, content, null);
  }

  public Tab addNewTab(File file) {
    // Check if file is already open
    for (Tab tab : view.getTabPane().getTabs()) {
      if (tab == view.getAddTab()) continue;
      CodeEditorController controller = model.getControllerForTab(tab);
      if (controller != null && file.getAbsolutePath().equals(controller.getModel().getFilePath())) {
        view.getTabPane().getSelectionModel().select(tab);
        return tab;
      }
    }

    try {
      String content = Files.readString(file.toPath());
      return addNewTabHelper(file.getName(), content, file.getPath());
    } catch (IOException e) {
      showError("Could not read file: " + e.getMessage());
      return null;
    }
  }

  private void initializeKeyboardShortcuts() {
    view.addEventHandler(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
            handleSaveShortcut();
            event.consume();
          }
        });
  }

  private void handleSaveShortcut() {
    Tab selectedTab = view.getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab != null && selectedTab != view.getAddTab()) {
      CodeEditorController activeController = model.getControllerForTab(selectedTab);
      if (activeController != null) activeController.saveFile();
    }
  }

  public boolean handleCloseFile(Tab tab) {
    if (tab == null || tab == view.getAddTab()) return false;
    CodeEditorController controller = model.getControllerForTab(tab);
    
    if (controller == null || !controller.getModel().isModified()) {
        closeTab(tab);
        return true;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Unsaved Changes");
    alert.setHeaderText("Save changes to " + controller.getModel().getDisplayName() + "?");
    ButtonType save = new ButtonType("Save");
    ButtonType dontSave = new ButtonType("Don't Save");
    ButtonType cancel = new ButtonType("Cancel");
    alert.getButtonTypes().setAll(save, dontSave, cancel);

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent()) {
      if (result.get() == save) {
        controller.saveFile();
        if (!controller.getModel().isModified()) {
            closeTab(tab);
            return true;
        }
      } else if (result.get() == dontSave) {
        closeTab(tab);
        return true;
      }
    }
    return false;
  }

  private void closeTab(Tab tab) {
      view.getTabPane().getTabs().remove(tab);
      model.removeTabModel(tab);
  }

  private void showError(String content) {
    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, content).showAndWait());
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
