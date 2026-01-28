package fr.inria.corese.gui.core.factory.popup;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class RuleInfoPopup extends BasePopup {
  private final Label nameLabel;
  private final Label typeLabel;
  private final Label moreInfoLabel;
  private final Label modifiedLabel;
  private final Label sizeLabel;
  private final Label loadingTimeLabel;
  private final Label fileSizeLabel;

  public RuleInfoPopup() {
    // Configurer le titre et le style
    setTitle("About Rule");
    getDialogPane().setPrefWidth(400);

    // Créer les labels
    nameLabel = new Label();
    typeLabel = new Label();
    moreInfoLabel = new Label();
    modifiedLabel = new Label();
    sizeLabel = new Label();
    loadingTimeLabel = new Label();
    fileSizeLabel = new Label();

    setupUI();
  }

  private void setupUI() {
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(5);
    grid.setPadding(new Insets(20, 20, 10, 20));

    // Ajouter les labels avec leurs titres
    addLabelRow(grid, "Name: ", nameLabel, 0);
    addLabelRow(grid, "Type: ", typeLabel, 1);
    addLabelRow(grid, "More information: ", moreInfoLabel, 2);

    // Créer une ligne pour les dates et tailles
    GridPane bottomGrid = new GridPane();
    bottomGrid.setHgap(20);
    bottomGrid.add(new Label("Modified: "), 0, 0);
    bottomGrid.add(modifiedLabel, 1, 0);
    bottomGrid.add(new Label("File size: "), 2, 0);
    bottomGrid.add(fileSizeLabel, 3, 0);

    GridPane bottomGrid2 = new GridPane();
    bottomGrid2.setHgap(20);
    bottomGrid2.add(new Label("Size: "), 0, 0);
    bottomGrid2.add(sizeLabel, 1, 0);
    bottomGrid2.add(new Label("Loading time: "), 2, 0);
    bottomGrid2.add(loadingTimeLabel, 3, 0);

    grid.add(bottomGrid, 0, 3, 2, 1);
    grid.add(bottomGrid2, 0, 4, 2, 1);

    // Ajouter le bouton OK
    ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    getDialogPane().getButtonTypes().add(okButton);

    getDialogPane().setContent(grid);
  }

  private void addLabelRow(GridPane grid, String labelText, Label valueLabel, int row) {
    Label label = new Label(labelText);
    grid.add(label, 0, row);
    grid.add(valueLabel, 1, row);
  }
}
