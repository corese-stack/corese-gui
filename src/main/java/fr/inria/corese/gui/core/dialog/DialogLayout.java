package fr.inria.corese.gui.core.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.ModalService.UnsavedChangesResult;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

/**
 * Standard dialog component for the application.
 *
 * <p>
 * Provides a consistent visual style for all dialogs with title, content area,
 * and action buttons. Integrates with AtlantaFX theme system for automatic styling.
 *
 * <p>
 * Styling is defined in {@code app-dialog.css} for maintainability and consistency.
 *
 * <p>
 * Factory methods are provided for common dialog types:
 * <ul>
 * <li>{@link #createError(String, String)} - Error notification dialogs</li>
 * <li>{@link #createInfo(String, String)} - Information dialogs</li>
 * <li>{@link #createUnsavedChanges(String, Consumer)} - Unsaved changes confirmation</li>
 * </ul>
 */
public class DialogLayout extends VBox {

    private static final String STYLESHEET = DialogLayout.class.getResource("/css/components/app-dialog.css").toExternalForm();

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    /**
     * Creates a new dialog with the specified components.
     *
     * @param title   The dialog title.
     * @param content The main content area.
     * @param actions Optional action buttons to display at the bottom.
     */
    public DialogLayout(String title, Node content, Node... actions) {
        getStyleClass().addAll(Styles.ELEVATED_1, "app-dialog");
        getStylesheets().add(STYLESHEET);
        setMaxHeight(Region.USE_PREF_SIZE);

        // Header with title and close button
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(Styles.TITLE_4);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        IconButtonWidget closeBtn = new IconButtonWidget(
                ButtonFactory.custom(ButtonIcon.CLOSE_WINDOW, "Close", ModalService.getInstance()::hide));

        HBox header = new HBox(titleLabel, spacer, closeBtn);
        header.getStyleClass().add("dialog-header");

        // Content area
        if (content instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        if (content instanceof Label label) {
            label.getStyleClass().add("dialog-message");
        }

        getChildren().addAll(header, content);

        // Action buttons
        if (actions != null && actions.length > 0) {
            HBox actionBox = new HBox();
            actionBox.getStyleClass().add("dialog-actions");
            actionBox.getChildren().addAll(actions);
            getChildren().add(actionBox);
        }
    }

    // ==============================================================================================
    // Factory Methods
    // ==============================================================================================

    /**
     * Creates an error dialog.
     *
     * @param title   The dialog title.
     * @param message The error message to display.
     * @return A new error dialog instance.
     */
    public static DialogLayout createError(String title, String message) {
        return createSimpleDialog(title, message, Styles.DANGER);
    }

    /**
     * Creates an information dialog.
     *
     * @param title   The dialog title.
     * @param message The information message to display.
     * @return A new information dialog instance.
     */
    public static DialogLayout createInfo(String title, String message) {
        return createSimpleDialog(title, message, Styles.ACCENT);
    }

    /**
     * Creates an unsaved changes confirmation dialog.
     *
     * @param message  The confirmation message.
     * @param callback The callback to receive the user's choice.
     * @return A new unsaved changes dialog instance.
     */
    public static DialogLayout createUnsavedChanges(String message, Consumer<UnsavedChangesResult> callback) {
        Label msgLabel = new Label(message);

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add(Styles.ACCENT);
        saveBtn.setOnAction(e -> {
            ModalService.getInstance().hide();
            callback.accept(UnsavedChangesResult.SAVE);
        });

        Button dontSaveBtn = new Button("Don't Save");
        dontSaveBtn.setOnAction(e -> {
            ModalService.getInstance().hide();
            callback.accept(UnsavedChangesResult.DONT_SAVE);
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> {
            ModalService.getInstance().hide();
            callback.accept(UnsavedChangesResult.CANCEL);
        });

        return new DialogLayout("Unsaved Changes", msgLabel, cancelBtn, dontSaveBtn, saveBtn);
    }

    // ==============================================================================================
    // Private Helpers
    // ==============================================================================================

    /**
     * Creates a simple dialog with a message and an OK button.
     *
     * @param title      The dialog title.
     * @param message    The message to display.
     * @param buttonStyle The style to apply to the OK button.
     * @return A new dialog instance.
     */
    private static DialogLayout createSimpleDialog(String title, String message, String buttonStyle) {
        Label msgLabel = new Label(message);

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add(buttonStyle);
        okBtn.setOnAction(e -> ModalService.getInstance().hide());

        return new DialogLayout(title, msgLabel, okBtn);
    }
}
