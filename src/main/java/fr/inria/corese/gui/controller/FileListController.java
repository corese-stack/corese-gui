package fr.inria.corese.gui.controller;

import java.io.File;

import fr.inria.corese.gui.model.fileList.FileListModel;
import fr.inria.corese.gui.view.FileListView;
import javafx.stage.FileChooser;

/**
 * Contrôleur de gestion de la liste de fichiers dans une application.
 *
 * <p>Responsabilités principales : - Gestion du modèle de liste de fichiers - Liaison entre le
 * modèle et la vue de la liste de fichiers - Gestion des interactions utilisateur avec la liste de
 * fichiers
 *
 * <p>Fonctionnalités clés : - Initialisation des écouteurs d'événements - Liaison du modèle à la
 * vue - Gestion des actions sur la liste de fichiers (chargement, effacement, rechargement)
 *
 * <p>Le contrôleur gère : - FileListModel pour la gestion des données - FileListView pour
 * l'affichage des fichiers
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class FileListController {
  private final FileListModel model;
  private final FileListView view;

  /**
   * Constructeur du contrôleur de liste de fichiers.
   *
   * <p>Actions réalisées : - Création du modèle de liste de fichiers - Création de la vue de liste
   * de fichiers - Initialisation des écouteurs d'événements - Liaison du modèle à la vue
   */
  public FileListController() {
    this.model = new FileListModel();
    this.view = new FileListView();

    initializeListeners();
    bindModelToView();
  }

  /**
   * Initialise les écouteurs d'événements pour les boutons de la vue.
   *
   * <p>Configure les actions pour : - Le bouton de suppression du graphe - Le bouton de
   * rechargement des fichiers - Le bouton de chargement de fichiers
   */
  private void initializeListeners() {
    view.getClearButton().setOnAction(e -> handleClearGraph());
    view.getReloadButton().setOnAction(e -> handleReloadFiles());
    view.getLoadButton().setOnAction(e -> handleLoadFiles());
  }

  /**
   * Lie le modèle de liste de fichiers à sa vue correspondante.
   *
   * <p>Synchronise les données du modèle avec l'affichage de la liste de fichiers.
   */
  private void bindModelToView() {
    view.getFileList().setItems(model.getFiles());
  }

  /**
   * Gère l'action de suppression du graphe.
   *
   * <p>Efface tous les fichiers du modèle de liste de fichiers.
   */
  private void handleClearGraph() {
    model.clearFiles();
  }

  /**
   * Gère l'action de rechargement des fichiers.
   *
   * <p>Logique à implémenter pour recharger les fichiers existants.
   */
  private void handleReloadFiles() {
    // TODO: Implement reload logic
  }

  /**
   * Gère le chargement de nouveaux fichiers.
   *
   * <p>Workflow : 1. Ouvre un sélecteur de fichiers 2. Filtre pour les fichiers TTL 3. Ajoute le
   * fichier sélectionné à la liste
   */
  private void handleLoadFiles() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TTL files", "*.ttl"));

    File file = fileChooser.showOpenDialog(view.getScene().getWindow());
    if (file != null) {
      model.addFile(file);
    }
  }

  /**
   * Récupère la vue de la liste de fichiers.
   *
   * @return La FileListView associée à ce contrôleur
   */
  public FileListView getView() {
    return view;
  }
}
