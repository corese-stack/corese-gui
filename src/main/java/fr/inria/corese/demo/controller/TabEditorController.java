package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonBarType;
import fr.inria.corese.demo.model.TabEditorModel;
import fr.inria.corese.demo.view.TabEditorView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class TabEditorController {
  private final TabEditorView view;
  private final TabEditorModel model;
  private final IconButtonBarType type;

  public TabEditorController(IconButtonBarType type) {
    this.view = new TabEditorView();
    this.model = new TabEditorModel();
    this.type = type;
    initializeTabPane();
    initializeKeyboardShortcuts();
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
    view.getAddTabButton().setOnAction(e -> addNewTab("Untitled", ""));
  }

  private Tab addNewTabHelper(String title, String content, String filePath) {
    CodeEditorController codeEditorController = new CodeEditorController(type, content);
    Tab tab = view.createEditorTab(title, codeEditorController.getView());
    model.addTabModel(tab, codeEditorController);

    tab.textProperty().bind(codeEditorController.getModel().displayNameProperty());

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
    try {
      String content = Files.readString(file.toPath());
      return addNewTabHelper(file.getName(), content, file.getPath());
    } catch (IOException e) {
      showError("Error Opening File", "Could not read file: " + e.getMessage());
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
    boolean shouldClose = true;

    if (controller != null && controller.getModel().isModified()) {
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
          if (controller.getModel().isModified()) shouldClose = false;
        } else if (result.get() == cancel) {
          shouldClose = false;
        }
      } else {
        shouldClose = false;
      }
    }
    if (shouldClose) {
      view.getTabPane().getTabs().remove(tab);
      model.removeTabModel(tab);
    }
    return shouldClose;
  }

  private void showError(String title, String content) {
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
