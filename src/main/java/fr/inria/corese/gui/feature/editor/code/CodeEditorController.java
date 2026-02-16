package fr.inria.corese.gui.feature.editor.code;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.RdfConversionService;
import fr.inria.corese.gui.feature.editor.code.support.CodeEditorFileSupport;
import fr.inria.corese.gui.utils.AppExecutors;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Code Editor component.
 *
 * <p>
 * Manages:
 *
 * <ul>
 * <li>The editor life-cycle and state (via {@link CodeEditorModel}).
 * <li>Toolbar actions (Open, Save, Undo, Redo, etc.).
 * <li>File I/O operations.
 * <li>Syntax highlighting detection.
 * </ul>
 */
public class CodeEditorController {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeEditorController.class);

	private final CodeEditorView view;
	private final CodeEditorModel model;
	private final List<String> allowedExtensions;
	private final CodeEditorModeDetector modeDetector;

	/**
	 * Creates a new CodeEditorController.
	 *
	 * @param buttons
	 *            Configuration for toolbar buttons.
	 * @param initialContent
	 *            Initial text content.
	 * @param allowedExtensions
	 *            List of allowed file extensions (e.g. ".ttl", ".rq"). If empty,
	 *            all supported types are allowed.
	 */
	public CodeEditorController(List<ButtonConfig> buttons, String initialContent, List<String> allowedExtensions) {
		this.allowedExtensions = FileTypeSupport.normalizeExtensions(allowedExtensions);
		this.modeDetector = new CodeEditorModeDetector(this.allowedExtensions);
		this.view = new CodeEditorView();
		this.model = new CodeEditorModel();

		initializeToolbar(buttons);
		bindToolbarToModel();

		model.setContent(initialContent);
		Platform.runLater(this::initializeEditorBehavior);
	}

	public CodeEditorController(List<ButtonConfig> buttons, String initialContent) {
		this(buttons, initialContent, List.of());
	}

	// ==============================================================================================
	// Initialization
	// ==============================================================================================

	private void initializeToolbar(List<ButtonConfig> buttons) {
		List<ButtonConfig> config = new ArrayList<>();

		// 1. Process provided buttons (enrich with default actions if missing)
		if (buttons != null) {
			for (ButtonConfig btn : buttons) {
				if (btn.getAction() == null) {
					Runnable defaultAction = getDefaultAction(btn.getIcon());
					if (defaultAction != null) {
						config.add(ButtonFactory.custom(btn.getIcon(), btn.getTooltip(), defaultAction));
					} else {
						config.add(btn);
					}
				} else {
					config.add(btn);
				}
			}
		}

		// Zoom controls in editor toolbar
		config.add(ButtonFactory.zoomIn(this::zoomIn));
		config.add(ButtonFactory.zoomOut(this::zoomOut));

		view.getToolbarWidget().setButtons(config);
	}

	private void initializeEditorBehavior() {
		// Two-way binding for content
		view.getCodeMirrorView().contentProperty().bindBidirectional(model.contentProperty());

		// Mode detection triggers
		model.filePathProperty().addListener((obs, oldVal, newVal) -> detectAndSetMode());
		model.contentProperty().addListener((obs, oldVal, newVal) -> {
			if (model.getFilePath() == null) {
				detectAndSetMode();
			}
		});

		detectAndSetMode();
	}

	private void bindToolbarToModel() {
		ToolbarWidget toolbar = view.getToolbarWidget();

		BooleanBinding isEmpty = Bindings.createBooleanBinding(() -> {
			String s = model.getContent();
			return s == null || s.trim().isEmpty();
		}, model.contentProperty());

		// Bind Save
		Button saveBtn = toolbar.getButton(ButtonIcon.SAVE);
		if (saveBtn != null) {
			// Enable save if modified OR if file is new (never saved) and has content?
			// Logic: Disable if NOT (modified AND not empty) -> Enable if modified AND not
			// empty.
			// Simplified: Enable if modified.
			saveBtn.disableProperty().bind(model.modifiedProperty().not().or(isEmpty));
		}

		// Bind Export
		toolbar.setButtonDisabled(ButtonIcon.EXPORT, isEmpty.get());

		// Listen to isEmpty for dynamic updates
		isEmpty.addListener((obs, old, empty) -> toolbar.setButtonDisabled(ButtonIcon.EXPORT, empty));

		// Bind Undo/Redo
		model.contentProperty().addListener((obs, o, n) -> updateUndoRedoState());
		updateUndoRedoState();
	}

	private void updateUndoRedoState() {
		ToolbarWidget toolbar = view.getToolbarWidget();
		toolbar.setButtonDisabled(ButtonIcon.UNDO, !model.canUndo());
		toolbar.setButtonDisabled(ButtonIcon.REDO, !model.canRedo());
	}

	// ==============================================================================================
	// Mode Detection
	// ==============================================================================================

	private void detectAndSetMode() {
		view.getCodeMirrorView().setMode(modeDetector.resolve(model.getFilePath(), model.getContent()));
	}

	// ==============================================================================================
	// Actions
	// ==============================================================================================

	private Runnable getDefaultAction(ButtonIcon type) {
		if (type == null)
			return null;
		return switch (type) {
			case SAVE -> this::saveFile;
			case OPEN_FILE -> this::openFile;
			case EXPORT -> this::exportContent;
			case IMPORT -> this::importFile;
			case UNDO -> this::undo;
			case REDO -> this::redo;
			case ZOOM_IN -> this::zoomIn;
			case ZOOM_OUT -> this::zoomOut;
			default -> null; // Other actions (like Documentation) handled externally or not implemented
		};
	}

	private void openFile() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open File");
		FileDialogState.applyInitialDirectory(chooser);

		CodeEditorFileSupport.addOpenFilters(chooser, allowedExtensions);

		File file = chooser.showOpenDialog(view.getRoot().getScene().getWindow());
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading("File Open",
					"Opening " + file.getName() + "...");
			AppExecutors.execute(() -> {
				try {
					String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
					Platform.runLater(() -> loadingHandle.closeThen(() -> {
						model.setContent(content);
						model.setFilePath(file.getAbsolutePath());
						model.markAsSaved();
					}));
				} catch (IOException e) {
					LOGGER.error("Failed to open file", e);
					Platform.runLater(() -> loadingHandle.closeThen(() -> ModalService.getInstance()
							.showException("Error Opening File", "Could not open file.", e)));
				}
			});
		}
	}

	private void importFile() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Import File");
		FileDialogState.applyInitialDirectory(chooser);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md", "*.*"));

		File file = chooser.showOpenDialog(view.getRoot().getScene().getWindow());
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading("Import",
					"Importing " + file.getName() + "...");
			AppExecutors.execute(() -> {
				try {
					String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
					Platform.runLater(() -> loadingHandle.closeThen(() -> {
						// Append or Replace? Standard import usually replaces content in editors unless
						// "Insert"
						model.setContent(content);
						NotificationWidget.getInstance().showSuccess("Imported file: " + file.getName() + ".");
					}));
				} catch (IOException e) {
					LOGGER.error("Failed to import file", e);
					Platform.runLater(() -> loadingHandle.closeThen(() -> NotificationWidget.getInstance()
							.showErrorWithDetails("Import Error", "Import failed: " + e.getMessage(), e)));
				}
			});
		}
	}

	public void saveFile() {
		if (model.getFilePath() == null) {
			saveFileAs();
		} else {
			File file = new File(model.getFilePath());
			writeToFile(file, true);
		}
	}

	public void saveFileAs() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save File As");

		chooser.setInitialFileName(
				CodeEditorFileSupport.resolveDefaultBaseName(model.getFilePath(), allowedExtensions, false));
		FileDialogState.applyInitialDirectory(chooser, model.getFilePath());
		String preferredExtension = CodeEditorFileSupport.resolvePreferredSaveExtension(model.getFilePath(),
				model.getContent(), allowedExtensions, modeDetector);
		CodeEditorFileSupport.addSaveFilters(chooser, allowedExtensions, preferredExtension, true);
		CodeEditorFileSupport.selectDefaultSaveFilter(chooser, preferredExtension);

		File file = chooser.showSaveDialog(view.getRoot().getScene().getWindow());
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			// Enforce extension if selected filter implies one
			file = enforceExtension(file, chooser.getSelectedExtensionFilter());
			writeToFile(file, true);
		}
	}

	private File enforceExtension(File file, FileChooser.ExtensionFilter filter) {
		if (filter != null && !filter.getExtensions().isEmpty()) {
			String ext = filter.getExtensions().get(0).replace("*", ""); // e.g. ".ttl"
			if (!file.getName().toLowerCase().endsWith(ext)) {
				return new File(file.getAbsolutePath() + ext);
			}
		}
		return file;
	}

	private void writeToFile(File file, boolean updateModel) {
		String contentSnapshot = model.getContent();
		writeToFile(file, contentSnapshot, updateModel);
	}

	private void writeToFile(File file, String content, boolean updateModel) {
		String contentSnapshot = content != null ? content : "";
		String loadingTitle = updateModel ? "Save" : "Export";
		String loadingMessage = (updateModel ? "Saving " : "Exporting ") + file.getName() + "...";
		NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading(loadingTitle,
				loadingMessage);
		AppExecutors.execute(() -> {
			try {
				Files.writeString(file.toPath(), contentSnapshot, StandardCharsets.UTF_8);
				Platform.runLater(() -> loadingHandle.closeThen(() -> {
					if (updateModel) {
						model.setFilePath(file.getAbsolutePath());
						if (contentSnapshot.equals(model.getContent())) {
							model.markAsSaved();
						}
						NotificationWidget.getInstance().showSuccess("Saved file: " + file.getName() + ".");
					} else {
						NotificationWidget.getInstance().showSuccess("Exported file: " + file.getName() + ".");
					}
				}));
			} catch (IOException e) {
				LOGGER.error("Save failed", e);
				Platform.runLater(() -> loadingHandle.closeThen(() -> ModalService.getInstance()
						.showException("Save Error", "Could not save file: " + e.getMessage(), e)));
			}
		});
	}

	private void exportContent() {
		String content = model.getContent();
		if (content == null || content.isBlank()) {
			NotificationWidget.getInstance().showWarning("No content to export.");
			return;
		}

		if (CodeEditorFileSupport.isGraphEditor(allowedExtensions)) {
			exportGraphContent(content);
			return;
		}

		FileChooser chooser = new FileChooser();
		chooser.setTitle("Export File");
		chooser.setInitialFileName(
				CodeEditorFileSupport.resolveDefaultBaseName(model.getFilePath(), allowedExtensions, true));
		FileDialogState.applyInitialDirectory(chooser, model.getFilePath());
		String preferredExtension = CodeEditorFileSupport.resolvePreferredSaveExtension(model.getFilePath(),
				model.getContent(), allowedExtensions, modeDetector);
		CodeEditorFileSupport.addSaveFilters(chooser, allowedExtensions, preferredExtension, false);
		CodeEditorFileSupport.selectDefaultSaveFilter(chooser, preferredExtension);

		File file = chooser.showSaveDialog(view.getRoot().getScene().getWindow());
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			file = enforceExtension(file, chooser.getSelectedExtensionFilter());
			writeToFile(file, false);
		}
	}

	private void exportGraphContent(String content) {
		List<SerializationFormat> formats = CodeEditorFileSupport.graphExportFormats();
		if (formats.isEmpty()) {
			NotificationWidget.getInstance().showWarning("No export format is available.");
			return;
		}

		GraphExportSelection selection = promptGraphExportSelection(formats);
		if (selection == null) {
			return;
		}

		SerializationFormat sourceFormat = CodeEditorFileSupport.resolveSourceGraphFormat(model.getFilePath(),
				model.getContent(), modeDetector);
		if (sourceFormat == null) {
			ModalService.getInstance().showError("Export Error", "Unable to detect the source RDF format.");
			return;
		}

		try {
			String converted = RdfConversionService.getInstance().convertGraphContent(content, sourceFormat,
					selection.format());
			writeToFile(selection.file(), converted, false);
		} catch (Exception e) {
			LOGGER.error("Export conversion failed", e);
			ModalService.getInstance().showException("Export Error", e.getMessage(), e);
		}
	}

	private GraphExportSelection promptGraphExportSelection(List<SerializationFormat> formats) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Export Graph");

		String preferredExt = CodeEditorFileSupport.resolvePreferredSaveExtension(model.getFilePath(),
				model.getContent(), allowedExtensions, modeDetector);
		chooser.setInitialFileName(
				CodeEditorFileSupport.resolveDefaultBaseName(model.getFilePath(), allowedExtensions, true));
		FileDialogState.applyInitialDirectory(chooser, model.getFilePath());

		Map<FileChooser.ExtensionFilter, SerializationFormat> filterMap = populateExportFilters(chooser, formats);
		selectPreferredFilter(chooser, preferredExt);

		File file = chooser.showSaveDialog(view.getRoot().getScene().getWindow());
		if (file == null) {
			return null;
		}
		FileDialogState.updateLastDirectory(file);

		SerializationFormat targetFormat = resolveTargetFormat(chooser, filterMap, formats, file);
		File finalFile = resolveExportFile(file, chooser, targetFormat);
		return new GraphExportSelection(finalFile, targetFormat);
	}

	private Map<FileChooser.ExtensionFilter, SerializationFormat> populateExportFilters(FileChooser chooser,
			List<SerializationFormat> formats) {
		Map<FileChooser.ExtensionFilter, SerializationFormat> filterMap = new LinkedHashMap<>();
		for (SerializationFormat format : formats) {
			String ext = format.getExtension();
			FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(format.getLabel() + " (*" + ext + ")",
					"*" + ext);
			chooser.getExtensionFilters().add(filter);
			filterMap.put(filter, format);
		}
		return filterMap;
	}

	private void selectPreferredFilter(FileChooser chooser, String preferredExt) {
		if (preferredExt == null) {
			return;
		}
		for (FileChooser.ExtensionFilter filter : chooser.getExtensionFilters()) {
			if (filter.getExtensions().contains("*" + preferredExt)) {
				chooser.setSelectedExtensionFilter(filter);
				return;
			}
		}
	}

	private SerializationFormat resolveTargetFormat(FileChooser chooser,
			Map<FileChooser.ExtensionFilter, SerializationFormat> filterMap, List<SerializationFormat> formats,
			File file) {
		SerializationFormat targetFormat = filterMap.get(chooser.getSelectedExtensionFilter());
		if (targetFormat != null) {
			return targetFormat;
		}
		targetFormat = SerializationFormat.forExtension(CodeEditorFileSupport.extractExtension(file.getName()));
		return targetFormat != null ? targetFormat : formats.get(0);
	}

	private File resolveExportFile(File file, FileChooser chooser, SerializationFormat targetFormat) {
		File finalFile = enforceExtension(file, chooser.getSelectedExtensionFilter());
		if (chooser.getSelectedExtensionFilter() == null && targetFormat != null
				&& !file.getName().toLowerCase().endsWith(targetFormat.getExtension())) {
			return new File(file.getAbsolutePath() + targetFormat.getExtension());
		}
		return finalFile;
	}

	private record GraphExportSelection(File file, SerializationFormat format) {
	}

	private void undo() {
		model.undo();
	}

	private void redo() {
		model.redo();
	}

	private void zoomIn() {
		view.zoomIn();
	}

	private void zoomOut() {
		view.zoomOut();
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Gets the editor model. Note: Exposed to allow data binding and state
	 * management by parent controllers.
	 *
	 * @return The model.
	 */
	public CodeEditorModel getModel() {
		return model;
	}

	/**
	 * Gets the current text content.
	 *
	 * @return The text content.
	 */
	public String getContent() {
		return model.getContent();
	}

	/**
	 * Sets the current text content.
	 *
	 * @param content
	 *            The new content.
	 */
	public void setContent(String content) {
		model.setContent(content);
	}

	/**
	 * Disables or enables the editor view.
	 *
	 * @param disable
	 *            True to disable, false to enable.
	 */
	public void setDisable(boolean disable) {
		view.getRoot().setDisable(disable);
	}

	/**
	 * Requests keyboard focus for the underlying editor.
	 */
	public void requestEditorFocus() {
		view.requestEditorFocus();
	}

	/** Cleans up resources. */
	public void dispose() {
		view.getCodeMirrorView().contentProperty().unbindBidirectional(model.contentProperty());
		view.getCodeMirrorView().close();
	}

	public javafx.scene.Node getViewRoot() {
		return view.getRoot();
	}
}
