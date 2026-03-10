package fr.inria.corese.gui.feature.validation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.config.ResultViewConfig;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.model.ValidationResult;
import fr.inria.corese.gui.core.shortcut.KeyboardShortcutRegistry;
import fr.inria.corese.gui.core.dialog.ModalService;
import fr.inria.corese.gui.feature.editor.code.CodeEditorController;
import fr.inria.corese.gui.feature.editor.tab.TabContext;
import fr.inria.corese.gui.feature.editor.tab.TabEditorConfig;
import fr.inria.corese.gui.feature.editor.tab.TabEditorController;
import fr.inria.corese.gui.feature.query.support.QueryExecutionCancellationSupport;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.feature.validation.support.ValidationResultRenderSupport;
import fr.inria.corese.gui.feature.validation.support.ValidationResultTabPreferenceSupport;
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
	private static final String VALIDATION_NOTIFICATION_TITLE = "Validation";
	private static final String VALIDATION_EXECUTING_MESSAGE = "Running SHACL validation...";
	private static final String VALIDATION_RENDERING_MESSAGE = "Rendering validation report...";
	private static final String VALIDATION_STOP_ACTION_LABEL = "Stop";
	private static final String MSG_VALIDATION_ALREADY_RUNNING = "A validation is already running in this tab.";
	private static final String MSG_VALIDATION_CANCELLED = "Validation cancelled.";
	private static final String MSG_NO_VALIDATION_RESULT = "Validation failed: no result was returned.";
	private static final String DEFAULT_SHACL_TEMPLATE = """
			@prefix sh: <http://www.w3.org/ns/shacl#> .
			@prefix ex: <http://example.com/ns#> .
			@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

			ex:ExampleShape
			    a sh:NodeShape ;
			    sh:targetClass ex:Person ;
			    sh:property [
			        sh:path ex:name ;
			        sh:datatype xsd:string ;
			        sh:minCount 1 ;
			    ] .
			""";

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private final ValidationView view;
	private final Map<Tab, ValidationModel> tabModels = new HashMap<>();
	private final ValidationResultTabPreferenceSupport tabPreferenceSupport = new ValidationResultTabPreferenceSupport();
	private TabEditorController tabEditorController;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	public ValidationController(ValidationView view) {
		this.view = view;
		this.view.setController(this);
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
		Node emptyState = view.createEmptyState(this::onNewTabButtonClick, this::onOpenFileButtonClick,
				this::onTemplateButtonClick);

		// Build configuration
		TabEditorConfig config = TabEditorConfig.builder()
				.withEditorButtons(List.of(ButtonFactory.save(), ButtonFactory.export(), ButtonFactory.undo(),
						ButtonFactory.redo()))
				.withExecution(ButtonFactory.custom(ButtonIcon.PLAY, view.getRunValidationLabel(),
						KeyboardShortcutRegistry.Action.EXECUTE_PRIMARY_ACTION), this::executeValidation)
				.withResultView(view.getResultToolbarButtons(),
						ResultViewConfig.builder().withTextTab().withGraphTab().build())
				.withEmptyState(emptyState).withAllowedExtensions(FileTypeSupport.rdfExtensions())
				.withOpenFileAction(this::onOpenFileButtonClick).withTemplateAction(this::onTemplateButtonClick)
				.build();

		// Create controller
		tabEditorController = new TabEditorController(config);
	}

	private void setupViewIntegration() {
		// Embed the editor's view into the main ValidationView
		view.setMainContent(tabEditorController.getViewRoot());

		tabEditorController
				.addSelectionListener((obs, oldTab, newTab) -> updateResultsForSelectedValidationTab(newTab, false));

		// Manage model lifecycle and result tab preference listeners for opened tabs.
		tabEditorController.addTabListener(this::onTabsChanged);
		for (Tab tab : tabEditorController.getTabs()) {
			registerResultTabPreference(tab);
		}
	}

	// ==============================================================================================
	// Public Actions (User Interactions)
	// ==============================================================================================

	public void executeValidation() {
		Tab selectedTab = tabEditorController.getSelectedTab();
		if (selectedTab == null) {
			return;
		}

		TabContext context = TabContext.get(selectedTab);
		if (context == null) {
			return;
		}
		if (context.isExecutionRunning()) {
			NotificationWidget.getInstance().showInfo(VALIDATION_NOTIFICATION_TITLE, MSG_VALIDATION_ALREADY_RUNNING);
			return;
		}

		ResultController resultController = context.getResultController();
		if (resultController == null) {
			return;
		}

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
		setExecutionState(context, true);
		AtomicBoolean cancellationRequested = new AtomicBoolean(false);
		AtomicBoolean completionSignaled = new AtomicBoolean(false);
		AtomicReference<Future<?>> futureReference = new AtomicReference<>();
		AtomicReference<NotificationWidget.LoadingHandle> loadingHandleReference = new AtomicReference<>();

		Runnable stopAction = () -> onValidationCancellationRequested(context, cancellationRequested,
				completionSignaled, futureReference, loadingHandleReference.get());
		NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading(
				VALIDATION_NOTIFICATION_TITLE, VALIDATION_EXECUTING_MESSAGE, VALIDATION_STOP_ACTION_LABEL, stopAction);
		loadingHandleReference.set(loadingHandle);

		// Execute validation asynchronously
		Future<?> executionFuture = AppExecutors.submit(() -> runValidationTask(selectedTab, context, model,
				shapesContent, loadingHandle, cancellationRequested, completionSignaled));
		QueryExecutionCancellationSupport.bindFuture(cancellationRequested, futureReference, executionFuture);
	}

	// ==============================================================================================
	// Background Task & Callbacks
	// ==============================================================================================

	private void runValidationTask(Tab sourceTab, TabContext context, ValidationModel model, String shapesContent,
			NotificationWidget.LoadingHandle loadingHandle, AtomicBoolean cancellationRequested,
			AtomicBoolean completionSignaled) {
		try {
			ValidationResult result = model.validate(shapesContent);
			Platform.runLater(() -> onValidationCompleted(sourceTab, context, model, result, loadingHandle,
					cancellationRequested, completionSignaled));
		} catch (Exception e) {
			handleValidationFailure(context, loadingHandle, cancellationRequested, completionSignaled, e);
		}
	}

	private void onValidationCompleted(Tab sourceTab, TabContext context, ValidationModel model,
			ValidationResult result, NotificationWidget.LoadingHandle loadingHandle,
			AtomicBoolean cancellationRequested, AtomicBoolean completionSignaled) {
		if (cancellationRequested.get()) {
			model.discardResult(result);
			completeExecution(context, loadingHandle, completionSignaled, null);
			return;
		}

		completeExecution(context, loadingHandle, completionSignaled, () -> {
			if (result == null) {
				if (isTabSelected(sourceTab)) {
					tabEditorController.hideResultPane();
				}
				ModalService.getInstance().showError("Validation Error", MSG_NO_VALIDATION_RESULT);
				return;
			}

			if (!isTabOpen(sourceTab)) {
				model.discardResult(result);
				return;
			}

			if (result.getErrorMessage() != null && !result.getErrorMessage().isBlank()) {
				if (isTabSelected(sourceTab)) {
					tabEditorController.hideResultPane();
				}
				if (result.getErrorDetails() != null && !result.getErrorDetails().isBlank()) {
					ModalService.getInstance().showError("Validation Error", result.getErrorMessage(),
							result.getErrorDetails());
				} else {
					ModalService.getInstance().showError("Validation Error", result.getErrorMessage());
				}
				return;
			}

			if (isTabSelected(sourceTab)) {
				updateResultsForSelectedValidationTab(sourceTab, true);
			}
		});
	}

	private void handleValidationFailure(TabContext context, NotificationWidget.LoadingHandle loadingHandle,
			AtomicBoolean cancellationRequested, AtomicBoolean completionSignaled, Exception error) {
		boolean cancellationLike = QueryExecutionCancellationSupport.shouldTreatAsCancellation(cancellationRequested,
				error);
		Platform.runLater(() -> {
			if (cancellationLike) {
				completeExecution(context, loadingHandle, completionSignaled, () -> NotificationWidget.getInstance()
						.showInfo(VALIDATION_NOTIFICATION_TITLE, MSG_VALIDATION_CANCELLED));
				return;
			}
			completeExecution(context, loadingHandle, completionSignaled, () -> ModalService.getInstance()
					.showException("Validation Error", buildValidationErrorMessage(error), error));
		});
		if (cancellationLike) {
			LOGGER.debug("SHACL validation cancelled");
			return;
		}
		LOGGER.error("Error during validation", error);
	}

	private void onValidationCancellationRequested(TabContext context, AtomicBoolean cancellationRequested,
			AtomicBoolean completionSignaled, AtomicReference<Future<?>> futureReference,
			NotificationWidget.LoadingHandle loadingHandle) {
		boolean cancelRequested = QueryExecutionCancellationSupport.requestCancellation(cancellationRequested,
				futureReference);
		if (!cancelRequested) {
			return;
		}
		completeExecution(context, loadingHandle, completionSignaled, () -> NotificationWidget.getInstance()
				.showInfo(VALIDATION_NOTIFICATION_TITLE, MSG_VALIDATION_CANCELLED));
	}

	private void completeExecution(TabContext context, NotificationWidget.LoadingHandle loadingHandle,
			AtomicBoolean completionSignaled, Runnable followUp) {
		if (!completionSignaled.compareAndSet(false, true)) {
			return;
		}
		Runnable completion = () -> {
			setExecutionState(context, false);
			if (followUp != null) {
				followUp.run();
			}
		};
		if (loadingHandle == null) {
			completion.run();
			return;
		}
		loadingHandle.closeThen(completion);
	}

	private void updateResultsForSelectedValidationTab(Tab selectedValidationTab, boolean forceRefresh) {
		if (selectedValidationTab == null) {
			return;
		}

		TabContext context = TabContext.get(selectedValidationTab);
		if (context == null) {
			return;
		}
		ResultController resultController = context.getResultController();
		if (resultController == null) {
			return;
		}

		ValidationModel model = tabModels.get(selectedValidationTab);
		if (model == null) {
			return;
		}
		ValidationResult result = model.getLastResult();
		if (result == null || (result.getErrorMessage() != null && !result.getErrorMessage().isBlank())) {
			return;
		}
		if (!forceRefresh && model.isResultRendered(result)) {
			return;
		}

		resultController.clearResults();
		tabEditorController.showResultPane();

		resultController.configureTabsForResult(true, false, true, tabPreferenceSupport.preferredTab());

		SerializationFormat[] formats = SerializationFormat.rdfFormats();
		resultController.configureTextFormats(formats, SerializationFormat.TURTLE);
		SerializationFormat preferredFormat = resultController.getPreferredTextFormat(formats,
				SerializationFormat.TURTLE);

		NotificationWidget.LoadingHandle renderLoading = NotificationWidget.getInstance()
				.showLoading(VALIDATION_NOTIFICATION_TITLE, VALIDATION_RENDERING_MESSAGE);
		ValidationResultRenderSupport.loadGraphAndTextAsync(resultController, model, preferredFormat,
				renderLoading::close);
		ValidationResultRenderSupport.bindOnFormatChanged(resultController, model);
		model.markResultRendered(result);
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
			if (change.wasAdded()) {
				for (Tab tab : change.getAddedSubList()) {
					registerResultTabPreference(tab);
				}
			}
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

	private void registerResultTabPreference(Tab tab) {
		TabContext context = TabContext.get(tab);
		if (context == null) {
			return;
		}
		ResultController resultController = context.getResultController();
		if (resultController == null) {
			return;
		}
		resultController.setOnTabSelected(tabPreferenceSupport::rememberPreferredTab);
	}

	private boolean isTabOpen(Tab tab) {
		return tab != null && tabEditorController.getTabs().contains(tab);
	}

	private boolean isTabSelected(Tab tab) {
		return tab != null && tab.equals(tabEditorController.getSelectedTab());
	}

	private static void setExecutionState(TabContext context, boolean running) {
		if (context == null) {
			return;
		}
		if (Platform.isFxApplicationThread()) {
			context.executionRunningProperty().set(running);
			return;
		}
		Platform.runLater(() -> context.executionRunningProperty().set(running));
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

	private void onTemplateButtonClick() {
		insertTemplateInNewTab(DEFAULT_SHACL_TEMPLATE);
	}

	public TabEditorController getTabEditorController() {
		return tabEditorController;
	}

	public boolean createTabFromShortcut() {
		tabEditorController.createNewTab();
		return true;
	}

	public boolean closeTabFromShortcut() {
		return tabEditorController.closeSelectedTab();
	}

	public boolean selectNextTabFromShortcut() {
		return tabEditorController.selectNextTab();
	}

	public boolean selectPreviousTabFromShortcut() {
		return tabEditorController.selectPreviousTab();
	}

	public boolean executeFromShortcut() {
		executeValidation();
		return true;
	}

	public boolean focusEditorFromShortcut() {
		return tabEditorController.focusSelectedEditor();
	}

	public boolean openFileFromShortcut() {
		onOpenFileButtonClick();
		return true;
	}

	public boolean openTemplateFromShortcut() {
		insertTemplateInNewTab(DEFAULT_SHACL_TEMPLATE);
		return true;
	}

	public boolean saveFromShortcut() {
		return tabEditorController.saveSelectedEditorFromShortcut();
	}

	public boolean exportFromShortcut() {
		return tabEditorController.exportSelectedContextFromShortcut();
	}

	public boolean exportGraphFromShortcut() {
		return tabEditorController.exportSelectedGraphFromShortcut();
	}

	public boolean reenergizeGraphFromShortcut() {
		return tabEditorController.reenergizeSelectedGraphFromShortcut();
	}

	public boolean centerGraphFromShortcut() {
		return tabEditorController.centerSelectedGraphFromShortcut();
	}

	private void insertTemplateInNewTab(String content) {
		if (content == null || content.isBlank()) {
			return;
		}
		Tab tab = tabEditorController.createNewTab();
		CodeEditorController editorController = tabEditorController.getEditorControllerForTab(tab);
		if (editorController == null) {
			return;
		}
		editorController.setContent(content);
	}
}
