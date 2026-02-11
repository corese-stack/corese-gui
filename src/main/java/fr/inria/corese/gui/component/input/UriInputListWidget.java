package fr.inria.corese.gui.component.input;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Reusable widget that manages a dynamic list of URI input fields.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Automatically keeps a trailing empty field</li>
 * <li>Animates field add/remove vertically</li>
 * <li>Validates URIs and highlights invalid fields</li>
 * <li>Displays an animated inline error message area</li>
 * </ul>
 */
public final class UriInputListWidget extends VBox {

	private static final String STYLESHEET = "/css/components/uri-input-list-widget.css";
	private static final String STYLE_CLASS_ROOT = "uri-input-list-widget";
	private static final String STYLE_CLASS_FIELDS = "uri-input-fields";
	private static final String STYLE_CLASS_URI_INVALID = "uri-input-invalid";
	private static final String STYLE_CLASS_INLINE_ERROR = "uri-inline-error";
	private static final String VALIDATION_ANIMATION_KEY = "uriInlineErrorAnimation";
	private static final String HTTP_SCHEME = "http";
	private static final String HTTPS_SCHEME = "https";
	private static final String FILE_SCHEME = "file";
	private static final Duration FIELD_ANIMATION_DURATION = Duration.millis(140);
	private static final Duration MESSAGE_ANIMATION_DURATION = Duration.millis(140);
	private static final double FALLBACK_FIELD_HEIGHT = 30.0;
	private static final double FALLBACK_MESSAGE_HEIGHT = 18.0;

	private final String uriPrompt;
	private final VBox uriFieldsBox = new VBox(8);
	private final Label validationLabel = new Label();
	private final List<TextField> uriFields = new ArrayList<>();

	/**
	 * Creates a widget instance with the provided prompt for each URI field.
	 *
	 * @param uriPrompt
	 *            prompt shown inside URI fields
	 */
	public UriInputListWidget(String uriPrompt) {
		this.uriPrompt = normalize(uriPrompt);
		initialize();
	}

	/**
	 * Returns distinct non-blank URIs entered by the user.
	 *
	 * @return distinct URI list preserving insertion order
	 */
	public List<String> getDistinctNonBlankUris() {
		return uriFields.stream().map(TextField::getText).map(UriInputListWidget::normalize)
				.filter(value -> !value.isBlank()).distinct().toList();
	}

	/**
	 * Validates all non-empty fields, updates invalid styling, and returns whether
	 * at least one invalid URI exists.
	 *
	 * @return true if one or more non-empty fields contain invalid URI values
	 */
	public boolean highlightInvalidUris() {
		boolean hasInvalidUri = false;
		for (TextField field : uriFields) {
			String value = normalize(field.getText());
			boolean invalid = !value.isBlank() && !isValidUri(value);
			applyValidationState(field, invalid);
			hasInvalidUri = hasInvalidUri || invalid;
		}
		return hasInvalidUri;
	}

	/**
	 * Shows an inline validation message under the URI list.
	 *
	 * @param message
	 *            message to display
	 */
	public void showValidationError(String message) {
		String safeMessage = normalize(message);
		if (safeMessage.isBlank()) {
			hideValidationMessage();
			return;
		}
		stopValidationAnimation();
		validationLabel.setText(safeMessage);
		if (validationLabel.isManaged() && validationLabel.isVisible()) {
			validationLabel.setOpacity(1);
			validationLabel.setMinHeight(Region.USE_COMPUTED_SIZE);
			validationLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
			validationLabel.setMaxHeight(Region.USE_COMPUTED_SIZE);
			return;
		}
		animateValidationMessageExpand();
	}

	/**
	 * Hides the inline validation message.
	 */
	public void clearValidationError() {
		hideValidationMessage();
	}

	private void initialize() {
		String stylesheet = getClass().getResource(STYLESHEET).toExternalForm();
		getStylesheets().add(stylesheet);
		getStyleClass().add(STYLE_CLASS_ROOT);
		setFillWidth(true);
		setSpacing(10);

		uriFieldsBox.getStyleClass().add(STYLE_CLASS_FIELDS);
		uriFieldsBox.setFillWidth(true);

		validationLabel.getStyleClass().add(STYLE_CLASS_INLINE_ERROR);
		validationLabel.setWrapText(true);
		prepareCollapsedValidationLabel();

		getChildren().addAll(uriFieldsBox, validationLabel);
		addUriField("", false);
	}

