package fr.inria.corese.gui.feature.result.graph;

import java.util.List;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderMode;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderStatus;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphStats;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.ExportHelper;
import javafx.scene.Node;

/**
 * Controller for the Graph Visualization feature.
 * <p>
 * This class coordinates the interaction between the graph data (JSON-LD) and
 * the {@link GraphResultView}. It manages the toolbar actions (Copy, Export,
 * Zoom, Reset) and delegates the actual graph rendering to the view's widget.
 * </p>
 */
public class GraphResultController implements AutoCloseable {

	private static final String MSG_EXPORT_EMPTY = "No graph to export.";
	private static final String RENDER_DETAIL_INTERACTION_LOCKED = "Graph interactions disabled for very large graph.";

	private final GraphResultView view;
	private boolean hasGraphData = false;
	private boolean graphInteractionsLocked = false;

	/**
	 * Constructs a new GraphResultController. Initializes the view and configures
	 * the toolbar buttons.
	 */
	public GraphResultController() {
		this.view = new GraphResultView();
		initialize();
	}

	/**
	 * Initializes the controller by setting up the toolbar actions on the view.
	 */
	private void initialize() {
		view.setToolbarActions(List.of(ButtonFactory.exportGraph(this::exportGraph),
				ButtonFactory.resetLayout(view.getGraphWidget()::resetLayout),
				ButtonFactory.centerView(view.getGraphWidget()::centerView),
				ButtonFactory.zoomIn(view.getGraphWidget()::zoomIn),
				ButtonFactory.zoomOut(view.getGraphWidget()::zoomOut)));
		view.getGraphWidget().setOnGraphStatsChanged(this::handleGraphStatsChanged);
		view.getGraphWidget().setOnRenderStatusChanged(this::handleGraphRenderStatusChanged);
		updateToolbarActionStates();
	}

	private void handleGraphStatsChanged(GraphStats stats) {
		GraphStats safeStats = stats == null ? new GraphStats(0, 0, List.of()) : stats;
		hasGraphData = safeStats.tripleCount() > 0;
		view.updateGraphStatus(safeStats);
		updateToolbarActionStates();
	}

	private void handleGraphRenderStatusChanged(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		graphInteractionsLocked = isGraphInteractionLocked(safeStatus);
		view.updateGraphRenderStatus(safeStatus);
		updateToolbarActionStates();
	}

	private static boolean isGraphInteractionLocked(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		if (safeStatus.mode() == GraphRenderMode.PAUSED) {
			return true;
		}
		return safeStatus.details().stream().anyMatch(detail -> RENDER_DETAIL_INTERACTION_LOCKED.equals(detail));
	}

	private void updateToolbarActionStates() {
		view.setToolbarButtonDisabled(ButtonIcon.EXPORT, !hasGraphData);
		boolean disableInteractions = !hasGraphData || graphInteractionsLocked;
		view.setToolbarButtonDisabled(ButtonIcon.LAYOUT_FORCE, disableInteractions);
		view.setToolbarButtonDisabled(ButtonIcon.CENTER_VIEW, disableInteractions);
		view.setToolbarButtonDisabled(ButtonIcon.ZOOM_IN, disableInteractions);
		view.setToolbarButtonDisabled(ButtonIcon.ZOOM_OUT, disableInteractions);
	}

	/**
	 * Triggers the export process to save the current graph as SVG, PNG, or PDF.
	 * Delegates the file handling to {@link ExportHelper}.
	 */
	private void exportGraph() {
		String svg = view.getGraphWidget().getSvgContent();
		if (svg != null && !svg.isEmpty()) {
			ExportHelper.exportGraph(view.getGraphWidget().getScene().getWindow(), svg);
		} else {
			NotificationWidget.getInstance().showWarning(MSG_EXPORT_EMPTY);
		}
	}

	public boolean exportGraphFromShortcut() {
		exportGraph();
		return true;
	}

	public boolean reenergizeLayoutFromShortcut() {
		view.getGraphWidget().resetLayout();
		return true;
	}

	public boolean centerGraphFromShortcut() {
		view.getGraphWidget().centerView();
		return true;
	}

	/**
	 * Displays an RDF graph using the provided JSON-LD data.
	 *
	 * @param jsonLdData
	 *            The RDF data to visualize, formatted as a JSON-LD string. Must be
	 *            a valid JSON-LD structure compatible with the graph visualizer.
	 */
	public void displayGraph(String jsonLdData) {
		displayGraph(jsonLdData, -1);
	}

	public void displayGraph(String jsonLdData, int tripleCountHint) {
		view.getGraphWidget().displayGraph(jsonLdData, tripleCountHint);
	}

	/**
	 * Clears the current graph visualization, removing all nodes and links from the
	 * view.
	 */
	public void clear() {
		hasGraphData = false;
		graphInteractionsLocked = false;
		updateToolbarActionStates();
		view.getGraphWidget().clear();
	}

	/**
	 * Retrieves the root node of the view managed by this controller.
	 *
	 * @return The root {@link Node} of the {@link GraphResultView}.
	 */
	public Node getView() {
		return view.getRoot();
	}

	@Override
	public void close() {
		view.getGraphWidget().close();
	}
}
