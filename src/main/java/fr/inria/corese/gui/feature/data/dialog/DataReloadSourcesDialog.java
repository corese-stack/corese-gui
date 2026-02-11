package fr.inria.corese.gui.feature.data.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.dialog.DialogLayout.ImpactTone;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Modal dialog that lets users select tracked sources to reload.
 */
public final class DataReloadSourcesDialog {

	private static final String STYLESHEET = "/css/features/data-reload-sources-dialog.css";
	private static final String STYLE_CLASS_DIALOG = "data-reload-dialog";
	private static final String STYLE_CLASS_CONTENT = "data-reload-dialog-content";
	private static final String STYLE_CLASS_SOURCE_SCROLL = "data-reload-dialog-source-scroll";
	private static final String STYLE_CLASS_SOURCE_LIST = "data-reload-dialog-source-list";
	private static final String STYLE_CLASS_SOURCE_ITEM = "data-reload-dialog-source-item";
	private static final String STYLE_CLASS_INLINE_ERROR = "data-reload-dialog-inline-error";

	private static final String DIALOG_TITLE = "Reload Sources";
	private static final String WARNING_TITLE = "Warning";
	private static final List<String> WARNING_ITEMS = List.of("SPARQL UPDATE modifications are discarded.",
			"Reasoning profiles are reset to OFF.", "Unchecked sources are excluded from future reloads.");
	private static final String EMPTY_SELECTION_MESSAGE = "Select at least one source to reload.";
	private static final int FILE_PATH_MAX_CHARS = 56;
	private static final int URI_MAX_CHARS = 72;
	private static final int SOURCE_VIEWPORT_HEIGHT = 240;
	private static final int SOURCE_MIN_VIEWPORT_HEIGHT = 120;
	private static final Duration MESSAGE_ANIMATION_DURATION = Duration.millis(140);
	private static final double FALLBACK_MESSAGE_HEIGHT = 18.0;
	private static final String VALIDATION_ANIMATION_KEY = "reloadValidationAnimation";
	private static final String BUTTON_ANIMATION_KEY = "reloadButtonAnimation";
	private static final double RELOAD_BUTTON_DISABLED_OPACITY = 0.62;
	private static final double RELOAD_BUTTON_ENABLED_OPACITY = 1.0;

	private DataReloadSourcesDialog() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Shows the source selection dialog for graph reload.
	 *
	 * @param trackedSources
	 *            currently tracked data sources
	 * @param onReloadRequested
	 *            callback invoked with selected sources when user confirms
	 */
	public static void show(List<DataSource> trackedSources, Consumer<List<DataSource>> onReloadRequested) {
		Objects.requireNonNull(onReloadRequested, "onReloadRequested must not be null");
		List<DataSource> safeSources = trackedSources == null ? List.of() : List.copyOf(trackedSources);

		VBox sourceList = new VBox(8);
		sourceList.getStyleClass().add(STYLE_CLASS_SOURCE_LIST);
		sourceList.setFillWidth(true);

		Map<CheckBox, DataSource> sourceSelections = new LinkedHashMap<>();
		for (DataSource source : safeSources) {
			CheckBox checkBox = createSourceCheckBox(source, sourceList);
			sourceSelections.put(checkBox, source);
			sourceList.getChildren().add(checkBox);
		}

		ScrollPane sourceScroll = new ScrollPane(sourceList);
		sourceScroll.getStyleClass().add(STYLE_CLASS_SOURCE_SCROLL);
		sourceScroll.setFitToWidth(true);
		sourceScroll.setPrefViewportHeight(SOURCE_VIEWPORT_HEIGHT);
		sourceScroll.setMinViewportHeight(SOURCE_MIN_VIEWPORT_HEIGHT);
		sourceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		sourceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		sourceScroll.setFitToHeight(false);

		VBox warningBox = DialogLayout.createImpactBlock(WARNING_TITLE, WARNING_ITEMS, ImpactTone.WARNING);

		VBox content = new VBox(10, warningBox, sourceScroll);
		content.getStyleClass().add(STYLE_CLASS_CONTENT);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button reloadButton = new Button("Reload");
		reloadButton.getStyleClass().add(Styles.DANGER);
		Label selectionErrorLabel = new Label(EMPTY_SELECTION_MESSAGE);
		selectionErrorLabel.getStyleClass().add(STYLE_CLASS_INLINE_ERROR);
		selectionErrorLabel.setWrapText(false);
		prepareCollapsedValidationLabel(selectionErrorLabel);

		Runnable updateSelectionState = () -> {
			boolean anySelected = sourceSelections.keySet().stream().anyMatch(CheckBox::isSelected);
			updateReloadActionState(anySelected, reloadButton, selectionErrorLabel);
		};
		sourceSelections.keySet().forEach(checkBox -> checkBox.selectedProperty()
				.addListener((obs, oldValue, newValue) -> updateSelectionState.run()));
		updateSelectionState.run();

		reloadButton.setOnAction(event -> {
			List<DataSource> selectedSources = sourceSelections.entrySet().stream()
					.filter(entry -> entry.getKey().isSelected()).map(Map.Entry::getValue).toList();
			if (selectedSources.isEmpty()) {
				showValidationMessage(selectionErrorLabel, EMPTY_SELECTION_MESSAGE);
				return;
			}
			ModalService.getInstance().hide();
			onReloadRequested.accept(selectedSources);
		});

		DialogLayout dialog = new DialogLayout(DIALOG_TITLE, content, selectionErrorLabel, cancelButton, reloadButton);
		dialog.getStyleClass().add(STYLE_CLASS_DIALOG);
		CssUtils.applyViewStyles(dialog, STYLESHEET);
		ModalService.getInstance().show(dialog);
	}

