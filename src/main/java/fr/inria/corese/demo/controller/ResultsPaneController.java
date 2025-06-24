package fr.inria.corese.demo.controller;

import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.manager.ApplicationStateManager;
import fr.inria.corese.demo.model.QueryResult;
import fr.inria.corese.demo.view.rule.CustomPagination;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

import java.util.ArrayList;
import java.util.List;


public class ResultsPaneController {
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
    private final WebView graphView;

    public ResultsPaneController() {
        customPagination = new CustomPagination(1, this::updateTableForPage);
        customPagination.setVisible(false);
        customPagination.setManaged(false);

        rowsPerPageField.setPrefWidth(60);
        Label perPageLabel = new Label("Rows per page:");
        rowsPerPageField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                rowsPerPage = Math.max(1, Integer.parseInt(newVal));
            } catch (NumberFormatException ex) {
                // Ignore invalid input
            }
            updatePagination();
        });

        controlsPane.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controlsPane.getChildren().addAll(perPageLabel, rowsPerPageField, spacer, totalRowsLabel);

        tableBox = new VBox(5, controlsPane, resultTable, customPagination);
        VBox.setVgrow(resultTable, Priority.ALWAYS);

        textFormatComboBox.getItems().setAll("XML", "JSON", "CSV", "TSV", "MARKDOWN");
        textFormatComboBox.getSelectionModel().select("XML");

        // The listener for this ComboBox is now managed by the QueryViewController.

        copyXmlButton.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(xmlResultTextArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
            String originalText = copyXmlButton.getText();
            copyXmlButton.setText("Copied!");
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> copyXmlButton.setText(originalText));
            pause.play();
        });

        this.graphView = new WebView();
    }

    public Node getTableBox() {
        return tableBox;
    }

    public WebView getGraphView() {
        return this.graphView;
    }

    public ComboBox<String> getTextFormatComboBox() {
        return textFormatComboBox;
    }

    public ComboBox<String> getXmlFormatComboBox() {
        return textFormatComboBox;
    }

    public TextArea getXmlResultTextArea() {
        return xmlResultTextArea;
    }

    public Button getCopyXmlButton() {
        return copyXmlButton;
    }

    public void updateTableView(String csvResult) {
        Platform.runLater(() -> {
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
                    tc.setCellValueFactory(cdf -> new javafx.beans.property.SimpleStringProperty(
                            (colIndex < cdf.getValue().length) ? cdf.getValue()[colIndex] : ""));
                    tc.prefWidthProperty().bind(resultTable.widthProperty().divide(Math.max(1, headers.length)));
                    resultTable.getColumns().add(tc);
                }
                for (int i = 1; i < lines.length; i++) {
                    allRows.add(lines[i].split(",", -1));
                }
            }
            updatePagination();
        });
    }


    public void displayGraph(String ttlData) {
        System.out.println("\n[RESULTS_PANE] displayGraph called.");
        if (ttlData == null || ttlData.isBlank()) {
            System.out.println("[RESULTS_PANE] TTL data is EMPTY. Clearing view.");
        } else {
            System.out.println("[RESULTS_PANE] Received TTL data to display:");
            System.out.println("----- BEGIN TTL -----");
            System.out.println(ttlData);
            System.out.println("----- END TTL -----");
        }
        Platform.runLater(() -> {
            if (ttlData == null || ttlData.isBlank()) {
                graphView.getEngine().load("about:blank");
                return;
            }
            try {
                System.out.println("[RESULTS_PANE] Attempting to load index.html...");
                String htmlPath = getClass().getResource("/web/index.html").toExternalForm();
                System.out.println("[RESULTS_PANE] Found index.html at: " + htmlPath);
                final ChangeListener<Worker.State> loadListener = new ChangeListener<>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Worker.State> observable,
                            Worker.State oldValue, Worker.State newValue) {
                        if (newValue == Worker.State.SUCCEEDED) {
                            graphView.getEngine().getLoadWorker().stateProperty().removeListener(this);

                            String escapedTtl = ttlData
                                    .replace("\\", "\\\\")
                                    .replace("'", "\\'")
                                    .replace("\n", "\\n")
                                    .replace("\r", "");

                            // The full, original script to create and populate the graph element
                            String script = "(function() {" +
                                    "    var container = document.getElementById('container');" +
                                    "    if (!container) { setTimeout(arguments.callee, 50); return; }" +
                                    "    var old = document.getElementById('myGraph');" +
                                    "    if (old && old.parentNode) { old.parentNode.removeChild(old); }" +
                                    "    var newGraph = document.createElement('kg-graph');" +
                                    "    newGraph.id = 'myGraph';" +
                                    "    newGraph.setAttribute('width', '100%');" +
                                    "    newGraph.setAttribute('height', '100%');" +
                                    "    container.appendChild(newGraph);" +
                                    "    function setTTL() {" +
                                    "      var el = document.getElementById('myGraph');" +
                                    "      if (el) { el.ttl = '" + escapedTtl + "'; }" +
                                    "      else { setTimeout(setTTL, 50); }" +
                                    "    }" +
                                    "    setTTL();" +
                                    "  })();";

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

 
    public void populateXmlView(ApplicationStateManager.TabCacheEntry cacheEntry, String formatLabel) {
        if (cacheEntry == null || formatLabel == null) {
            updateXMLView("");
            return;
        }

        // Get the mappings from the cache entry
        fr.inria.corese.core.kgram.core.Mappings mappings = cacheEntry.getMappingsResult();
        if (mappings == null) {
            updateXMLView("");
            return;
        }
        String formatted;
        ApplicationStateManager stateManager = ApplicationStateManager.getInstance();
        switch (formatLabel) {
            case "XML" -> formatted = stateManager.formatMappings(mappings, ResultFormat.format.XML_FORMAT);
            case "JSON" -> formatted = stateManager.formatMappings(mappings, ResultFormat.format.JSON_FORMAT);
            case "CSV" -> formatted = stateManager.formatMappings(mappings, ResultFormat.format.CSV_FORMAT);
            case "TSV" -> formatted = stateManager.formatMappings(mappings, ResultFormat.format.TSV_FORMAT);
            case "MARKDOWN" -> formatted = stateManager.formatMappings(mappings, ResultFormat.format.MARKDOWN_FORMAT);
            default -> formatted = "Unsupported format selected.";
        }
        updateXMLView(formatted);
    }

    private void updateXMLView(String content) {
        Platform.runLater(() -> xmlResultTextArea.setText(content != null ? content : ""));
    }

    public void resetXmlFormatSelector() {
        Platform.runLater(() -> textFormatComboBox.getSelectionModel().select("XML"));
    }

    public void clearResults() {
        Platform.runLater(() -> {
            // Clear table
            resultTable.getItems().clear();
            resultTable.getColumns().clear();
            allRows.clear();
            updatePagination();
            xmlResultTextArea.clear();
            if (graphView != null) {
                graphView.getEngine().load("about:blank");
            }
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

}
