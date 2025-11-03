package fr.inria.corese.demo.view;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.view.icon.IconButtonView;
import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ConsoleView extends StackPane {
  private final TextArea consoleOutput;
  private final IconButtonView exportButton;

  public ConsoleView() {
    String cssPath = "/styles/console-validation.css";
    this.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

    consoleOutput = new TextArea();
    consoleOutput.setEditable(true);
    consoleOutput.setWrapText(true);
    consoleOutput.getStyleClass().add("console-output");

    VBox.setVgrow(consoleOutput, Priority.ALWAYS);
    this.setMaxWidth(Double.MAX_VALUE);
    this.setMaxHeight(Double.MAX_VALUE);

    this.getStyleClass().add("console-container");

    exportButton = new IconButtonView(IconButtonType.EXPORT);
    setAlignment(exportButton, Pos.TOP_RIGHT);
    this.getChildren().addAll(consoleOutput, exportButton);
  }

  public TextArea getConsoleOutput() {
    return consoleOutput;
  }

  public IconButtonView getExportButton() {
    return exportButton;
  }
}
