package fr.inria.corese.gui.core.factory.popup;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SaveConfirmationPopup extends Stage implements IPopup {
  private final Label messageLabel;
  private final Button saveButton;
  private final Button closeWithoutSavingButton;
  private final Button cancelButton;
  private final VBox layout;
  private String result;
  private Runnable onSaveCallback;

  public SaveConfirmationPopup() {
    initModality(Modality.APPLICATION_MODAL);
    initStyle(StageStyle.UNDECORATED);

    Label warningIconLabel = new Label("⚠");
    warningIconLabel.setStyle("-fx-text-fill: #c04139; -fx-font-size: 24px;");
    Label headerLabel = new Label("UNSAVED CHANGES");
    headerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #c04139;");

    HBox header = new HBox(10, warningIconLabel, headerLabel);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setStyle(
        "-fx-background-color: #fff; -fx-padding: 10; -fx-border-width: 0 0 1 0; -fx-border-color:"
            + " #c0413980;");

    // Message
    messageLabel = new Label();
    messageLabel.setWrapText(true);
    messageLabel.setMaxWidth(400);
    messageLabel.setStyle("-fx-padding: 15;");
    messageLabel.setText("You have unsaved changes. Do you want to save them before closing?");

    // Buttons
    saveButton = new Button("Save and Close");
    saveButton.setStyle(
        "-fx-background-color: #c04139; -fx-text-fill: white; -fx-background-radius: 3;");
    saveButton.setOnAction(
        e -> {
          result = "save";
          if (onSaveCallback != null) {
            onSaveCallback.run();
          }
          closePopup();
        });

    closeWithoutSavingButton = new Button("Close without Saving");
    closeWithoutSavingButton.setStyle("-fx-background-color: #E0E0E0; -fx-background-radius: 3;");
    closeWithoutSavingButton.setOnAction(
        e -> {
          result = "close";
          closePopup();
        });

    cancelButton = new Button("Cancel");
    cancelButton.setStyle("-fx-background-color: #E0E0E0; -fx-background-radius: 3;");
    cancelButton.setOnAction(
        e -> {
          result = "cancel";
          closePopup();
        });

    HBox buttonBox = new HBox(10, cancelButton, closeWithoutSavingButton, saveButton);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);
    buttonBox.setStyle(
        "-fx-padding: 10; -fx-background-color: #FAFAFA; -fx-border-width: 1 0 0 0;"
            + " -fx-border-color: #E0E0E0;");

    layout = new VBox();
    layout.getChildren().addAll(header, messageLabel, buttonBox);
    layout.setStyle(
        "-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10,"
            + " 0, 0, 0); -fx-border-color: #c04139; -fx-border-width: 1;");

    Scene scene = new Scene(layout);
    scene.setFill(null);

    setScene(scene);

    // Set Save as default button
    saveButton.setDefaultButton(true);
    cancelButton.setCancelButton(true);
  }

  public void setOnSaveCallback(Runnable callback) {
    this.onSaveCallback = callback;
  }

  @Override
  public void setMessage(String message) {
    messageLabel.setText(message);
  }

  @Override
  public void closePopup() {
    close();
  }

  @Override
  public void displayPopup() {
    show();
  }

  @Override
  public String getPopupTitle() {
    return "Save Changes";
  }

  public String getResult() {
    showAndWait();
    return result;
  }
}
