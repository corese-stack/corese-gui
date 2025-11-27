package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * View for the Validation screen.
 *
 * <p>Displays a code editor for SHACL shapes and a results pane for validation reports.
 */
public class ValidationView extends AbstractView {

  private final TopBar topBar;
  private final StackPane editorContainer;
  private final TabPane resultsTabPane;
  private final Tab tableTab;
  private final Tab graphTab;
  private final Tab textTab;

  public ValidationView() {
    super(new BorderPane(), null); // No specific CSS for now, or reuse existing if needed

    // Initialize components
    this.topBar = new TopBar();
    this.editorContainer = new StackPane();
    this.resultsTabPane = new TabPane();
    this.tableTab = new Tab("Table");
    this.graphTab = new Tab("Graph");
    this.textTab = new Tab("Text");

    initializeLayout();
  }

  private void initializeLayout() {
    BorderPane root = (BorderPane) getRoot();

    // Top Bar
    root.setTop(topBar);

    // Editor Container
    editorContainer.setMinHeight(150.0);

    // Results Tab Pane
    resultsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    resultsTabPane.getTabs().addAll(tableTab, graphTab, textTab);

    // Main Split Pane (Vertical split between Editor and Results)
    SplitPane mainSplitPane = new SplitPane();
    mainSplitPane.setOrientation(Orientation.VERTICAL);
    
    // Note: In the original FXML, there was an inner SplitPane around resultsTabPane.
    // It seems redundant if it only contains one item, so we add resultsTabPane directly.
    mainSplitPane.getItems().addAll(editorContainer, resultsTabPane);

    root.setCenter(mainSplitPane);
  }

  // ===== Getters =====

  public TopBar getTopBar() {
    return topBar;
  }

  public StackPane getEditorContainer() {
    return editorContainer;
  }

  public TabPane getResultsTabPane() {
    return resultsTabPane;
  }

  public Tab getTableTab() {
    return tableTab;
  }

  public Tab getGraphTab() {
    return graphTab;
  }

  public Tab getTextTab() {
    return textTab;
  }
}
