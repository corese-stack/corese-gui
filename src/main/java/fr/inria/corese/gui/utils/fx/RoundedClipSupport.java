package fr.inria.corese.gui.utils.fx;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;

/**
 * Utility methods for applying rounded corner clipping on JavaFX regions.
 *
 * <p>
 * JavaFX border/background radius do not clip child content by default.
 * Applying this clip ensures visual corners remain rounded when children fill
 * the full surface.
 */
public final class RoundedClipSupport {

	private RoundedClipSupport() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Applies a rounded rectangle clip synchronized with the region bounds.
	 *
	 * @param region
	 *            target region
	 * @param radius
	 *            corner radius in px
	 */
	public static void applyRoundedClip(Region region, double radius) {
		if (region == null || radius < 0) {
			return;
		}
		Rectangle clip = new Rectangle();
		clip.setArcWidth(radius * 2);
		clip.setArcHeight(radius * 2);
		region.layoutBoundsProperty().addListener((observable, previous, bounds) -> {
			clip.setWidth(bounds.getWidth());
			clip.setHeight(bounds.getHeight());
		});
		region.setClip(clip);
	}

	/**
	 * Applies a clip with only bottom corners rounded.
	 *
	 * @param region
	 *            target region
	 * @param radius
	 *            bottom corner radius in px
	 */
	public static void applyBottomRoundedClip(Region region, double radius) {
		if (region == null || radius < 0) {
			return;
		}

		Path clip = new Path();
		clip.setFill(Color.BLACK);
		region.layoutBoundsProperty().addListener((observable, previous, bounds) -> {
			double width = bounds.getWidth();
			double height = bounds.getHeight();
			double safeRadius = Math.min(radius, Math.min(width, height) / 2.0);

			clip.getElements().setAll(new MoveTo(0, 0), new LineTo(0, Math.max(0, height - safeRadius)),
					new QuadCurveTo(0, height, safeRadius, height), new LineTo(Math.max(0, width - safeRadius), height),
					new QuadCurveTo(width, height, width, Math.max(0, height - safeRadius)), new LineTo(width, 0),
					new ClosePath());
		});
		region.setClip(clip);
	}
}
