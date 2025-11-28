package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class QueryView extends AbstractView {

    private TopBar topBar;
    private SplitPane mainSplitPane;
    private StackPane editorContainer;
    private SplitPane resultsSplitPane;
    private TabPane resultsTabPane;
    private Tab tableTab;
    private Tab graphTab;
    private Tab textTab;
    private StackPane resultsContainer;

    public QueryView() {
        super(new BorderPane(), null);
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        topBar = new TopBar();
        mainSplitPane = new SplitPane();
        editorContainer = new StackPane();
        resultsSplitPane = new SplitPane();
        resultsTabPane = new TabPane();
        tableTab = new Tab("Table");
        graphTab = new Tab("Graph");
        textTab = new Tab("Text");
        resultsContainer = new StackPane();
    }

    private void setupLayout() {
        BorderPane root = (BorderPane) getRoot();
        root.setPrefSize(800, 600);

        // Top
        root.setTop(topBar);

        // Center
        mainSplitPane.setOrientation(Orientation.VERTICAL);
        VBox.setVgrow(mainSplitPane, javafx.scene.layout.Priority.ALWAYS);

        // Editor Container
        VBox.setVgrow(editorContainer, javafx.scene.layout.Priority.ALWAYS);
        editorContainer.setMinHeight(150.0);

        // Results
        resultsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        resultsTabPane.getTabs().addAll(tableTab, graphTab, textTab);

        resultsContainer.getChildren().add(resultsTabPane);

        resultsSplitPane.getItems().add(resultsContainer);

        mainSplitPane.getItems().addAll(editorContainer, resultsSplitPane);

        root.setCenter(mainSplitPane);
    }

    public TopBar getTopBar() {
        return topBar;
    }

    public SplitPane getMainSplitPane() {
        return mainSplitPane;
    }

    public StackPane getEditorContainer() {
        return editorContainer;
    }

    public SplitPane getResultsSplitPane() {
        return resultsSplitPane;
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
    
    public StackPane getResultsContainer() {
        return resultsContainer;
    }
}
