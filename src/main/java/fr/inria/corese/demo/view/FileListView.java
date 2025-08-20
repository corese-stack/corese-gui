package fr.inria.corese.demo.view;

import fr.inria.corese.demo.model.fileList.FileItem;
import fr.inria.corese.demo.model.fileList.FileListModel;
import fr.inria.corese.demo.factory.popup.IPopup;
import fr.inria.corese.demo.factory.popup.PopupFactory;
import fr.inria.corese.demo.factory.popup.WarningPopup;
import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.view.icon.IconButtonView;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Vue de gestion de liste de fichiers dans une application sémantique.
 *
 * Responsabilités principales :
 * - Affichage de la liste des fichiers chargés
 * - Gestion des interactions utilisateur avec les fichiers
 * - Gestion des états vides et peuplés
 *
 * Caractéristiques principales :
 * - Liste personnalisable des fichiers
 * - Boutons d'actions (charger, recharger, effacer)
 * - Gestion des popups de confirmation
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class FileListView extends VBox {
    private FileListModel model;
    private Consumer<FileItem> onRemoveAction;
    private EmptyStateView emptyStateView;
    private Button clearButton;
    private Button reloadButton;
    private Button loadButton;

    @FXML
    private ListView<FileItem> fileList;
    @FXML
    private FlowPane buttonContainer;

    /**
     * Constructeur par défaut.
     *
     * Initialise :
     * - Chargement du FXML
     * - Configuration des boutons d'icônes
     */
    public FileListView() {
        loadFxml();
        setupIconButtons();
    }

    /**
     * Charge la vue à partir d'un fichier FXML.
     *
     * Gère :
     * - Le chargement du fichier FXML
     * - La configuration de la vue de liste
     * - La configuration de l'état vide
     */
    private void loadFxml() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fr/inria/corese/demo/fileList-view.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            setupListView();
            setupEmptyState();

        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger FileListView.fxml", e);
        }
    }

    /**
     * Configure les boutons d'icônes pour les actions de fichiers.
     *
     * Crée et positionne les boutons :
     * - Charger
     * - Recharger
     * - Effacer
     */
    private void setupIconButtons() {
        // Créer les boutons avec IconButtonView
        loadButton = new IconButtonView(IconButtonType.IMPORT);
        reloadButton = new IconButtonView(IconButtonType.RELOAD);
        clearButton = new IconButtonView(IconButtonType.DELETE);

        // Ajouter les boutons au conteneur vertical dans l'ordre souhaité
        buttonContainer.getChildren().clear();
        buttonContainer.getChildren().addAll(loadButton, reloadButton, clearButton);

        // S'assurer que les boutons sont bien alignés en haut à gauche
        buttonContainer.setAlignment(Pos.TOP_LEFT);
    }


    /**
     * Configure la vue de liste.
     *
     * Définit :
     * - L'usine de cellules personnalisées
     * - Les styles de la liste
     */
    private void setupListView() {
        if (fileList != null) {
            fileList.setCellFactory(lv -> new FileListCell(onRemoveAction));
            fileList.getStyleClass().add("always-visible-border");
        }
    }

    /**
     * Définit le modèle de liste de fichiers.
     *
     * @param model Le modèle de liste de fichiers à associer
     */
    public void setModel(FileListModel model) {
        this.model = model;
        if (model != null && fileList != null) {
            fileList.setItems(model.getFiles());
            emptyStateView.visibleProperty().bind(Bindings.isEmpty(model.getFiles()));
        }
    }

    public void setOnRemoveAction(Consumer<FileItem> onRemoveAction) {
        this.onRemoveAction = onRemoveAction;
        if (fileList != null) {
            fileList.setCellFactory(lv -> new FileListCell(this.onRemoveAction));
        }
    }

    /**
     * Retourne l'espace quand il n'y a pas de fichiers chargés.
     */
    private void setupEmptyState() {
        // Create empty state view
        Label emptyStateTitle = new Label("No files loaded");
        Label emptyStateMessage = new Label("Open a folder or load a TTL file\nto visualize semantic graphs");
        String emptyStateImage = "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z";
        emptyStateView = new EmptyStateView(emptyStateTitle, emptyStateMessage, emptyStateImage);

        // Create a StackPane to hold both the ListView and empty state
        StackPane contentContainer = new StackPane();

        // Get the current fileList from the FXML
        if (fileList != null) {
            // Remove the ListView from its current parent (maintenant un HBox)
            HBox parent = (HBox)fileList.getParent();
            int index = parent.getChildren().indexOf(fileList);
            parent.getChildren().remove(fileList);

            // Add both to the stack pane, with the ListView ALWAYS visible
            contentContainer.getChildren().addAll(fileList, emptyStateView);

            // Add the stack pane to the parent at the same position
            parent.getChildren().add(index, contentContainer);
            HBox.setHgrow(contentContainer, Priority.ALWAYS);

            // Assurez-vous que le conteneur a les mêmes styles que fileList pour l'encadré
            contentContainer.getStyleClass().add("list-view-container");
        } else {
            // Si fileList est toujours null
            getChildren().add(0, contentContainer);
            VBox.setVgrow(contentContainer, Priority.ALWAYS);
        }

        // Initially hide the empty state until model is set
        emptyStateView.setVisible(false);
    }

    public Button getClearButton() {
        return clearButton;
    }

    public Button getReloadButton() {
        return reloadButton;
    }

    public Button getLoadButton() {
        return loadButton;
    }

    public ListView<FileItem> getFileList() {
        return fileList;
    }

    /**
     * Classe interne représentant une cellule personnalisée de liste de fichiers.
     */
    private static class FileListCell extends ListCell<FileItem> {
        private final Consumer<FileItem> onRemove;

        public FileListCell(Consumer<FileItem> onRemove) {
            this.onRemove = onRemove;
        }

        /**
         * Met à jour l'affichage de la cellule.
         *
         * @param item
         * @param empty
         */
        @Override
        protected void updateItem(FileItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox cell = new HBox();
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setSpacing(5);

                Label nameLabel = new Label(item.getName());
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                IconButtonView deleteButton = new IconButtonView(IconButtonType.DELETE);
                deleteButton.setOnAction(e -> {
                    if (onRemove != null) {
                        onRemove.accept(item);
                    }
                });

                if (item.isLoading()) {
                    ProgressIndicator progress = new ProgressIndicator();
                    progress.setMaxSize(16, 16);
                    cell.getChildren().addAll(nameLabel, spacer, progress);
                } else {
                    cell.getChildren().addAll(nameLabel, spacer, deleteButton);
                }

                setGraphic(cell);
            }
        }
    }


}