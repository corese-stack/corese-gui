package fr.inria.corese.gui.component.notification;

import atlantafx.base.theme.Styles;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Singleton manager for displaying toast notifications.
 * <p>
 * Notifications are displayed in a VBox overlay and automatically disappear after a timeout.
 */
public class NotificationManager {

    private static NotificationManager instance;
    private VBox container;

    private NotificationManager() {
        // Private constructor
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    /**
     * Registers the container where notifications will be added.
     * 
     * @param container A VBox designated for notifications.
     */
    public void setContainer(VBox container) {
        this.container = container;
    }

    public void showInfo(String message) {
        show(message, Styles.ACCENT, Feather.INFO);
    }

    public void showSuccess(String message) {
        show(message, Styles.SUCCESS, Feather.CHECK_CIRCLE);
    }

    public void showWarning(String message) {
        show(message, Styles.WARNING, Feather.ALERT_TRIANGLE);
    }

    public void showError(String message) {
        show(message, Styles.DANGER, Feather.X_CIRCLE);
    }

    private void show(String message, String styleClass, Feather icon) {
        if (container == null) return;

        HBox toast = createToast(message, styleClass, icon);
        container.getChildren().add(toast); // Add to bottom

        // Animation: Slide In + Fade In -> Wait -> Fade Out -> Remove
        
        // Initial state
        toast.setOpacity(0);
        toast.setTranslateY(20);

        // In Animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
        slideIn.setFromY(20);
        slideIn.setToY(0);
        
        ParallelTransition appear = new ParallelTransition(fadeIn, slideIn);

        // Wait
        PauseTransition stay = new PauseTransition(Duration.seconds(3));

        // Out Animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition animation = new SequentialTransition(appear, stay, fadeOut);
        animation.setOnFinished(e -> container.getChildren().remove(toast));
        animation.play();
    }

    private HBox createToast(String message, String styleClass, Feather iconCode) {
        HBox toast = new HBox(10);
        toast.getStyleClass().addAll(Styles.ELEVATED_2, "toast-notification");
        toast.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-border-color: -color-border-subtle; -fx-border-width: 1px;");
        toast.setPadding(new Insets(12, 16, 12, 16));
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMinWidth(300);
        toast.setMaxWidth(300);

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        if (styleClass != null) {
            icon.getStyleClass().add(styleClass);
        }

        Label label = new Label(message);
        label.setWrapText(true);
        label.getStyleClass().add(Styles.TEXT_BOLD);

        toast.getChildren().addAll(icon, label);
        
        // Add left border strip for color
        if (styleClass != null) {
             toast.setStyle(toast.getStyle() + " -fx-border-color: transparent transparent transparent -color-" + getColorName(styleClass) + "-fg; -fx-border-width: 0 0 0 4px;");
        }
        
        return toast;
    }
    
    // Helper to map AtlantaFX style class to CSS color variable name part
    private String getColorName(String styleClass) {
        if (Styles.ACCENT.equals(styleClass)) return "accent";
        if (Styles.SUCCESS.equals(styleClass)) return "success";
        if (Styles.WARNING.equals(styleClass)) return "warning";
        if (Styles.DANGER.equals(styleClass)) return "danger";
        return "accent";
    }
}
