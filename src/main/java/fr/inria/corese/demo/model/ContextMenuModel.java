package fr.inria.corese.demo.model;

import java.io.File;

public class ContextMenuModel {
  private File selectedFile;
  private String selectedItemPath;

  public void setSelectedFile(File file) {
    this.selectedFile = file;
  }

  public File getSelectedFile() {
    return selectedFile;
  }

  public void setSelectedItemPath(String path) {
    this.selectedItemPath = path;
  }

  public String getSelectedItemPath() {
    return selectedItemPath;
  }

  public boolean renameFile(String newName) {
    if (selectedFile != null && selectedFile.exists()) {
      File newFile = new File(selectedFile.getParent(), newName);
      return selectedFile.renameTo(newFile);
    }
    return false;
  }

  public boolean deleteFile() {
    if (selectedFile != null && selectedFile.exists()) {
      return deleteFileOrDirectory(selectedFile);
    }
    return false;
  }

  private boolean deleteFileOrDirectory(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File child : files) {
          deleteFileOrDirectory(child);
        }
      }
    }
    return file.delete();
  }
}
