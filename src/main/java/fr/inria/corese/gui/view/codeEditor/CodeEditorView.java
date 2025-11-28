package fr.inria.corese.gui.view.codeEditor;

import fr.inria.corese.gui.enums.button.ButtonType;
import fr.inria.corese.gui.view.CustomButton;
import fr.inria.corese.gui.view.icon.IconButtonBarView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class CodeEditorView extends BorderPane {
  private final CodeMirrorView codeMirrorView;
  private final IconButtonBarView iconButtonBarView;
  private CustomButton runButton;
  private boolean runButtonDisplayed = false;
  private final VBox rightContainer;

  public CodeEditorView() {
    this.codeMirrorView = new CodeMirrorView();
    this.iconButtonBarView = new IconButtonBarView();
    
    this.rightContainer = new VBox(10);
    this.rightContainer.setAlignment(Pos.TOP_CENTER);
    this.rightContainer.setPadding(new Insets(5));
    this.rightContainer.getChildren().add(iconButtonBarView);

    this.runButton = new CustomButton.Builder(ButtonType.RUN)
        .withTooltip("Run Validation")
        .build();
    this.runButton.setText("Validate");
    
    // Increase button size and style
    runButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-font-weight: bold;");
    
    setCenter(codeMirrorView);
    setRight(rightContainer);
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

      rightContainer.getChildren().add(runButton);
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
