package fr.inria.corese.gui.feature.query;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * View for the Query feature.
 *
 * <p>
 * Provides a container for the tabbed query editor and handles the empty state
 * display.
 */
public class QueryView extends AbstractView {

	private static final String STYLESHEET_PATH = "/css/split-editor-view.css";
	@SuppressWarnings("java:S1075")
	private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";

	private final StackPane workspaceCard = new StackPane();
	private QueryViewController controller;

	public QueryView() {
		super(new BorderPane(), STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		initializeLayout();
	}

	private void initializeLayout() {
		BorderPane root = (BorderPane) getRoot();
		root.getStyleClass().addAll("query-page-root", "app-workspace-root");
		workspaceCard.getStyleClass().addAll("app-workspace-card", "app-card", "app-card-default");
		workspaceCard.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		RoundedClipSupport.applyRoundedClip(workspaceCard, 8);
		root.setCenter(workspaceCard);
	}

	/**
	 * Sets the main content (typically the TabEditor).
	 *
	 * @param node
	 *            The content node.
	 */
	public void setMainContent(Node node) {
		if (node instanceof Region region) {
			region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		}
		workspaceCard.getChildren().setAll(node);
	}

	public void setController(QueryViewController controller) {
		this.controller = controller;
	}

	public QueryViewController getController() {
		return controller;
	}

	/**
	 * Creates the empty state widget shown when no queries are open.
	 *
	 * @param onNewAction
	 *            Callback for "New Query".
	 * @param onLoadAction
	 *            Callback for "Load Query".
	 * @param onTemplateAction
	 *            Callback for "Templates" (can be null if disabled).
	 * @return The configured EmptyStateWidget.
	 */
	public Node createEmptyState(Runnable onNewAction, Runnable onLoadAction, Runnable onTemplateAction) {
		java.util.List<Node> actions = new java.util.ArrayList<>();

		actions.add(EmptyStateWidget.createAction("New Query", ButtonIcon.EMPTY_ACTION_NEW, onNewAction));
		actions.add(EmptyStateWidget.createAction("Load Query", ButtonIcon.EMPTY_ACTION_OPEN, onLoadAction));

		if (onTemplateAction != null) {
			actions.add(EmptyStateWidget.createAction("Templates", ButtonIcon.EMPTY_ACTION_TEMPLATE, onTemplateAction));
		}

		EmptyStateWidget emptyState = new EmptyStateWidget(ButtonIcon.EMPTY_QUERY, "No queries open",
				FileTypeSupport.withAcceptedExtensions(
						"Create a new query, load one, use a template, or drop a query file here.",
						FileTypeSupport.queryExtensions()),
				actions.toArray(Node[]::new));
		emptyState.getStyleClass().add("query-empty-state");
		return emptyState;
	}
}
