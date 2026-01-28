package fr.inria.corese.gui.feature.main;

import fr.inria.corese.gui.core.ViewId;






import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Model holding the logical state of the navigation bar.
 *
 * <p>It exposes observable properties for:
 *
 * <ul>
 *   <li>whether the sidebar is collapsed
 *   <li>which view is currently active
 * </ul>
 */
public final class NavigationBarModel {

  /** Whether the sidebar is currently collapsed. */
  private final BooleanProperty collapsed = new SimpleBooleanProperty(false);

  /** The currently active view identifier. */
  private final ObjectProperty<ViewId> activeView =
      new SimpleObjectProperty<>(ViewId.DATA); // default view

  // ===== Collapsed =====

  /**
   * Returns the collapsed property.
   *
   * @return the collapsed property
   */
  public BooleanProperty collapsedProperty() {
    return collapsed;
  }

  /**
   * Returns whether the sidebar is currently collapsed.
   *
   * @return {@code true} if collapsed, {@code false} otherwise
   */
  public boolean isCollapsed() {
    return collapsed.get();
  }

  /**
   * Sets the collapsed state of the sidebar.
   *
   * @param value {@code true} to collapse, {@code false} to expand
   */
  public void setCollapsed(boolean value) {
    collapsed.set(value);
  }

  // ===== Active view =====

  /**
   * Returns the active view property.
   *
   * @return the active view property
   */
  public ObjectProperty<ViewId> activeViewProperty() {
    return activeView;
  }

  /**
   * Returns the currently active view.
   *
   * @return the active view
   */
  public ViewId getActiveView() {
    return activeView.get();
  }

  /**
   * Sets the currently active view.
   *
   * @param id the view to activate
   */
  public void setActiveView(ViewId id) {
    activeView.set(id);
  }
}
