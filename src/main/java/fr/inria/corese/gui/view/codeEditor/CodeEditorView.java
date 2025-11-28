package fr.inria.corese.gui.view.codeEditor;

import fr.inria.corese.gui.enums.button.ButtonType;
import fr.inria.corese.gui.view.CustomButton;
import fr.inria.corese.gui.view.icon.IconButtonBarView;
import javafx.scene.layout.BorderPane;

public class CodeEditorView extends BorderPane {
  private final CodeMirrorView codeMirrorView;
  private final IconButtonBarView iconButtonBarView;
  private CustomButton runButton;
  private boolean runButtonDisplayed = false;

  public CodeEditorView() {
    this.codeMirrorView = new CodeMirrorView();
    this.iconButtonBarView = new IconButtonBarView();

    this.runButton = new CustomButton.Builder(ButtonType.RUN).withTooltip("Run code").build();

    setCenter(codeMirrorView);
    setRight(iconButtonBarView);
  }

  public String getText() {

    return codeMirrorView.getContent();
  }

  public void displayRunButton() {
    if (runButton == null) {
      runButton = new CustomButton(ButtonType.RUN);
    }
    runButton.setVisible(true);

    if (!runButtonDisplayed) {
      String cssPath = "/styles/buttons.css";
      this.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

      getChildren().add(runButton);
      runButtonDisplayed = true;
    }
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

  public CustomButton getRunButton() {
    return runButton;
  }
}
