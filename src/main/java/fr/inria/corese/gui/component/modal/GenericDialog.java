package fr.inria.corese.gui.component.modal;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A generic, clean dialog layout for use with {@link ModalManager}.
 * <p>
 * Features:
 * <ul>
 *   <li>Title bar with close button</li>
 *   <li>Content area</li>
 *   <li>Action button bar (optional)</li>
 * </ul>
 */
public class GenericDialog extends VBox {

    public GenericDialog(String title, Node content, Node... actions) {
        getStyleClass().add(Styles.ELEVATED_1);
        setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 10; -fx-min-width: 400px; -fx-max-width: 600px;");
        setPadding(new Insets(20));
        setSpacing(15);
        setMaxHeight(Region.USE_PREF_SIZE); // Prevent vertical stretching

        // Header
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(Styles.TITLE_4);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Close button: Icon only
        IconButtonWidget closeBtn = new IconButtonWidget(new ButtonConfig(ButtonIcon.CLEAR, "Close", ModalManager.getInstance()::hide));
        // We can force it to be icon-only by ensuring the widget handles null text or by using a style
        // If IconButtonWidget constructor takes text, we might need to modify it or just pass null.
        // Assuming IconButtonWidget handles text+icon. If we want just icon, we might need to adjust ButtonConfig or widget.
        // Let's pass null for text if supported, or empty string.
        // ButtonConfig(icon, tooltip, action) -> wait, the constructor used previously was (icon, text, action)? 
        // Let's check ButtonConfig.
        
        HBox header = new HBox(titleLabel, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Content
        // Ensure content expands
        if (content instanceof Region) {
            ((Region) content).setMaxWidth(Double.MAX_VALUE);
        }

        // Actions
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        if (actions != null) {
            actionBox.getChildren().addAll(actions);
        }

        getChildren().addAll(header, content);
        
        if (actions != null && actions.length > 0) {
            getChildren().add(actionBox);
        }
    }

    public static GenericDialog createError(String title, String message) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add(Styles.ACCENT);
        okBtn.setOnAction(e -> ModalManager.getInstance().hide());
        
        return new GenericDialog(title, msgLabel, okBtn);
    }

    public static GenericDialog createConfirm(String title, String message, Runnable onConfirm) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> ModalManager.getInstance().hide());
        
        Button confirmBtn = new Button("Confirm");
        confirmBtn.getStyleClass().add(Styles.DANGER);
        confirmBtn.setOnAction(e -> {
            onConfirm.run();
            ModalManager.getInstance().hide();
        });
        
        return new GenericDialog(title, msgLabel, cancelBtn, confirmBtn);
    }
}
