package fr.inria.corese.gui.feature.query.template;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.prefs.Preferences;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Query template modal with a simple-first UI and optional advanced controls.
 */
public final class QueryTemplateDialog {

	private static final String STYLESHEET = "/css/features/query-template-dialog.css";
	private static final String STYLE_CLASS_DIALOG = "query-template-dialog";
	private static final String STYLE_CLASS_CONTENT = "query-template-content";
	private static final String STYLE_CLASS_OPTIONS = "query-template-options";
	private static final String STYLE_CLASS_PREVIEW_COLUMN = "query-template-preview-column";
	private static final String STYLE_CLASS_PREVIEW = "query-template-preview";
	private static final String STYLE_CLASS_INLINE_ROW = "query-template-inline-row";
	private static final String STYLE_CLASS_SECTION_TITLE = "query-template-section-title";

	private static final int DEFAULT_LIMIT = 100;
	private static final int DEFAULT_OFFSET = 0;
	private static final int MAX_LIMIT_OFFSET = 1_000_000;

	private static final Preferences PREFS = Preferences.userNodeForPackage(QueryTemplateDialog.class);
	private static final String PREF_TYPE = "queryTemplate.type";
	private static final String PREF_USE_GRAPH = "queryTemplate.useGraphPattern";
	private static final String PREF_USE_DISTINCT = "queryTemplate.useDistinct";
	private static final String PREF_USE_LIMIT = "queryTemplate.useLimit";
	private static final String PREF_LIMIT_VALUE = "queryTemplate.limitValue";
	private static final String PREF_ADVANCED_EXPANDED = "queryTemplate.advancedExpanded";
	private static final String PREF_ORDER_BY = "queryTemplate.orderBySubject";
	private static final String PREF_PATTERN = "queryTemplate.pattern";
	private static final String PREF_USE_OFFSET = "queryTemplate.useOffset";
	private static final String PREF_OFFSET_VALUE = "queryTemplate.offsetValue";

	private QueryTemplateDialog() {
		throw new AssertionError("Utility class");
	}

	public static void show(Consumer<String> onConfirmTemplate) {
		Objects.requireNonNull(onConfirmTemplate, "onConfirmTemplate callback must not be null");

		Form form = new Form();
		restorePreferences(form);

		VBox optionsColumn = buildOptionsColumn(form);
		VBox previewColumn = buildPreviewColumn(form.previewEditor);

		HBox content = new HBox(16, optionsColumn, previewColumn);
		content.getStyleClass().add(STYLE_CLASS_CONTENT);
		content.setPadding(new Insets(4, 0, 0, 0));
		content.setFillHeight(true);
		content.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(content, Priority.ALWAYS);
		HBox.setHgrow(previewColumn, Priority.ALWAYS);

		Runnable refreshPreview = () -> updatePreview(form);
		bindRefreshListeners(form, refreshPreview);
		refreshPreview.run();

		Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction(e -> {
			form.previewEditor.close();
			ModalService.getInstance().hide();
		});

		Button okButton = new Button("OK");
		okButton.getStyleClass().add(Styles.ACCENT);
		okButton.setOnAction(e -> {
			onConfirmTemplate.accept(form.previewEditor.getContent());
			form.previewEditor.close();
			ModalService.getInstance().hide();
		});

		DialogLayout dialog = new DialogLayout("Query Templates", content, cancelButton, okButton);
		dialog.getStyleClass().add(STYLE_CLASS_DIALOG);
		CssUtils.applyViewStyles(dialog, STYLESHEET);
		ModalService.getInstance().show(dialog);
	}

	private static void bindRefreshListeners(Form form, Runnable refreshPreview) {
		form.templateTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.useGraphPatternCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.useDistinctCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.applyLimitCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.limitSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.limitSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.advancedOptionsPane.expandedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.orderBySubjectCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.patternCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.applyOffsetCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.offsetSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.offsetSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
	}

	private static VBox buildOptionsColumn(Form form) {
		VBox optionsColumn = new VBox(10, form.templateTypeLabel, form.templateTypeCombo, form.optionsLabel,
				form.useGraphPatternCheck, form.useDistinctCheck, form.limitOptionRow, form.advancedOptionsPane);
		optionsColumn.getStyleClass().add(STYLE_CLASS_OPTIONS);
		optionsColumn.setPrefWidth(280);
		optionsColumn.setMinWidth(260);
		optionsColumn.setMaxHeight(Double.MAX_VALUE);
		return optionsColumn;
	}

