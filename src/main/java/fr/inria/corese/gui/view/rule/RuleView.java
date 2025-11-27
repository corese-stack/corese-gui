package fr.inria.corese.gui.view.rule;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

/**
 * Vue représentant une liste de règles dans l'interface utilisateur.
 *
 * <p>Cette classe gère l'affichage et l'organisation des règles, incluant les règles prédéfinies et
 * personnalisées.
 *
 * <p>Caractéristiques principales : - Conteneur vertical (VBox) pour les éléments de règles -
 * Gestion d'une liste observable d'éléments de règles - Initialisation des règles prédéfinies
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class RuleView extends VBox {
  private final ObservableList<RuleItem> ruleItems;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Une liste observable des éléments de règles - Un map des règles prédéfinies -
   * Ajout des règles prédéfinies
   */
  public RuleView() {
    ruleItems = FXCollections.observableArrayList();
  }
}
