package fr.inria.corese.gui.feature.filelist;

import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.core.enums.ButtonIcon;







import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

/**
 * Vue de gestion de liste de fichiers dans une application sémantique.
 *
 * <p>Responsabilités principales : - Affichage de la liste des fichiers chargés - Gestion des
 * interactions utilisateur avec les fichiers - Gestion des états vides et peuplés
 *
 * <p>Caractéristiques principales : - Liste personnalisable des fichiers - Boutons d'actions
 * (charger, recharger, effacer) - Gestion des popups de confirmation
 *
 * @author Clervie Causer
 * @version 1.0
 * @since 2025
 */
public class FileListView extends VBox {
  private Consumer<FileItem> onRemoveAction;
  private EmptyStateWidget emptyStateWidget;
  private Button clearButton;
  private Button reloadButton;
  private Button loadButton;

  private ListView<FileItem> fileList;
  private FlowPane buttonContainer;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Chargement du FXML - Configuration des boutons d'icônes
   */
  public FileListView() {
    initializeLayout();
    setupIconButtons();
  }

  /**
   * Charge la vue.
   */
  private void initializeLayout() {
    fileList = new ListView<>();
    buttonContainer = new FlowPane();
    
    setupListView();
    setupEmptyState();
  }

  /**
   * Configure les boutons d'icônes pour les actions de fichiers.
   *
   * <p>Crée et positionne les boutons : - Charger - Recharger - Effacer
   */
  private void setupIconButtons() {
    // Créer les boutons avec IconButtonWidget et ButtonConfig pour les tooltips
    loadButton = new IconButtonWidget(new ButtonConfig(ButtonIcon.IMPORT, "Import files"));
    reloadButton = new IconButtonWidget(new ButtonConfig(ButtonIcon.RELOAD, "Reload all files"));
    clearButton = new IconButtonWidget(new ButtonConfig(ButtonIcon.DELETE, "Clear all files"));

    // Ajouter les boutons au conteneur vertical dans l'ordre souhaité
    buttonContainer.setOrientation(javafx.geometry.Orientation.VERTICAL);
    buttonContainer.setVgap(5);
    buttonContainer.setAlignment(Pos.TOP_LEFT);
    buttonContainer.setPrefWidth(40);
    buttonContainer.setPadding(new Insets(5, 5, 0, 0));
    
    buttonContainer.getChildren().clear();
    buttonContainer.getChildren().addAll(loadButton, reloadButton, clearButton);
  }

  /**
   * Configure la vue de liste.
   *
   * <p>Définit : - L'usine de cellules personnalisées - Les styles de la liste
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
    if (model != null && fileList != null) {
      fileList.setItems(model.getFiles());
      emptyStateWidget.visibleProperty().bind(Bindings.isEmpty(model.getFiles()));
    }
  }

  public void setOnRemoveAction(Consumer<FileItem> onRemoveAction) {
    this.onRemoveAction = onRemoveAction;
    if (fileList != null) {
      fileList.setCellFactory(lv -> new FileListCell(this.onRemoveAction));
    }
  }

  /** Retourne l'espace quand il n'y a pas de fichiers chargés. */
  private void setupEmptyState() {
    // Create empty state view
    emptyStateWidget =
        new EmptyStateWidget(
            MaterialDesignF.FILE_DOCUMENT,
            "No files loaded",
            "Open a folder or load a TTL file to visualize semantic graphs");

    // Create a StackPane to hold both the ListView and empty state
    StackPane contentContainer = new StackPane();

    // Add both to the stack pane, with the ListView ALWAYS visible
    contentContainer.getChildren().addAll(fileList, emptyStateWidget);
    HBox.setHgrow(contentContainer, Priority.ALWAYS);

    // Assurez-vous que le conteneur a les mêmes styles que fileList pour l'encadré
    contentContainer.getStyleClass().add("list-view-container");

    HBox mainContainer = new HBox();
    VBox.setVgrow(mainContainer, Priority.ALWAYS);
    mainContainer.getChildren().addAll(buttonContainer, contentContainer);
    
    getChildren().add(mainContainer);

    // Initially hide the empty state until model is set
    emptyStateWidget.setVisible(false);
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

  /** Classe interne représentant une cellule personnalisée de liste de fichiers. */
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

        // Use smart constructor
        IconButtonWidget deleteButton =
            new IconButtonWidget(new ButtonConfig(ButtonIcon.DELETE, "Remove file"));
        deleteButton.setOnAction(
            e -> {
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