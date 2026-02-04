package fr.inria.corese.gui.feature.editor.code;

import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.view.AbstractView;
import javafx.scene.layout.BorderPane;

/**
 * View for the Code Editor component.
 *
 * <p>Composed of:
 *
 * <ul>
 *   <li>A {@link CodeMirrorWidget} for the text editor.
 *   <li>A {@link ToolbarWidget} for actions (Save, Open, etc.).
 * </ul>
 */
public class CodeEditorView extends AbstractView {

  private final CodeMirrorWidget codeMirrorView;
  private final ToolbarWidget toolbarWidget;

  public CodeEditorView() {
    super(new BorderPane(), null); // No specific stylesheet, relies on global/component styles

    this.codeMirrorView = new CodeMirrorWidget();
    this.toolbarWidget = new ToolbarWidget();

    BorderPane root = (BorderPane) getRoot();
    root.setCenter(codeMirrorView);
    root.setRight(toolbarWidget);
  }

  // ==============================================================================================
  // Accessors
  // ==============================================================================================

  /**
   * Gets the underlying CodeMirror widget.
   *
   * <p>Exposed primarily for property binding in the controller.
   *
   * @return The editor widget.
   */
  public CodeMirrorWidget getCodeMirrorView() {
    return codeMirrorView;
  }

  /**
   * Gets the toolbar widget.
   *
   * @return The toolbar widget.
   */
  public ToolbarWidget getToolbarWidget() {
    return toolbarWidget;
  }

  // ==============================================================================================
  // Delegates
  // ==============================================================================================

  public void zoomIn() {
    codeMirrorView.zoomIn();
  }

  public void zoomOut() {
    codeMirrorView.zoomOut();
  }

  public void setContent(String content) {
    codeMirrorView.setContent(content);
  }

  public String getContent() {
    return codeMirrorView.getContent();
  }
}
