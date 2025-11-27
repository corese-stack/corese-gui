package fr.inria.corese.gui.view;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeView;

public class ContextMenuView {
  private ContextMenu contextMenu;
  private MenuItem renameItem;
  private MenuItem deleteItem;

  public ContextMenuView() {
    initializeMenu();
  }

  private void initializeMenu() {
    contextMenu = new ContextMenu();
    renameItem = new MenuItem("Rename");
    deleteItem = new MenuItem("Delete");
    contextMenu.getItems().addAll(renameItem, deleteItem);
  }

  public void show(TreeView<?> anchor, double x, double y) {
    contextMenu.show(anchor, x, y);
  }

  public MenuItem getRenameItem() {
    return renameItem;
  }

  public MenuItem getDeleteItem() {
    return deleteItem;
  }
}
