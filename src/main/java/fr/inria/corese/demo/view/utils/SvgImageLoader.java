package fr.inria.corese.demo.view.utils;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvgImageLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(SvgImageLoader.class);

  private SvgImageLoader() {
    // Utility class
  }

  /**
   * Loads an SVG file from resources and renders it to a JavaFX Image.
   *
   * @param resourcePath The path to the SVG resource.
   * @param width The desired width of the image.
   * @param height The desired height of the image.
   * @return A JavaFX Image containing the rendered SVG, or null if loading fails.
   */
  public static Image loadSvgImage(String resourcePath, double width, double height) {
    java.net.URL url = SvgImageLoader.class.getResource(resourcePath);
    if (url == null) {
      LOGGER.error("SVG resource not found: {}", resourcePath);
      return null;
    }

    try {
      SVGLoader loader = new SVGLoader();
      SVGDocument svgDocument = loader.load(url);

      if (svgDocument == null) {
        LOGGER.error("Failed to parse SVG document: {}", resourcePath);
        return null;
      }

      // Calculate dimensions
      // If width/height are <= 0, use the SVG's intrinsic size or a default
      float svgWidth = svgDocument.size().width;
      float svgHeight = svgDocument.size().height;

      int targetWidth = (int) width;
      int targetHeight = (int) height;

      if (targetWidth <= 0) targetWidth = (int) svgWidth;
      if (targetHeight <= 0) targetHeight = (int) svgHeight;

      // Render to BufferedImage
      BufferedImage bufferedImage =
          new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = bufferedImage.createGraphics();

      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

      // Scale to fit the target dimensions
      double scaleX = (double) targetWidth / svgWidth;
      double scaleY = (double) targetHeight / svgHeight;
      // Maintain aspect ratio if needed, but here we assume caller knows what they want or we fit
      // to box
      double scale = Math.min(scaleX, scaleY);

      // Center the image
      double tx = (targetWidth - svgWidth * scale) / 2;
      double ty = (targetHeight - svgHeight * scale) / 2;

      g2d.translate(tx, ty);
      g2d.scale(scale, scale);

      svgDocument.render(null, g2d, null);
      g2d.dispose();

      // Convert to JavaFX Image
      return SwingFXUtils.toFXImage(bufferedImage, null);

    } catch (Exception e) {
      LOGGER.error("Error loading SVG image: " + resourcePath, e);
      return null;
    }
  }

  /**
   * Loads an SVG file from resources and renders it to a JavaFX Image with a scaling factor. Useful
   * for high-DPI displays.
   *
   * @param resourcePath The path to the SVG resource.
   * @param width The desired display width.
   * @param height The desired display height.
   * @param scaleFactor The scaling factor (e.g. 2.0 for Retina).
   * @return A JavaFX Image.
   */
  public static Image loadSvgImage(
      String resourcePath, double width, double height, double scaleFactor) {
    return loadSvgImage(resourcePath, width * scaleFactor, height * scaleFactor);
  }
}
