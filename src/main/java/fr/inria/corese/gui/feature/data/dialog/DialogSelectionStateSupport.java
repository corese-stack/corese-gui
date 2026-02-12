package fr.inria.corese.gui.feature.data.dialog;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Shared animation helper for selection dialogs with an inline validation label
 * and one primary action button.
 */
final class DialogSelectionStateSupport {

	private static final Duration ANIMATION_DURATION = Duration.millis(140);
	private static final double FALLBACK_MESSAGE_HEIGHT = 18.0;
	private static final double ACTION_BUTTON_DISABLED_OPACITY = 0.62;
	private static final double ACTION_BUTTON_ENABLED_OPACITY = 1.0;

	private DialogSelectionStateSupport() {
		throw new AssertionError("Utility class");
	}

	static void prepareCollapsedValidationLabel(Label label) {
		label.setText("");
		label.setOpacity(0);
		label.setManaged(false);
		label.setVisible(false);
		label.setMinHeight(0);
		label.setPrefHeight(0);
		label.setMaxHeight(0);
	}

	static void showValidationMessage(Label label, String message, String validationAnimationKey) {
		String safeMessage = message == null ? "" : message.trim();
		if (safeMessage.isBlank()) {
			hideValidationMessage(label, validationAnimationKey);
			return;
		}
		stopValidationAnimation(label, validationAnimationKey);
		label.setText(safeMessage);
		if (label.isManaged() && label.isVisible()) {
			label.setOpacity(1);
			label.setMinHeight(Region.USE_COMPUTED_SIZE);
			label.setPrefHeight(Region.USE_COMPUTED_SIZE);
			label.setMaxHeight(Region.USE_COMPUTED_SIZE);
			return;
		}
		animateValidationMessageExpand(label, validationAnimationKey);
	}

	static void updateActionState(boolean hasSelection, Button actionButton, Label validationLabel,
			String emptySelectionMessage, String validationAnimationKey, String buttonAnimationKey) {
		if (hasSelection) {
			hideValidationMessage(validationLabel, validationAnimationKey);
			animateActionButtonState(actionButton, true, buttonAnimationKey);
		} else {
			showValidationMessage(validationLabel, emptySelectionMessage, validationAnimationKey);
			animateActionButtonState(actionButton, false, buttonAnimationKey);
		}
	}

	private static void hideValidationMessage(Label label, String validationAnimationKey) {
		if (!label.isManaged() && !label.isVisible()) {
			label.setText("");
			return;
		}
		animateValidationMessageCollapse(label, validationAnimationKey);
	}

	private static void animateValidationMessageExpand(Label label, String validationAnimationKey) {
		stopValidationAnimation(label, validationAnimationKey);
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
					new KeyFrame(ANIMATION_DURATION, new KeyValue(label.opacityProperty(), 1, Interpolator.EASE_OUT),
							new KeyValue(label.minHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(label.prefHeightProperty(), targetHeight, Interpolator.EASE_OUT),
							new KeyValue(label.maxHeightProperty(), targetHeight, Interpolator.EASE_OUT)));
			timeline.setOnFinished(event -> {
				label.setOpacity(1);
				label.setMinHeight(Region.USE_COMPUTED_SIZE);
				label.setPrefHeight(Region.USE_COMPUTED_SIZE);
				label.setMaxHeight(Region.USE_COMPUTED_SIZE);
				label.getProperties().remove(validationAnimationKey);
			});
			label.getProperties().put(validationAnimationKey, timeline);
			timeline.play();
		});
	}

	private static void animateValidationMessageCollapse(Label label, String validationAnimationKey) {
		stopValidationAnimation(label, validationAnimationKey);
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
					new KeyFrame(ANIMATION_DURATION, new KeyValue(label.opacityProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(label.minHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(label.prefHeightProperty(), 0, Interpolator.EASE_IN),
							new KeyValue(label.maxHeightProperty(), 0, Interpolator.EASE_IN)));
			timeline.setOnFinished(event -> {
				prepareCollapsedValidationLabel(label);
				label.getProperties().remove(validationAnimationKey);
			});
			label.getProperties().put(validationAnimationKey, timeline);
			timeline.play();
		});
	}

	private static void stopValidationAnimation(Label label, String validationAnimationKey) {
		Object animation = label.getProperties().remove(validationAnimationKey);
		if (animation instanceof Timeline timeline) {
			timeline.stop();
		}
	}

	private static void animateActionButtonState(Button button, boolean enabled, String buttonAnimationKey) {
		stopActionButtonAnimation(button, buttonAnimationKey);
		double targetOpacity = enabled ? ACTION_BUTTON_ENABLED_OPACITY : ACTION_BUTTON_DISABLED_OPACITY;
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
					new KeyFrame(ANIMATION_DURATION,
							new KeyValue(button.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH)));
			timeline.setOnFinished(event -> {
				button.setOpacity(targetOpacity);
				button.setDisable(!enabled);
				button.setMouseTransparent(false);
				button.getProperties().remove(buttonAnimationKey);
			});
			button.getProperties().put(buttonAnimationKey, timeline);
			timeline.play();
		});
	}

	private static void stopActionButtonAnimation(Button button, String buttonAnimationKey) {
		Object animation = button.getProperties().remove(buttonAnimationKey);
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
