package fr.inria.corese.demo.factory.popup;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToastNotification implements IPopup {
  private final Stage notificationStage;
  private final Label messageLabel;
  private Timeline timeline;
  private static final int TOAST_TIMEOUT = 3000; // 3 secondes d'affichage

  public ToastNotification() {
    notificationStage = new Stage(StageStyle.TRANSPARENT);
    notificationStage.setAlwaysOnTop(true);

    messageLabel = new Label();
    messageLabel.setStyle(
        "-fx-background-color: #333333;"
            + "-fx-text-fill: white;"
            + "-fx-padding: 15px;"
            + "-fx-background-radius: 5px;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

    StackPane container = new StackPane(messageLabel);
    container.setStyle("-fx-background-color: transparent;");
    container.setPrefWidth(300);
    container.setPrefHeight(80);
    container.setAlignment(Pos.CENTER);

    Scene scene = new Scene(container);
    scene.setFill(null);
    notificationStage.setScene(scene);
  }

  @Override
  public void setMessage(String message) {
    messageLabel.setText(message);
  }

  @Override
  public void displayPopup() {
    if (timeline != null) {
      timeline.stop();
    }

    // Positionner en bas à droite
    Screen screen = Screen.getPrimary();
    notificationStage.setX(screen.getVisualBounds().getMaxX() - notificationStage.getWidth() - 20);
    notificationStage.setY(screen.getVisualBounds().getMaxY() - notificationStage.getHeight() - 40);

    // Animation d'apparition
    messageLabel.setOpacity(0);
    notificationStage.show();

    timeline =
        new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(messageLabel.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(100), new KeyValue(messageLabel.opacityProperty(), 1)),
            new KeyFrame(
                Duration.millis(TOAST_TIMEOUT - 500),
                new KeyValue(messageLabel.opacityProperty(), 1)),
            new KeyFrame(
                Duration.millis(TOAST_TIMEOUT), new KeyValue(messageLabel.opacityProperty(), 0)));

    timeline.setOnFinished(e -> closePopup());
    timeline.play();
  }

  @Override
  public void closePopup() {
    notificationStage.hide();
  }

  @Override
  public String getPopupTitle() {
    return "Notification";
  }
}
