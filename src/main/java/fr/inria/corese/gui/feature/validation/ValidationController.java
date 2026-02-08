package fr.inria.corese.gui.feature.validation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.ValidationResult;
import fr.inria.corese.gui.feature.editor.tab.TabEditorConfig;
import fr.inria.corese.gui.feature.editor.tab.TabEditorController;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.utils.AppExecutors;
import fr.inria.corese.gui.core.io.FileDialogState;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.stage.FileChooser;

/**
 * Controller for the Validation View.
 *
 * <p>
 * This controller acts as the main orchestrator for the SHACL Validation
 * feature. It follows the MVC pattern, mediating between the
 * {@link ValidationView} and the {@link ValidationModel}.
 *
 * <p>
 * Responsibilities:
 *
 * <ul>
 * <li>Initializing the editor interface via {@link TabEditorController}.
 * <li>Managing the lifecycle of {@link ValidationModel} instances associated
 * with each tab.
 * <li>Handling user actions such as running validation, opening files, and
 * updating results.
 * </ul>
 */
public class ValidationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationController.class);

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private final ValidationView view;
	private final Map<Tab, ValidationModel> tabModels = new HashMap<>();
	private TabEditorController tabEditorController;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	public ValidationController(ValidationView view) {
		this.view = view;
		initialize();
	}

	// ==============================================================================================
	// Initialization
	// ==============================================================================================

	private void initialize() {
		configureEditor();
		setupViewIntegration();
	}

	private void configureEditor() {
		// Configure empty state
		Node emptyState = view.createEmptyState(this::onNewTabButtonClick, this::onOpenFileButtonClick);

		// Build configuration
		TabEditorConfig config = TabEditorConfig.builder()
				.withEditorButtons(List.of(ButtonFactory.save(), ButtonFactory.export(),
						ButtonFactory.undo(), ButtonFactory.redo()))
				.withExecution(ButtonFactory.custom(ButtonIcon.PLAY, view.getRunValidationLabel()),
						this::executeValidation)
				.withResultView(view.getResultToolbarButtons(), ResultViewConfig.builder().withTextTab().build())
				.withEmptyState(emptyState)
				.withAllowedExtensions(buildValidationExtensions())
				.withOpenFileAction(this::onOpenFileButtonClick)
				.build();

		// Create controller
		tabEditorController = new TabEditorController(config);
	}

	private static List<String> buildValidationExtensions() {
		java.util.LinkedHashSet<String> extensions = new java.util.LinkedHashSet<>();
		for (SerializationFormat format : SerializationFormat.rdfFormats()) {
			String ext = format.getExtension();
			if (ext != null && !ext.isBlank()) {
				extensions.add(ext);
			}
		}
		return List.copyOf(extensions);
	}

	private void setupViewIntegration() {
		// Embed the editor's view into the main ValidationView
		view.setMainContent(tabEditorController.getViewRoot());

		// Manage memory: Remove the model when a tab is closed
		tabEditorController.addTabListener(this::onTabsChanged);
	}

	// ==============================================================================================
	// Public Actions (User Interactions)
	// ==============================================================================================

	public void executeValidation() {
		Tab selectedTab = tabEditorController.getSelectedTab();
		if (selectedTab == null)
			return;

		ResultController resultController = tabEditorController.getCurrentResultController();
		if (resultController == null)
			return;

		// Clear previous results
		resultController.clearResults();

		// Retrieve or create the model for the current tab
		ValidationModel model = tabModels.computeIfAbsent(selectedTab, k -> new ValidationModel());

		// Pre-check: Ensure data is loaded
		if (!model.hasData()) {
			tabEditorController.hideResultPane();
			tabEditorController.showError("No Data Loaded", "Validation requires an RDF graph to be loaded.\n"
					+ "Please go to the 'Data' view and load an RDF file.");
			return;
		}

		// Retrieve shapes content from the editor
		final String shapesContent = tabEditorController.getEditorContent(selectedTab);
		if (shapesContent == null || shapesContent.trim().isEmpty()) {
			tabEditorController.hideResultPane();
			tabEditorController.showError("Empty Shapes", "The shapes file is empty.\n"
					+ "Please write or load SHACL shapes in the editor before validating.");
			return;
		}

		// UI: Indicate execution start
		tabEditorController.setExecutionState(true);

		// Execute validation asynchronously
		AppExecutors.execute(() -> runValidationTask(model, shapesContent, resultController));
	}

	// ==============================================================================================
	// Background Task & Callbacks
	// ==============================================================================================

	private void runValidationTask(ValidationModel model, String shapesContent, ResultController resultController) {
		try {
			// Perform validation logic
			ValidationResult result = model.validate(shapesContent);

			// Update UI on JavaFX Application Thread
			Platform.runLater(() -> handleValidationResult(result, resultController));
		} catch (Exception e) {
			LOGGER.error("Error during validation", e);
			Platform.runLater(() -> {
				tabEditorController.setExecutionState(false);
				tabEditorController.hideResultPane();
				tabEditorController.showError("Validation Error",
						"An unexpected error occurred during validation.\n" + "Please check the logs for more details.",
						e.getMessage());
			});
		}
	}

	private void handleValidationResult(ValidationResult result, ResultController resultController) {
		tabEditorController.setExecutionState(false);

		if (result.getErrorMessage() != null) {
			// Handle validation errors (e.g., syntax errors in shapes)
			tabEditorController.hideResultPane();
			tabEditorController.showError("Invalid SHACL Syntax",
					"The SHACL shapes contain syntax errors.\nPlease correct the errors listed below:",
					result.getErrorMessage());
		} else {
			// Success: Display the report
			Tab selectedTab = tabEditorController.getSelectedTab();
			ValidationModel model = tabModels.get(selectedTab);

			tabEditorController.showResultPane();

			// Configure tabs: Validation results have text only
			resultController.configureTabsForResult(true, // text: enabled (TURTLE/RDF/XML report)
					false, // table: disabled
					false // graph: disabled (not used for validation)
			);

			// Ensure the text tab is visible to show the report
			resultController.selectTextTab();

			// Ensure text formats are configured for RDF outputs
			SerializationFormat[] formats = SerializationFormat.rdfFormats();
			resultController.configureTextFormats(formats, SerializationFormat.TURTLE);
			SerializationFormat preferredFormat = resultController
					.getPreferredTextFormat(formats, SerializationFormat.TURTLE);

			// Display initial report using the preferred format
			AppExecutors.execute(() -> {
				String initialReport = model.formatLastReport(preferredFormat.getLabel());
				if (initialReport != null) {
					Platform.runLater(() -> resultController.updateText(initialReport));
				}
			});

			// Configure callback for format changes
			resultController.setOnFormatChanged(format -> AppExecutors.execute(() -> {
				String formattedReport = model.formatLastReport(format.getLabel());
				if (formattedReport != null) {
					Platform.runLater(() -> resultController.updateText(formattedReport));
				}
			}));
		}
	}

	// ==============================================================================================
	// Helper Methods
	// ==============================================================================================

	private void onTabsChanged(ListChangeListener.Change<? extends Tab> change) {
		while (change.next()) {
			if (change.wasRemoved()) {
				for (Tab tab : change.getRemoved()) {
					ValidationModel model = tabModels.remove(tab);
					if (model != null) {
						model.dispose();
					}
				}
			}
		}
	}

	private void onNewTabButtonClick() {
		tabEditorController.createNewTab();
	}

	private void onOpenFileButtonClick() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Shapes File");
		FileDialogState.applyInitialDirectory(fileChooser);
		List<String> extensions = buildValidationExtensions();
		String[] patterns = extensions.stream().map(ext -> "*" + ext).toArray(String[]::new);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("RDF Files", patterns));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

		File file = fileChooser
				.showOpenDialog(view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null);

		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			LOGGER.info("Loading SHACL file: {}", file.getAbsolutePath());
			tabEditorController.openFile(file);
		}
	}
}
