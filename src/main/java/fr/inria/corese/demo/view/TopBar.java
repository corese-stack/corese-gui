package fr.inria.corese.demo.view;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.view.icon.IconButtonView;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Barre supérieure de navigation et d'actions dans l'interface utilisateur.
 *
 * <p>Responsabilités principales : - Gestion des boutons de gauche et de droite - Configuration
 * dynamique des actions des boutons - Chargement de l'interface à partir d'un fichier FXML
 *
 * <p>Caractéristiques principales : - Disposition horizontale (HBox) - Conteneurs séparés pour
 * boutons gauche et droite - Gestion centralisée des boutons
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class TopBar extends HBox {

  @FXML private HBox leftButtonsContainer;
  @FXML private HBox rightButtonsContainer;

  private final Map<IconButtonType, IconButtonView> buttons;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Un map des boutons - Chargement du fichier FXML - Application du style de
   * barre supérieure
   */
  public TopBar() {
    this.buttons = new HashMap<>();
    loadFxml();
    getStyleClass().add("top-bar");
  }

  /**
   * Charge l'interface à partir d'un fichier FXML.
   *
   * <p>Gère le chargement et les erreurs potentielles.
   */
  private void loadFxml() {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fr/inria/corese/demo/topbar.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Ajoute des boutons dans le conteneur de gauche.
   *
   * @param buttonTypes Liste des types de boutons à ajouter
   */
  public void addLeftButtons(List<IconButtonType> buttonTypes) {
    leftButtonsContainer.getChildren().clear();
    buttonTypes.forEach(
        type -> {
          IconButtonView button = new IconButtonView(type);
          buttons.put(type, button);
          leftButtonsContainer.getChildren().add(button);
        });
  }

  /**
   * Ajoute des boutons dans le conteneur de droite.
   *
   * @param buttonTypes Liste des types de boutons à ajouter
   */
  public void addRightButtons(List<IconButtonType> buttonTypes) {
    rightButtonsContainer.getChildren().clear();
    buttonTypes.forEach(
        type -> {
          IconButtonView button = new IconButtonView(type);
          buttons.put(type, button);
          rightButtonsContainer.getChildren().add(button);
        });
  }

  /**
   * Récupère un bouton spécifique par son type.
   *
   * @param type Le type de bouton recherché
   * @return Le bouton correspondant, ou null s'il n'existe pas
   */
  public Button getButton(IconButtonType type) {
    return buttons.get(type);
  }

  /**
   * Définit l'action à exécuter pour un bouton spécifique.
   *
   * @param type Le type de bouton
   * @param action L'action à exécuter lors du clic
   */
  public void setOnAction(IconButtonType type, Runnable action) {
    IconButtonView button = buttons.get(type);
    if (button != null) {
      button.setOnAction(e -> action.run());
    }
  }
}
