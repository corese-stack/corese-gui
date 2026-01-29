package fr.inria.corese.gui.feature.result;

import fr.inria.corese.gui.component.pagination.CustomPagination;






import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class ResultsPaneController {
  private static final Logger logger = LoggerFactory.getLogger(ResultsPaneController.class);
  
  private final TableView<String[]> resultTable = new TableView<>();
  private final CustomPagination customPagination;
  private final TextField rowsPerPageField = new TextField("10");
  private final Label totalRowsLabel = new Label("total rows: 0");
  private final HBox controlsPane = new HBox(10);
  private final VBox tableBox;
  private final List<String[]> allRows = new ArrayList<>();
  private int rowsPerPage = 50;

  private final ComboBox<String> textFormatComboBox = new ComboBox<>();
  private final TextArea xmlResultTextArea = new TextArea();
  private final Button copyXmlButton = new Button("Copy");
  private final BorderPane textViewBox;
  private final WebView graphView;

  public ResultsPaneController() {
    customPagination = new CustomPagination(1, this::updateTableForPage);
    customPagination.setVisible(false);
    customPagination.setManaged(false);

    rowsPerPageField.setPrefWidth(60);
    Label perPageLabel = new Label("Rows per page:");
    rowsPerPageField
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              try {
                rowsPerPage = Math.max(1, Integer.parseInt(newVal));
              } catch (NumberFormatException ex) {
              }
              updatePagination();
            });

    controlsPane.setAlignment(Pos.CENTER_LEFT);

    Button exportButton = new Button("Export");
    exportButton.setOnAction((event -> handleExport()));
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    controlsPane.getChildren().addAll(perPageLabel, rowsPerPageField, spacer, totalRowsLabel);

    tableBox = new VBox(5, controlsPane, resultTable, customPagination);
    VBox.setVgrow(resultTable, Priority.ALWAYS);

    textFormatComboBox.getItems().setAll("XML", "JSON", "CSV", "TSV", "MARKDOWN");
    textFormatComboBox.getSelectionModel().select("XML");

    copyXmlButton.setOnAction(
        event -> {
          ClipboardContent content = new ClipboardContent();
          content.putString(xmlResultTextArea.getText());
          Clipboard.getSystemClipboard().setContent(content);
          String originalText = copyXmlButton.getText();
          copyXmlButton.setText("Copied!");
          PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
          pause.setOnFinished(e -> copyXmlButton.setText(originalText));
          pause.play();
        });

    Region textSpacer = new Region();
    HBox.setHgrow(textSpacer, Priority.ALWAYS);
    HBox textControls = new HBox(10, textFormatComboBox, textSpacer, copyXmlButton, exportButton);
    textControls.setAlignment(Pos.CENTER_LEFT);
    textControls.setStyle("-fx-padding: 5;");

    this.textViewBox = new BorderPane();
    this.textViewBox.setTop(textControls);
    this.textViewBox.setCenter(xmlResultTextArea);

    this.graphView = new WebView();
  }

  public Node getTableBox() {
    return tableBox;
  }

  public WebView getGraphView() {
    return this.graphView;
  }

  public Node getTextViewBox() {
    return this.textViewBox;
  }

  public ComboBox<String> getTextFormatComboBox() {
    return textFormatComboBox;
  }

  public Button getCopyXmlButton() {
    return copyXmlButton;
  }

  public void updateTableView(String csvResult) {
    Platform.runLater(
        () -> {
          resultTable.getItems().clear();
          resultTable.getColumns().clear();
          allRows.clear();
          if (csvResult == null || csvResult.isEmpty()) {
            updatePagination();
            return;
          }
          String[] lines = csvResult.split("\\r?\\n");
          if (lines.length > 0) {
            String[] headers = lines[0].split(",", -1);
            for (int col = 0; col < headers.length; col++) {
              final int colIndex = col;
              TableColumn<String[], String> tc = new TableColumn<>(headers[col].trim());
              tc.setCellValueFactory(
                  cdf ->
                      new javafx.beans.property.SimpleStringProperty(
                          (colIndex < cdf.getValue().length) ? cdf.getValue()[colIndex] : ""));
              tc.prefWidthProperty()
                  .bind(resultTable.widthProperty().divide(Math.max(1, headers.length)));
              resultTable.getColumns().add(tc);
            }
            for (int i = 1; i < lines.length; i++) {
              allRows.add(lines[i].split(",", -1));
            }
          }
          updatePagination();
        });
  }

  public void setTextFormats(List<String> formats, String defaultFormat) {
    Platform.runLater(
        () -> {
          textFormatComboBox.getItems().setAll(formats);
          if (defaultFormat != null && formats.contains(defaultFormat)) {
            textFormatComboBox.getSelectionModel().select(defaultFormat);
          } else if (!formats.isEmpty()) {
            textFormatComboBox.getSelectionModel().selectFirst();
          } else {
            textFormatComboBox.getSelectionModel().clearSelection();
          }
        });
  }

  public void displayGraph(String ttlData) {
    logger.debug("displayGraph called");
    if (ttlData == null || ttlData.isBlank()) {
      logger.debug("TTL data is empty. Clearing view.");
    } else {
      logger.debug("Received TTL data to display:\n{}", ttlData);
    }
    Platform.runLater(
        () -> {
          if (ttlData == null || ttlData.isBlank()) {
            graphView.getEngine().load("about:blank");
            return;
          }
          try {
            logger.debug("Attempting to load index.html...");
            String htmlPath = getClass().getResource("/web/index.html").toExternalForm();
            logger.debug("Found index.html at: {}", htmlPath);
            final ChangeListener<Worker.State> loadListener =
                new ChangeListener<>() {
                  @Override
                  public void changed(
                      javafx.beans.value.ObservableValue<? extends Worker.State> observable,
                      Worker.State oldValue,
                      Worker.State newValue) {
                    if (newValue == Worker.State.SUCCEEDED) {
                      graphView.getEngine().getLoadWorker().stateProperty().removeListener(this);

                      String escapedTtl =
                          ttlData
                              .replace("\\", "\\\\")
                              .replace("'", "\\'")
                              .replace("\n", "\\n")
                              .replace("\r", "");

                      String script =
                          "(function() {    var container = document.getElementById('container');  "
                              + "  if (!container) { setTimeout(arguments.callee, 50); return; }   "
                              + " var old = document.getElementById('myGraph');    if (old &&"
                              + " old.parentNode) { old.parentNode.removeChild(old); }    var"
                              + " newGraph = document.createElement('kg-graph');    newGraph.id ="
                              + " 'myGraph';    newGraph.setAttribute('width', '100%');   "
                              + " newGraph.setAttribute('height', '100%');   "
                              + " container.appendChild(newGraph);    function setTTL() {      var"
                              + " el = document.getElementById('myGraph');      if (el) { el.ttl ="
                              + " '"
                              + escapedTtl
                              + "'; }"
                              + "      else { setTimeout(setTTL, 50); }"
                              + "    }"
                              + "    setTTL();"
                              + "  })();";

                      graphView.getEngine().executeScript(script);
                    }
                  }
                };
            graphView.getEngine().getLoadWorker().stateProperty().addListener(loadListener);
            graphView.getEngine().load(htmlPath);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }

  private void handleExport() {
    String contentToExport = xmlResultTextArea.getText();
    if (contentToExport == null || contentToExport.isBlank()) {
      showError("Export Error", "There is no text content to export.");
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Result As");

    String selectedFormat = textFormatComboBox.getValue();
    if (selectedFormat == null) {
      selectedFormat = "Text";
    }
    String extension = getExtensionForFormat(selectedFormat);

    fileChooser.setInitialFileName("output" + extension);
    FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter(
            selectedFormat + " file (*" + extension + ")", "*" + extension);
    fileChooser.getExtensionFilters().add(extFilter);
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

    File file = fileChooser.showSaveDialog(xmlResultTextArea.getScene().getWindow());

    if (file != null) {
      File fileToSave = file;
      if (!file.getName().toLowerCase().endsWith(extension)) {
        fileToSave = new File(file.getAbsolutePath() + extension);
      }

      try {
        Files.writeString(fileToSave.toPath(), contentToExport);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText(null);
        alert.setContentText(
            "The result has been successfully exported to:\n" + fileToSave.getAbsolutePath());
        alert.showAndWait();
      } catch (IOException e) {
        e.printStackTrace();
        showError("Export Failed", "An error occurred while saving the file:\n" + e.getMessage());
      }
    }
  }

  private String getExtensionForFormat(String format) {

    return switch (format.toUpperCase()) {
      case "RDF/XML" -> ".rdf";
      case "JSON-LD" -> ".jsonld";
      case "N-TRIPLES" -> ".nt";
      case "N-QUADS" -> ".nq";
      case "TRIG" -> ".trig";
      case "TURTLE" -> ".ttl";
      case "JSON" -> ".json";
      case "CSV" -> ".csv";
      case "TSV" -> ".tsv";
      case "XML" -> ".xml";
      case "MARKDOWN" -> ".md";
      default -> ".ttl";
    };
  }

  public void updateXMLView(String content) {
    Platform.runLater(() -> xmlResultTextArea.setText(content != null ? content : ""));
  }

  public void resetXmlFormatSelector() {
    Platform.runLater(() -> textFormatComboBox.getSelectionModel().select("XML"));
  }

  public void clearResults() {
    Platform.runLater(
        () -> {
          resultTable.getItems().clear();
          resultTable.getColumns().clear();
          allRows.clear();
          updatePagination();
          xmlResultTextArea.clear();
          if (graphView != null) {
            graphView.getEngine().load("about:blank");
          }
          textFormatComboBox.getItems().clear();
        });
  }

  private void updatePagination() {
    totalRowsLabel.setText("total rows: " + allRows.size());
    if (allRows.isEmpty()) {
      customPagination.setVisible(false);
      customPagination.setManaged(false);
      return;
    }
    int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
    customPagination.setPageCount(pageCount);
    customPagination.setVisible(pageCount > 1);
    customPagination.setManaged(pageCount > 1);
    customPagination.setCurrentPageIndex(0);
  }

  private void updateTableForPage(int pageIndex) {
    int fromIndex = pageIndex * rowsPerPage;
    int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
    if (fromIndex >= allRows.size()) {
      resultTable.getItems().clear();
      return;
    }
    resultTable.getItems().setAll(allRows.subList(fromIndex, toIndex));
  }

  private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
