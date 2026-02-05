package fr.inria.corese.gui.feature.result.graph;

import java.util.List;

import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.io.ExportHelper;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for the Graph Visualization feature.
 * <p>
 * This class coordinates the interaction between the graph data (JSON-LD) and
 * the
 * {@link GraphResultView}. It manages the toolbar actions (Copy, Export, Zoom,
 * Reset)
 * and delegates the actual graph rendering to the view's widget.
 * </p>
 */
public class GraphResultController {

  private static final String MSG_COPY_SUCCESS = "Graph SVG copied to clipboard";
  private static final String MSG_COPY_EMPTY = "No graph to copy";
  private static final String MSG_EXPORT_EMPTY = "No graph to export";

  private final GraphResultView view;

  /**
   * Constructs a new GraphResultController.
   * Initializes the view and configures the toolbar buttons.
   */
  public GraphResultController() {
    this.view = new GraphResultView();
    initialize();
  }

  /**
   * Initializes the controller by setting up the toolbar actions on the view.
   */
  private void initialize() {
    view.setToolbarActions(
        List.of(
            ButtonFactory.copySvg(this::copySvg),
            ButtonFactory.exportSvg(this::exportSvg),
            ButtonFactory.resetLayout(view.getGraphWidget()::resetLayout),
            ButtonFactory.zoomIn(view.getGraphWidget()::zoomIn),
            ButtonFactory.zoomOut(view.getGraphWidget()::zoomOut)));
  }

  /**
   * Captures the current graph as an SVG string and copies it to the system
   * clipboard.
   * Shows a notification upon success or failure.
   */
  private void copySvg() {
    String svg = view.getGraphWidget().getSvgContent();
    if (svg != null && !svg.isEmpty()) {
      ClipboardContent content = new ClipboardContent();
      content.putString(svg);
      Clipboard.getSystemClipboard().setContent(content);
      NotificationWidget.getInstance().showSuccess(MSG_COPY_SUCCESS);
    } else {
      NotificationWidget.getInstance().showWarning(MSG_COPY_EMPTY);
    }
  }

  /**
   * Triggers the export process to save the current graph as an SVG file.
   * Delegates the file handling to {@link ExportHelper}.
   */
  private void exportSvg() {
    String svg = view.getGraphWidget().getSvgContent();
    if (svg != null && !svg.isEmpty()) {
      ExportHelper.exportSvg(view.getGraphWidget().getScene().getWindow(), svg);
    } else {
      NotificationWidget.getInstance().showWarning(MSG_EXPORT_EMPTY);
    }
  }

  /**
   * Displays an RDF graph using the provided JSON-LD data.
   *
   * @param jsonLdData The RDF data to visualize, formatted as a JSON-LD string.
   *                   Must be a valid JSON-LD structure compatible with the graph
   *                   visualizer.
   */
  public void displayGraph(String jsonLdData) {
    view.getGraphWidget().displayGraph(jsonLdData);
  }

  /**
   * Clears the current graph visualization, removing all nodes and links from the
   * view.
   */
  public void clear() {
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
}