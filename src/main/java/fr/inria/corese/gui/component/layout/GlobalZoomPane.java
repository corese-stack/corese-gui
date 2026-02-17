package fr.inria.corese.gui.component.layout;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/**
 * A single-child pane that applies a uniform global zoom while preserving
 * viewport fit.
 *
 * <p>
 * The child is laid out at inverse size ({@code width / zoom},
 * {@code height / zoom}) and then scaled, so content remains fully visible in
 * the scene without overflowing the stage bounds.
 */
public final class GlobalZoomPane extends Pane {

	private static final double MIN_ZOOM = 0.5;
	private static final double MAX_ZOOM = 2.0;
	private static final double DEFAULT_ZOOM = 1.0;
	private static final double EPSILON = 0.0001;

	private final Node content;
	private final Scale scaleTransform = new Scale(1.0, 1.0, 0.0, 0.0);
	private final DoubleProperty zoom = new SimpleDoubleProperty(DEFAULT_ZOOM);

	public GlobalZoomPane(Node content) {
		this.content = content;
		this.content.getTransforms().add(scaleTransform);
		getChildren().add(content);

		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(widthProperty());
		clip.heightProperty().bind(heightProperty());
		setClip(clip);

		zoom.addListener((obs, oldValue, newValue) -> {
			double requested = newValue == null ? DEFAULT_ZOOM : newValue.doubleValue();
			double safeZoom = clampZoom(requested);
			if (Math.abs(safeZoom - requested) > EPSILON) {
				zoom.set(safeZoom);
				return;
			}
			scaleTransform.setX(safeZoom);
			scaleTransform.setY(safeZoom);
			requestLayout();
		});
	}

	public DoubleProperty zoomProperty() {
		return zoom;
	}

	public double getZoom() {
		return zoom.get();
	}

	public void setZoom(double zoom) {
		this.zoom.set(clampZoom(zoom));
	}

	@Override
	protected void layoutChildren() {
		double zoomValue = clampZoom(getZoom());
		double contentWidth = getWidth() / zoomValue;
		double contentHeight = getHeight() / zoomValue;
		layoutInArea(content, 0, 0, contentWidth, contentHeight, 0, HPos.LEFT, VPos.TOP);
	}

	private static double clampZoom(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return DEFAULT_ZOOM;
		}
		return Math.clamp(value, MIN_ZOOM, MAX_ZOOM);
	}
}
