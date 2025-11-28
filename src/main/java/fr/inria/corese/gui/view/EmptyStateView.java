package fr.inria.corese.gui.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * View representing an empty or initial state in the UI.
 *
 * <p>Displays a centered icon, message, and optional action buttons.
 */
public class EmptyStateView extends VBox {

    private final FontIcon iconView;
    private final Label messageLabel;
    private final HBox buttonBox;

    public EmptyStateView(Ikon icon, String message, Node... buttons) {
        this.iconView = new FontIcon(icon);
        this.messageLabel = new Label(message);
        this.buttonBox = new HBox(10, buttons);

        initialize();
    }

    private void initialize() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getStyleClass().add("empty-state-view");

        iconView.setIconSize(80);
        iconView.getStyleClass().add("empty-state-icon");

        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");
        messageLabel.getStyleClass().add("empty-state-message");

        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getStyleClass().add("empty-state-buttons");

        getChildren().addAll(iconView, messageLabel, buttonBox);
    }
}
