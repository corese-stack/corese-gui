package fr.inria.corese.demo.view.codeEditor;

import fr.inria.corese.demo.enums.button.ButtonType;
import fr.inria.corese.demo.view.CustomButton;
import fr.inria.corese.demo.view.icon.IconButtonBarView;
import javafx.scene.layout.AnchorPane;

public class CodeEditorView extends AnchorPane {
  private final CodeMirrorView codeMirrorView;
  private final IconButtonBarView iconButtonBarView;
  private CustomButton runButton;
  private boolean runButtonDisplayed = false;

  public CodeEditorView() {
    this.codeMirrorView = new CodeMirrorView();
    this.iconButtonBarView = new IconButtonBarView();

    this.runButton = new CustomButton.Builder(ButtonType.RUN).withTooltip("Run code").build();

    setTopAnchor(codeMirrorView, 0.0);
    setRightAnchor(codeMirrorView, 0.0);
    setBottomAnchor(codeMirrorView, 0.0);
    setLeftAnchor(codeMirrorView, 0.0);

    setTopAnchor(iconButtonBarView, 5.0);
    setRightAnchor(iconButtonBarView, 5.0);

    setRightAnchor(runButton, 15.0);
    setBottomAnchor(runButton, 30.0);

    getChildren().addAll(codeMirrorView, iconButtonBarView);
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