	private void addUriField(String initialValue, boolean animated) {
		TextField uriField = new TextField(initialValue);
		uriField.setPromptText(uriPrompt);
		HBox.setHgrow(uriField, Priority.ALWAYS);
		uriField.textProperty().addListener((obs, oldValue, newValue) -> handleUriFieldChange(uriField));
		applyValidationState(uriField, false);
		if (animated) {
			prepareCollapsedField(uriField);
		}
		uriFields.add(uriField);
		uriFieldsBox.getChildren().add(uriField);
		if (animated) {
			animateFieldExpand(uriField);
		}
	}

	private void handleUriFieldChange(TextField changedField) {
		String value = normalize(changedField.getText());
		applyValidationState(changedField, !value.isBlank() && !isValidUri(value));
		hideValidationMessage();
		if (value.isBlank() && uriFields.size() > 1) {
			Platform.runLater(() -> removeUriFieldIfBlank(changedField));
			return;
		}
		ensureTrailingBlankUriField();
	}

	private void removeUriFieldIfBlank(TextField field) {
		if (!uriFields.contains(field)) {
			return;
		}
		if (!normalize(field.getText()).isBlank()) {
			return;
		}
		if (uriFields.size() <= 1) {
			return;
		}

		int fieldIndex = uriFields.indexOf(field);
		int lastIndex = uriFields.size() - 1;
		boolean hasTrailingBlankField = lastIndex >= 0 && normalize(uriFields.get(lastIndex).getText()).isBlank();
		boolean isPenultimateField = fieldIndex == lastIndex - 1;

		TextField fieldToRemove = (isPenultimateField && hasTrailingBlankField) ? uriFields.get(lastIndex) : field;
		removeFieldWithAnimation(fieldToRemove);
	}

	private void ensureTrailingBlankUriField() {
		if (uriFields.isEmpty()) {
			addUriField("", false);
			return;
		}
		TextField lastField = uriFields.get(uriFields.size() - 1);
		if (!normalize(lastField.getText()).isBlank()) {
			addUriField("", true);
		}
	}

