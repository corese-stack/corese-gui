package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.core.manager.CoreseGraphManager;
import java.io.File;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;

/**
 * Controller for the simple DataView.
 * Handles loading files into the application.
 */
public class DataViewController {

    private final DataView view;
    private final CoreseGraphManager graphManager;

    public DataViewController(DataView view) {
        this.view = view;
        this.graphManager = CoreseGraphManager.getInstance();
        initialize();
    }

    private void initialize() {
        view.getLoadButton().setOnAction(e -> handleLoadFile());
    }

    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open RDF Data File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("RDF Files", "*.ttl", "*.rdf", "*.xml", "*.jsonld", "*.nt", "*.nq", "*.trig"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(view.getRoot().getScene().getWindow());

        if (files != null && !files.isEmpty()) {
            StringBuilder message = new StringBuilder();
            boolean errorOccurred = false;

            for (File file : files) {
                try {
                    graphManager.loadFromFile(file);
                    message.append("Loaded: ").append(file.getName()).append("\n");
                } catch (Exception ex) {
                    errorOccurred = true;
                    message.append("Error loading ").append(file.getName()).append(": ").append(ex.getMessage()).append("\n");
                }
            }

            Alert alert = new Alert(errorOccurred ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
            alert.setTitle("Load Result");
            alert.setHeaderText(null);
            alert.setContentText(message.toString());
            alert.showAndWait();
        }
    }
}