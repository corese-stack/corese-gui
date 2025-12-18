package fr.inria.corese.gui.view.component;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;

/**
 * A generic error dialog component that uses AtlantaFX's ModalBox.
 *
 * <p>It displays an error icon, a title, a header message, and optional details (e.g., stack trace).
 */
public class ErrorDialog extends ModalBox {

  public ErrorDialog(ModalPane modalPane, String title, String header, String details) {
    super(modalPane);
    this.getStylesheets().add(getClass().getResource("/styles/error-dialog.css").toExternalForm());

    this.setMaxSize(600, 500);

    // Main content container
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setAlignment(Pos.TOP_LEFT);

    // Header section with Icon and Message
    HBox headerBox = new HBox(15);
    headerBox.setAlignment(Pos.CENTER_LEFT);

    FontIcon errorIcon = new FontIcon(MaterialDesignA.ALERT);
    errorIcon.getStyleClass().add("error-dialog-icon");

    VBox titleBox = new VBox(5);
    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("error-dialog-title");
    
    Label headerLabel = new Label(header);
    headerLabel.setWrapText(true);
    headerLabel.getStyleClass().add("error-dialog-header");
    
    titleBox.getChildren().addAll(titleLabel, headerLabel);

    headerBox.getChildren().addAll(errorIcon, titleBox);
    content.getChildren().add(headerBox);

    // Details section (optional)
    if (details != null && !details.isEmpty()) {
      TextArea detailsArea = new TextArea(details);
      detailsArea.setEditable(false);
      detailsArea.setWrapText(false);
      detailsArea.setPrefRowCount(8);
      detailsArea.getStyleClass().add("error-dialog-details");
      VBox.setVgrow(detailsArea, Priority.ALWAYS);
      content.getChildren().add(detailsArea);
    }

    this.addContent(content);
  }
}
