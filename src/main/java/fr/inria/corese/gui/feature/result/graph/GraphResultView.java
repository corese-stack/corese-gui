package fr.inria.corese.gui.feature.result.graph;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderMode;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderStatus;
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
import javafx.scene.layout.Region;
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
	private static final String TOOLTIP_TITLE_RENDER = "View";
	private static final String STYLE_CLASS_ROOT = "graph-result-root";
	private static final String STYLE_CLASS_RENDER_STATUS_LABEL = "data-status-label-render-mode";
	private static final String STYLE_CLASS_RENDER_STATUS_NORMAL = "data-status-label-render-normal";
	private static final String STYLE_CLASS_RENDER_STATUS_DEGRADED = "data-status-label-render-degraded";
	private static final String STYLE_CLASS_RENDER_STATUS_PAUSED = "data-status-label-render-paused";

	private final GraphDisplayWidget graphWidget;
	private final ToolbarWidget toolbarWidget;
	private final Label tripleCountLabel = new Label();
	private final Label namedGraphCountLabel = new Label();
	private final Label renderModeLabel = new Label();

	public GraphResultView() {
		super(new BorderPane(), null);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), VIEW_STYLESHEET_PATH);

		this.graphWidget = new GraphDisplayWidget();
		graphWidget.setBorderVisible(false);
		this.toolbarWidget = new ToolbarWidget();
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
		updateGraphRenderStatus(GraphRenderStatus.normal());
	}

	private void initializeStatusLabels() {
		tripleCountLabel.getStyleClass().add("data-status-label");
		namedGraphCountLabel.getStyleClass().add("data-status-label");
		renderModeLabel.getStyleClass().addAll("data-status-label", STYLE_CLASS_RENDER_STATUS_LABEL);
		tripleCountLabel.setFocusTraversable(false);
		namedGraphCountLabel.setFocusTraversable(false);
		renderModeLabel.setFocusTraversable(false);
	}

	private HBox createStatusBar() {
		HBox primaryStatusGroup = createStatusGroup(tripleCountLabel, namedGraphCountLabel);
		HBox secondaryStatusGroup = createStatusGroup(renderModeLabel);
		secondaryStatusGroup.getStyleClass().add("data-status-group-secondary");

		Region statusSpacer = new Region();
		HBox.setHgrow(statusSpacer, Priority.ALWAYS);

		HBox statusBar = new HBox(10, primaryStatusGroup, statusSpacer, secondaryStatusGroup);
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

	public void updateGraphRenderStatus(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		String summary = switch (safeStatus.mode()) {
			case NORMAL -> "Standard";
			case DEGRADED -> "Adaptive";
			case PAUSED -> "Paused";
		};
		List<String> tooltipLines = DataStatusTooltipSupport.buildRenderTooltipLines(safeStatus);
		DataStatusTooltipSupport.updateStatusTextMetric(renderModeLabel, TOOLTIP_TITLE_RENDER, summary, tooltipLines);
		applyRenderStatusStyle(safeStatus.mode());
	}

	private void applyRenderStatusStyle(GraphRenderMode mode) {
		renderModeLabel.getStyleClass().removeAll(STYLE_CLASS_RENDER_STATUS_NORMAL, STYLE_CLASS_RENDER_STATUS_DEGRADED,
				STYLE_CLASS_RENDER_STATUS_PAUSED);
		switch (mode) {
			case DEGRADED -> renderModeLabel.getStyleClass().add(STYLE_CLASS_RENDER_STATUS_DEGRADED);
			case PAUSED -> renderModeLabel.getStyleClass().add(STYLE_CLASS_RENDER_STATUS_PAUSED);
			default -> renderModeLabel.getStyleClass().add(STYLE_CLASS_RENDER_STATUS_NORMAL);
		}
	}

	public void setToolbarActions(List<ButtonConfig> buttons) {
		toolbarWidget.setButtons(buttons);
	}

	public void setToolbarButtonDisabled(ButtonIcon buttonIcon, boolean disabled) {
		toolbarWidget.setButtonDisabled(buttonIcon, disabled);
	}

	public GraphDisplayWidget getGraphWidget() {
		return graphWidget;
	}
}
