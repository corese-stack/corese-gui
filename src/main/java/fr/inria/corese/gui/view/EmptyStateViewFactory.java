package fr.inria.corese.gui.view;

import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

public class EmptyStateViewFactory {

  /** Creates an empty state view specifically for the Query context. */
  public static Node createQueryEmptyStateView(
      Runnable onNewQuery, Runnable onLoadQuery, Consumer<Stage> onTemplateSelection, Stage stage) {

    FontIcon icon = new FontIcon(MaterialDesignF.FILE_DOCUMENT_OUTLINE);
    Label message = new Label("No queries open.\nCreate a new query or load an existing one.");

    Button newTabButton = new Button("New Query");
    newTabButton.setTooltip(new Tooltip("CTRL + N"));
    newTabButton.setOnAction(e -> onNewQuery.run());

    Button loadQueryButton = new Button("Load Query");
    loadQueryButton.setTooltip(new Tooltip("CTRL + O"));
    loadQueryButton.setOnAction(e -> onLoadQuery.run());

    Button templateSelectionButton = new Button("Templates");
    templateSelectionButton.setTooltip(new Tooltip("CTRL + T"));
    templateSelectionButton.setOnAction(e -> onTemplateSelection.accept(stage));

    HBox buttonBox = new HBox(10, newTabButton, loadQueryButton, templateSelectionButton);
    buttonBox.setAlignment(Pos.CENTER);

    return buildEmptyStateLayout(icon, message, buttonBox);
  }

  /**
   * Creates an empty state view specifically for the SHACL Validation context.
   *
   * @param onNewShapesFile Action to run when "New Shapes File" is clicked.
   * @param onLoadShapesFile Action to run when "Load Shapes File" is clicked.
   * @return A Node representing the empty state view.
   */
  public static Node createValidationEmptyStateView(
      Runnable onNewShapesFile, Runnable onLoadShapesFile) {

    FontIcon icon = new FontIcon(MaterialDesignS.SHIELD_CHECK_OUTLINE);
    Label message =
        new Label("No shapes files open.\nCreate a new shapes file or load an existing one.");

    Button newButton = new Button("New Shapes File");
    newButton.setTooltip(new Tooltip("CTRL + N"));
    newButton.setOnAction(e -> onNewShapesFile.run());

    Button loadButton = new Button("Load Shapes File");
    loadButton.setTooltip(new Tooltip("CTRL + O"));
    loadButton.setOnAction(e -> onLoadShapesFile.run());

    HBox buttonBox = new HBox(10, newButton, loadButton);
    buttonBox.setAlignment(Pos.CENTER);

    return buildEmptyStateLayout(icon, message, buttonBox);
  }

  private static Node buildEmptyStateLayout(FontIcon icon, Label message, HBox buttonBox) {
    VBox emptyBox = new VBox(20);
    emptyBox.setAlignment(Pos.CENTER);

    icon.setIconSize(80);
    icon.getStyleClass().add("empty-state-icon");

    message.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");
    message.getStyleClass().add("empty-state-message");

    buttonBox.getStyleClass().add("empty-state-buttons");

    emptyBox.getChildren().addAll(icon, message, buttonBox);
    emptyBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    emptyBox.getStyleClass().add("empty-state-view");

    return emptyBox;
  }
}
