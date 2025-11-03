package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.factory.popup.DeleteConfirmationPopup;
import fr.inria.corese.demo.factory.popup.PopupFactory;
import fr.inria.corese.demo.factory.popup.RenamePopup;
import fr.inria.corese.demo.model.ContextMenuModel;
import fr.inria.corese.demo.view.ContextMenuView;
import java.io.File;
import java.util.function.Consumer;
import javafx.scene.control.*;

public class ContextMenuController {
  private final ContextMenuModel model;
  private final ContextMenuView view;
  private Consumer<TreeItem<String>> onItemRenamed;
  private Consumer<TreeItem<String>> onItemDeleted;

  public ContextMenuController() {
    this.model = new ContextMenuModel();
    this.view = new ContextMenuView();
    initializeEventHandlers();
  }

  private void initializeEventHandlers() {
    view.getRenameItem().setOnAction(e -> handleRename());
    view.getDeleteItem().setOnAction(e -> handleDelete());
  }

  public void show(double x, double y, File file, String itemPath, TreeView<?> anchor) {
    model.setSelectedFile(file);
    model.setSelectedItemPath(itemPath);
    view.show(anchor, x, y);
  }

  private void handleRename() {
    RenamePopup renamePopup = (RenamePopup) PopupFactory.getInstance().createPopup("rename");
    renamePopup.setInitialName(model.getSelectedFile().getName());
    renamePopup.setOnConfirm(
        () -> {
          String newName = renamePopup.getNewName();
          if (!newName.isEmpty()) {
            if (model.renameFile(newName)) {
              if (onItemRenamed != null) {
                TreeItem<String> item = new TreeItem<>(newName);
                onItemRenamed.accept(item);
              }
            }
          }
        });
    renamePopup.displayPopup();
  }

  private void handleDelete() {
    DeleteConfirmationPopup deletePopup =
        (DeleteConfirmationPopup) PopupFactory.getInstance().createPopup("delete");

    if (deletePopup.getResult()) {
      if (model.deleteFile()) {
        if (onItemDeleted != null) {
          TreeItem<String> item = new TreeItem<>(model.getSelectedFile().getName());
          onItemDeleted.accept(item);
        }
      }
    }
  }

  public void setOnItemRenamed(Consumer<TreeItem<String>> handler) {
    this.onItemRenamed = handler;
  }

  public void setOnItemDeleted(Consumer<TreeItem<String>> handler) {
    this.onItemDeleted = handler;
  }
}
