package fr.inria.corese.demo.factory.popup;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

public class DocumentationPopup extends BasePopup {
  private final TextArea documentationTextArea;

  public DocumentationPopup() {
    setTitle("Documentation");

    documentationTextArea = new TextArea();
    documentationTextArea.setEditable(false);
    documentationTextArea.setWrapText(true);

    initializeDocumentation();

    BorderPane contentPane = new BorderPane();
    contentPane.setPadding(new Insets(10, 10, -15, 10));

    contentPane.setCenter(documentationTextArea);

    getDialogPane().setContent(contentPane);
    getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    setOnCloseRequest(event -> closePopup());

    setupUI();
  }

  private void initializeDocumentation() {
    documentationTextArea.setText("Documentation goes here");
  }

  private void setupUI() {
    documentationTextArea.setPrefRowCount(10);
  }
}
