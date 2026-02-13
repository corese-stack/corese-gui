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
import javafx.beans.binding.Bindings;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Query template modal with a simple and contextualized form.
 */
public final class QueryTemplateDialog {

	private static final String STYLESHEET = "/css/features/query-template-dialog.css";
	private static final String STYLE_CLASS_DIALOG = "query-template-dialog";
	private static final String STYLE_CLASS_CONTENT = "query-template-content";
	private static final String STYLE_CLASS_OPTIONS = "query-template-options";
	private static final String STYLE_CLASS_SECTION = "query-template-section";
	private static final String STYLE_CLASS_SECTION_CONTENT = "query-template-section-content";
	private static final String STYLE_CLASS_PREVIEW_COLUMN = "query-template-preview-column";
	private static final String STYLE_CLASS_PREVIEW = "query-template-preview";
	private static final String STYLE_CLASS_OPTION_CHECK = "query-template-option-check";
	private static final String STYLE_CLASS_OPTION_ROW = "query-template-option-row";
	private static final String STYLE_CLASS_SECTION_TITLE = "query-template-section-title";
	private static final String DIALOG_SUBTITLE = "Choose a query type, adjust options, and insert the preview.";

	private static final int DEFAULT_LIMIT = 100;
	private static final int DEFAULT_OFFSET = 0;
	private static final int MAX_LIMIT_OFFSET = 1_000_000;
	private static final Duration ROW_ANIMATION_DURATION = Duration.millis(150);
	private static final double FALLBACK_ROW_HEIGHT = 34.0;

	private static final Preferences PREFS = Preferences.userNodeForPackage(QueryTemplateDialog.class);
	private static final String PREF_TYPE = "queryTemplate.type";
	private static final String PREF_USE_GRAPH = "queryTemplate.useGraphPattern";
	private static final String PREF_USE_DISTINCT = "queryTemplate.useDistinct";
	private static final String PREF_USE_LIMIT = "queryTemplate.useLimit";
	private static final String PREF_LIMIT_VALUE = "queryTemplate.limitValue";
	private static final String PREF_ORDER_BY = "queryTemplate.orderBySubject";
	private static final String PREF_USE_OPTIONAL_PATTERN = "queryTemplate.useOptionalPattern";
	private static final String PREF_USE_UNION_PATTERN = "queryTemplate.useUnionPattern";
	private static final String PREF_USE_OFFSET = "queryTemplate.useOffset";
	private static final String PREF_OFFSET_VALUE = "queryTemplate.offsetValue";

	private static final String OFFSET_ROW_ANIMATION_KEY = "queryTemplateOffsetRowAnimation";

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

		Runnable refreshPreview = () -> updatePreview(form, true);
		bindRefreshListeners(form, refreshPreview);
		updatePreview(form, false);

		Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction(event -> {
			form.previewEditor.close();
			ModalService.getInstance().hide();
		});

		Button okButton = new Button("Insert");
		okButton.getStyleClass().add(Styles.ACCENT);
		okButton.setOnAction(event -> {
			onConfirmTemplate.accept(form.previewEditor.getContent());
			form.previewEditor.close();
			ModalService.getInstance().hide();
		});

