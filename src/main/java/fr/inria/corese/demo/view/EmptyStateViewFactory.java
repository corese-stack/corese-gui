package fr.inria.corese.demo.view;

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

import java.util.function.Consumer;

public class EmptyStateViewFactory {

    public static Node createEmptyStateView(
            Runnable onNewQuery,
            Runnable onLoadQuery,
            Consumer<Stage> onTemplateSelection,
            Stage stage) {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon(MaterialDesignF.FILE_DOCUMENT_OUTLINE);
        icon.setIconSize(80);

        Label message = new Label("No queries open.\nCreate a new query or load an existing one.");
        message.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");

        Button newTabButtonEmptyState = new Button("New Query");
        newTabButtonEmptyState.setTooltip(new Tooltip("CTRL + N"));

        Button loadQueryButton = new Button("Load Query");
        loadQueryButton.setTooltip(new Tooltip("CTRL + O"));

        Button templateSelectionButton = new Button("Templates");
        templateSelectionButton.setTooltip(new Tooltip("CTRL + T"));

        newTabButtonEmptyState.setOnAction(e -> onNewQuery.run());
        loadQueryButton.setOnAction(e -> onLoadQuery.run());
        templateSelectionButton.setOnAction(e -> onTemplateSelection.accept(stage));

        HBox buttonBox = new HBox(10, newTabButtonEmptyState, loadQueryButton, templateSelectionButton);
        buttonBox.setAlignment(Pos.CENTER);

        emptyBox.getChildren().addAll(icon, message, buttonBox);
        emptyBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        return emptyBox;
    }
}