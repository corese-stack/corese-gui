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
 * Controller for graph visualization.
 * Delegates rendering to GraphResultView and GraphDisplayWidget.
 */
public class GraphResultController {

  private final GraphResultView view;

  public GraphResultController() {
    this.view = new GraphResultView();
    initialize();
  }

  private void initialize() {
    view.setToolbarActions(
        List.of(
            new ButtonConfig(ButtonIcon.COPY, "Copy SVG", this::copySvg),
            new ButtonConfig(ButtonIcon.EXPORT, "Export SVG", this::exportSvg),
            new ButtonConfig(ButtonIcon.RELOAD, "Reset Layout", view.getGraphWidget()::resetLayout),
            new ButtonConfig(ButtonIcon.ZOOM_IN, "Zoom In", view.getGraphWidget()::zoomIn),
            new ButtonConfig(ButtonIcon.ZOOM_OUT, "Zoom Out", view.getGraphWidget()::zoomOut)));
  }

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

  private void exportSvg() {
    String svg = view.getGraphWidget().getSvgContent();
    if (svg != null && !svg.isEmpty()) {
      ExportHelper.exportSvg(view.getGraphWidget().getScene().getWindow(), svg);
    } else {
      NotificationManager.getInstance().showWarning("No graph to export");
    }
  }

  /**
   * Displays an RDF graph from JSON-LD data.
   *
   * @param jsonLdData The RDF data in JSON-LD format
   */
  public void displayGraph(String jsonLdData) {
    view.getGraphWidget().displayGraph(jsonLdData);
  }

  /**
   * Clears the graph view.
   */
  public void clear() {
    view.getGraphWidget().clear();
  }

  /**
   * Returns the root view node.
   *
   * @return The view root
   */
  public Node getView() {
    return view.getRoot();
  }
}