package fr.inria.corese.gui.view;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

public class FloatingButton extends Button {
    private static final String STYLESHEET = "/styles/floating-button.css";

    public FloatingButton(Ikon icon, String tooltipText) {
        getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        getStyleClass().add("floating-button");
        
        FontIcon fontIcon = new FontIcon(icon);
        setGraphic(fontIcon);
        
        if (tooltipText != null && !tooltipText.isBlank()) {
            setTooltip(new Tooltip(tooltipText));
        }
    }
}
