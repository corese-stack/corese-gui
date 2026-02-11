package fr.inria.corese.gui.feature.editor.code;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.DefaultFileNameResolver;
import fr.inria.corese.gui.core.io.FileDialogState;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.service.RdfConversionService;
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
		SerializationFormat format = SerializationFormat.TEXT;
		String path = model.getFilePath();
		String content = model.getContent();

		if (path != null) {
			format = detectModeFromExtension(path);
		} else if (content != null) {
			format = detectModeFromContent(content);
		}

		view.getCodeMirrorView().setMode(format);
	}

	private SerializationFormat detectModeFromExtension(String path) {
		if (path == null)
			return SerializationFormat.TEXT;

		String extension = FileTypeSupport.extractExtension(path);
		if (extension == null) {
			return SerializationFormat.TEXT;
		}

		// Use the Enum's built-in lookup
		SerializationFormat format = SerializationFormat.forExtension(extension);

		// Fallback if not found or restricted
		if (format == null || !isModeAllowed(format)) {
			return SerializationFormat.TEXT;
		}

		return format;
	}

	private SerializationFormat detectModeFromContent(String content) {
		String lower = content.toLowerCase();
		String trimmed = content.trim();

		SerializationFormat format = detectSparqlFormat(lower);
		if (format != null) {
			return format;
		}

		format = detectTurtleOrTrigFormat(trimmed, lower);
		if (format != null) {
			return format;
		}

		format = detectNTriplesOrQuadsFormat(trimmed);
		if (format != null) {
			return format;
		}

		format = detectJsonFormat(trimmed);
		if (format != null) {
			return format;
		}

		format = detectXmlFormat(trimmed);
		return format != null ? format : SerializationFormat.TEXT;
	}

	private SerializationFormat detectSparqlFormat(String lower) {
		if (!isModeAllowed(SerializationFormat.SPARQL_QUERY)) {
			return null;
		}

		String normalized = " " + lower.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ') + " ";
		boolean looksLikeSparql = normalized.contains(" select ") || normalized.contains(" construct ")
				|| normalized.contains(" ask ") || normalized.contains(" describe ") || normalized.contains(" prefix ")
				|| normalized.contains(" base ") || normalized.contains(" insert ") || normalized.contains(" delete ")
				|| normalized.contains(" load ") || normalized.contains(" clear ") || normalized.contains(" create ")
				|| normalized.contains(" drop ") || normalized.contains(" move ") || normalized.contains(" copy ")
				|| normalized.contains(" add ") || normalized.contains(" with ") || normalized.contains(" using ");
		return looksLikeSparql ? SerializationFormat.SPARQL_QUERY : null;
	}

	private SerializationFormat detectTurtleOrTrigFormat(String trimmed, String lower) {
		if (!(isModeAllowed(SerializationFormat.TURTLE) || isModeAllowed(SerializationFormat.TRIG))) {
			return null;
		}
		boolean looksLikeTurtle = lower.contains("@prefix") || lower.contains("@base") || lower.contains(" a ")
				|| trimmed.endsWith(".");
		if (!looksLikeTurtle) {
			return null;
		}
		boolean trigLike = isModeAllowed(SerializationFormat.TRIG) && looksLikeTrig(trimmed, lower);
		if (trigLike) {
			return SerializationFormat.TRIG;
		}
		return isModeAllowed(SerializationFormat.TURTLE) ? SerializationFormat.TURTLE : SerializationFormat.TRIG;
	}

	private SerializationFormat detectNTriplesOrQuadsFormat(String trimmed) {
		if (!(isModeAllowed(SerializationFormat.N_TRIPLES) || isModeAllowed(SerializationFormat.N_QUADS))) {
			return null;
		}
		if (!looksLikeNTriplesOrQuads(trimmed)) {
			return null;
		}
		return isModeAllowed(SerializationFormat.N_QUADS) ? SerializationFormat.N_QUADS : SerializationFormat.N_TRIPLES;
	}

	private SerializationFormat detectJsonFormat(String trimmed) {
		if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
			return null;
		}
		if (!(isModeAllowed(SerializationFormat.JSON_LD) || isModeAllowed(SerializationFormat.JSON))) {
			return null;
		}
		if (isModeAllowed(SerializationFormat.JSON_LD) && looksLikeJsonLd(trimmed)) {
			return SerializationFormat.JSON_LD;
		}
		if (isModeAllowed(SerializationFormat.JSON)) {
			return SerializationFormat.JSON;
		}
		return SerializationFormat.JSON_LD;
	}

	private SerializationFormat detectXmlFormat(String trimmed) {
		if (!trimmed.startsWith("<")) {
			return null;
		}
		if (isModeAllowed(SerializationFormat.RDF_XML)) {
			return SerializationFormat.RDF_XML;
		}
		if (isModeAllowed(SerializationFormat.XML)) {
			return SerializationFormat.XML;
		}
		return null;
	}

	private boolean looksLikeJsonLd(String trimmed) {
		String lower = trimmed.toLowerCase();
		return lower.contains("\"@context\"") || lower.contains("\"@id\"") || lower.contains("\"@graph\"");
	}

	private boolean looksLikeTrig(String trimmed, String lower) {
		if (trimmed.isEmpty()) {
			return false;
		}
		return lower.contains("graph ") || (trimmed.contains("{") && trimmed.contains("}"));
	}

	private boolean looksLikeNTriplesOrQuads(String trimmed) {
		if (trimmed.isEmpty()) {
			return false;
		}
		String firstLine = trimmed.split("\n", 2)[0].trim();
		if (firstLine.isEmpty()) {
			return false;
		}
		boolean startsLikeTriple = firstLine.startsWith("<") || firstLine.startsWith("_:");
		boolean endsWithDot = firstLine.endsWith(".");
		return startsLikeTriple && endsWithDot && !firstLine.contains("{") && !firstLine.contains("}");
	}

	private boolean isModeAllowed(SerializationFormat format) {
		// If no restriction, everything is allowed
		if (allowedExtensions.isEmpty())
			return true;
		if (format == null)
			return false;

		// Check if the format's extension is in the allowed list
		// Also consider related extensions (e.g. .ttl for TURTLE)
		// For simplicity, we check if the format's primary extension is allowed
		// Or if any of the allowed extensions map to this format

		// Check main extension
		if (allowedExtensions.contains(format.getExtension()))
			return true;

		// Check if any allowed extension maps to this format
		for (String ext : allowedExtensions) {
			SerializationFormat f = SerializationFormat.forExtension(ext);
			if (f == format)
				return true;
		}

		return false;
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

		addOpenFilters(chooser);

		File file = chooser.showOpenDialog(view.getRoot().getScene().getWindow());
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			AppExecutors.execute(() -> {
				try {
					String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
					Platform.runLater(() -> {
						model.setContent(content);
						model.setFilePath(file.getAbsolutePath());
						model.markAsSaved();
					});
				} catch (IOException e) {
					LOGGER.error("Failed to open file", e);
					Platform.runLater(() -> ModalService.getInstance().showError("Error Opening File", e.getMessage()));
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
			AppExecutors.execute(() -> {
				try {
					String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
					Platform.runLater(() -> {
						// Append or Replace? Standard import usually replaces content in editors unless
						// "Insert"
						model.setContent(content);
						NotificationWidget.getInstance().showSuccess("Imported file: " + file.getName() + ".");
					});
				} catch (IOException e) {
					LOGGER.error("Failed to import file", e);
					Platform.runLater(
							() -> NotificationWidget.getInstance().showError("Import failed: " + e.getMessage()));
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

		chooser.setInitialFileName(resolveDefaultBaseName(false));
		FileDialogState.applyInitialDirectory(chooser, model.getFilePath());
		addSaveFilters(chooser, true);
		selectDefaultSaveFilter(chooser);

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
		AppExecutors.execute(() -> {
			try {
				Files.writeString(file.toPath(), contentSnapshot, StandardCharsets.UTF_8);
				Platform.runLater(() -> {
					if (updateModel) {
						model.setFilePath(file.getAbsolutePath());
						if (contentSnapshot.equals(model.getContent())) {
							model.markAsSaved();
						}
						NotificationWidget.getInstance().showSuccess("Saved file: " + file.getName() + ".");
					} else {
						NotificationWidget.getInstance().showSuccess("Exported file: " + file.getName() + ".");
					}
				});
			} catch (IOException e) {
				LOGGER.error("Save failed", e);
				Platform.runLater(() -> ModalService.getInstance().showError("Save Error",
						"Could not save file: " + e.getMessage()));
			}
		});
	}

	private void exportContent() {
		String content = model.getContent();
		if (content == null || content.isBlank()) {
			NotificationWidget.getInstance().showWarning("No content to export.");
			return;
		}

		if (isGraphEditor()) {
			exportGraphContent(content);
			return;
		}

		FileChooser chooser = new FileChooser();
		chooser.setTitle("Export File");
		chooser.setInitialFileName(resolveDefaultBaseName(true));
		FileDialogState.applyInitialDirectory(chooser, model.getFilePath());
		addSaveFilters(chooser, false);
		selectDefaultSaveFilter(chooser);

		File file = chooser.showSaveDialog(view.getRoot().getScene().getWindow());
		if (file != null) {
			FileDialogState.updateLastDirectory(file);
			file = enforceExtension(file, chooser.getSelectedExtensionFilter());
			writeToFile(file, false);
		}
	}

	private void exportGraphContent(String content) {
		List<SerializationFormat> formats = getGraphExportFormats();
		if (formats.isEmpty()) {
			NotificationWidget.getInstance().showWarning("No export format is available.");
			return;
		}

		GraphExportSelection selection = promptGraphExportSelection(formats);
		if (selection == null) {
			return;
		}

		SerializationFormat sourceFormat = resolveSourceGraphFormat();
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
			ModalService.getInstance().showError("Export Error", e.getMessage());
		}
	}

	private GraphExportSelection promptGraphExportSelection(List<SerializationFormat> formats) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Export Graph");

		String preferredExt = resolvePreferredSaveExtension();
		chooser.setInitialFileName(resolveDefaultBaseName(true));
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
		targetFormat = SerializationFormat.forExtension(extractExtension(file.getName()));
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

	private SerializationFormat resolveSourceGraphFormat() {
		SerializationFormat fromPath = SerializationFormat.forExtension(extractExtension(model.getFilePath()));
		if (fromPath != null) {
			return fromPath;
		}
		SerializationFormat fromContent = detectModeFromContent(model.getContent());
		if (fromContent != null && fromContent != SerializationFormat.TEXT) {
			return fromContent;
		}
		return SerializationFormat.TURTLE;
	}

	private List<SerializationFormat> getGraphExportFormats() {
		return new ArrayList<>(List.of(SerializationFormat.rdfFormats()));
	}

	private void addOpenFilters(FileChooser chooser) {
		List<String> allowed = getNormalizedAllowedExtensions();
		if (allowed.isEmpty()) {
			FileChooser.ExtensionFilter defaultFilter = FileTypeSupport.createExtensionFilter("RDF and SPARQL",
					FileTypeSupport.defaultEditorOpenExtensions(), true);
			chooser.getExtensionFilters().add(defaultFilter);
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
			chooser.setSelectedExtensionFilter(defaultFilter);
			return;
		}

		FileChooser.ExtensionFilter allowedFilter = FileTypeSupport.createExtensionFilter("Allowed Files", allowed,
				true);
		chooser.getExtensionFilters().add(allowedFilter);
		chooser.setSelectedExtensionFilter(allowedFilter);
	}

	private void addSaveFilters(FileChooser chooser, boolean restrictToCurrentFormat) {
		List<String> allowed = getNormalizedAllowedExtensions();
		String preferred = resolvePreferredSaveExtension();

		if (restrictToCurrentFormat && preferred != null && !preferred.equals(".txt")
				&& (allowed.isEmpty() || allowed.contains(preferred))) {
			String label = formatLabelForExtension(preferred);
			chooser.getExtensionFilters()
					.add(new FileChooser.ExtensionFilter(label + " (*" + preferred + ")", "*" + preferred));
			return;
		}

		if (allowed.isEmpty()) {
			addDefaultSaveFilters(chooser);
			return;
		}

		for (String ext : allowed) {
			String label = formatLabelForExtension(ext);
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(label + " (*" + ext + ")", "*" + ext));
		}
	}

	private void selectDefaultSaveFilter(FileChooser chooser) {
		if (chooser.getExtensionFilters().isEmpty()) {
			return;
		}
		String preferred = resolvePreferredSaveExtension();
		if (preferred != null) {
			for (FileChooser.ExtensionFilter filter : chooser.getExtensionFilters()) {
				if (filter.getExtensions().contains("*" + preferred)) {
					chooser.setSelectedExtensionFilter(filter);
					return;
				}
			}
		}
		chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().get(0));
	}

	private String resolvePreferredSaveExtension() {
		String fromPath = extractExtension(model.getFilePath());
		if (fromPath == null || fromPath.isBlank()) {
			SerializationFormat format = detectModeFromContent(model.getContent());
			if (format != null) {
				fromPath = format.getExtension();
			}
		}

		List<String> allowed = getNormalizedAllowedExtensions();
		if (!allowed.isEmpty() && (fromPath == null || !allowed.contains(fromPath))) {
			return allowed.get(0);
		}

		return fromPath != null ? fromPath : ".txt";
	}

	private void addDefaultSaveFilters(FileChooser chooser) {
		addSaveFilter(chooser, SerializationFormat.TURTLE.getLabel(), SerializationFormat.TURTLE.getExtension());
		for (String extension : FileTypeSupport.queryExtensions()) {
			addSaveFilter(chooser, SerializationFormat.SPARQL_QUERY.getLabel(), extension);
		}
		addSaveFilter(chooser, SerializationFormat.RDF_XML.getLabel(), SerializationFormat.RDF_XML.getExtension());
		addSaveFilter(chooser, SerializationFormat.JSON_LD.getLabel(), SerializationFormat.JSON_LD.getExtension());
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
	}

	private void addSaveFilter(FileChooser chooser, String label, String extension) {
		String normalizedExtension = FileTypeSupport.normalizeExtension(extension);
		if (normalizedExtension == null) {
			return;
		}
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(label + " (*" + normalizedExtension + ")", "*" + normalizedExtension));
	}

	private String resolveDefaultBaseName(boolean forExport) {
		return DefaultFileNameResolver.editorBaseName(model.getFilePath(), allowedExtensions, forExport);
	}

	private String extractExtension(String path) {
		return FileTypeSupport.extractExtension(path);
	}

	private String formatLabelForExtension(String extension) {
		SerializationFormat format = SerializationFormat.forExtension(extension);
		if (format != null) {
			return format.getLabel();
		}
		String ext = extension.startsWith(".") ? extension.substring(1) : extension;
		return ext.toUpperCase();
	}

	private boolean isGraphEditor() {
		List<String> allowed = getNormalizedAllowedExtensions();
		if (allowed.isEmpty()) {
			return false;
		}
		for (String ext : allowed) {
			SerializationFormat format = SerializationFormat.forExtension(ext);
			if (format != null && format != SerializationFormat.SPARQL_QUERY && format != SerializationFormat.TEXT) {
				return true;
			}
		}
		return false;
	}

	private List<String> getNormalizedAllowedExtensions() {
		return allowedExtensions;
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

	/** Cleans up resources. */
	public void dispose() {
		view.getCodeMirrorView().contentProperty().unbindBidirectional(model.contentProperty());
		view.getCodeMirrorView().close();
	}

	public javafx.scene.Node getViewRoot() {
		return view.getRoot();
	}
}
