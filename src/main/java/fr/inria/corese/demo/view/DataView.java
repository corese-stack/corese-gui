package fr.inria.corese.demo.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Vue principale pour la gestion des données dans l'application.
 *
 * <p>Cette classe organise l'interface utilisateur pour : - La gestion de projet - La liste des
 * fichiers - Les statistiques du projet
 *
 * <p>Caractéristiques principales : - Disposition basée sur BorderPane - Barre d'outils supérieure
 * - Zone de logs à droite - Contenu central avec liste de fichiers et statistiques
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class DataView extends BorderPane {
  private Button openProjectButton;
  private Button saveAsButton;
  private Button showLogsButton;
  private FileListView fileListView;
  private ProjectStatisticsView projectStatisticsView;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Les composants de l'interface - La disposition des éléments
   */
  public DataView() {
    initializeComponents();
    setupLayout();
  }

  /**
   * Initialise les composants de l'interface.
   *
   * <p>Crée : - Boutons d'ouverture et de sauvegarde de projet - Bouton d'affichage des logs - Vue
   * de liste de fichiers - Vue des statistiques de projet
   */
  private void initializeComponents() {
    openProjectButton = new Button("Open project");
    saveAsButton = new Button("Save as");
    showLogsButton = new Button("Show logs");

    fileListView = new FileListView();
    projectStatisticsView = new ProjectStatisticsView();
  }

  /**
   * Configure la disposition des éléments de l'interface.
   *
   * <p>Organise : - Barre d'outils en haut - Zone de logs à droite - Contenu central avec liste de
   * fichiers et statistiques
   */
  private void setupLayout() {
    // Top toolbar
    HBox toolbar = new HBox(10);
    toolbar.setPadding(new Insets(10));
    toolbar.getChildren().addAll(openProjectButton, saveAsButton);

    // Right area for logs button
    VBox rightArea = new VBox(showLogsButton);
    rightArea.setPadding(new Insets(10));

    // Center content with files and rules
    BorderPane centerContent = new BorderPane();
    centerContent.setLeft(fileListView);
    centerContent.setBottom(projectStatisticsView);

    setTop(toolbar);
    setRight(rightArea);
    setCenter(centerContent);
  }
}
