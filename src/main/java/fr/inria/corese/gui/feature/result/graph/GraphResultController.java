package fr.inria.corese.gui.feature.result.graph;

import fr.inria.corese.gui.component.notification.NotificationManager;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.enums.ButtonIcon;
import fr.inria.corese.gui.utils.ExportHelper;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for the Graph Visualization feature.
 * <p>
 * This class coordinates the interaction between the graph data (JSON-LD) and the
 * {@link GraphResultView}. It manages the toolbar actions (Copy, Export, Zoom, Reset)
 * and delegates the actual graph rendering to the view's widget.
 * </p>
 */
public class GraphResultController {

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
            new ButtonConfig(ButtonIcon.COPY, "Copy SVG", this::copySvg),
            new ButtonConfig(ButtonIcon.EXPORT, "Export SVG", this::exportSvg),
            new ButtonConfig(ButtonIcon.RELOAD, "Reset Layout", view.getGraphWidget()::resetLayout),
            new ButtonConfig(ButtonIcon.ZOOM_IN, "Zoom In", view.getGraphWidget()::zoomIn),
            new ButtonConfig(ButtonIcon.ZOOM_OUT, "Zoom Out", view.getGraphWidget()::zoomOut)));
  }

  /**
   * Captures the current graph as an SVG string and copies it to the system clipboard.
   * Shows a notification upon success or failure.
   */
  private void copySvg() {
    String svg = view.getGraphWidget().getSvgContent();
    if (svg != null && !svg.isEmpty()) {
      ClipboardContent content = new ClipboardContent();
      content.putString(svg);
      Clipboard.getSystemClipboard().setContent(content);
      NotificationManager.getInstance().showSuccess("Graph SVG copied to clipboard");
    } else {
      NotificationManager.getInstance().showWarning("No graph to copy");
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
      NotificationManager.getInstance().showWarning("No graph to export");
    }
  }

  /**
   * Displays an RDF graph using the provided JSON-LD data.
   *
   * @param jsonLdData The RDF data to visualize, formatted as a JSON-LD string.
   *                   Must be a valid JSON-LD structure compatible with the graph visualizer.
   */
  public void displayGraph(String jsonLdData) {
    view.getGraphWidget().displayGraph(jsonLdData);
  }

  /**
   * Clears the current graph visualization, removing all nodes and links from the view.
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