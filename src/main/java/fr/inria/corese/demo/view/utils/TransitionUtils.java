package fr.inria.corese.demo.view.utils;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** Utility class for common JavaFX transitions and animations. */
public final class TransitionUtils {

  // ==== Constructor =====

  /** Private constructor to prevent instantiation of this utility class. */
  private TransitionUtils() {}

  // ==== Static Methods =====

  /**
   * Applies a fade-out transition to the current content of the container, replaces it with the new
   * content, and then applies a fade-in transition.
   *
   * @param container the parent node containing the content to be replaced
   * @param newContent the new content to display
   * @param duration the duration of the fade transitions
   */
  public static void fadeReplace(Node container, Node newContent, Duration duration) {
    if (container == null) return;

    FadeTransition fadeOut = new FadeTransition(duration, container);
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.0);

    fadeOut.setOnFinished(
        e -> {
          if (container instanceof javafx.scene.layout.BorderPane pane) {
            pane.setCenter(newContent);
          } else if (container instanceof javafx.scene.layout.StackPane pane) {
            pane.getChildren().setAll(newContent);
          }

          FadeTransition fadeIn = new FadeTransition(duration, container);
          fadeIn.setFromValue(0.0);
          fadeIn.setToValue(1.0);
          fadeIn.play();
        });

    fadeOut.play();
  }
}
