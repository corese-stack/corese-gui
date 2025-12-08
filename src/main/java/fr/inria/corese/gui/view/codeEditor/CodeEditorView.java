package fr.inria.corese.gui.view.codeEditor;

import fr.inria.corese.gui.view.icon.IconButtonBarView;
import javafx.scene.layout.BorderPane;

public class CodeEditorView extends BorderPane {
  private final CodeMirrorView codeMirrorView;
  private final IconButtonBarView iconButtonBarView;

  public CodeEditorView() {
    this.codeMirrorView = new CodeMirrorView();
    this.iconButtonBarView = new IconButtonBarView();

    setCenter(codeMirrorView);
    setRight(iconButtonBarView);
  }

  public String getText() {

    return codeMirrorView.getContent();
  }

  public CodeMirrorView getCodeMirrorView() {
    return codeMirrorView;
  }

  public void setCodeMirrorViewContent(String content) {
    codeMirrorView.setContent(content);
  }

  public IconButtonBarView getIconButtonBarView() {
    return iconButtonBarView;
  }
}
