package fr.inria.corese.gui.core.factory.popup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class TemplatePopup {

  private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

  static {
    TEMPLATES.put(
        "SELECT",
        "SELECT * WHERE {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}\n"
            + //
            "");

    TEMPLATES.put(
        "CONSTRUCT",
        "CONSTRUCT {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "} \n"
            + //
            "WHERE {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}\n"
            + //
            "");

    TEMPLATES.put(
        "ASK",
        "ask {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}\n"
            + //
            "");

    TEMPLATES.put(
        "DESCRIBE",
        "DESCRIBE ?x\n"
            + //
            "WHERE {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}\n"
            + //
            "");

    TEMPLATES.put(
        "INSERT",
        "INSERT {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}\n"
            + //
            "WHERE {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}");

    TEMPLATES.put(
        "INSERT DATA",
        "prefix foaf: <http://xmlns.com/foaf/0.1/>\n"
            + //
            "\n"
            + //
            "INSERT data {\n"
            + //
            "  us:John foaf:name 'John' ;\n"
            + //
            "    foaf:knows us:James .\n"
            + //
            "}\n"
            + //
            "");

    TEMPLATES.put(
        "DELETE DATA",
        "DELETE {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}\n"
            + //
            "WHERE {\n"
            + //
            "  ?x ?p ?y\n"
            + //
            "}");

    TEMPLATES.put(
        "SELECT GRAPH",
        "SELECT * WHERE {\n"
            + //
            "  graph ?g {\n"
            + //
            "    ?x ?p ?y\n"
            + //
            "  }\n"
            + //
            "}\n"
            + //
            "");

    TEMPLATES.put(
        "CONSTRUCT GRAPH",
        "CONSTRUCT {\n"
            + //
            "  graph ?g { ?x ?p ?y }\n"
            + //
            "} \n"
            + //
            "WHERE {\n"
            + //
            "  graph ?g { ?x ?p ?y }\n"
            + //
            "}\n"
            + //
            "");

    TEMPLATES.put(
        "SERVICE",
        "SELECT * \n"
            + //
            "WHERE {\n"
            + //
            "  service <http://corese.inria.fr/sparql> {\n"
            + //
            "     SELECT * WHERE {\n"
            + //
            "       ?x ?p ?y \n"
            + //
            "     } \n"
            + //
            "     limit 10\n"
            + //
            "  }\n"
            + //
            "}");
  }

  public static void show(Stage owner, Consumer<String> onUse) {
    Stage dialog = new Stage();
    dialog.initOwner(owner);
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle("Templates");

    ComboBox<String> comboBox = new ComboBox<>();
    comboBox.getItems().addAll(TEMPLATES.keySet());
    comboBox.getSelectionModel().selectFirst();

    Label formatLabel = new Label("Template outline:");
    TextArea formatArea = new TextArea();
    formatArea.setEditable(true);
    formatArea.setWrapText(true);
    formatArea.setPrefRowCount(10);
    formatArea.setPrefWidth(860);
    formatArea.setPrefHeight(300);
    formatArea.setMaxWidth(Double.MAX_VALUE);
    formatArea.setMaxHeight(Double.MAX_VALUE);
    formatArea.setText(TEMPLATES.get(comboBox.getValue()));

    comboBox
        .valueProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              formatArea.setText(TEMPLATES.get(newVal));
            });

    // "Use" button at the bottom
    Button useButton = new Button("Use");
    useButton.setDefaultButton(true);
    useButton.setOnAction(
        e -> {
          String selectedTemplate = formatArea.getText();
          if (onUse != null) {
            onUse.accept(selectedTemplate);
          }
          dialog.close();
        });

    HBox buttonBox = new HBox(useButton);
    buttonBox.setAlignment(Pos.CENTER_RIGHT);
    buttonBox.setPadding(new Insets(10, 0, 0, 0));

    VBox vbox = new VBox(10, comboBox, formatLabel, formatArea, buttonBox);
    vbox.setAlignment(Pos.CENTER);
    vbox.setPadding(new Insets(20));

    Scene scene = new Scene(vbox, 900, 400);
    dialog.setScene(scene);
    dialog.setWidth(900);
    dialog.setHeight(400);
    dialog.setMinWidth(900);
    dialog.setMinHeight(400);
    dialog.showAndWait();
  }
}
