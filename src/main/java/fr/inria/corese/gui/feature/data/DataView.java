package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.core.view.AbstractView;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

/**
 * Simple DataView for loading files.
 * Replaces the previous complex implementation.
 */
public class DataView extends AbstractView {

    private final Button loadButton;

    public DataView() {
        super(new StackPane(), null);
        
        loadButton = new Button("Load Data File");
        loadButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        
        StackPane root = (StackPane) getRoot();
        root.getChildren().add(loadButton);
        StackPane.setAlignment(loadButton, Pos.CENTER);
    }

    public Button getLoadButton() {
        return loadButton;
    }
}