package fr.inria.corese.gui.feature.codeeditor;

import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.view.AbstractView;
import javafx.scene.layout.BorderPane;

public class CodeEditorView extends AbstractView {
  private final BorderPane root;
  private final CodeMirrorWidget codeMirrorView;
  private final ToolbarWidget toolbarWidget;

  public CodeEditorView() {
    super(new BorderPane(), null);
    this.root = (BorderPane) getRoot();
    this.codeMirrorView = new CodeMirrorWidget();
    this.toolbarWidget = new ToolbarWidget();

    root.setCenter(codeMirrorView);
    root.setRight(toolbarWidget);
  }

  public String getText() {
    return codeMirrorView.getContent();
  }

  public CodeMirrorWidget getCodeMirrorView() {
    return codeMirrorView;
  }

  public void setCodeMirrorViewContent(String content) {
    codeMirrorView.setContent(content);
  }

  public ToolbarWidget getToolbarWidget() {
    return toolbarWidget;
  }

  public void zoomIn() {
    codeMirrorView.zoomIn();
  }

  public void zoomOut() {
    codeMirrorView.zoomOut();
  }
}
