package fr.inria.corese.demo.factory.popup;

import fr.inria.corese.demo.manager.QueryManager;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class LogDialog extends BasePopup {
  private final TextArea logTextArea;
  private final QueryManager queryManager;

  public LogDialog() {
    this.queryManager = QueryManager.getInstance();

    setTitle("Application Logs");
    logTextArea = new TextArea();
    logTextArea.setEditable(false);
    logTextArea.setWrapText(true);
    logTextArea.setPrefRowCount(20);
    logTextArea.setPrefColumnCount(50);

    setupUI();
  }

  private void setupUI() {
    BorderPane contentPane = new BorderPane();
    contentPane.setCenter(logTextArea);

    HBox buttonBox = new HBox(10);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);
    buttonBox.setPadding(new Insets(10, 0, 0, 0));

    Button clearButton = new Button("Clear Log View");
    Button refreshButton = new Button("Refresh");

    clearButton.setOnAction(e -> clearLogView());
    refreshButton.setOnAction(e -> updateLogs());

    buttonBox.getChildren().addAll(clearButton, refreshButton);
    contentPane.setBottom(buttonBox);

    getDialogPane().setContent(contentPane);
    getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    getDialogPane().getStyleClass().add("log-dialog");
    getDialogPane().setPrefSize(600, 400);

    setResultConverter(
        dialogButton -> {
          if (dialogButton == ButtonType.CLOSE) {
            closePopup();
          }
          return null;
        });
  }

  /** Fetches the latest logs from the QueryManager and displays them. */
  public void updateLogs() {
    String logs = queryManager.getLogEntries().stream().collect(Collectors.joining("\n"));
    logTextArea.setText(logs);
  }

  /**
   * Clears the text area in the dialog. It does NOT clear the application's main graph or log
   * history.
   */
  public void clearLogView() {
    logTextArea.clear();
  }

  @Override
  public void displayPopup() {
    updateLogs();
    super.displayPopup();
  }
}
