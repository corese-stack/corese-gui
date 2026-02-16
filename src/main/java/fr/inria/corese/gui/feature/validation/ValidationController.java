package fr.inria.corese.gui.feature.validation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.model.ValidationResult;
import fr.inria.corese.gui.core.service.ModalService;
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
				.withEditorButtons(List.of(ButtonFactory.save(), ButtonFactory.export(), ButtonFactory.undo(),
						ButtonFactory.redo()))
				.withExecution(ButtonFactory.custom(ButtonIcon.PLAY, view.getRunValidationLabel()),
						this::executeValidation)
				.withResultView(view.getResultToolbarButtons(), ResultViewConfig.builder().withTextTab().build())
				.withEmptyState(emptyState).withAllowedExtensions(FileTypeSupport.rdfExtensions())
				.withOpenFileAction(this::onOpenFileButtonClick).build();

		// Create controller
		tabEditorController = new TabEditorController(config);
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
		NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading("Validation",
				"Running SHACL validation...");

		// Execute validation asynchronously
		AppExecutors.execute(() -> runValidationTask(model, shapesContent, resultController, loadingHandle));
	}

	// ==============================================================================================
	// Background Task & Callbacks
	// ==============================================================================================

	private void runValidationTask(ValidationModel model, String shapesContent, ResultController resultController,
			NotificationWidget.LoadingHandle loadingHandle) {
		try {
			// Perform validation logic
			ValidationResult result = model.validate(shapesContent);

			// Update UI on JavaFX Application Thread
			Platform.runLater(() -> handleValidationResult(result, resultController));
		} catch (Throwable e) { // Broad catch prevents background-thread crashes on malformed shapes
			LOGGER.error("Error during validation", e);
			Platform.runLater(() -> {
				tabEditorController.setExecutionState(false);
				tabEditorController.hideResultPane();
				ModalService.getInstance().showException("Validation Error", buildValidationErrorMessage(e), e);
			});
		} finally {
			if (loadingHandle != null) {
				loadingHandle.close();
			}
		}
	}

	private void handleValidationResult(ValidationResult result, ResultController resultController) {
		tabEditorController.setExecutionState(false);

		if (result == null) {
			tabEditorController.hideResultPane();
			ModalService.getInstance().showError("Validation Error", "Validation failed: no result was returned.");
			return;
		}

		if (result.getErrorMessage() != null && !result.getErrorMessage().isBlank()) {
			// Handle validation errors (e.g., syntax errors in shapes) with query-like
			// modal
			tabEditorController.hideResultPane();
			ModalService.getInstance().showError("Validation Error", result.getErrorMessage());
			return;
		}

		// Success: Display the report
		Tab selectedTab = tabEditorController.getSelectedTab();
		ValidationModel model = selectedTab != null ? tabModels.get(selectedTab) : null;
		if (model == null) {
			tabEditorController.hideResultPane();
			ModalService.getInstance().showError("Validation Error", "Validation context is no longer available.");
			return;
		}

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
		SerializationFormat preferredFormat = resultController.getPreferredTextFormat(formats,
				SerializationFormat.TURTLE);

		// Display initial report using the preferred format
		NotificationWidget.LoadingHandle renderLoading = NotificationWidget.getInstance().showLoading("Validation",
				"Rendering validation report...");
		AppExecutors.execute(() -> {
			try {
				String initialReport = model.formatLastReport(preferredFormat.getLabel());
				if (initialReport != null) {
					Platform.runLater(() -> resultController.updateText(initialReport));
				}
			} finally {
				renderLoading.close();
			}
		});

		// Configure callback for format changes
		resultController.setOnFormatChanged(format -> AppExecutors.execute(() -> {
			NotificationWidget.LoadingHandle formatLoading = NotificationWidget.getInstance().showLoading("Validation",
					"Formatting validation report...");
			try {
				String formattedReport = model.formatLastReport(format.getLabel());
				if (formattedReport != null) {
					Platform.runLater(() -> resultController.updateText(formattedReport));
				}
			} finally {
				formatLoading.close();
			}
		}));
	}

	private static String buildValidationErrorMessage(Throwable throwable) {
		if (throwable == null) {
			return "Validation failed: unknown error.";
		}
		String message = throwable.getMessage();
		Throwable cause = throwable.getCause();
		while ((message == null || message.isBlank()) && cause != null) {
			message = cause.getMessage();
			cause = cause.getCause();
		}
		if (message == null || message.isBlank()) {
			return "Validation failed: " + throwable.getClass().getSimpleName();
		}
		return "Validation failed: " + message;
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
		FileChooser.ExtensionFilter rdfFilter = FileTypeSupport.createExtensionFilter("RDF Files",
				FileTypeSupport.rdfExtensions(), true);
		fileChooser.getExtensionFilters().add(rdfFilter);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
		fileChooser.setSelectedExtensionFilter(rdfFilter);

		List<File> files = fileChooser.showOpenMultipleDialog(
				view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null);
		if (files == null || files.isEmpty()) {
			return;
		}

		FileDialogState.updateLastDirectory(files.get(files.size() - 1));
		for (File file : files) {
			LOGGER.info("Loading SHACL file: {}", file.getAbsolutePath());
			tabEditorController.openFile(file);
		}
	}
}
