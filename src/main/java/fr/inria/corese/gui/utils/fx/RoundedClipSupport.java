package fr.inria.corese.gui.utils.fx;

import javafx.scene.layout.Region;
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
}