	private static VBox buildPreviewColumn(CodeMirrorWidget previewEditor) {
		Label previewLabel = createSectionTitle("Preview");
		VBox previewColumn = new VBox(8, previewLabel, previewEditor);
		previewColumn.getStyleClass().add(STYLE_CLASS_PREVIEW_COLUMN);
		previewColumn.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(previewColumn, Priority.ALWAYS);
		return previewColumn;
	}

	private static void updatePreview(Form form) {
		QueryTemplateType selectedType = form.templateTypeCombo.getValue() != null
				? form.templateTypeCombo.getValue()
				: QueryTemplateType.SELECT;

		boolean supportsGraphPattern = selectedType.supportsGraphClause();
		configureCheckBoxSupport(form.useGraphPatternCheck, supportsGraphPattern);

		boolean supportsDistinct = selectedType.supportsDistinct();
		configureCheckBoxSupport(form.useDistinctCheck, supportsDistinct);

		boolean supportsLimit = selectedType.supportsLimit();
		configureCheckBoxSupport(form.applyLimitCheck, supportsLimit);
		setNodeVisible(form.limitOptionRow, supportsLimit);
		form.limitSpinner.setDisable(!supportsLimit || !form.applyLimitCheck.isSelected());

		boolean supportsOrderBy = selectedType.supportsOrderBy();
		configureCheckBoxSupport(form.orderBySubjectCheck, supportsOrderBy);

		boolean supportsPatternVariant = selectedType.supportsPatternVariant();
		setNodeVisible(form.patternLabel, supportsPatternVariant);
		setNodeVisible(form.patternCombo, supportsPatternVariant);
		form.patternCombo.setDisable(!supportsPatternVariant);
		if (!supportsPatternVariant) {
			form.patternCombo.getSelectionModel().select(QueryTemplatePattern.BASIC);
		}

		boolean supportsOffset = selectedType.supportsOffset();
		configureCheckBoxSupport(form.applyOffsetCheck, supportsOffset);
		setNodeVisible(form.offsetOptionRow, supportsOffset);
		form.offsetSpinner.setDisable(!supportsOffset || !form.applyOffsetCheck.isSelected());

		boolean hasAdvancedOptions = supportsOrderBy || supportsPatternVariant || supportsOffset;
		setNodeVisible(form.advancedOptionsPane, hasAdvancedOptions);
		if (!hasAdvancedOptions) {
			form.advancedOptionsPane.setExpanded(false);
		}

		boolean hasOptions = supportsGraphPattern || supportsDistinct || supportsLimit || hasAdvancedOptions;
		setNodeVisible(form.optionsLabel, hasOptions);

		QueryTemplatePattern selectedPattern = form.patternCombo.getValue() != null
				? form.patternCombo.getValue()
				: QueryTemplatePattern.BASIC;
		int parsedLimit = readSpinnerValue(form.limitSpinner, DEFAULT_LIMIT, 1);
		int parsedOffset = readSpinnerValue(form.offsetSpinner, DEFAULT_OFFSET, 0);
		Integer limit = form.applyLimitCheck.isSelected() ? parsedLimit : null;
		Integer offset = form.applyOffsetCheck.isSelected() ? parsedOffset : null;

		QueryTemplateOptions options = new QueryTemplateOptions(selectedType, form.useGraphPatternCheck.isSelected(),
				form.useDistinctCheck.isSelected(), form.orderBySubjectCheck.isSelected(), selectedPattern, limit,
				offset);
		form.previewEditor.setContent(QueryTemplateGenerator.generate(options));

		savePreferences(form, selectedType, selectedPattern, parsedLimit, parsedOffset);
	}

	private static void configureCheckBoxSupport(CheckBox checkBox, boolean supported) {
		setNodeVisible(checkBox, supported);
		checkBox.setDisable(!supported);
		if (!supported) {
			checkBox.setSelected(false);
		}
	}

	private static void setNodeVisible(Node node, boolean visible) {
		node.setVisible(visible);
		node.setManaged(visible);
	}

