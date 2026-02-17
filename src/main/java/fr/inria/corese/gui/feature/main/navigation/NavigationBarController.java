package fr.inria.corese.gui.feature.main.navigation;

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.gui.AppConstants;
import fr.inria.corese.gui.core.enums.ViewId;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.utils.BrowserUtils;
import javafx.scene.Parent;

/**
 * Controller responsible for handling sidebar navigation actions.
 *
 * <p>
 * Coordinates between {@link NavigationBarView} and {@link NavigationBarModel}:
 *
 * <ul>
 * <li>updates the model when the user clicks navigation or toggle
 * <li>updates the view when the model changes
 * <li>notifies the outside world via an {@code onNavigate} callback
 * </ul>
 */
public final class NavigationBarController {

	private static final Logger LOGGER = LoggerFactory.getLogger(NavigationBarController.class);

	private final NavigationBarView view;
	private final NavigationBarModel model;

	/** Callback invoked when navigation occurs. */
	private Consumer<ViewId> onNavigate;

	public NavigationBarController() {
		this.model = new NavigationBarModel();
		this.view = new NavigationBarView();

		initializeBindings();

		// Bind model collapsed state to ThemeManager preference
		model.collapsedProperty().bindBidirectional(ThemeManager.getInstance().sidebarCollapsedProperty());

		initializeHandlers();

		// Ensure default view is highlighted at startup
		view.setActiveView(model.getActiveView());
	}

	// ===== Initialization =====

	/** Wires model changes to view updates. */
	private void initializeBindings() {
		// When model.collapsed changes → update view
		model.collapsedProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.booleanValue() != view.isCollapsed()) {
				view.setCollapsed(newVal);
			}
		});

		// When model.activeView changes → update selected button
		model.activeViewProperty().addListener((obs, oldVal, newVal) -> view.setActiveView(newVal));
	}

	/** Wires view events to model updates and navigation callback. */
	private void initializeHandlers() {
		// Navigation clicks in the view → update model + notify external handler
		view.setNavigationHandler(this::navigate);

		// Toggle button in the view → update model.collapsed
		view.setOnToggle(model::setCollapsed);

		// Logo click -> open website
		view.setOnLogoClick(() -> BrowserUtils.openUrl(AppConstants.PROJECT_URL));
	}

	// ===== Internal behavior =====

	/** Called when the user clicks a navigation button. */
	private void navigate(ViewId viewId) {
		LOGGER.debug("Navigation triggered: {}", viewId);
		model.setActiveView(viewId);

		if (onNavigate != null) {
			onNavigate.accept(viewId);
		}
	}

	// ===== Public API =====

	/**
	 * Returns the root node of the navigation bar for embedding in parent layouts.
	 */
	public Parent getRoot() {
		return view.getRoot();
	}

	/**
	 * Sets the callback to be invoked when navigation occurs.
	 *
	 * @param handler
	 *            callback accepting the target ViewId
	 */
	public void setOnNavigate(Consumer<ViewId> handler) {
		this.onNavigate = Objects.requireNonNull(handler, "handler must not be null");
	}

	/**
	 * Programmatically selects a view by updating the model.
	 *
	 * @param viewId
	 *            the view to select
	 */
	public void selectView(ViewId viewId) {
		model.setActiveView(viewId);
	}

	/** Toggles the sidebar collapsed state. */
	public void toggleCollapsed() {
		model.setCollapsed(!model.isCollapsed());
	}
}