	private void removeFieldWithAnimation(TextField fieldToRemove) {
		if (fieldToRemove == null || !uriFields.contains(fieldToRemove)) {
			return;
		}
		uriFields.remove(fieldToRemove);
		animateFieldCollapse(fieldToRemove, () -> {
			uriFieldsBox.getChildren().remove(fieldToRemove);
			ensureTrailingBlankUriField();
		});
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private void applyValidationState(TextField field, boolean invalid) {
		if (field == null) {
			return;
		}
		if (invalid) {
			if (!field.getStyleClass().contains(STYLE_CLASS_URI_INVALID)) {
				field.getStyleClass().add(STYLE_CLASS_URI_INVALID);
			}
			return;
		}
		field.getStyleClass().remove(STYLE_CLASS_URI_INVALID);
	}

	private static boolean isValidUri(String value) {
		try {
			URI uri = URI.create(value);
			String scheme = uri.getScheme();
			if (scheme == null || scheme.isBlank()) {
				return false;
			}
			if (HTTP_SCHEME.equalsIgnoreCase(scheme) || HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
				return uri.getHost() != null && !uri.getHost().isBlank();
			}
			if (FILE_SCHEME.equalsIgnoreCase(scheme)) {
				return uri.getPath() != null && !uri.getPath().isBlank();
			}
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private void hideValidationMessage() {
		if (!validationLabel.isManaged() && !validationLabel.isVisible()) {
			validationLabel.setText("");
			return;
		}
		animateValidationMessageCollapse();
	}

	private static void prepareCollapsedField(TextField field) {
		field.setOpacity(0);
		field.setMinHeight(0);
		field.setPrefHeight(0);
		field.setMaxHeight(0);
	}

	private static void animateFieldExpand(TextField field) {
		Platform.runLater(() -> {
			double targetHeight = resolveFieldHeight(field);
			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO, new KeyValue(field.opacityProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(field.minHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(field.prefHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(field.maxHeightProperty(), 0, Interpolator.EASE_BOTH)),
					new KeyFrame(FIELD_ANIMATION_DURATION,
							new KeyValue(field.opacityProperty(), 1, Interpolator.EASE_OUT),
							new KeyValue(field.minHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(field.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(field.maxHeightProperty(), targetHeight, Interpolator.EASE_OUT)));
			timeline.setOnFinished(event -> {
				field.setOpacity(1);
				field.setMinHeight(Region.USE_COMPUTED_SIZE);
				field.setPrefHeight(Region.USE_COMPUTED_SIZE);
				field.setMaxHeight(Region.USE_COMPUTED_SIZE);
			});
			timeline.play();
		});
	}

	private static void animateFieldCollapse(TextField field, Runnable onFinished) {
		Platform.runLater(() -> {
			double startHeight = resolveFieldHeight(field);
			field.setMinHeight(startHeight);
			field.setPrefHeight(startHeight);
			field.setMaxHeight(startHeight);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(field.opacityProperty(), field.getOpacity(), Interpolator.EASE_BOTH),
							new KeyValue(field.minHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(field.prefHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(field.maxHeightProperty(), startHeight, Interpolator.EASE_BOTH)),
					new KeyFrame(FIELD_ANIMATION_DURATION,
							new KeyValue(field.opacityProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(field.minHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(field.prefHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(field.maxHeightProperty(), 0, Interpolator.EASE_IN)));
			timeline.setOnFinished(event -> onFinished.run());
			timeline.play();
		});
	}

	private void prepareCollapsedValidationLabel() {
		validationLabel.setText("");
		validationLabel.setOpacity(0);
		validationLabel.setManaged(false);
		validationLabel.setVisible(false);
		validationLabel.setMinHeight(0);
		validationLabel.setPrefHeight(0);
		validationLabel.setMaxHeight(0);
	}

	private void animateValidationMessageExpand() {
		stopValidationAnimation();
		Platform.runLater(() -> {
			validationLabel.applyCss();
			double targetHeight = resolveValidationLabelHeight(validationLabel);
			validationLabel.setManaged(true);
			validationLabel.setVisible(true);
			validationLabel.setOpacity(0);
			validationLabel.setMinHeight(0);
			validationLabel.setPrefHeight(0);
			validationLabel.setMaxHeight(0);

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
				validationLabel.setMinHeight(Region.USE_COMPUTED_SIZE);
				validationLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
				validationLabel.setMaxHeight(Region.USE_COMPUTED_SIZE);
				validationLabel.getProperties().remove(VALIDATION_ANIMATION_KEY);
			});
			validationLabel.getProperties().put(VALIDATION_ANIMATION_KEY, timeline);
			timeline.play();
		});
	}

	private void animateValidationMessageCollapse() {
		stopValidationAnimation();
		Platform.runLater(() -> {
			double startHeight = resolveValidationLabelHeight(validationLabel);
			double startOpacity = validationLabel.getOpacity() > 0 ? validationLabel.getOpacity() : 1;
			validationLabel.setMinHeight(startHeight);
			validationLabel.setPrefHeight(startHeight);
			validationLabel.setMaxHeight(startHeight);

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
				prepareCollapsedValidationLabel();
				validationLabel.getProperties().remove(VALIDATION_ANIMATION_KEY);
			});
			validationLabel.getProperties().put(VALIDATION_ANIMATION_KEY, timeline);
			timeline.play();
		});
	}

	private void stopValidationAnimation() {
		Object animation = validationLabel.getProperties().remove(VALIDATION_ANIMATION_KEY);
		if (animation instanceof Timeline timeline) {
			timeline.stop();
		}
	}

	private static double resolveFieldHeight(TextField field) {
		if (field == null) {
			return FALLBACK_FIELD_HEIGHT;
		}
		double measuredHeight = field.getHeight();
		if (measuredHeight > 0) {
			return measuredHeight;
		}
		double preferredHeight = field.prefHeight(-1);
		if (preferredHeight > 0) {
			return preferredHeight;
		}
		return FALLBACK_FIELD_HEIGHT;
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
}
