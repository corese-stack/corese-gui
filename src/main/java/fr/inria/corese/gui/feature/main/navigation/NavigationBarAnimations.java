package fr.inria.corese.gui.feature.main.navigation;

import fr.inria.corese.gui.core.enums.ViewId;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Animation utilities for the navigation bar.
 *
 * <p>Provides smooth transitions when collapsing/expanding the sidebar, including:
 *
 * <ul>
 *   <li>Sidebar width animation
 *   <li>Logo size animation
 *   <li>Label fade in/out animations
 * </ul>
 */
public final class NavigationBarAnimations {

  private static final Duration ANIMATION_DURATION = Duration.millis(200);

  // Prevent instantiation
  private NavigationBarAnimations() {}

  /**
   * Animates the sidebar collapsing to a smaller width.
   *
   * @param sidebar the root container to resize
   * @param logoImageView the logo to shrink
   * @param navButtons map of navigation buttons containing labels to hide
   * @param targetWidth the target width for the collapsed sidebar
   * @param targetLogoSize the target logo size
   */
  public static void animateCollapse(
      VBox sidebar,
      ImageView logoImageView,
      Map<ViewId, HBox> navButtons,
      double targetWidth,
      double targetLogoSize) {

    List<Transition> transitions = new ArrayList<>();

    // Sidebar width animation
    transitions.add(createWidthAnimation(sidebar, targetWidth));

    // Logo size animation
    transitions.add(createLogoAnimation(logoImageView, targetLogoSize));

    // Fade out labels
    for (HBox button : navButtons.values()) {
      Node label = findLabelNode(button);
      if (label != null) {
        transitions.add(createFadeOut(label));
      }
    }

    ParallelTransition parallel = new ParallelTransition();
    parallel.getChildren().addAll(transitions);
    parallel.play();
  }

  /**
   * Animates the sidebar expanding to its full width.
   *
   * @param sidebar the root container to resize
   * @param logoImageView the logo to enlarge
   * @param navButtons map of navigation buttons containing labels to show
   * @param targetWidth the target width for the expanded sidebar
   * @param targetLogoSize the target logo size
   */
  public static void animateExpand(
      VBox sidebar,
      ImageView logoImageView,
      Map<ViewId, HBox> navButtons,
      double targetWidth,
      double targetLogoSize) {

    List<Transition> transitions = new ArrayList<>();

    // Sidebar width animation
    transitions.add(createWidthAnimation(sidebar, targetWidth));

    // Logo size animation
    transitions.add(createLogoAnimation(logoImageView, targetLogoSize));

    // Fade in labels
    for (HBox button : navButtons.values()) {
      Node label = findLabelNode(button);
      if (label != null) {
        transitions.add(createFadeIn(label));
      }
    }

    ParallelTransition parallel = new ParallelTransition();
    parallel.getChildren().addAll(transitions);
    parallel.play();
  }

  // ===== Animation Helpers =====

  /**
   * Creates an animation to smoothly change the sidebar width.
   *
   * @param sidebar the sidebar VBox
   * @param targetWidth the target width
   * @return a Transition animating the width change
   */
  private static Transition createWidthAnimation(VBox sidebar, double targetWidth) {
    return new Transition() {
      {
        setCycleDuration(ANIMATION_DURATION);
      }

      private final double startWidth = sidebar.getPrefWidth();

      @Override
      protected void interpolate(double frac) {
        double newWidth = startWidth + (targetWidth - startWidth) * frac;
        sidebar.setPrefWidth(newWidth);
        sidebar.setMinWidth(newWidth);
        sidebar.setMaxWidth(newWidth);
      }
    };
  }

  /**
   * Creates an animation to smoothly change the logo size.
   *
   * @param logoImageView the logo ImageView
   * @param targetSize the target size (both width and height)
   * @return a Transition animating the size change
   */
  private static Transition createLogoAnimation(ImageView logoImageView, double targetSize) {
    return new Transition() {
      {
        setCycleDuration(ANIMATION_DURATION);
      }

      private final double startWidth = logoImageView.getFitWidth();
      private final double startHeight = logoImageView.getFitHeight();

      @Override
      protected void interpolate(double frac) {
        double newSize = startWidth + (targetSize - startWidth) * frac;
        logoImageView.setFitWidth(newSize);
        logoImageView.setFitHeight(newSize);
      }
    };
  }

  /**
   * Creates a fade-out animation for a node.
   *
   * @param node the node to fade out
   * @return a FadeTransition
   */
  private static FadeTransition createFadeOut(Node node) {
    FadeTransition fade = new FadeTransition(ANIMATION_DURATION, node);
    fade.setFromValue(node.getOpacity());
    fade.setToValue(0.0);
    fade.setOnFinished(event -> node.setVisible(false));
    return fade;
  }

  /**
   * Creates a fade-in animation for a node.
   *
   * @param node the node to fade in
   * @return a FadeTransition
   */
  private static FadeTransition createFadeIn(Node node) {
    node.setVisible(true);
    FadeTransition fade = new FadeTransition(ANIMATION_DURATION, node);
    fade.setFromValue(0.0);
    fade.setToValue(1.0);
    return fade;
  }

  /**
   * Finds the label node in a navigation button.
   *
   * @param button the HBox containing icon + label
   * @return the label node, or null if not found
   */
  private static Node findLabelNode(HBox button) {
    // Assumes the second child is the label
    if (button.getChildren().size() > 1) {
      return button.getChildren().get(1);
    }
    return null;
  }
}