	private static int readSpinnerValue(Spinner<Integer> spinner, int fallback, int minValue) {
		String text = spinner.getEditor().getText();
		if (text != null && !text.isBlank()) {
			try {
				return Math.max(minValue, Integer.parseInt(text.trim()));
			} catch (NumberFormatException ignored) {
				// Fallback to spinner value.
			}
		}
		Integer value = spinner.getValue();
		if (value == null) {
			return fallback;
		}
		return Math.max(minValue, value);
	}

	private static void restorePreferences(Form form) {
		form.templateTypeCombo.getSelectionModel()
				.select(readEnum(PREF_TYPE, QueryTemplateType.SELECT, QueryTemplateType::valueOf));
		form.useGraphPatternCheck.setSelected(PREFS.getBoolean(PREF_USE_GRAPH, false));
		form.useDistinctCheck.setSelected(PREFS.getBoolean(PREF_USE_DISTINCT, false));
		form.applyLimitCheck.setSelected(PREFS.getBoolean(PREF_USE_LIMIT, false));
		form.advancedOptionsPane.setExpanded(PREFS.getBoolean(PREF_ADVANCED_EXPANDED, false));
		form.orderBySubjectCheck.setSelected(PREFS.getBoolean(PREF_ORDER_BY, false));
		form.patternCombo.getSelectionModel()
				.select(readEnum(PREF_PATTERN, QueryTemplatePattern.BASIC, QueryTemplatePattern::valueOf));
		form.applyOffsetCheck.setSelected(PREFS.getBoolean(PREF_USE_OFFSET, false));

		IntegerSpinnerValueFactory limitFactory = (IntegerSpinnerValueFactory) form.limitSpinner.getValueFactory();
		int storedLimit = Math.max(1, PREFS.getInt(PREF_LIMIT_VALUE, DEFAULT_LIMIT));
		limitFactory.setValue(Math.min(storedLimit, MAX_LIMIT_OFFSET));

		IntegerSpinnerValueFactory offsetFactory = (IntegerSpinnerValueFactory) form.offsetSpinner.getValueFactory();
		int storedOffset = Math.max(0, PREFS.getInt(PREF_OFFSET_VALUE, DEFAULT_OFFSET));
		offsetFactory.setValue(Math.min(storedOffset, MAX_LIMIT_OFFSET));
	}

	private static <E extends Enum<E>> E readEnum(String key, E defaultValue, Function<String, E> parser) {
		String stored = PREFS.get(key, null);
		if (stored == null || stored.isBlank()) {
			return defaultValue;
		}
		try {
			return parser.apply(stored);
		} catch (IllegalArgumentException ignored) {
			return defaultValue;
		}
	}

	private static void savePreferences(Form form, QueryTemplateType type, QueryTemplatePattern pattern, int limitValue,
			int offsetValue) {
		PREFS.put(PREF_TYPE, type.name());
		PREFS.putBoolean(PREF_USE_GRAPH, form.useGraphPatternCheck.isSelected());
		PREFS.putBoolean(PREF_USE_DISTINCT, form.useDistinctCheck.isSelected());
		PREFS.putBoolean(PREF_USE_LIMIT, form.applyLimitCheck.isSelected());
		PREFS.putInt(PREF_LIMIT_VALUE, limitValue);
		PREFS.putBoolean(PREF_ADVANCED_EXPANDED, form.advancedOptionsPane.isExpanded());
		PREFS.putBoolean(PREF_ORDER_BY, form.orderBySubjectCheck.isSelected());
		PREFS.put(PREF_PATTERN, pattern.name());
		PREFS.putBoolean(PREF_USE_OFFSET, form.applyOffsetCheck.isSelected());
		PREFS.putInt(PREF_OFFSET_VALUE, offsetValue);
	}

	private static Label createSectionTitle(String title) {
		Label label = new Label(title);
		label.getStyleClass().add(STYLE_CLASS_SECTION_TITLE);
		return label;
	}

	private static ComboBox<QueryTemplateType> createTypeCombo() {
		ComboBox<QueryTemplateType> typeCombo = new ComboBox<>();
		typeCombo.getItems().setAll(QueryTemplateType.values());
		typeCombo.getSelectionModel().select(QueryTemplateType.SELECT);
		typeCombo.setMaxWidth(Double.MAX_VALUE);
		return typeCombo;
	}

