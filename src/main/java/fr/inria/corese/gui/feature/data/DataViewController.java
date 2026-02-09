package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.RdfDataService;
import fr.inria.corese.gui.utils.AppExecutors;

import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.stage.FileChooser;

/**
 * Controller for the simple DataView. Handles loading files into the
 * application.
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
		FileDialogState.applyInitialDirectory(fileChooser);
		FileChooser.ExtensionFilter rdfFilter = FileTypeSupport.createExtensionFilter("RDF Files",
				FileTypeSupport.rdfExtensions(), true);
		fileChooser.getExtensionFilters().addAll(rdfFilter, new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setSelectedExtensionFilter(rdfFilter);

		List<File> files = fileChooser.showOpenMultipleDialog(view.getRoot().getScene().getWindow());

		if (files != null && !files.isEmpty()) {
			FileDialogState.updateLastDirectory(files);
			AppExecutors.execute(() -> {
				int successCount = 0;

				for (File file : files) {
					try {
						rdfDataService.loadFile(file);
						successCount++;
					} catch (Exception ex) {
						String message = "Error loading " + file.getName() + ": " + ex.getMessage();
						Platform.runLater(() -> NotificationWidget.getInstance().showError(message));
					}
				}

				if (successCount > 0) {
					int count = successCount;
					Platform.runLater(
							() -> NotificationWidget.getInstance().showSuccess("Loaded " + count + " file(s)."));
				}
			});
		}
	}
}
