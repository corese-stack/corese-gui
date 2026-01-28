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

public class WarningPopup extends Stage implements IPopup {
  private final Label messageLabel;
  private final Button closeButton;
  private final Button continueButton;
  private final VBox layout;
  private boolean result;

  public WarningPopup() {
    initModality(Modality.APPLICATION_MODAL);
    initStyle(StageStyle.UNDECORATED);

    // Header with warning icon
    Label warningIconLabel = new Label("⚠");
    warningIconLabel.setStyle("-fx-text-fill: #c04139; -fx-font-size: 24px;");
    Label headerLabel = new Label("WARNING");
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

    // Buttons
    closeButton = new Button("Cancel");
    closeButton.setStyle("-fx-background-color: #E0E0E0; -fx-background-radius: 3;");
    closeButton.setOnAction(
        e -> {
          result = false;
          closePopup();
        });

    continueButton = new Button("Continue");
    continueButton.setStyle(
        "-fx-background-color: #c04139; -fx-text-fill: white; -fx-background-radius: 3;");
    continueButton.setOnAction(
        e -> {
          result = true;
          closePopup();
        });

    HBox buttonBox = new HBox(10, closeButton, continueButton);
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

    // Définir le bouton Continue comme bouton par défaut
    continueButton.setDefaultButton(true);
    closeButton.setCancelButton(true);
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
    return "Warning";
  }

  public boolean getResult() {
    super.showAndWait();
    return result;
  }
}
