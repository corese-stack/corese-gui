package fr.inria.corese.gui.component.dialog;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.utils.CssUtils;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A modal dialog for displaying error messages with optional details.
 *
 * <p>This dialog presents a title, a header message, and an optional detailed error message (e.g.,
 * a stack trace) in a scrollable text area.
 * 
 * <p>It extends {@link ModalBox} from AtlantaFX for direct integration with {@link ModalPane}.
 */
public class ErrorDialogWidget extends ModalBox {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  private static final int MAX_WIDTH = 400;
  private static final int MAX_HEIGHT_WITH_DETAIL = 500;
  private static final int MAX_HEIGHT_WITHOUT_DETAIL = 100;
  private static final String STYLESHEET = "/styles/error-dialog.css";

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Creates a new ErrorDialogWidget.
   *
   * @param modalPane the parent ModalPane to which this dialog belongs
   * @param title the dialog title (e.g., "Error")
   * @param header the header message (e.g., "An unexpected error occurred")
   * @param details the detailed error message (optional, e.g., stack trace)
   */
  public ErrorDialogWidget(ModalPane modalPane, String title, String header, String details) {
    super(modalPane);
    CssUtils.applyViewStyles(this, STYLESHEET);
    initialize(title, header, details);
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /**
   * Initializes the dialog content and size.
   *
   * @param title the dialog title
   * @param header the header message
   * @param details the detailed error message
   */
  private void initialize(String title, String header, String details) {
    // Set size based on whether details are present
    if (details == null || details.isBlank()) {
      setMaxSize(MAX_WIDTH, MAX_HEIGHT_WITHOUT_DETAIL);
    } else {
      setMaxSize(MAX_WIDTH, MAX_HEIGHT_WITH_DETAIL);
    }

    VBox content = createContent(title, header, details);
    addContent(content);
  }

  /**
   * Creates the main content layout of the dialog.
   *
   * @param title the dialog title
   * @param header the header message
   * @param details the detailed error message
   * @return the VBox containing the dialog content
   */
  private VBox createContent(String title, String header, String details) {
    VBox content = new VBox();
    content.getStyleClass().add("error-dialog-content");

    content.getChildren().add(createHeaderSection(title, header));

    if (details != null && !details.isBlank()) {
      content.getChildren().add(createDetailsSection(details));
    }

    return content;
  }

  // ==============================================================================================
  // UI Components
  // ==============================================================================================

  /**
   * Creates the header section containing the icon, title, and header message.
   *
   * @param title the dialog title
   * @param header the header message
   * @return the HBox containing the header section
   */
  private HBox createHeaderSection(String title, String header) {
    HBox headerBox = new HBox();
    headerBox.getStyleClass().add("error-dialog-header");

    FontIcon errorIcon = new FontIcon(Feather.ALERT_TRIANGLE);
    errorIcon.getStyleClass().addAll(Styles.DANGER, "error-dialog-icon");

    VBox titleBox = new VBox();
    titleBox.getStyleClass().add("error-dialog-title-box");

    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add(Styles.TITLE_4);

    Label headerLabel = new Label(header);
    headerLabel.setWrapText(true);

    titleBox.getChildren().addAll(titleLabel, headerLabel);
    headerBox.getChildren().addAll(errorIcon, titleBox);

    return headerBox;
  }

  /**
   * Creates the details section containing the scrollable text area.
   *
   * @param details the detailed error message
   * @return the TextArea containing the details
   */
  private TextArea createDetailsSection(String details) {
    TextArea detailsArea = new TextArea(details);
    detailsArea.setEditable(false);
    detailsArea.setWrapText(false);
    detailsArea.getStyleClass().addAll("text-monospace", "error-dialog-details");

    VBox.setVgrow(detailsArea, Priority.ALWAYS);
    return detailsArea;
  }
}