	private static ComboBox<QueryTemplatePattern> createPatternCombo() {
		ComboBox<QueryTemplatePattern> patternCombo = new ComboBox<>();
		patternCombo.getItems().setAll(QueryTemplatePattern.values());
		patternCombo.getSelectionModel().select(QueryTemplatePattern.BASIC);
		patternCombo.setMaxWidth(Double.MAX_VALUE);
		return patternCombo;
	}

	private static Spinner<Integer> createLimitSpinner() {
		Spinner<Integer> spinner = new Spinner<>();
		spinner.setEditable(true);
		spinner.setMaxWidth(Double.MAX_VALUE);
		spinner.setValueFactory(new IntegerSpinnerValueFactory(1, MAX_LIMIT_OFFSET, DEFAULT_LIMIT, 10));
		return spinner;
	}

	private static Spinner<Integer> createOffsetSpinner() {
		Spinner<Integer> spinner = new Spinner<>();
		spinner.setEditable(true);
		spinner.setMaxWidth(Double.MAX_VALUE);
		spinner.setValueFactory(new IntegerSpinnerValueFactory(0, MAX_LIMIT_OFFSET, DEFAULT_OFFSET, 10));
		return spinner;
	}

	private static HBox createInlineOptionRow(CheckBox checkBox, Spinner<Integer> spinner) {
		HBox row = new HBox(10, checkBox, spinner);
		row.getStyleClass().add(STYLE_CLASS_INLINE_ROW);
		return row;
	}

	private static TitledPane createAdvancedPane(CheckBox orderBySubjectCheck, Label patternLabel,
			ComboBox<QueryTemplatePattern> patternCombo, HBox offsetOptionRow) {
		VBox advancedContent = new VBox(10, orderBySubjectCheck, patternLabel, patternCombo, offsetOptionRow);
		TitledPane advancedPane = new TitledPane("Advanced", advancedContent);
		advancedPane.setAnimated(false);
		advancedPane.setExpanded(false);
		return advancedPane;
	}

	private static CodeMirrorWidget createPreviewEditor() {
		CodeMirrorWidget previewEditor = new CodeMirrorWidget(true);
		previewEditor.getStyleClass().add(STYLE_CLASS_PREVIEW);
		previewEditor.setMode(SerializationFormat.SPARQL_QUERY);
		previewEditor.setMinHeight(320);
		previewEditor.zoomInForCurrentEditorOnly();
		previewEditor.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (oldScene != null && newScene == null) {
				previewEditor.close();
			}
		});
		VBox.setVgrow(previewEditor, Priority.ALWAYS);
		return previewEditor;
	}

	private static final class Form {
		private final ComboBox<QueryTemplateType> templateTypeCombo = createTypeCombo();
		private final CheckBox useGraphPatternCheck = new CheckBox("Use GRAPH ?g pattern");
		private final CheckBox useDistinctCheck = new CheckBox("Use DISTINCT");

		private final CheckBox applyLimitCheck = new CheckBox("Use LIMIT");
		private final Spinner<Integer> limitSpinner = createLimitSpinner();
		private final HBox limitOptionRow = createInlineOptionRow(applyLimitCheck, limitSpinner);

		private final CheckBox orderBySubjectCheck = new CheckBox("Order by ?s");
		private final Label patternLabel = new Label("Pattern");
		private final ComboBox<QueryTemplatePattern> patternCombo = createPatternCombo();

		private final CheckBox applyOffsetCheck = new CheckBox("Use OFFSET");
		private final Spinner<Integer> offsetSpinner = createOffsetSpinner();
		private final HBox offsetOptionRow = createInlineOptionRow(applyOffsetCheck, offsetSpinner);

		private final TitledPane advancedOptionsPane = createAdvancedPane(orderBySubjectCheck, patternLabel,
				patternCombo, offsetOptionRow);
		private final CodeMirrorWidget previewEditor = createPreviewEditor();

		private final Label templateTypeLabel = createSectionTitle("Template Type");
		private final Label optionsLabel = createSectionTitle("Options");
	}
}