	private static String formatDisplayLabel(DataSource source) {
		if (source == null) {
			return "Unknown source";
		}
		int maxChars = source.type() == SourceType.FILE ? FILE_PATH_MAX_CHARS : URI_MAX_CHARS;
		return abbreviateKeepingTail(source.location(), maxChars);
	}

	private static CheckBox createSourceCheckBox(DataSource source, VBox sourceList) {
		CheckBox checkBox = new CheckBox(formatDisplayLabel(source));
		checkBox.setSelected(true);
		checkBox.setWrapText(false);
		checkBox.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
		checkBox.setMaxWidth(Double.MAX_VALUE);
		checkBox.prefWidthProperty().bind(sourceList.widthProperty());
		checkBox.getStyleClass().add(STYLE_CLASS_SOURCE_ITEM);
		checkBox.setTooltip(new Tooltip(formatTooltipText(source)));
		return checkBox;
	}

	private static String formatTooltipText(DataSource source) {
		if (source == null) {
			return "Unknown source";
		}
		if (source.type() == SourceType.FILE) {
			return "File: " + source.location();
		}
		return "URI: " + source.location();
	}

	private static String abbreviateKeepingTail(String value, int maxChars) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String normalized = value.trim();
		if (normalized.length() <= maxChars) {
			return normalized;
		}
		if (maxChars <= 3) {
			return normalized.substring(normalized.length() - maxChars);
		}
		return "..." + normalized.substring(normalized.length() - (maxChars - 3));
	}

	private static void prepareCollapsedValidationLabel(Label label) {
		label.setText("");
		label.setOpacity(0);
		label.setManaged(false);
		label.setVisible(false);
		label.setMinHeight(0);
		label.setPrefHeight(0);
		label.setMaxHeight(0);
	}

	private static void showValidationMessage(Label label, String message) {
		String safeMessage = message == null ? "" : message.trim();
		if (safeMessage.isBlank()) {
			hideValidationMessage(label);
			return;
		}
		stopValidationAnimation(label);
		label.setText(safeMessage);
		if (label.isManaged() && label.isVisible()) {
			label.setOpacity(1);
			label.setMinHeight(Region.USE_COMPUTED_SIZE);
			label.setPrefHeight(Region.USE_COMPUTED_SIZE);
			label.setMaxHeight(Region.USE_COMPUTED_SIZE);
			return;
		}
		animateValidationMessageExpand(label);
	}

	private static void hideValidationMessage(Label label) {
		if (!label.isManaged() && !label.isVisible()) {
			label.setText("");
			return;
		}
		animateValidationMessageCollapse(label);
	}

	private static void updateReloadActionState(boolean hasSelection, Button reloadButton, Label validationLabel) {
		if (hasSelection) {
			hideValidationMessage(validationLabel);
			animateReloadButtonState(reloadButton, true);
		} else {
			showValidationMessage(validationLabel, EMPTY_SELECTION_MESSAGE);
			animateReloadButtonState(reloadButton, false);
		}
	}

	private static void animateValidationMessageExpand(Label label) {
		stopValidationAnimation(label);
		Platform.runLater(() -> {
			label.applyCss();
			double targetHeight = resolveValidationLabelHeight(label);
			label.setManaged(true);
			label.setVisible(true);
			label.setOpacity(0);
			label.setMinHeight(0);
			label.setPrefHeight(0);
			label.setMaxHeight(0);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO, new KeyValue(label.opacityProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(label.minHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(label.prefHeightProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(label.maxHeightProperty(), 0, Interpolator.EASE_BOTH)),
					new KeyFrame(MESSAGE_ANIMATION_DURATION,
							new KeyValue(label.opacityProperty(), 1, Interpolator.EASE_OUT),
							new KeyValue(label.minHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(label.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(label.maxHeightProperty(), targetHeight, Interpolator.EASE_OUT)));
			timeline.setOnFinished(event -> {
				label.setOpacity(1);
				label.setMinHeight(Region.USE_COMPUTED_SIZE);
				label.setPrefHeight(Region.USE_COMPUTED_SIZE);
				label.setMaxHeight(Region.USE_COMPUTED_SIZE);
				label.getProperties().remove(VALIDATION_ANIMATION_KEY);
			});
			label.getProperties().put(VALIDATION_ANIMATION_KEY, timeline);
			timeline.play();
		});
	}

	private static void animateValidationMessageCollapse(Label label) {
		stopValidationAnimation(label);
		Platform.runLater(() -> {
			double startHeight = resolveValidationLabelHeight(label);
			double startOpacity = label.getOpacity() > 0 ? label.getOpacity() : 1;
			label.setMinHeight(startHeight);
			label.setPrefHeight(startHeight);
			label.setMaxHeight(startHeight);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(label.opacityProperty(), startOpacity, Interpolator.EASE_BOTH),
							new KeyValue(label.minHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(label.prefHeightProperty(), startHeight, Interpolator.EASE_BOTH),
							new KeyValue(label.maxHeightProperty(), startHeight, Interpolator.EASE_BOTH)),
					new KeyFrame(MESSAGE_ANIMATION_DURATION,
							new KeyValue(label.opacityProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(label.minHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(label.prefHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(label.maxHeightProperty(), 0, Interpolator.EASE_IN)));
			timeline.setOnFinished(event -> {
				prepareCollapsedValidationLabel(label);
				label.getProperties().remove(VALIDATION_ANIMATION_KEY);
			});
			label.getProperties().put(VALIDATION_ANIMATION_KEY, timeline);
			timeline.play();
		});
	}

	private static void stopValidationAnimation(Label label) {
		Object animation = label.getProperties().remove(VALIDATION_ANIMATION_KEY);
		if (animation instanceof Timeline timeline) {
			timeline.stop();
		}
	}

	private static void animateReloadButtonState(Button button, boolean enabled) {
		stopReloadButtonAnimation(button);
		double targetOpacity = enabled ? RELOAD_BUTTON_ENABLED_OPACITY : RELOAD_BUTTON_DISABLED_OPACITY;
		boolean sameState = button.isDisable() == !enabled;
		boolean sameOpacity = Math.abs(button.getOpacity() - targetOpacity) < 0.001;
		if (sameState && sameOpacity) {
			button.setMouseTransparent(false);
			return;
		}

		if (enabled) {
			button.setDisable(false);
		}
		button.setMouseTransparent(true);

		Platform.runLater(() -> {
			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(button.opacityProperty(), button.getOpacity(), Interpolator.EASE_BOTH)),
					new KeyFrame(MESSAGE_ANIMATION_DURATION,
							new KeyValue(button.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH)));
			timeline.setOnFinished(event -> {
				button.setOpacity(targetOpacity);
				button.setDisable(!enabled);
				button.setMouseTransparent(false);
				button.getProperties().remove(BUTTON_ANIMATION_KEY);
			});
			button.getProperties().put(BUTTON_ANIMATION_KEY, timeline);
			timeline.play();
		});
	}

	private static void stopReloadButtonAnimation(Button button) {
		Object animation = button.getProperties().remove(BUTTON_ANIMATION_KEY);
		if (animation instanceof Timeline timeline) {
			timeline.stop();
		}
	}

	private static double resolveValidationLabelHeight(Label label) {
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
