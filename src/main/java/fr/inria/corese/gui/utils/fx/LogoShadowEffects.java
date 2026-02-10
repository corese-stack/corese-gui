package fr.inria.corese.gui.utils.fx;

import fr.inria.corese.gui.core.theme.ThemeManager;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Shared logo shadow defaults and helpers.
 *
 * <p>
 * Centralizes shadow behavior so logos across the app stay visually consistent.
 */
public final class LogoShadowEffects {

	public static final Duration TRANSITION_DURATION = Duration.millis(160);
	public static final HoverShadowAnimator.ShadowState IDLE = new HoverShadowAnimator.ShadowState(10, 0.0, 1);
	public static final HoverShadowAnimator.ShadowState HOVER = new HoverShadowAnimator.ShadowState(18, 0.0, 3);
	public static final HoverShadowAnimator.ShadowState PRESSED = new HoverShadowAnimator.ShadowState(12, 0.0, 1);

	private LogoShadowEffects() {
		// Utility class
	}

	/** Creates a theme-aware DropShadow initialized with the default idle state. */
	public static DropShadow createThemeAwareShadow() {
		DropShadow shadow = new DropShadow();
		shadow.setColor(resolveLogoShadowColor());
		shadow.setRadius(IDLE.radius());
		shadow.setSpread(IDLE.spread());
		shadow.setOffsetY(IDLE.offsetY());
		ThemeManager.getInstance().themeProperty()
				.addListener((obs, oldTheme, newTheme) -> shadow.setColor(resolveLogoShadowColor()));
		return shadow;
	}

	/** Installs the default smooth hover/press animation on a logo shadow. */
	public static void installDefaultAnimation(Node interactiveNode, DropShadow shadow) {
		HoverShadowAnimator.install(interactiveNode, shadow, IDLE, HOVER, PRESSED, TRANSITION_DURATION);
	}

	private static Color resolveLogoShadowColor() {
		return ThemeManager.getInstance().getLogoShadowColor();
	}
}
