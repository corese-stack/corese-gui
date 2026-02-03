package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.component.notification.NotificationManager;
import fr.inria.corese.gui.core.adapter.RdfDataService;
import java.io.File;
import java.util.List;
import javafx.stage.FileChooser;

/**
 * Controller for the simple DataView.
 * Handles loading files into the application.
 */
public class DataViewController {

    private final DataView view;
    private final RdfDataService rdfDataService;

    public DataViewController(DataView view) {
        this.view = view;
        this.rdfDataService = RdfDataService.getInstance();
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
            int successCount = 0;

            for (File file : files) {
                try {
                    rdfDataService.loadFile(file);
                    successCount++;
                } catch (Exception ex) {
                    NotificationManager.getInstance().showError("Error loading " + file.getName() + ": " + ex.getMessage());
                }
            }

            if (successCount > 0) {
                NotificationManager.getInstance().showSuccess("Loaded " + successCount + " file(s).");
            }
        }
    }
}
