package fr.inria.corese.demo.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Vue représentant un état vide ou initial dans l'interface utilisateur.
 *
 * <p>Cette classe permet d'afficher un écran personnalisé lorsqu'aucun contenu n'est disponible,
 * avec un titre, un message et une icône SVG.
 *
 * <p>Caractéristiques principales : - Disposition centrée sur un StackPane - Personnalisation du
 * titre et du message - Affichage d'une icône SVG - Style configurable
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class EmptyStateView extends StackPane {
  private final Label titleLabel;
  private final Label messageLabel;
  private final String image;

  /**
   * Constructeur pour créer une vue d'état vide.
   *
   * @param title Le label de titre à afficher
   * @param message Le label de message à afficher
   * @param img Le chemin ou le contenu SVG de l'icône
   */
  public EmptyStateView(Label title, Label message, String img) {
    titleLabel = title;
    messageLabel = message;
    image = img;

    setupUI();
  }

  /**
   * Configure l'interface utilisateur de la vue.
   *
   * <p>Initialise : - Le style de fond - Le conteneur vertical - L'icône SVG - Les labels de titre
   * et de message
   */
  private void setupUI() {
    setStyle("-fx-background-color: white;");
    getStyleClass().add("empty-state-view");

    VBox container = new VBox(15);
    container.setAlignment(Pos.CENTER);
    container.setMaxWidth(400);
    container.setMaxHeight(300);

    SVGPath folderOpenIcon = new SVGPath();
    folderOpenIcon.setContent(image);
    folderOpenIcon.setFill(Color.web("#2196F3")); // Couleur bleue primaire
    folderOpenIcon.setScaleX(2.5);
    folderOpenIcon.setScaleY(2.5);

    titleLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));
    titleLabel.setTextFill(Color.web("#333333"));

    messageLabel.setFont(Font.font("System", 14));
    messageLabel.setTextFill(Color.web("#666666"));
    messageLabel.setAlignment(Pos.CENTER);
    messageLabel.setWrapText(true);
    messageLabel.setStyle("-fx-text-alignment: center;");

    container.getChildren().addAll(folderOpenIcon, titleLabel, messageLabel);
    getChildren().add(container);
  }
}
