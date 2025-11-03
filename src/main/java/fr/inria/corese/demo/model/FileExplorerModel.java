package fr.inria.corese.demo.model;

import fr.inria.corese.demo.model.fileList.FileItem;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TreeItem;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

public class FileExplorerModel {
  private final List<FileItem> files;
  private List<FileItem> folders;
  private String rootPath;

  private final BooleanProperty isOpen = new SimpleBooleanProperty(true);

  public FileExplorerModel() {
    this.files = new ArrayList<>();
    this.folders = new ArrayList<>();
  }

  public FileExplorerModel(List<FileItem> files) {
    this.files = files;
  }

  public BooleanProperty isOpenProperty() {
    return isOpen;
  }

  public boolean getIsOpen() {
    return isOpen.get();
  }

  public void setIsOpen(boolean isOpen) {
    this.isOpen.set(isOpen);
  }

  public List<FileItem> getFiles() {
    return files;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String path) {
    rootPath = path;
  }

  public void addFile(TreeItem node, FileItem file) {
    files.add(file);
    TreeItem<String> newFile = new TreeItem<>(file.getName());
    FontIcon fileIcon = new FontIcon(MaterialDesignF.FILE_OUTLINE);
    fileIcon.setIconSize(20);
    newFile.setGraphic(fileIcon);
    node.getChildren().add(newFile);
  }

  public void addFolder(TreeItem node, FileItem folder) {
    folders.add(folder);
    TreeItem<String> newFolder = new TreeItem<>(folder.getName());
    FontIcon folderIcon = new FontIcon(MaterialDesignF.FOLDER_OUTLINE);
    folderIcon.setIconSize(20);
    newFolder.setGraphic(folderIcon);
    node.getChildren().add(newFolder);
  }
}
