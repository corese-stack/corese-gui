package fr.inria.corese.gui.component.notification;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import atlantafx.base.theme.Styles;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Singleton manager for displaying toast-style notifications.
 * 
 * <p>
 * Notifications are displayed in a VBox overlay and automatically disappear
 * after a timeout.
 * Four notification types are supported: info, success, warning, and error.
 * 
 * <p>
 * The Singleton pattern is used here because:
 * <ul>
 * <li>There should be only one notification container for the entire
 * application</li>
 * <li>Notifications must be accessible from any part of the application</li>
 * <li>State (the container reference) needs to be globally managed</li>
 * </ul>
 * 
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * // First, register the notification container in your main view
 * NotificationWidget.getInstance().setContainer(notificationVBox);
 * 
 * // Then, show notifications from anywhere in your application
 * NotificationWidget.getInstance().showSuccess("Operation completed");
 * NotificationWidget.getInstance().showError("An error occurred");
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for global notification management
public class NotificationWidget {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final NotificationWidget INSTANCE = new NotificationWidget();

    private static final String STYLESHEET = "/css/components/notification-widget.css";
    private static final String STYLE_CLASS_TOAST = "notification-toast";
    private static final String STYLE_CLASS_ICON = "notification-icon";
    private static final String STYLE_CLASS_LABEL = "notification-label";

    private static final int ANIMATION_DURATION_MS = 300;
    private static final int DISPLAY_DURATION_SEC = 3;
    private static final int TOAST_SLIDE_OFFSET = 20;
    private static final int TOAST_MIN_WIDTH = 300;
    private static final int TOAST_MAX_WIDTH = 300;

    private VBox container;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    private NotificationWidget() {
        // Private constructor for singleton pattern
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Returns the singleton instance of the notification manager.
     * 
     * @return The NotificationWidget instance.
     */
    public static NotificationWidget getInstance() {
        return INSTANCE;
    }

    /**
     * Registers the container where notifications will be displayed.
     * 
     * <p>
     * This must be called once during application initialization before
     * showing any notifications.
     * 
     * @param container A VBox designated for notifications.
     */
    public void setContainer(VBox container) {
        this.container = container;
    }

    /**
     * Displays an informational notification.
     * 
     * @param message The message to display.
     */
    public void showInfo(String message) {
        show(message, Styles.ACCENT, Feather.INFO);
    }

    /**
     * Displays a success notification.
     * 
     * @param message The message to display.
     */
    public void showSuccess(String message) {
        show(message, Styles.SUCCESS, Feather.CHECK_CIRCLE);
    }

    /**
     * Displays a warning notification.
     * 
     * @param message The message to display.
     */
    public void showWarning(String message) {
        show(message, Styles.WARNING, Feather.ALERT_TRIANGLE);
    }

    /**
     * Displays an error notification.
     * 
     * @param message The message to display.
     */
    public void showError(String message) {
        show(message, Styles.DANGER, Feather.X_CIRCLE);
    }

    // ==============================================================================================
    // Private Methods
    // ==============================================================================================

    /**
     * Creates and displays a notification with the specified styling.
     * 
     * @param message    The message to display.
     * @param styleClass The AtlantaFX style class for coloring.
     * @param icon       The icon to display.
     */
    private void show(String message, String styleClass, Feather icon) {
        if (container == null) {
            return;
        }

        HBox toast = createToast(message, styleClass, icon);
        container.getChildren().add(toast);

        playAnimation(toast);
    }

    /**
     * Creates the visual toast notification component.
     * 
     * @param message    The message to display.
     * @param styleClass The AtlantaFX style class for coloring.
     * @param iconCode   The icon to display.
     * @return The configured HBox toast component.
     */
    private HBox createToast(String message, String styleClass, Feather iconCode) {
    HBox toast = new HBox(10);
    toast.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
    toast.getStyleClass().addAll(Styles.ELEVATED_2, STYLE_CLASS_TOAST);
    if (styleClass != null && !styleClass.isBlank()) {
        toast.getStyleClass().add(styleClass);
    }
    toast.setAlignment(Pos.CENTER_LEFT);
    toast.setMinWidth(TOAST_MIN_WIDTH);
    toast.setMaxWidth(TOAST_MAX_WIDTH);

        FontIcon icon = new FontIcon(iconCode);
        icon.getStyleClass().add(STYLE_CLASS_ICON);
        icon.setIconSize(20);
        if (styleClass != null) {
            icon.getStyleClass().add(styleClass);
        }

        Label label = new Label(message);
        label.getStyleClass().addAll(STYLE_CLASS_LABEL, Styles.TEXT_BOLD);
    label.setWrapText(true);

    toast.getChildren().addAll(icon, label);

    return toast;
  }

    /**
     * Plays the entrance, display, and exit animation for a toast notification.
     * 
     * @param toast The toast component to animate.
     */
    private void playAnimation(HBox toast) {
        // Set initial state
        toast.setOpacity(0);
        toast.setTranslateY(TOAST_SLIDE_OFFSET);

        // Create entrance animation (fade in + slide in)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(ANIMATION_DURATION_MS), toast);
        slideIn.setFromY(TOAST_SLIDE_OFFSET);
        slideIn.setToY(0);

        ParallelTransition entrance = new ParallelTransition(fadeIn, slideIn);

        // Create display pause
        PauseTransition display = new PauseTransition(Duration.seconds(DISPLAY_DURATION_SEC));

        // Create exit animation (fade out)
        FadeTransition fadeOut = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        // Play full animation sequence
        SequentialTransition animation = new SequentialTransition(entrance, display, fadeOut);
        animation.setOnFinished(e -> container.getChildren().remove(toast));
        animation.play();
    }

}
