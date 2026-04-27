package fr.inria.corese.gui.feature.query.template;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.AppPreferences;
import fr.inria.corese.gui.core.dialog.ModalService;
import fr.inria.corese.gui.core.theme.CssUtils;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Query template modal with a simple and contextualized form.
 */
@SuppressWarnings("java:S3398") // Helpers stay static to keep Form immutable and avoid capturing UI state.
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
	private static final String STYLE_CLASS_INLINE_ERROR = "query-template-inline-error";
	private static final String STYLE_CLASS_INPUT_INVALID = "query-template-input-invalid";
	private static final String DIALOG_SUBTITLE = "Select a query template, adjust options, and insert the generated query.";

	private static final int DEFAULT_LIMIT = 100;
	private static final int DEFAULT_OFFSET = 0;
	private static final int MAX_LIMIT_OFFSET = 1_000_000;
	private static final String DEFAULT_SERVICE_ENDPOINT = "https://dbpedia.org/sparql";
	private static final Duration ROW_ANIMATION_DURATION = Duration.millis(150);
	private static final Duration MESSAGE_ANIMATION_DURATION = Duration.millis(140);
	private static final double FALLBACK_ROW_HEIGHT = 34.0;
	private static final double FALLBACK_MESSAGE_HEIGHT = 18.0;

	private static final AppPreferences.Node PREFS = AppPreferences.nodeForClass(QueryTemplateDialog.class);
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
	private static final String PREF_USE_SERVICE = "queryTemplate.useService";
	private static final String PREF_SERVICE_URL = "queryTemplate.serviceUrl";

	private static final String OFFSET_ROW_ANIMATION_KEY = "queryTemplateOffsetRowAnimation";
	private static final String VALIDATION_ANIMATION_KEY = "queryTemplateValidationAnimation";

	private QueryTemplateDialog() {
		throw new AssertionError("Utility class");
	}

	public static void show(Consumer<String> onConfirmTemplate) {
		Objects.requireNonNull(onConfirmTemplate, "onConfirmTemplate callback must not be null");

		Form form = new Form();
		restorePreferences(form);

		ScrollPane optionsColumn = buildOptionsColumn(form);
		VBox previewColumn = buildPreviewColumn(form.previewEditor);

		HBox content = new HBox(16, optionsColumn, previewColumn);
		content.getStyleClass().add(STYLE_CLASS_CONTENT);
		content.setPadding(new Insets(4, 0, 0, 0));
		content.setFillHeight(true);
		content.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(content, Priority.ALWAYS);
		HBox.setHgrow(optionsColumn, Priority.NEVER);
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
		okButton.disableProperty().bind(form.hasValidationError);
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
		bindRefresh(refreshPreview, form.templateTypeCombo.valueProperty());
		bindRefresh(refreshPreview, form.useGraphPatternCheck.selectedProperty());
		bindRefresh(refreshPreview, form.useDistinctCheck.selectedProperty());
		bindRefresh(refreshPreview, form.orderBySubjectCheck.selectedProperty());
		bindRefresh(refreshPreview, form.useOptionalPatternCheck.selectedProperty());
		bindRefresh(refreshPreview, form.useUnionPatternCheck.selectedProperty());
		bindRefresh(refreshPreview, form.useServiceClauseCheck.selectedProperty());
		bindRefresh(refreshPreview, form.serviceUrlField.textProperty());
		bindRefresh(refreshPreview, form.applyLimitCheck.selectedProperty());
		bindRefresh(refreshPreview, form.limitSpinner.valueProperty());
		bindRefresh(refreshPreview, form.limitSpinner.getEditor().textProperty());
		bindRefresh(refreshPreview, form.applyOffsetCheck.selectedProperty());
		bindRefresh(refreshPreview, form.offsetSpinner.valueProperty());
		bindRefresh(refreshPreview, form.offsetSpinner.getEditor().textProperty());
	}

	private static void bindRefresh(Runnable refreshPreview, ObservableValue<?> source) {
		source.addListener((obs, oldVal, newVal) -> refreshPreview.run());
	}

	private static ScrollPane buildOptionsColumn(Form form) {
		VBox optionsContent = new VBox(10, form.typeSection, form.patternSection, form.serviceSection, form.resultSection,
				form.paginationSection, form.validationLabel);
		ScrollPane optionsColumn = new ScrollPane(optionsContent);
		optionsColumn.getStyleClass().add(STYLE_CLASS_OPTIONS);
		optionsColumn.setFitToWidth(true);
		optionsColumn.setFitToHeight(false);
		optionsColumn.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		optionsColumn.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		optionsColumn.setPrefWidth(300);
		optionsColumn.setMinWidth(280);
		optionsColumn.setMinHeight(0);
		optionsColumn.setMaxHeight(Double.MAX_VALUE);
		return optionsColumn;
	}

	private static VBox buildPreviewColumn(CodeMirrorWidget previewEditor) {
		Label previewLabel = createSectionTitle("Generated Query");
		VBox previewColumn = new VBox(8, previewLabel, previewEditor);
		previewColumn.getStyleClass().add(STYLE_CLASS_PREVIEW_COLUMN);
		previewColumn.setMaxHeight(Double.MAX_VALUE);
		previewEditor.prefHeightProperty().bind(Bindings.max(340.0,
				previewColumn.heightProperty().subtract(previewLabel.heightProperty()).subtract(8.0)));
		return previewColumn;
	}

	private static void updatePreview(Form form, boolean animateOffsetRow) {
		QueryTemplateType selectedType = selectedType(form);
		FormState formState = updateFormAvailability(form, selectedType, animateOffsetRow);
		ServiceValidation serviceValidation = validateServiceInputs(form, formState.serviceEnabled());
		boolean validateLimit = formState.limitEnabled();
		boolean validateOffset = formState.offsetEnabled();
		NumericValidation numericValidation = validateNumericInputs(form, validateLimit, validateOffset);
		boolean hasValidationError = !serviceValidation.valid() || !numericValidation.valid();
		form.hasValidationError.set(hasValidationError);

		savePreferences(form, selectedType, numericValidation.limitValue(), numericValidation.offsetValue());
		if (hasValidationError) {
			String validationMessage = !serviceValidation.valid() ? serviceValidation.message()
					: numericValidation.message();
			showValidationError(form, validationMessage);
			return;
		}
		clearValidationError(form);

		QueryTemplateOptions options = buildTemplateOptions(form, selectedType, serviceValidation, numericValidation,
				formState);
		form.previewEditor.setContent(QueryTemplateGenerator.generate(options));
	}

	private static QueryTemplateType selectedType(Form form) {
		return form.templateTypeCombo.getValue() == null ? QueryTemplateType.SELECT : form.templateTypeCombo.getValue();
	}

	private static FormState updateFormAvailability(Form form, QueryTemplateType selectedType, boolean animateOffsetRow) {
		boolean supportsGraphPattern = selectedType.supportsGraphClause();
		configureOptionAvailability(form.useGraphPatternCheck, supportsGraphPattern);

		boolean supportsPatternVariant = selectedType.supportsPatternVariant();
		configureOptionAvailability(form.useOptionalPatternCheck, supportsPatternVariant);
		configureOptionAvailability(form.useUnionPatternCheck, supportsPatternVariant);
		setNodeVisible(form.patternSection, supportsGraphPattern || supportsPatternVariant);

		boolean supportsService = selectedType.supportsServiceClause();
		setNodeVisible(form.serviceSection, supportsService);
		configureOptionAvailability(form.useServiceClauseCheck, supportsService);
		boolean serviceEnabled = supportsService && form.useServiceClauseCheck.isSelected();
		configureServiceRow(form.serviceUrlRow, form.serviceUrlField, serviceEnabled);

		boolean supportsDistinct = selectedType.supportsDistinct();
		configureOptionAvailability(form.useDistinctCheck, supportsDistinct);

		boolean supportsOrderBy = selectedType.supportsOrderBy();
		configureOptionAvailability(form.orderBySubjectCheck, supportsOrderBy);
		setNodeVisible(form.resultSection, supportsDistinct || supportsOrderBy);

		boolean limitEnabled = updatePaginationAvailability(form, selectedType, animateOffsetRow);
		boolean offsetEnabled = form.offsetOptionRow.isManaged() && form.applyOffsetCheck.isSelected();
		return new FormState(serviceEnabled, limitEnabled, offsetEnabled);
	}

	private static boolean updatePaginationAvailability(Form form, QueryTemplateType selectedType,
			boolean animateOffsetRow) {
		boolean supportsLimit = selectedType.supportsLimit();
		setNodeVisible(form.paginationSection, supportsLimit);
		setNodeVisible(form.limitOptionRow, supportsLimit);
		form.applyLimitCheck.setDisable(!supportsLimit);
		if (!supportsLimit) {
			form.applyLimitCheck.setSelected(false);
		}
		boolean limitEnabled = supportsLimit && form.applyLimitCheck.isSelected();
		form.limitSpinner.setDisable(!limitEnabled);

		boolean supportsOffset = selectedType.supportsOffset();
		boolean offsetAvailable = limitEnabled && supportsOffset;
		if (!offsetAvailable) {
			form.applyOffsetCheck.setSelected(false);
		}
		form.applyOffsetCheck.setDisable(!offsetAvailable);
		form.offsetSpinner.setDisable(!offsetAvailable || !form.applyOffsetCheck.isSelected());
		setCollapsibleRowVisible(form.offsetOptionRow, offsetAvailable, animateOffsetRow, OFFSET_ROW_ANIMATION_KEY);
		return limitEnabled;
	}

	private static QueryTemplateOptions buildTemplateOptions(Form form, QueryTemplateType selectedType,
			ServiceValidation serviceValidation, NumericValidation numericValidation, FormState formState) {
		Integer limit = formState.limitEnabled() ? numericValidation.limitValue() : null;
		Integer offset = formState.offsetEnabled() ? numericValidation.offsetValue() : null;
		return new QueryTemplateOptions(selectedType, form.useGraphPatternCheck.isSelected(),
				form.useDistinctCheck.isSelected(), form.orderBySubjectCheck.isSelected(),
				form.useOptionalPatternCheck.isSelected(), form.useUnionPatternCheck.isSelected(),
				form.useServiceClauseCheck.isSelected(), serviceValidation.endpointUrl(), limit, offset);
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
			setComputedHeightConstraints(row);
			return;
		}
		row.setOpacity(0);
		row.setManaged(false);
		row.setVisible(false);
		setCollapsedHeightConstraints(row);
	}

	private static void animateRowExpand(Region row, String animationKey) {
		Platform.runLater(() -> {
			row.applyCss();
			double targetHeight = resolveRowHeight(row);
			row.setManaged(true);
			row.setVisible(true);
			row.setOpacity(0);
			setCollapsedHeightConstraints(row);

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
				setComputedHeightConstraints(row);
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
			setFixedHeightConstraints(row, startHeight);

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

	private static void configureServiceRow(HBox row, Node input, boolean visible) {
		setNodeVisible(row, visible);
		if (input instanceof Control control) {
			control.setDisable(!visible);
		}
	}

	private static NumericValidation validateNumericInputs(Form form, boolean validateLimit, boolean validateOffset) {
		NumericFieldValidation limitValidation = validateNumericField(form.limitSpinner, DEFAULT_LIMIT, 1,
				validateLimit, "LIMIT");
		NumericFieldValidation offsetValidation = validateNumericField(form.offsetSpinner, DEFAULT_OFFSET, 0,
				validateOffset, "OFFSET");

		applyNumericValidationState(form.limitSpinner, limitValidation.invalid());
		applyNumericValidationState(form.offsetSpinner, offsetValidation.invalid());

		String message = "";
		if (limitValidation.invalid()) {
			message = limitValidation.message();
		} else if (offsetValidation.invalid()) {
			message = offsetValidation.message();
		}
		boolean valid = message.isBlank();
		return new NumericValidation(valid, limitValidation.value(), offsetValidation.value(), message);
	}

	private static ServiceValidation validateServiceInputs(Form form, boolean serviceEnabled) {
		TextFieldValidation endpointValidation = validateServiceEndpoint(form.serviceUrlField, serviceEnabled);
		applyTextValidationState(form.serviceUrlField, endpointValidation.invalid());
		return new ServiceValidation(!endpointValidation.invalid(), endpointValidation.value(), endpointValidation.message());
	}

	private static NumericFieldValidation validateNumericField(Spinner<Integer> spinner, int fallback, int minValue,
			boolean required, String fieldName) {
		if (!required) {
			return new NumericFieldValidation(readSpinnerValue(spinner, fallback, minValue), false, "");
		}

		String text = normalize(spinner.getEditor().getText());
		if (text.isBlank()) {
			return new NumericFieldValidation(readSpinnerValue(spinner, fallback, minValue), true,
					fieldName + " is required.");
		}

		long parsedValue;
		try {
			parsedValue = Long.parseLong(text);
		} catch (NumberFormatException _) {
			return new NumericFieldValidation(readSpinnerValue(spinner, fallback, minValue), true,
					fieldName + " must be an integer.");
		}

		if (parsedValue < minValue || parsedValue > MAX_LIMIT_OFFSET) {
			return new NumericFieldValidation(readSpinnerValue(spinner, fallback, minValue), true,
					fieldName + " must be between " + minValue + " and " + MAX_LIMIT_OFFSET + ".");
		}
		return new NumericFieldValidation((int) parsedValue, false, "");
	}

	private static TextFieldValidation validateServiceEndpoint(TextField field, boolean required) {
		String value = normalize(field.getText());
		if (!required) {
			return new TextFieldValidation(value, false, "");
		}
		if (value.isBlank()) {
			return new TextFieldValidation(value, true, "Service endpoint URL is required.");
		}
		if (value.contains("<") || value.contains(">")) {
			return new TextFieldValidation(value, true, "Service endpoint URL must not contain angle brackets.");
		}
		try {
			URI uri = URI.create(value);
			String scheme = uri.getScheme();
			if (!uri.isAbsolute() || scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
					|| uri.getRawAuthority() == null) {
				return new TextFieldValidation(value, true, "Service endpoint must be an absolute HTTP(S) URL.");
			}
			return new TextFieldValidation(value, false, "");
		} catch (IllegalArgumentException _) {
			return new TextFieldValidation(value, true, "Service endpoint URL is invalid.");
		}
	}

	private static void applyNumericValidationState(Spinner<Integer> spinner, boolean invalid) {
		toggleStyleClass(spinner, STYLE_CLASS_INPUT_INVALID, invalid);
		toggleStyleClass(spinner.getEditor(), STYLE_CLASS_INPUT_INVALID, invalid);
	}

	private static void applyTextValidationState(TextField field, boolean invalid) {
		toggleStyleClass(field, STYLE_CLASS_INPUT_INVALID, invalid);
	}

	private static void toggleStyleClass(Node node, String styleClass, boolean enabled) {
		if (node == null || styleClass == null || styleClass.isBlank()) {
			return;
		}
		if (enabled) {
			if (!node.getStyleClass().contains(styleClass)) {
				node.getStyleClass().add(styleClass);
			}
			return;
		}
		node.getStyleClass().remove(styleClass);
	}

	private static void showValidationError(Form form, String message) {
		String safeMessage = normalize(message);
		if (safeMessage.isBlank()) {
			clearValidationError(form);
			return;
		}

		stopValidationAnimation(form.validationLabel);
		form.validationLabel.setText(safeMessage);
		if (form.validationLabel.isManaged() && form.validationLabel.isVisible()) {
			form.validationLabel.setOpacity(1);
			setComputedHeightConstraints(form.validationLabel);
			return;
		}
		animateValidationMessageExpand(form.validationLabel);
	}

	private static void clearValidationError(Form form) {
		hideValidationMessage(form.validationLabel);
	}

	private static void hideValidationMessage(Label validationLabel) {
		if (!validationLabel.isManaged() && !validationLabel.isVisible()) {
			validationLabel.setText("");
			return;
		}
		animateValidationMessageCollapse(validationLabel);
	}

	private static void prepareCollapsedValidationLabel(Label validationLabel) {
		validationLabel.setText("");
		validationLabel.setOpacity(0);
		validationLabel.setManaged(false);
		validationLabel.setVisible(false);
		setCollapsedHeightConstraints(validationLabel);
	}

	private static void animateValidationMessageExpand(Label validationLabel) {
		stopValidationAnimation(validationLabel);
		Platform.runLater(() -> {
			validationLabel.applyCss();
			double targetHeight = resolveValidationLabelHeight(validationLabel);
			validationLabel.setManaged(true);
			validationLabel.setVisible(true);
			validationLabel.setOpacity(0);
			setCollapsedHeightConstraints(validationLabel);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(validationLabel.opacityProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(validationLabel.minHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(validationLabel.prefHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(validationLabel.maxHeightProperty(), 0, Interpolator.EASE_BOTH)),
					new KeyFrame(MESSAGE_ANIMATION_DURATION,
							new KeyValue(validationLabel.opacityProperty(), 1, Interpolator.EASE_OUT),
							new KeyValue(validationLabel.minHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(validationLabel.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(validationLabel.maxHeightProperty(), targetHeight, Interpolator.EASE_OUT)));
			timeline.setOnFinished(event -> {
				validationLabel.setOpacity(1);
				setComputedHeightConstraints(validationLabel);
				validationLabel.getProperties().remove(VALIDATION_ANIMATION_KEY);
			});
			validationLabel.getProperties().put(VALIDATION_ANIMATION_KEY, timeline);
			timeline.play();
		});
	}

	private static void animateValidationMessageCollapse(Label validationLabel) {
		stopValidationAnimation(validationLabel);
		Platform.runLater(() -> {
			double startHeight = resolveValidationLabelHeight(validationLabel);
			double startOpacity = validationLabel.getOpacity() > 0 ? validationLabel.getOpacity() : 1;
			setFixedHeightConstraints(validationLabel, startHeight);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(validationLabel.opacityProperty(), startOpacity, Interpolator.EASE_BOTH),
							new KeyValue(validationLabel.minHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(validationLabel.prefHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(validationLabel.maxHeightProperty(), startHeight, Interpolator.EASE_BOTH)),
					new KeyFrame(MESSAGE_ANIMATION_DURATION,
							new KeyValue(validationLabel.opacityProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(validationLabel.minHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(validationLabel.prefHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(validationLabel.maxHeightProperty(), 0, Interpolator.EASE_IN)));
			timeline.setOnFinished(event -> {
				prepareCollapsedValidationLabel(validationLabel);
				validationLabel.getProperties().remove(VALIDATION_ANIMATION_KEY);
			});
			validationLabel.getProperties().put(VALIDATION_ANIMATION_KEY, timeline);
			timeline.play();
		});
	}

	private static void stopValidationAnimation(Label validationLabel) {
		Object animation = validationLabel.getProperties().remove(VALIDATION_ANIMATION_KEY);
		if (animation instanceof Timeline timeline) {
			timeline.stop();
		}
	}

	private static double resolveValidationLabelHeight(Label label) {
		if (label == null) {
			return FALLBACK_MESSAGE_HEIGHT;
		}
		double measuredHeight = label.getHeight();
		if (measuredHeight > 0) {
			return measuredHeight;
		}
		double preferredHeight = label.prefHeight(-1);
		if (preferredHeight > 0) {
			return preferredHeight;
		}
		return FALLBACK_MESSAGE_HEIGHT;
	}

	private static Label createInlineErrorLabel() {
		Label label = new Label();
		label.getStyleClass().add(STYLE_CLASS_INLINE_ERROR);
		label.setWrapText(true);
		return label;
	}

	private static void setComputedHeightConstraints(Region region) {
		setFixedHeightConstraints(region, Region.USE_COMPUTED_SIZE);
	}

	private static void setCollapsedHeightConstraints(Region region) {
		setFixedHeightConstraints(region, 0);
	}

	private static void setFixedHeightConstraints(Region region, double value) {
		region.setMinHeight(value);
		region.setPrefHeight(value);
		region.setMaxHeight(value);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private static int readSpinnerValue(Spinner<Integer> spinner, int fallback, int minValue) {
		String text = spinner.getEditor().getText();
		if (text != null && !text.isBlank()) {
			try {
				return clampNumericValue(Integer.parseInt(text.trim()), minValue);
			} catch (NumberFormatException _) {
				// Fallback to spinner value.
			}
		}
		Integer value = spinner.getValue();
		if (value == null) {
			return clampNumericValue(fallback, minValue);
		}
		return clampNumericValue(value, minValue);
	}

	private static int clampNumericValue(int value, int minValue) {
		return Math.clamp(value, minValue, MAX_LIMIT_OFFSET);
	}

	private static void restorePreferences(Form form) {
		form.templateTypeCombo.getSelectionModel().select(readTemplateTypePreference());
		form.useGraphPatternCheck.setSelected(PREFS.getBoolean(PREF_USE_GRAPH, false));
		form.useDistinctCheck.setSelected(PREFS.getBoolean(PREF_USE_DISTINCT, false));
		form.orderBySubjectCheck.setSelected(PREFS.getBoolean(PREF_ORDER_BY, false));
		form.useOptionalPatternCheck.setSelected(PREFS.getBoolean(PREF_USE_OPTIONAL_PATTERN, false));
		form.useUnionPatternCheck.setSelected(PREFS.getBoolean(PREF_USE_UNION_PATTERN, false));
		form.useServiceClauseCheck.setSelected(PREFS.getBoolean(PREF_USE_SERVICE, false));
		form.serviceUrlField.setText(PREFS.get(PREF_SERVICE_URL, DEFAULT_SERVICE_ENDPOINT));
		form.applyLimitCheck.setSelected(PREFS.getBoolean(PREF_USE_LIMIT, false));
		form.applyOffsetCheck.setSelected(PREFS.getBoolean(PREF_USE_OFFSET, false));

		restoreSpinnerValue(form.limitSpinner, PREF_LIMIT_VALUE, DEFAULT_LIMIT, 1);
		restoreSpinnerValue(form.offsetSpinner, PREF_OFFSET_VALUE, DEFAULT_OFFSET, 0);
	}

	private static QueryTemplateType readTemplateTypePreference() {
		String stored = PREFS.get(PREF_TYPE, null);
		if (stored == null || stored.isBlank()) {
			return QueryTemplateType.SELECT;
		}
		try {
			return QueryTemplateType.valueOf(stored);
		} catch (IllegalArgumentException _) {
			return QueryTemplateType.SELECT;
		}
	}

	private static void restoreSpinnerValue(Spinner<Integer> spinner, String prefKey, int defaultValue, int minValue) {
		IntegerSpinnerValueFactory valueFactory = (IntegerSpinnerValueFactory) spinner.getValueFactory();
		int storedValue = Math.max(minValue, PREFS.getInt(prefKey, defaultValue));
		valueFactory.setValue(Math.min(storedValue, MAX_LIMIT_OFFSET));
	}

	private static void savePreferences(Form form, QueryTemplateType type, int limitValue, int offsetValue) {
		PREFS.put(PREF_TYPE, type.name());
		PREFS.putBoolean(PREF_USE_GRAPH, form.useGraphPatternCheck.isSelected());
		PREFS.putBoolean(PREF_USE_DISTINCT, form.useDistinctCheck.isSelected());
		PREFS.putBoolean(PREF_ORDER_BY, form.orderBySubjectCheck.isSelected());
		PREFS.putBoolean(PREF_USE_OPTIONAL_PATTERN, form.useOptionalPatternCheck.isSelected());
		PREFS.putBoolean(PREF_USE_UNION_PATTERN, form.useUnionPatternCheck.isSelected());
		PREFS.putBoolean(PREF_USE_SERVICE, form.useServiceClauseCheck.isSelected());
		PREFS.put(PREF_SERVICE_URL, normalize(form.serviceUrlField.getText()));
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

	private static TextField createTextField(String promptText) {
		TextField textField = new TextField();
		textField.setPromptText(promptText);
		textField.setMaxWidth(Double.MAX_VALUE);
		return textField;
	}

	private static Spinner<Integer> createLimitSpinner() {
		return createIntegerSpinner(1, DEFAULT_LIMIT, 10);
	}

	private static Spinner<Integer> createOffsetSpinner() {
		return createIntegerSpinner(0, DEFAULT_OFFSET, 10);
	}

	private static Spinner<Integer> createIntegerSpinner(int minValue, int defaultValue, int step) {
		Spinner<Integer> spinner = new Spinner<>();
		spinner.setEditable(true);
		spinner.setMaxWidth(Double.MAX_VALUE);
		spinner.setValueFactory(new IntegerSpinnerValueFactory(minValue, MAX_LIMIT_OFFSET, defaultValue, step));
		return spinner;
	}

	private static HBox createNumericOptionRow(CheckBox checkBox, Spinner<Integer> spinner) {
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox row = new HBox(10, checkBox, spacer, spinner);
		row.getStyleClass().add(STYLE_CLASS_OPTION_ROW);
		return row;
	}

	private static HBox createLabeledInputRow(String labelText, Node input) {
		Label label = new Label(labelText);
		HBox row = new HBox(10, label, input);
		row.getStyleClass().add(STYLE_CLASS_OPTION_ROW);
		HBox.setHgrow(input, Priority.ALWAYS);
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
		private final BooleanProperty hasValidationError = new SimpleBooleanProperty(false);

		private final CheckBox useGraphPatternCheck = createOptionCheck("Use GRAPH block (?g)");
		private final CheckBox useOptionalPatternCheck = createOptionCheck("Add OPTIONAL block");
		private final CheckBox useUnionPatternCheck = createOptionCheck("Add UNION block");

		private final CheckBox useServiceClauseCheck = createOptionCheck("Wrap WHERE pattern in SERVICE");
		private final TextField serviceUrlField = createTextField(DEFAULT_SERVICE_ENDPOINT);
		private final HBox serviceUrlRow = createLabeledInputRow("Endpoint", serviceUrlField);

		private final CheckBox useDistinctCheck = createOptionCheck("Use DISTINCT");
		private final CheckBox orderBySubjectCheck = createOptionCheck("Order by ?s (ASC)");

		private final CheckBox applyLimitCheck = createOptionCheck("Apply LIMIT");
		private final Spinner<Integer> limitSpinner = createLimitSpinner();
		private final HBox limitOptionRow = createNumericOptionRow(applyLimitCheck, limitSpinner);

		private final CheckBox applyOffsetCheck = createOptionCheck("Apply OFFSET");
		private final Spinner<Integer> offsetSpinner = createOffsetSpinner();
		private final HBox offsetOptionRow = createNumericOptionRow(applyOffsetCheck, offsetSpinner);
		private final Label validationLabel = createInlineErrorLabel();

		private final CodeMirrorWidget previewEditor = createPreviewEditor();

		private final VBox typeSection = createOptionsSection("Query Type", templateTypeCombo);
		private final VBox patternSection = createOptionsSection("Pattern", useGraphPatternCheck,
				useOptionalPatternCheck, useUnionPatternCheck);
		private final VBox serviceSection = createOptionsSection("Remote Service", useServiceClauseCheck, serviceUrlRow);
		private final VBox resultSection = createOptionsSection("Result Options", useDistinctCheck,
				orderBySubjectCheck);
		private final VBox paginationSection = createOptionsSection("Pagination", limitOptionRow, offsetOptionRow);

		private Form() {
			setRowVisibilityImmediately(offsetOptionRow, false);
			setNodeVisible(serviceUrlRow, false);
			prepareCollapsedValidationLabel(validationLabel);
		}
	}

	private record NumericFieldValidation(int value, boolean invalid, String message) {
	}

	private record NumericValidation(boolean valid, int limitValue, int offsetValue, String message) {
	}

	private record TextFieldValidation(String value, boolean invalid, String message) {
	}

	private record ServiceValidation(boolean valid, String endpointUrl, String message) {
	}

	private record FormState(boolean serviceEnabled, boolean limitEnabled, boolean offsetEnabled) {
	}
}
