package fr.inria.corese.gui.factory.popup;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class NewFilePopup extends BasePopup {
  private final TextField fileNameField;
  private Runnable onConfirm;

  public NewFilePopup() {
    setTitle("New File");
    setHeaderText("Create new file");

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    fileNameField = new TextField();
    grid.add(new Label("File name:"), 0, 0);
    grid.add(fileNameField, 1, 0);

    getDialogPane().setContent(grid);

    ButtonType confirmButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);

    setResultConverter(
        dialogButton -> {
          if (dialogButton == confirmButtonType) {
            if (onConfirm != null) {
              onConfirm.run();
            }
          }
          return null;
        });
  }

  public String getFileName() {
    return fileNameField.getText();
  }

  public void setInitialName(String name) {
    fileNameField.setText(name);
  }

  public void setOnConfirm(Runnable callback) {
    this.onConfirm = callback;
  }
}
