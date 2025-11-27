package fr.inria.corese.gui.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Utility class for creating navigation bar animations.
 *
 * <p>This class encapsulates all animation logic for the sidebar collapse/expand transitions,
 * separating animation concerns from the main view logic.
 */
final class NavigationBarAnimations {

  // ==== Animation constants ====
  // Width
  private static final double EXPANDED_WIDTH = 200;
  private static final double COLLAPSED_WIDTH = 72;

  // Logo sizing
  private static final double LOGO_EXPANDED_SIZE = 52;
  private static final double LOGO_COLLAPSED_SIZE = 28;

  // Animation timing
  private static final Duration ANIM_DURATION = Duration.millis(250);
  private static final Duration TEXT_FADE_DURATION = Duration.millis(150);
  private static final Duration TEXT_FADE_DELAY = Duration.millis(50);

  // Text animation
  private static final double TEXT_SLIDE_DISTANCE = 18;

  private NavigationBarAnimations() {
    // Utility class - prevent instantiation
  }

  /** Returns the expanded logo size for initial view setup. */
  static double getLogoExpandedSize() {
    return LOGO_EXPANDED_SIZE;
  }

  /**
   * Creates a complete collapse/expand animation for the navigation bar.
   *
   * @param root the root VBox container
   * @param toggleIcon the chevron icon that rotates
   * @param logoView the logo ImageView that resizes
   * @param navigationButtons array of navigation buttons with text labels
   * @param collapsed true if collapsing, false if expanding
   * @return the complete animation ready to play
   */
  static ParallelTransition createToggleAnimation(
      VBox root,
      FontIcon toggleIcon,
      ImageView logoView,
      Button[] navigationButtons,
      boolean collapsed) {

    double targetWidth = collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
    double iconRotation = collapsed ? 180 : 0;
    double targetLogoSize = collapsed ? LOGO_COLLAPSED_SIZE : LOGO_EXPANDED_SIZE;

    Timeline widthAnim = createWidthAnimation(root, targetWidth);
    RotateTransition iconRotate = createIconRotation(toggleIcon, iconRotation);
    Timeline logoAnim = createLogoAnimation(logoView, targetLogoSize);

    Duration delay = collapsed ? Duration.ZERO : TEXT_FADE_DELAY;
    ParallelTransition textAnim = createTextTransition(navigationButtons, !collapsed, delay);

    return new ParallelTransition(widthAnim, iconRotate, logoAnim, textAnim);
  }

  /** Creates the sidebar width animation. */
  private static Timeline createWidthAnimation(VBox root, double targetWidth) {
    return new Timeline(
        new KeyFrame(
            ANIM_DURATION,
            new KeyValue(root.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
            new KeyValue(root.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
            new KeyValue(root.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH)));
  }

  /** Creates the toggle icon rotation animation. */
  private static RotateTransition createIconRotation(FontIcon icon, double targetAngle) {
    RotateTransition rotation = new RotateTransition(ANIM_DURATION, icon);
    rotation.setToAngle(targetAngle);
    rotation.setInterpolator(Interpolator.EASE_BOTH);
    return rotation;
  }

  /** Creates the logo resize animation. */
  private static Timeline createLogoAnimation(ImageView logoView, double targetSize) {
    return new Timeline(
        new KeyFrame(
            ANIM_DURATION,
            new KeyValue(logoView.fitWidthProperty(), targetSize, Interpolator.EASE_BOTH),
            new KeyValue(logoView.fitHeightProperty(), targetSize, Interpolator.EASE_BOTH)));
  }

  /**
   * Creates fade in/out animation for button text labels.
   *
   * @param buttons array of buttons containing text labels
   * @param fadeIn true to fade in, false to fade out
   * @param delay delay before starting the animation
   * @return parallel animation for all text labels
   */
  private static ParallelTransition createTextTransition(
      Button[] buttons, boolean fadeIn, Duration delay) {
    ParallelTransition group = new ParallelTransition();

    for (Button button : buttons) {
      Label label = (Label) button.getUserData();
      if (label == null) {
        continue;
      }

      if (fadeIn) {
        group.getChildren().add(createFadeInAnimation(label, delay));
      } else {
        group.getChildren().add(createFadeOutAnimation(label));
      }
    }

    return group;
  }

  /**
   * Creates fade-in animation for a single label.
   *
   * @param label the label to animate
   * @param delay delay before starting the animation
   * @return the fade-in animation
   */
  private static SequentialTransition createFadeInAnimation(Label label, Duration delay) {
    // Initialize hidden state
    label.setVisible(false);
    label.setManaged(false);
    label.setOpacity(0);
    label.setTranslateX(-TEXT_SLIDE_DISTANCE);

    // Wait before showing
    PauseTransition wait = new PauseTransition(delay);
    wait.setOnFinished(
        ev -> {
          label.setVisible(true);
          label.setManaged(true);
        });

    // Fade in and slide
    Timeline appear =
        new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(label.opacityProperty(), 0),
                new KeyValue(label.translateXProperty(), -TEXT_SLIDE_DISTANCE)),
            new KeyFrame(
                TEXT_FADE_DURATION,
                new KeyValue(label.opacityProperty(), 1),
                new KeyValue(label.translateXProperty(), 0)));

    return new SequentialTransition(wait, appear);
  }

  /**
   * Creates fade-out animation for a single label.
   *
   * @param label the label to animate
   * @return the fade-out animation
   */
  private static Timeline createFadeOutAnimation(Label label) {
    Timeline disappear =
        new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(label.opacityProperty(), 1),
                new KeyValue(label.translateXProperty(), 0)),
            new KeyFrame(
                TEXT_FADE_DURATION,
                new KeyValue(label.opacityProperty(), 0),
                new KeyValue(label.translateXProperty(), -TEXT_SLIDE_DISTANCE)));

    disappear.setOnFinished(
        e -> {
          label.setVisible(false);
          label.setManaged(false);
          label.setTranslateX(0);
        });

    return disappear;
  }
}
