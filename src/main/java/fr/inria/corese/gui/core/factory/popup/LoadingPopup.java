package fr.inria.corese.gui.core.factory.popup;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class LoadingPopup implements IPopup {
  private Stage stage;
  private String message = "Loading...";
  private Label messageLabel;
  private ProgressIndicator progressIndicator;
  private Timeline pulseAnimation;
  private boolean indeterminate = true;
  private double progress = -1;

  public LoadingPopup() {
    // Constructeur par défaut
  }

  @Override
  public void setMessage(String message) {
    this.message = message;
    if (messageLabel != null) {
      Platform.runLater(() -> messageLabel.setText(message));
    }
  }

  @Override
  public void closePopup() {
    close();
  }

  @Override
  public void displayPopup() {
    createAndShowStage();
  }

  @Override
  public String getPopupTitle() {
    return "";
  }

  private void createAndShowStage() {
    if (stage != null && stage.isShowing()) {
      // Si le popup est déjà affiché, mettre à jour son message
      Platform.runLater(() -> messageLabel.setText(message));
      return;
    }

    // Créer un nouveau stage
    stage = new Stage();
    stage.initStyle(StageStyle.TRANSPARENT);
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setAlwaysOnTop(true);

    // Créer l'indicateur de progression
    progressIndicator = new ProgressIndicator();
    progressIndicator.setMinSize(60, 60);
    progressIndicator.setMaxSize(60, 60);
    if (!indeterminate) {
      progressIndicator.setProgress(progress);
    }

    // Créer le label de message
    messageLabel = new Label(message);
    messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

    // Créer le conteneur principal
    VBox contentBox = new VBox(15);
    contentBox.setAlignment(Pos.CENTER);
    contentBox.getChildren().addAll(progressIndicator, messageLabel);

    // Effet de pulsation pour l'indicateur
    setupPulseAnimation();

    // Appliquer un effet d'ombre
    DropShadow dropShadow = new DropShadow();
    dropShadow.setColor(Color.BLACK);
    dropShadow.setRadius(10);
    dropShadow.setSpread(0.2);
    contentBox.setEffect(dropShadow);

    // Créer le panneau principal avec un fond semi-transparent
    StackPane root = new StackPane(contentBox);
    root.setStyle("-fx-background-color: rgba(44, 62, 80, 0.9); -fx-background-radius: 15px;");
    root.setPrefWidth(300);
    root.setPrefHeight(200);
    root.setPadding(new javafx.geometry.Insets(20));

    // Configurer la scène
    Scene scene = new Scene(root);
    scene.setFill(Color.TRANSPARENT);
    stage.setScene(scene);

    // Positionner la fenêtre au centre de l'écran
    stage.centerOnScreen();

    // Montrer le stage
    stage.show();
  }

  private void setupPulseAnimation() {
    if (pulseAnimation != null) {
      pulseAnimation.stop();
    }

    pulseAnimation =
        new Timeline(
            new KeyFrame(
                Duration.ZERO, e -> progressIndicator.setStyle("-fx-progress-color: #3498db;")),
            new KeyFrame(
                Duration.seconds(1),
                e -> progressIndicator.setStyle("-fx-progress-color: #2980b9;")),
            new KeyFrame(
                Duration.seconds(2),
                e -> progressIndicator.setStyle("-fx-progress-color: #3498db;")));
    pulseAnimation.setCycleCount(Timeline.INDEFINITE);
    pulseAnimation.play();
  }

  /** Ferme la popup de chargement */
  public void close() {
    if (stage != null && stage.isShowing()) {
      Platform.runLater(
          () -> {
            if (pulseAnimation != null) {
              pulseAnimation.stop();
            }
            stage.close();
          });
    }
  }

  /**
   * Définit si l'indicateur de progression est indéterminé ou non
   *
   * @param indeterminate true pour un indicateur indéterminé, false pour un indicateur déterminé
   */
  public void setIndeterminate(boolean indeterminate) {
    this.indeterminate = indeterminate;
    if (progressIndicator != null) {
      Platform.runLater(
          () -> {
            if (indeterminate) {
              progressIndicator.setProgress(-1);
            } else {
              progressIndicator.setProgress(progress);
            }
          });
    }
  }

  /**
   * Met à jour la progression (entre 0.0 et 1.0)
   *
   * @param progress valeur de progression entre 0.0 et 1.0
   */
  public void updateProgress(double progress) {
    this.progress = progress;
    if (!indeterminate && progressIndicator != null) {
      Platform.runLater(() -> progressIndicator.setProgress(progress));
    }
  }

  /**
   * Ajoute un message secondaire en dessous du message principal
   *
   * @param detailMessage message de détail
   */
  public void setDetailMessage(String detailMessage) {
    if (stage != null && stage.isShowing()) {
      Platform.runLater(
          () -> {
            VBox contentBox = (VBox) ((StackPane) stage.getScene().getRoot()).getChildren().get(0);

            // Vérifier si un message de détail existe déjà
            if (contentBox.getChildren().size() > 2) {
              Label existingDetail = (Label) contentBox.getChildren().get(2);
              existingDetail.setText(detailMessage);
            } else {
              // Créer un nouveau label de détail
              Label detailLabel = new Label(detailMessage);
              detailLabel.setStyle(
                  "-fx-text-fill: #ecf0f1; -fx-font-size: 12px; -fx-font-style: italic;");
              contentBox.getChildren().add(detailLabel);
            }
          });
    }
  }
}
