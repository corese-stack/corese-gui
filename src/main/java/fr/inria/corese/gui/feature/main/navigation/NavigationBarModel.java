package fr.inria.corese.gui.feature.main.navigation;

import fr.inria.corese.gui.core.enums.ViewId;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Model representing the state of the navigation bar.
 *
 * <p>Tracks:
 *
 * <ul>
 *   <li>The currently active view ({@link ViewId})
 *   <li>Whether the sidebar is collapsed or expanded
 * </ul>
 *
 * <p>Uses JavaFX properties to enable reactive bindings in the controller and view.
 */
public final class NavigationBarModel {

  /** The currently active view. Defaults to DATA. */
  private final ObjectProperty<ViewId> activeView =
      new SimpleObjectProperty<>(ViewId.DATA);

  /** Whether the sidebar is collapsed. Defaults to expanded (false). */
  private final BooleanProperty collapsed = new SimpleBooleanProperty(false);

  // ===== Active View =====

  public ViewId getActiveView() {
    return activeView.get();
  }

  public void setActiveView(ViewId viewId) {
    this.activeView.set(viewId);
  }

  public ObjectProperty<ViewId> activeViewProperty() {
    return activeView;
  }

  // ===== Collapsed State =====

  public boolean isCollapsed() {
    return collapsed.get();
  }

  public void setCollapsed(boolean collapsed) {
    this.collapsed.set(collapsed);
  }

  public BooleanProperty collapsedProperty() {
    return collapsed;
  }
}
