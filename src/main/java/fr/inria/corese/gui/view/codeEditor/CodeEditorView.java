package fr.inria.corese.gui.view.codeEditor;

import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.icon.ToolbarView;
import javafx.scene.layout.BorderPane;

public class CodeEditorView extends AbstractView {
  private final BorderPane root;
  private final CodeMirrorView codeMirrorView;
  private final ToolbarView iconButtonBarView;

  public CodeEditorView() {
    super(new BorderPane(), null);
    this.root = (BorderPane) getRoot();
    this.codeMirrorView = new CodeMirrorView();
    this.iconButtonBarView = new ToolbarView();

    root.setCenter(codeMirrorView);
    root.setRight(iconButtonBarView.getRoot());
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

    public ToolbarView getToolbarView() {

      return iconButtonBarView;

    }

  }

  