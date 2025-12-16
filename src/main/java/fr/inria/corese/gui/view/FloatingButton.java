package fr.inria.corese.gui.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

public class FloatingButton extends Button {
    private static final String STYLESHEET = "/styles/floating-button.css";
    private final FontIcon fontIcon;
    private final ProgressIndicator progressIndicator;
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    public FloatingButton(Ikon icon, String tooltipText) {
        getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        getStyleClass().add("floating-button");
        
        this.fontIcon = new FontIcon(icon);
        this.progressIndicator = new ProgressIndicator();
        this.progressIndicator.setMaxSize(20, 20);
        
        setGraphic(fontIcon);
        
        if (tooltipText != null && !tooltipText.isBlank()) {
            setTooltip(new Tooltip(tooltipText));
        }

        loading.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                setGraphic(progressIndicator);
            } else {
                setGraphic(fontIcon);
            }
        });
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    public boolean isLoading() {
        return loading.get();
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }
}
