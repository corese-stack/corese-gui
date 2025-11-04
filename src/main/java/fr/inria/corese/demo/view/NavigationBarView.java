package fr.inria.corese.demo.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Vue de la barre de navigation principale de l'application.
 *
 * <p>Responsabilités principales : - Affichage des boutons de navigation - Gestion de la sélection
 * et du style des boutons - Présentation du logo de l'application
 *
 * <p>Caractéristiques principales : - Disposition verticale (VBox) - Boutons de navigation
 * personnalisés - Styles visuels différenciés
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class NavigationBarView extends VBox {
  private static final Logger logger = LoggerFactory.getLogger(NavigationBarView.class);
  
  private final ImageView logo;
  private final Button dataButton;
  private final Button rdfEditorButton;
  private final Button validationButton;
  private final Button queryButton;
  private final Button settingsButton;

  private static final String BUTTON_STYLE_NORMAL =
      """
      -fx-background-color: white;
      -fx-text-fill: #000000;
      -fx-alignment: CENTER_LEFT;
      -fx-border-color: #2196F3;
      -fx-border-width: 0 0 1 0;
      -fx-min-height: 35;
      -fx-font-size: 14;
      -fx-min-width: 50;
      -fx-max-width: 90;
      """;

  private static final String BUTTON_STYLE_SELECTED =
      """
      -fx-background-color: #E3F2FD;
      -fx-text-fill: #000000;
      -fx-alignment: CENTER_LEFT;
      -fx-border-color: #2196F3;
      -fx-border-width: 0 0 1 3;
      -fx-min-height: 35;
      -fx-font-size: 14;
      -fx-min-width: 50;
      -fx-max-width: 90;
      """;

  private static final String BUTTON_STYLE_HOVER =
      """
      -fx-background-color: #E3F2FD;
      -fx-text-fill: #000000;
      -fx-alignment: CENTER_LEFT;
      -fx-border-color: #2196F3;
      -fx-border-width: 0 0 1 0;
      -fx-min-height: 35;
      -fx-font-size: 14;
      -fx-min-width: 50;
      -fx-max-width: 90;
      """;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Le logo - Les boutons de navigation - Personnalisation visuelle
   */
  public NavigationBarView() {
    logo = new ImageView();

    try {
      Image logoImg =
          new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));

      logo.setImage(logoImg);
      logo.setFitWidth(40);
      logo.setFitHeight(40);

      VBox.setMargin(logo, new Insets(0, 25, 0, 25));
    } catch (Exception e) {
      logger.error("Erreur lors du chargement du logo", e);
    }

    dataButton = createNavigationButton("Data");
    rdfEditorButton = createNavigationButton("RDF Editor");
    validationButton = createNavigationButton("Validation");
    queryButton = createNavigationButton("Query");
    settingsButton = createNavigationButton("Settings");

    getChildren()
        .addAll(logo, dataButton, rdfEditorButton, validationButton, queryButton, settingsButton);
  }

  /**
   * Crée un bouton de navigation personnalisé.
   *
   * @param text Le texte du bouton
   * @return Un bouton de navigation configuré
   */
  private Button createNavigationButton(String text) {
    Button button = new Button(text);
    // button.setStyle(BUTTON_STYLE_NORMAL);

    button.setOnMouseEntered(
        e -> {
          if (!button.getStyle().equals(BUTTON_STYLE_SELECTED)) {
            // button.setStyle(BUTTON_STYLE_HOVER);
          }
        });

    button.setOnMouseExited(
        e -> {
          if (!button.getStyle().equals(BUTTON_STYLE_SELECTED)) {
            // button.setStyle(BUTTON_STYLE_NORMAL);
          }
        });

    button.setPrefWidth(90);
    return button;
  }

  /**
   * Définit le bouton actuellement sélectionné.
   *
   * @param selectedButton Le bouton à marquer comme sélectionné
   */
  public void setButtonSelected(Button selectedButton) {
    // Réinitialiser tous les boutons
    for (Button button :
        new Button[] {dataButton, rdfEditorButton, validationButton, queryButton, settingsButton}) {
      // button.setStyle(BUTTON_STYLE_NORMAL);
    }
    // Définir le style sélectionné pour le bouton actif
    // selectedButton.setStyle(BUTTON_STYLE_SELECTED);
  }

  // Getters
  public Button getDataButton() {
    return dataButton;
  }

  public Button getRdfEditorButton() {
    return rdfEditorButton;
  }

  public Button getValidationButton() {
    return validationButton;
  }

  public Button getQueryButton() {
    return queryButton;
  }

  public Button getSettingsButton() {
    return settingsButton;
  }
}
