package fr.inria.corese.gui.core.factory.popup;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class RenamePopup extends BasePopup {
  private final TextField nameField;
  private Runnable onConfirm;

  public RenamePopup() {
    setTitle("Rename");
    setHeaderText("Rename the item");

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    nameField = new TextField();
    grid.add(new Label("New name :"), 0, 0);
    grid.add(nameField, 1, 0);

    getDialogPane().setContent(grid);

    ButtonType confirmButtonType = new ButtonType("Rename", ButtonBar.ButtonData.OK_DONE);
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

  public String getNewName() {
    return nameField.getText();
  }

  public void setInitialName(String name) {
    nameField.setText(name);
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex > 0) {
      nameField.selectRange(0, dotIndex);
    } else {
      nameField.selectAll();
    }
  }

  public void setOnConfirm(Runnable callback) {
    this.onConfirm = callback;
  }
}
