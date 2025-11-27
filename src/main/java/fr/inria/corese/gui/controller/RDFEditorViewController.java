package fr.inria.corese.gui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.gui.enums.icon.IconButtonBarType;
import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.factory.popup.DocumentationPopup;
import fr.inria.corese.gui.view.TopBar;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RDFEditorViewController {
  @FXML private BorderPane mainContainer;
  @FXML private VBox fileExplorerContainer;
  @FXML private StackPane editorContainer;
  @FXML private SplitPane splitPane;
  @FXML private TopBar topbar;

  private FileExplorerController fileExplorerController;
  private TabEditorController tabEditorController;

  @FXML
  public void initialize() {
    setupFileTree();
    initializeTopBar();
    initializeTabEditor();
    initializeSplitPane();
    setupComponentInteractions();
    setupKeyboardShortcuts();
  }

  private void initializeTabEditor() {
    tabEditorController = new TabEditorController(IconButtonBarType.RDF_EDITOR);
    editorContainer.getChildren().add(tabEditorController.getView());
  }

  private void initializeTopBar() {
    List<IconButtonType> buttons = new ArrayList<>(List.of(IconButtonType.DOCUMENTATION));
    topbar.addRightButtons(buttons);
    topbar
        .getButton(IconButtonType.DOCUMENTATION)
        .setOnAction(e -> new DocumentationPopup().displayPopup());
  }

  private void setupFileTree() {
    fileExplorerController = new FileExplorerController();
    fileExplorerContainer.getChildren().add(fileExplorerController.getView());
  }

  private void setupComponentInteractions() {
    fileExplorerController.setOnFileOpenRequest(
        file -> {
          // FIXED: Call the correct method to open a file.
          openFileInEditor(file);
        });
  }

  /** New method to handle opening a file, focusing it if already open. */
  public void openFileInEditor(File file) {
    // Check if the file is already open
    for (Tab tab : tabEditorController.getView().getTabPane().getTabs()) {
      if (tab != tabEditorController.getView().getAddTab()) {
        CodeEditorController controller = tabEditorController.getModel().getControllerForTab(tab);
        if (controller != null
            && file.getAbsolutePath().equals(controller.getModel().getFilePath())) {
          tabEditorController.getView().getTabPane().getSelectionModel().select(tab);
          return;
        }
      }
    }
    tabEditorController.addNewTab(file);
  }

  private void initializeSplitPane() {
    splitPane
        .sceneProperty()
        .addListener(
            (observable, oldScene, newScene) -> {
              if (newScene != null) {
                Platform.runLater(
                    () -> {
                      splitPane
                          .lookupAll(".split-pane-divider")
                          .forEach(
                              div -> {
                                div.addEventHandler(
                                    MouseEvent.MOUSE_CLICKED,
                                    event -> {
                                      if (event.getClickCount() == 2) toggleLeftPane();
                                    });
                              });
                    });
              }
            });
    Platform.runLater(() -> splitPane.setDividerPositions(0.2));
  }

  private void toggleLeftPane() {
    splitPane.setDividerPositions(splitPane.getDividerPositions()[0] > 0.01 ? 0.0 : 0.2);
  }

  private void setupKeyboardShortcuts() {
    mainContainer
        .sceneProperty()
        .addListener(
            (obs, oldScene, newScene) -> {
              if (newScene != null) {
                newScene.addEventFilter(
                    KeyEvent.KEY_PRESSED,
                    event -> {
                      if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        Tab selectedTab =
                            tabEditorController
                                .getView()
                                .getTabPane()
                                .getSelectionModel()
                                .getSelectedItem();
                        if (selectedTab != null
                            && selectedTab != tabEditorController.getView().getAddTab()) {
                          CodeEditorController activeController =
                              tabEditorController.getModel().getControllerForTab(selectedTab);
                          if (activeController != null) activeController.saveFile();
                        }
                        event.consume();
                      } else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN)
                          .match(event)) {
                        Tab selectedTab =
                            tabEditorController
                                .getView()
                                .getTabPane()
                                .getSelectionModel()
                                .getSelectedItem();
                        tabEditorController.handleCloseFile(selectedTab);

                        event.consume();
                      }
                    });
              }
            });
  }
}
