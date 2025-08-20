package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.model.fileList.FileListModel;
import fr.inria.corese.demo.view.FileListView;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * Contrôleur de gestion de la liste de fichiers dans une application.
 *
 * Responsabilités principales :
 * - Gestion du modèle de liste de fichiers
 * - Liaison entre le modèle et la vue de la liste de fichiers
 * - Gestion des interactions utilisateur avec la liste de fichiers
 *
 * Fonctionnalités clés :
 * - Initialisation des écouteurs d'événements
 * - Liaison du modèle à la vue
 * - Gestion des actions sur la liste de fichiers (chargement, effacement, rechargement)
 *
 * Le contrôleur gère :
 * - FileListModel pour la gestion des données
 * - FileListView pour l'affichage des fichiers
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
     * Actions réalisées :
     * - Création du modèle de liste de fichiers
     * - Création de la vue de liste de fichiers
     * - Initialisation des écouteurs d'événements
     * - Liaison du modèle à la vue
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
     * Configure les actions pour :
     * - Le bouton de suppression du graphe
     * - Le bouton de rechargement des fichiers
     * - Le bouton de chargement de fichiers
     */
    private void initializeListeners() {
        view.getClearButton().setOnAction(e -> handleClearGraph());
        view.getReloadButton().setOnAction(e -> handleReloadFiles());
        view.getLoadButton().setOnAction(e -> handleLoadFiles());
    }

    /**
     * Lie le modèle de liste de fichiers à sa vue correspondante.
     *
     * Synchronise les données du modèle avec l'affichage de la liste de fichiers.
     */
    private void bindModelToView() {
        view.getFileList().setItems(model.getFiles());
    }

    /**
     * Gère l'action de suppression du graphe.
     *
     * Efface tous les fichiers du modèle de liste de fichiers.
     */
    private void handleClearGraph() {
        model.clearFiles();
    }

    /**
     * Gère l'action de rechargement des fichiers.
     *
     * Logique à implémenter pour recharger les fichiers existants.
     */
    private void handleReloadFiles() {
        // TODO: Implement reload logic
    }

    /**
     * Gère le chargement de nouveaux fichiers.
     *
     * Workflow :
     * 1. Ouvre un sélecteur de fichiers
     * 2. Filtre pour les fichiers TTL
     * 3. Ajoute le fichier sélectionné à la liste
     */
    private void handleLoadFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("TTL files", "*.ttl")
        );

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