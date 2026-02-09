package fr.inria.corese.gui.utils.fx;

import java.util.function.Consumer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Utility to animate DropShadow properties on hover/press interactions.
 *
 * <p>
 * JavaFX CSS does not interpolate {@code -fx-effect} changes, so transitions
 * between shadow states must be animated programmatically.
 */
public final class HoverShadowAnimator {

	private HoverShadowAnimator() {
		// Utility class
	}

	/**
	 * Installs hover and pressed animations for the provided shadow.
	 *
	 * @param interactiveNode
	 *            node receiving mouse events
	 * @param shadow
	 *            effect instance to animate
	 * @param idle
	 *            state when not hovered
	 * @param hover
	 *            state when hovered
	 * @param pressed
	 *            state when pressed
	 * @param duration
	 *            transition duration
	 */
	public static void install(Node interactiveNode, DropShadow shadow, ShadowState idle, ShadowState hover,
			ShadowState pressed, Duration duration) {
		if (interactiveNode == null || shadow == null || idle == null || hover == null || pressed == null) {
			return;
		}

		applyState(shadow, idle);
		final Timeline[] activeTimeline = new Timeline[1];

		Consumer<ShadowState> animateTo = state -> {
			if (activeTimeline[0] != null) {
				activeTimeline[0].stop();
			}
			activeTimeline[0] = new Timeline(new KeyFrame(duration,
					new KeyValue(shadow.radiusProperty(), state.radius(), Interpolator.EASE_BOTH),
					new KeyValue(shadow.spreadProperty(), state.spread(), Interpolator.EASE_BOTH),
					new KeyValue(shadow.offsetYProperty(), state.offsetY(), Interpolator.EASE_BOTH)));
			activeTimeline[0].play();
		};

		interactiveNode.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> animateTo.accept(hover));
		interactiveNode.addEventHandler(MouseEvent.MOUSE_EXITED, e -> animateTo.accept(idle));
		interactiveNode.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> animateTo.accept(pressed));
		interactiveNode.addEventHandler(MouseEvent.MOUSE_RELEASED,
				e -> animateTo.accept(interactiveNode.isHover() ? hover : idle));
	}

	private static void applyState(DropShadow shadow, ShadowState state) {
		shadow.setRadius(state.radius());
		shadow.setSpread(state.spread());
		shadow.setOffsetY(state.offsetY());
	}

	/** Shadow parameters for one interaction state. */
	public record ShadowState(double radius, double spread, double offsetY) {
	}
}
