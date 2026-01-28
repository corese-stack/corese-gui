package fr.inria.corese.gui.core.factory.popup;

import fr.inria.corese.gui.core.manager.QueryManager;






import fr.inria.corese.core.Graph;
import fr.inria.corese.core.print.ResultFormat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * A reusable popup dialog for previewing a Corese Graph in various RDF formats and saving the
 * result to a file.
 *
 * <p>This class is self-contained and uses a static method `show()` to be invoked.
 */
public class ExportFormatPopup {

  private static final List<String> FORMATS =
      List.of("TURTLE", "RDF/XML", "JSON-LD", "N-TRIPLES", "N-QUADS", "TRIG");

  /**
   * Displays the export dialog.
   *
   * @param owner The parent window, used for positioning and modality.
   * @param graphToExport The Corese Graph object to be exported.
   */
  public static void show(Stage owner, Graph graphToExport) {
    Stage dialog = new Stage();
    dialog.initOwner(owner);
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle("Export Content As...");

    ComboBox<String> formatComboBox = new ComboBox<>();
    formatComboBox.getItems().addAll(FORMATS);
    formatComboBox.getSelectionModel().selectFirst();

    Label formatLabel = new Label("Preview in selected format:");
    TextArea previewArea = new TextArea();
    previewArea.setEditable(false);
    previewArea.setWrapText(true);
    previewArea.setPrefHeight(300);

    Button saveButton = new Button("Save to File...");
    saveButton.setDefaultButton(true);

    HBox buttonBox = new HBox(saveButton);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);
    buttonBox.setPadding(new Insets(10, 0, 0, 0));

    VBox vbox = new VBox(10, formatComboBox, formatLabel, previewArea, buttonBox);
    vbox.setPadding(new Insets(20));
    vbox.setAlignment(Pos.CENTER_LEFT);

    formatComboBox
        .valueProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                previewArea.setText(formatGraph(graphToExport, newVal));
              }
            });

    previewArea.setText(formatGraph(graphToExport, formatComboBox.getValue()));

    saveButton.setOnAction(
        e -> {
          saveContentToFile(previewArea.getText(), formatComboBox.getValue(), owner);
          dialog.close();
        });

    Scene scene = new Scene(vbox, 700, 450);
    dialog.setScene(scene);
    dialog.setMinWidth(600);
    dialog.setMinHeight(400);
    dialog.showAndWait();
  }

  private static String formatGraph(Graph graph, String formatName) {
    if (graph == null) return "Error: Input graph is null.";
    ResultFormat.format coreseFormat = getCoreseFormat(formatName);
    return QueryManager.getInstance().formatGraph(graph, coreseFormat);
  }

  private static ResultFormat.format getCoreseFormat(String formatName) {
    return switch (formatName) {
      case "RDF/XML" -> ResultFormat.format.RDF_XML_FORMAT;
      case "JSON-LD" -> ResultFormat.format.JSONLD_FORMAT;
      case "N-TRIPLES" -> ResultFormat.format.NTRIPLES_FORMAT;
      case "N-QUADS" -> ResultFormat.format.NQUADS_FORMAT;
      case "TRIG" -> ResultFormat.format.TRIG_FORMAT;
      default -> ResultFormat.format.TURTLE_FORMAT;
    };
  }

  private static void saveContentToFile(String content, String format, Stage owner) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save As");

    String extension = getExtensionForFormat(format);
    FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter(format + " file (*" + extension + ")", "*" + extension);
    fileChooser.getExtensionFilters().add(extFilter);
    fileChooser.setSelectedExtensionFilter(extFilter);

    File file = fileChooser.showSaveDialog(owner);
    if (file != null) {
      try {
        if (!file.getName().toLowerCase().endsWith(extension)) {
          file = new File(file.getAbsolutePath() + extension);
        }
        Files.writeString(file.toPath(), content);
      } catch (IOException ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Save Error");
        alert.setHeaderText("Could not save the file.");
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
      }
    }
  }

  private static String getExtensionForFormat(String format) {
    return switch (format) {
      case "RDF/XML" -> ".rdf";
      case "JSON-LD" -> ".jsonld";
      case "N-TRIPLES" -> ".nt";
      case "N-QUADS" -> ".nq";
      case "TRIG" -> ".trig";
      default -> ".ttl";
    };
  }
}
