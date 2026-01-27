package fr.inria.corese.gui.view;

import fr.inria.corese.gui.core.ButtonConfig;
import fr.inria.corese.gui.enums.icon.ButtonIcon;
import fr.inria.corese.gui.view.icon.ActionButtonWidget;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

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

  private HBox leftButtonsContainer;
  private HBox rightButtonsContainer;

  private final Map<ButtonIcon, ActionButtonWidget> buttons;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Un map des boutons - Chargement du fichier FXML - Application du style de
   * barre supérieure
   */
  public TopBar() {
    this.buttons = new EnumMap<>(ButtonIcon.class);
    initializeLayout();
    getStyleClass().add("top-bar");
  }

  /**
   * Charge l'interface.
   */
  private void initializeLayout() {
    setSpacing(0);

    leftButtonsContainer = new HBox(2);
    leftButtonsContainer.setAlignment(Pos.CENTER_LEFT);
    leftButtonsContainer.setPadding(new Insets(0, 0, 0, 5));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    rightButtonsContainer = new HBox(2);
    rightButtonsContainer.setAlignment(Pos.CENTER_RIGHT);
    rightButtonsContainer.setPadding(new Insets(0, 5, 0, 0));

    getChildren().addAll(leftButtonsContainer, spacer, rightButtonsContainer);
  }

  /**
   * Ajoute des boutons dans le conteneur de gauche.
   *
   * @param configs Liste des configurations de boutons à ajouter
   */
  public void addLeftButtons(List<ButtonConfig> configs) {
    leftButtonsContainer.getChildren().clear();
    addButtonsToContainer(configs, leftButtonsContainer);
  }

  /**
   * Ajoute des boutons dans le conteneur de droite.
   *
   * @param configs Liste des configurations de boutons à ajouter
   */
  public void addRightButtons(List<ButtonConfig> configs) {
    rightButtonsContainer.getChildren().clear();
    addButtonsToContainer(configs, rightButtonsContainer);
  }

  private void addButtonsToContainer(List<ButtonConfig> configs, HBox container) {
    if (configs == null) return;

    for (ButtonConfig config : configs) {
        if (config.getIcon() == null) continue;

        ActionButtonWidget button = new ActionButtonWidget(config);

        buttons.put(config.getIcon(), button);
        container.getChildren().add(button);
    }
  }

  /**
   * Récupère un bouton spécifique par son type.
   *
   * @param type Le type de bouton recherché
   * @return Le bouton correspondant, ou null s'il n'existe pas
   */
  public Button getButton(ButtonIcon type) {
    return buttons.get(type);
  }
}