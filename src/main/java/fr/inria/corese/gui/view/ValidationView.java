package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.SplitEditorView;

/**
 * View for the Validation screen.
 *
 * <p>Displays a code editor for SHACL shapes and a results pane for validation reports.
 *
 * <pre>
 * +------------------------------------------------+
 * |  ValidationView (SplitEditorView)              |
 * |  +------------------------------------------+  |
 * |  |  TabEditorView (Shapes Editor)           |  |
 * |  +------------------------------------------+  |
 * |  |  ResultView (Validation Report)          |  |
 * |  +------------------------------------------+  |
 * +------------------------------------------------+
 * </pre>
 */
public class ValidationView extends SplitEditorView {

  // ===== Constructor =====

  /**
   * Creates the ValidationView with a horizontal split pane containing the editor and results
   * areas.
   */
  public ValidationView() {
    super("/styles/split-editor-view.css");
  }
}