		DialogLayout dialog = new DialogLayout("Query Templates", DIALOG_SUBTITLE, content, cancelButton, okButton);
		dialog.getStyleClass().add(STYLE_CLASS_DIALOG);
		CssUtils.applyViewStyles(dialog, STYLESHEET);
		ModalService.getInstance().show(dialog);
	}

	private static void bindRefreshListeners(Form form, Runnable refreshPreview) {
		form.templateTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.useGraphPatternCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.useDistinctCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.orderBySubjectCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.useOptionalPatternCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.useUnionPatternCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.applyLimitCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.limitSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.limitSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.applyOffsetCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.offsetSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
		form.offsetSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());
	}

	private static VBox buildOptionsColumn(Form form) {
		VBox optionsColumn = new VBox(10, form.typeSection, form.patternSection, form.resultSection,
				form.paginationSection);
		optionsColumn.getStyleClass().add(STYLE_CLASS_OPTIONS);
		optionsColumn.setPrefWidth(300);
		optionsColumn.setMinWidth(280);
		optionsColumn.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(optionsColumn, Priority.ALWAYS);
		return optionsColumn;
	}

	private static VBox buildPreviewColumn(CodeMirrorWidget previewEditor) {
		Label previewLabel = createSectionTitle("Preview");
		VBox previewColumn = new VBox(8, previewLabel, previewEditor);
		previewColumn.getStyleClass().add(STYLE_CLASS_PREVIEW_COLUMN);
		previewColumn.setMaxHeight(Double.MAX_VALUE);
		previewEditor.prefHeightProperty().bind(Bindings.max(340.0,
				previewColumn.heightProperty().subtract(previewLabel.heightProperty()).subtract(8.0)));
		VBox.setVgrow(previewColumn, Priority.ALWAYS);
		return previewColumn;
	}

	private static void updatePreview(Form form, boolean animateOffsetRow) {
		QueryTemplateType selectedType = form.templateTypeCombo.getValue() != null
				? form.templateTypeCombo.getValue()
				: QueryTemplateType.SELECT;

		boolean supportsGraphPattern = selectedType.supportsGraphClause();
		configureOptionAvailability(form.useGraphPatternCheck, supportsGraphPattern);

		boolean supportsPatternVariant = selectedType.supportsPatternVariant();
		configureOptionAvailability(form.useOptionalPatternCheck, supportsPatternVariant);
		configureOptionAvailability(form.useUnionPatternCheck, supportsPatternVariant);

		boolean supportsDistinct = selectedType.supportsDistinct();
		configureOptionAvailability(form.useDistinctCheck, supportsDistinct);

		boolean supportsOrderBy = selectedType.supportsOrderBy();
		configureOptionAvailability(form.orderBySubjectCheck, supportsOrderBy);

		boolean supportsLimit = selectedType.supportsLimit();
		setNodeVisible(form.paginationSection, supportsLimit);
		setNodeVisible(form.limitOptionRow, supportsLimit);
		form.applyLimitCheck.setDisable(!supportsLimit);
		if (!supportsLimit) {
			form.applyLimitCheck.setSelected(false);
		}
		form.limitSpinner.setDisable(!supportsLimit || !form.applyLimitCheck.isSelected());

		boolean supportsOffset = selectedType.supportsOffset();
		boolean offsetRowVisible = supportsLimit && supportsOffset && form.applyLimitCheck.isSelected();
		if (!offsetRowVisible) {
			form.applyOffsetCheck.setSelected(false);
		}
		form.applyOffsetCheck.setDisable(!offsetRowVisible);
		form.offsetSpinner.setDisable(!offsetRowVisible || !form.applyOffsetCheck.isSelected());
		setCollapsibleRowVisible(form.offsetOptionRow, offsetRowVisible, animateOffsetRow, OFFSET_ROW_ANIMATION_KEY);

		setNodeVisible(form.patternSection, supportsGraphPattern || supportsPatternVariant);
		setNodeVisible(form.resultSection, supportsDistinct || supportsOrderBy);

		int parsedLimit = readSpinnerValue(form.limitSpinner, DEFAULT_LIMIT, 1);
		int parsedOffset = readSpinnerValue(form.offsetSpinner, DEFAULT_OFFSET, 0);
		Integer limit = supportsLimit && form.applyLimitCheck.isSelected() ? parsedLimit : null;
		Integer offset = offsetRowVisible && form.applyOffsetCheck.isSelected() ? parsedOffset : null;

		QueryTemplateOptions options = new QueryTemplateOptions(selectedType, form.useGraphPatternCheck.isSelected(),
				form.useDistinctCheck.isSelected(), form.orderBySubjectCheck.isSelected(),
				form.useOptionalPatternCheck.isSelected(), form.useUnionPatternCheck.isSelected(), limit, offset);
		form.previewEditor.setContent(QueryTemplateGenerator.generate(options));

		savePreferences(form, selectedType, parsedLimit, parsedOffset);
	}

	private static void configureOptionAvailability(CheckBox checkBox, boolean supported) {
		setNodeVisible(checkBox, supported);
		checkBox.setDisable(!supported);
		if (!supported) {
			checkBox.setSelected(false);
		}
	}

	private static void setCollapsibleRowVisible(Region row, boolean visible, boolean animated, String animationKey) {
		boolean currentlyVisible = row.isManaged() && row.isVisible();
		if (currentlyVisible == visible) {
			if (visible) {
				row.setOpacity(1);
			}
			return;
		}
		stopRowVisibilityAnimation(row, animationKey);

		if (!animated) {
			setRowVisibilityImmediately(row, visible);
			return;
		}
		if (visible) {
			animateRowExpand(row, animationKey);
		} else {
			animateRowCollapse(row, animationKey);
		}
	}

	private static void setRowVisibilityImmediately(Region row, boolean visible) {
		if (visible) {
			row.setManaged(true);
			row.setVisible(true);
			row.setOpacity(1);
			row.setMinHeight(Region.USE_COMPUTED_SIZE);
			row.setPrefHeight(Region.USE_COMPUTED_SIZE);
			row.setMaxHeight(Region.USE_COMPUTED_SIZE);
			return;
		}
		row.setOpacity(0);
		row.setManaged(false);
		row.setVisible(false);
		row.setMinHeight(0);
		row.setPrefHeight(0);
		row.setMaxHeight(0);
	}

	private static void animateRowExpand(Region row, String animationKey) {
		Platform.runLater(() -> {
			row.applyCss();
			double targetHeight = resolveRowHeight(row);
			row.setManaged(true);
			row.setVisible(true);
			row.setOpacity(0);
			row.setMinHeight(0);
			row.setPrefHeight(0);
			row.setMaxHeight(0);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO, new KeyValue(row.opacityProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(row.minHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(row.prefHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(row.maxHeightProperty(), 0, Interpolator.EASE_BOTH)),
					new KeyFrame(ROW_ANIMATION_DURATION, new KeyValue(row.opacityProperty(), 1, Interpolator.EASE_OUT),
							new KeyValue(row.minHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(row.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(row.maxHeightProperty(), targetHeight, Interpolator.EASE_OUT)));
			timeline.setOnFinished(event -> {
				row.setOpacity(1);
				row.setMinHeight(Region.USE_COMPUTED_SIZE);
				row.setPrefHeight(Region.USE_COMPUTED_SIZE);
				row.setMaxHeight(Region.USE_COMPUTED_SIZE);
				row.getProperties().remove(animationKey);
			});
			row.getProperties().put(animationKey, timeline);
			timeline.play();
		});
	}

	private static void animateRowCollapse(Region row, String animationKey) {
		Platform.runLater(() -> {
			double startHeight = resolveRowHeight(row);
			double startOpacity = row.getOpacity() > 0 ? row.getOpacity() : 1;
			row.setMinHeight(startHeight);
			row.setPrefHeight(startHeight);
			row.setMaxHeight(startHeight);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(row.opacityProperty(), startOpacity, Interpolator.EASE_BOTH),
							new KeyValue(row.minHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(row.prefHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(row.maxHeightProperty(), startHeight, Interpolator.EASE_BOTH)),
					new KeyFrame(ROW_ANIMATION_DURATION, new KeyValue(row.opacityProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(row.minHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(row.prefHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(row.maxHeightProperty(), 0, Interpolator.EASE_IN)));
			timeline.setOnFinished(event -> {
				setRowVisibilityImmediately(row, false);
				row.getProperties().remove(animationKey);
			});
			row.getProperties().put(animationKey, timeline);
			timeline.play();
		});
	}

	private static void stopRowVisibilityAnimation(Region row, String animationKey) {
		Object animation = row.getProperties().remove(animationKey);
		if (animation instanceof Timeline timeline) {
			timeline.stop();
		}
	}

	private static double resolveRowHeight(Region row) {
		double measured = row.getHeight();
		if (measured > 0) {
			return measured;
		}
		double preferred = row.prefHeight(-1);
		if (preferred > 0) {
			return preferred;
		}
		return FALLBACK_ROW_HEIGHT;
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
		form.orderBySubjectCheck.setSelected(PREFS.getBoolean(PREF_ORDER_BY, false));
		form.useOptionalPatternCheck.setSelected(PREFS.getBoolean(PREF_USE_OPTIONAL_PATTERN, false));
		form.useUnionPatternCheck.setSelected(PREFS.getBoolean(PREF_USE_UNION_PATTERN, false));
		form.applyLimitCheck.setSelected(PREFS.getBoolean(PREF_USE_LIMIT, false));
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

	private static void savePreferences(Form form, QueryTemplateType type, int limitValue, int offsetValue) {
		PREFS.put(PREF_TYPE, type.name());
		PREFS.putBoolean(PREF_USE_GRAPH, form.useGraphPatternCheck.isSelected());
		PREFS.putBoolean(PREF_USE_DISTINCT, form.useDistinctCheck.isSelected());
		PREFS.putBoolean(PREF_ORDER_BY, form.orderBySubjectCheck.isSelected());
		PREFS.putBoolean(PREF_USE_OPTIONAL_PATTERN, form.useOptionalPatternCheck.isSelected());
		PREFS.putBoolean(PREF_USE_UNION_PATTERN, form.useUnionPatternCheck.isSelected());
		PREFS.putBoolean(PREF_USE_LIMIT, form.applyLimitCheck.isSelected());
		PREFS.putInt(PREF_LIMIT_VALUE, limitValue);
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

	private static CheckBox createOptionCheck(String labelText) {
		CheckBox checkBox = new CheckBox(labelText);
		checkBox.getStyleClass().add(STYLE_CLASS_OPTION_CHECK);
		checkBox.setMaxWidth(Double.MAX_VALUE);
		return checkBox;
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

	private static HBox createNumericOptionRow(CheckBox checkBox, Spinner<Integer> spinner) {
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox row = new HBox(10, checkBox, spacer, spinner);
		row.getStyleClass().add(STYLE_CLASS_OPTION_ROW);
		return row;
	}

	private static VBox createOptionsSection(String title, Node... content) {
		Label titleLabel = createSectionTitle(title);
		VBox sectionContent = new VBox(8, content);
		sectionContent.getStyleClass().add(STYLE_CLASS_SECTION_CONTENT);
		VBox section = new VBox(8, titleLabel, sectionContent);
		section.getStyleClass().add(STYLE_CLASS_SECTION);
		section.setFillWidth(true);
		return section;
	}

	private static CodeMirrorWidget createPreviewEditor() {
		CodeMirrorWidget previewEditor = new CodeMirrorWidget(true);
		previewEditor.getStyleClass().add(STYLE_CLASS_PREVIEW);
		previewEditor.setMode(SerializationFormat.SPARQL_QUERY);
		previewEditor.setMinHeight(340);
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

		private final CheckBox useGraphPatternCheck = createOptionCheck("Use GRAPH ?g scope");
		private final CheckBox useOptionalPatternCheck = createOptionCheck("Add OPTIONAL block");
		private final CheckBox useUnionPatternCheck = createOptionCheck("Add UNION block");

		private final CheckBox useDistinctCheck = createOptionCheck("Use DISTINCT");
		private final CheckBox orderBySubjectCheck = createOptionCheck("Order by ?s");

		private final CheckBox applyLimitCheck = createOptionCheck("Use LIMIT");
		private final Spinner<Integer> limitSpinner = createLimitSpinner();
		private final HBox limitOptionRow = createNumericOptionRow(applyLimitCheck, limitSpinner);

		private final CheckBox applyOffsetCheck = createOptionCheck("Use OFFSET");
		private final Spinner<Integer> offsetSpinner = createOffsetSpinner();
		private final HBox offsetOptionRow = createNumericOptionRow(applyOffsetCheck, offsetSpinner);

		private final CodeMirrorWidget previewEditor = createPreviewEditor();

		private final VBox typeSection = createOptionsSection("Query Type", templateTypeCombo);
		private final VBox patternSection = createOptionsSection("Pattern Scope", useGraphPatternCheck,
				useOptionalPatternCheck, useUnionPatternCheck);
		private final VBox resultSection = createOptionsSection("Result", useDistinctCheck, orderBySubjectCheck);
		private final VBox paginationSection = createOptionsSection("Pagination", limitOptionRow, offsetOptionRow);

		private Form() {
			setRowVisibilityImmediately(offsetOptionRow, false);
		}
	}
}
