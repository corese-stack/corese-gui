package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.SplitEditorView;
import fr.inria.corese.gui.view.utils.TabPaneUtils;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class QueryView extends SplitEditorView {

    private StackPane editorContainer;
    private SplitPane resultsSplitPane;
    private TabPane resultsTabPane;
    private Tab tableTab;
    private Tab graphTab;
    private Tab textTab;
    private StackPane resultsContainer;

    public QueryView() {
        super("/styles/split-editor-view.css");
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
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

        // Editor Container
        VBox.setVgrow(editorContainer, javafx.scene.layout.Priority.ALWAYS);
        editorContainer.setMinHeight(150.0);

        // Results
        resultsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        TabPaneUtils.enableFullWidth(resultsTabPane);
        resultsTabPane.getTabs().addAll(tableTab, graphTab, textTab);

        resultsContainer.getChildren().add(resultsTabPane);

        resultsSplitPane.getItems().add(resultsContainer);

        setEditorView(editorContainer);
        setResultView(resultsSplitPane);
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
