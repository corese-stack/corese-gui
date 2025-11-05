package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.view.NavigationBarView;
import fr.inria.corese.demo.view.ViewId;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Controller for the application's sidebar navigation.
 *
 * <p>Handles navigation button actions and notifies the parent {@link MainController} when a new
 * view is selected.
 */
public final class NavigationBarController {

  // ===== Fields =====

  private static final Logger LOGGER = Logger.getLogger(NavigationBarController.class.getName());

  /** The navigation bar view. */
  private final NavigationBarView view = new NavigationBarView();

  /** Callback invoked when a navigation action occurs. */
  private Consumer<ViewId> onNavigate;

  // ===== Constructor =====

  /** Creates the navigation bar controller and initializes button actions. */
  public NavigationBarController() {
    initializeButtonActions();
  }

  /** Assigns click actions to all navigation buttons. */
  private void initializeButtonActions() {
    view.getDataButton().setOnAction(e -> navigate(ViewId.DATA));
    view.getRdfEditorButton().setOnAction(e -> navigate(ViewId.RDF_EDITOR));
    view.getValidationButton().setOnAction(e -> navigate(ViewId.VALIDATION));
    view.getQueryButton().setOnAction(e -> navigate(ViewId.QUERY));
    view.getSettingsButton().setOnAction(e -> navigate(ViewId.SETTINGS));
  }

  // ===== Private Methods =====

  /** Notifies the parent controller when a navigation action occurs. */
  private void navigate(ViewId viewId) {
    LOGGER.fine(() -> "Navigation requested: " + viewId);
    if (onNavigate != null) {
      onNavigate.accept(viewId);
    }
  }

  // ===== Public Methods =====

  /** Highlights the active navigation button. */
  public void setActiveView(ViewId activeView) {
    view.setButtonSelected(view.getButtonForView(activeView));
  }

  // === Accessors ===

  public NavigationBarView getView() {
    return view;
  }

  public void setOnNavigate(Consumer<ViewId> handler) {
    this.onNavigate = handler;
  }
}
