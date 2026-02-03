package fr.inria.corese.gui.core.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import fr.inria.corese.gui.core.dialog.DialogService.UnsavedChangesResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

/**
 * The standard visual component for application dialogs.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Consistent styling (Title, Content, Actions)</li>
 * <li>Integration with AtlantaFX themes</li>
 * <li>Factory methods for common dialog types</li>
 * </ul>
 */
public class AppDialog extends VBox {

    public AppDialog(String title, Node content, Node... actions) {
        getStyleClass().add(Styles.ELEVATED_1);
        setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 10; -fx-min-width: 400px; -fx-max-width: 600px;");
        setPadding(new Insets(20));
        setSpacing(15);
        setMaxHeight(Region.USE_PREF_SIZE);

        // Header
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(Styles.TITLE_4);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Close button
        IconButtonWidget closeBtn = new IconButtonWidget(
                new ButtonConfig(ButtonIcon.CLOSE_WINDOW, "Close", DialogService.getInstance()::hide));

        HBox header = new HBox(titleLabel, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Content
        if (content instanceof Region) {
            ((Region) content).setMaxWidth(Double.MAX_VALUE);
        }

        getChildren().addAll(header, content);

        // Actions
        if (actions != null && actions.length > 0) {
            HBox actionBox = new HBox(10);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            actionBox.getChildren().addAll(actions);
            getChildren().add(actionBox);
        }
    }

    // ============================================================================================
    // Factories
    // ============================================================================================

    public static AppDialog createError(String title, String message) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add(Styles.ACCENT);
        okBtn.setOnAction(e -> DialogService.getInstance().hide());

        return new AppDialog(title, msgLabel, okBtn);
    }

    public static AppDialog createInfo(String title, String message) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add(Styles.ACCENT);
        okBtn.setOnAction(e -> DialogService.getInstance().hide());

        return new AppDialog(title, msgLabel, okBtn);
    }

    public static AppDialog createUnsavedChanges(String message, Consumer<UnsavedChangesResult> callback) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add(Styles.ACCENT);
        saveBtn.setOnAction(e -> {
            DialogService.getInstance().hide();
            callback.accept(UnsavedChangesResult.SAVE);
        });

        Button dontSaveBtn = new Button("Don't Save");
        dontSaveBtn.setOnAction(e -> {
            DialogService.getInstance().hide();
            callback.accept(UnsavedChangesResult.DONT_SAVE);
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> {
            DialogService.getInstance().hide();
            callback.accept(UnsavedChangesResult.CANCEL);
        });

        return new AppDialog("Unsaved Changes", msgLabel, cancelBtn, dontSaveBtn, saveBtn);
    }
}
