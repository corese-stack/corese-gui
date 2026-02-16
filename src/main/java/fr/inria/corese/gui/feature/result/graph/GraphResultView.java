package fr.inria.corese.gui.feature.result.graph;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphStats;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.feature.data.support.DataStatusTooltipSupport;
import fr.inria.corese.gui.feature.result.graph.support.GraphResultStatusTooltipSupport;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * View for displaying graph visualization results.
 *
 * <p>
 * The status footer reuses the Data page visual language while focusing on the
 * two metrics relevant to result graphs: triples and named graphs.
 * </p>
 */
public class GraphResultView extends AbstractView {

	@SuppressWarnings("java:S1075") // Internal stylesheet path
	private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";

	@SuppressWarnings("java:S1075") // Internal stylesheet path
	private static final String VIEW_STYLESHEET_PATH = "/css/features/graph-result.css";

	private static final String TOOLTIP_TITLE_TRIPLES = "Triples";
	private static final String TOOLTIP_TITLE_NAMED_GRAPHS = "Named Graphs";
	private static final String STYLE_CLASS_ROOT = "graph-result-root";

	private final GraphDisplayWidget graphWidget;
	private final ToolbarWidget toolbarWidget;
	private final Label tripleCountLabel = new Label();
	private final Label namedGraphCountLabel = new Label();

	public GraphResultView() {
		super(new BorderPane(), null);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), VIEW_STYLESHEET_PATH);

		this.graphWidget = new GraphDisplayWidget();
		graphWidget.setBorderVisible(false);
		this.toolbarWidget = new ToolbarWidget();
		setupToolbar();
		toolbarWidget.getStyleClass().add("graph-result-toolbar");

		initializeStatusLabels();
		HBox graphBody = new HBox(graphWidget, toolbarWidget);
		graphBody.getStyleClass().add("graph-result-body");
		HBox.setHgrow(graphWidget, Priority.ALWAYS);

		VBox graphCard = new VBox(graphBody, createStatusBar());
		graphCard.getStyleClass().add("graph-result-card");
		VBox.setVgrow(graphBody, Priority.ALWAYS);

		BorderPane root = (BorderPane) getRoot();
		root.getStyleClass().add(STYLE_CLASS_ROOT);
		root.setCenter(graphCard);

		updateGraphStatus(new GraphStats(0, 0, List.of()));
	}

	private void setupToolbar() {
		List<ButtonConfig> buttons = List.of(ButtonFactory.resetLayout(graphWidget::resetLayout),
				ButtonFactory.centerView(graphWidget::centerView), ButtonFactory.zoomIn(graphWidget::zoomIn),
				ButtonFactory.zoomOut(graphWidget::zoomOut));
		toolbarWidget.setButtons(buttons);
	}

	private void initializeStatusLabels() {
		tripleCountLabel.getStyleClass().add("data-status-label");
		namedGraphCountLabel.getStyleClass().add("data-status-label");
		tripleCountLabel.setFocusTraversable(false);
		namedGraphCountLabel.setFocusTraversable(false);
	}

	private HBox createStatusBar() {
		HBox primaryStatusGroup = createStatusGroup(tripleCountLabel, namedGraphCountLabel);
		HBox statusBar = new HBox(10, primaryStatusGroup);
		statusBar.getStyleClass().add("data-status-bar");
		return statusBar;
	}

	private static HBox createStatusGroup(Label... labels) {
		HBox group = new HBox(14, labels);
		group.getStyleClass().add("data-status-group");
		return group;
	}

	public void updateGraphStatus(GraphStats stats) {
		GraphStats safeStats = stats == null ? new GraphStats(0, 0, List.of()) : stats;
		DataStatusTooltipSupport.updateStatusMetric(tripleCountLabel, TOOLTIP_TITLE_TRIPLES, safeStats.tripleCount(),
				GraphResultStatusTooltipSupport.buildTriplesTooltipLines(safeStats));
		DataStatusTooltipSupport.updateStatusMetric(namedGraphCountLabel, TOOLTIP_TITLE_NAMED_GRAPHS,
				safeStats.namedGraphCount(), GraphResultStatusTooltipSupport.buildNamedGraphTooltipLines(safeStats));
	}

	public void updateGraphStatus(int tripleCount, int namedGraphCount) {
		updateGraphStatus(new GraphStats(tripleCount, namedGraphCount, List.of()));
	}

	public void setToolbarActions(List<ButtonConfig> buttons) {
		toolbarWidget.setButtons(buttons);
	}

	public GraphDisplayWidget getGraphWidget() {
		return graphWidget;
	}
}
