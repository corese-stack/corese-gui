package fr.inria.corese.gui.feature.codeEditor;

public class CodeEditorChange {
  private final String previousContent;
  private final String newContent;

  public CodeEditorChange(String previousContent, String newContent) {
    this.previousContent = previousContent;
    this.newContent = newContent;
  }

  public String getPreviousContent() {
    return previousContent;
  }

  public String getNewContent() {
    return newContent;
  }
}
