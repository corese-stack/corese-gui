package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
public class DataView extends AbstractView {
  private TopBar topBar;
  private FileListView fileListView;
  private ProjectStatisticsView projectStatisticsView;
  private RuleView ruleView;
  private HBox configActionBox;

  /**
   * Constructeur par défaut.
   *
   * <p>Initialise : - Les composants de l'interface - La disposition des éléments
   */
  public DataView() {
    super(new BorderPane(), null);
    initializeComponents();
    setupLayout();
  }

  /**
   * Initialise les composants de l'interface.
   */
  private void initializeComponents() {
    topBar = new TopBar();
    fileListView = new FileListView();
    projectStatisticsView = new ProjectStatisticsView();
    ruleView = new RuleView();
    configActionBox = new HBox(10);
  }

  /**
   * Configure la disposition des éléments de l'interface.
   */
  private void setupLayout() {
    BorderPane root = (BorderPane) getRoot();
    root.setTop(topBar);

    // Left Panel: Files and Stats
    VBox leftPanel = new VBox(5);
    leftPanel.setPadding(new Insets(4));
    leftPanel.getStyleClass().add("section-container");
    HBox.setHgrow(leftPanel, Priority.ALWAYS);

    VBox.setVgrow(fileListView, Priority.ALWAYS);
    fileListView.getStyleClass().add("file-container");

    projectStatisticsView.setPadding(new Insets(10));
    projectStatisticsView.getStyleClass().add("stats-container");

    leftPanel.getChildren().addAll(fileListView, projectStatisticsView);

    // Right Panel: Rules
    VBox rightPanel = new VBox(10);
    rightPanel.setPadding(new Insets(4));
    rightPanel.setPrefWidth(300);
    rightPanel.setMaxWidth(400);
    rightPanel.getStyleClass().add("section-container");

    VBox.setVgrow(ruleView, Priority.ALWAYS);
    
    configActionBox.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);

    rightPanel.getChildren().addAll(ruleView, configActionBox);

    // Center Content
    HBox centerContent = new HBox(0);
    centerContent.getChildren().addAll(leftPanel, rightPanel);

    root.setCenter(centerContent);
    
  }

  public TopBar getTopBar() {
    return topBar;
  }

  public FileListView getFileListView() {
    return fileListView;
  }

  public ProjectStatisticsView getProjectStatisticsView() {
    return projectStatisticsView;
  }

  public RuleView getRuleView() {
    return ruleView;
  }

  public HBox getConfigActionBox() {
    return configActionBox;
  }
}
