package fr.inria.corese.demo.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Vue affichant les statistiques globales d'un projet sémantique.
 *
 * <p>Cette classe présente différentes mesures quantitatives sur : - Les éléments sémantiques - Les
 * triplets - Les graphes - Les règles chargées
 *
 * <p>Caractéristiques principales : - Disposition verticale (VBox) - Mise à jour dynamique des
 * statistiques - Étiquettes de présentation des données
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class ProjectStatisticsView extends VBox {
  private Label semanticElementsLabel;
  private Label tripletLabel;
  private Label graphLabel;
  private Label rulesLoadedLabel;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Les étiquettes de statistiques - La disposition des composants
   */
  public ProjectStatisticsView() {
    initializeComponents();
    setupLayout();
  }

  /**
   * Initialise les composants de statistiques.
   *
   * <p>Crée les étiquettes pour : - Éléments sémantiques - Triplets - Graphes - Règles chargées
   */
  private void initializeComponents() {
    semanticElementsLabel = new Label("Number of semantic elements loaded: 0");
    tripletLabel = new Label("Number of triplet: 0");
    graphLabel = new Label("Number of graph: 0");
    rulesLoadedLabel = new Label("Number of rules loaded: 0");
  }

  /**
   * Configure la disposition des étiquettes.
   *
   * <p>Ajoute toutes les étiquettes de statistiques au conteneur.
   */
  private void setupLayout() {
    getChildren().addAll(semanticElementsLabel, tripletLabel, graphLabel, rulesLoadedLabel);
  }
}
