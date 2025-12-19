package fr.inria.corese.gui.view.component;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.view.base.AbstractView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A modal dialog for displaying error messages with optional details.
 */
public class ErrorDialog extends AbstractView {

    private static final String STYLESHEET_PATH = "/styles/error-dialog.css";
    private static final int MAX_WIDTH = 400;
    private static final int MAX_HEIGHT_WITH_DETAIL = 500;
    private static final int MAX_HEIGHT_WITHOUT_DETAIL = 100;

    /**
     * Creates a new ErrorDialog.
     *
     * @param modalPane the parent ModalPane
     * @param title     the dialog title
     * @param header    the header message
     * @param details   the detailed error message (optional)
     */
    public ErrorDialog(ModalPane modalPane, String title, String header, String details) {
        super(new ModalBox(modalPane), STYLESHEET_PATH);
        initialize(title, header, details);
    }

    private void initialize(String title, String header, String details) {
        ModalBox modalBox = (ModalBox) getRoot();

        if (details == null || details.isBlank()) {
            modalBox.setMaxSize(MAX_WIDTH, MAX_HEIGHT_WITHOUT_DETAIL);
        } else {
            modalBox.setMaxSize(MAX_WIDTH, MAX_HEIGHT_WITH_DETAIL);
        }

        VBox content = createContent(title, header, details);
        modalBox.addContent(content);
    }

    private VBox createContent(String title, String header, String details) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);
        content.getStyleClass().add("error-dialog-content");

        content.getChildren().add(createHeaderSection(title, header));

        if (details != null && !details.isBlank()) {
            content.getChildren().add(createDetailsSection(details));
        }

        return content;
    }

    private HBox createHeaderSection(String title, String header) {
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon errorIcon = new FontIcon(Feather.ALERT_TRIANGLE);
        errorIcon.getStyleClass().add("error-dialog-icon");

        VBox titleBox = new VBox(5);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll(Styles.TITLE_4, "error-dialog-title");

        Label headerLabel = new Label(header);
        headerLabel.setWrapText(true);
        headerLabel.getStyleClass().add("error-dialog-header");

        titleBox.getChildren().addAll(titleLabel, headerLabel);
        headerBox.getChildren().addAll(errorIcon, titleBox);

        return headerBox;
    }

    private TextArea createDetailsSection(String details) {
        TextArea detailsArea = new TextArea(details);
        detailsArea.setEditable(false);
        detailsArea.setWrapText(false);
        detailsArea.setPrefRowCount(18);
        detailsArea.getStyleClass().addAll("text-monospace", "error-dialog-details");
        VBox.setVgrow(detailsArea, Priority.ALWAYS);
        return detailsArea;
    }
}
