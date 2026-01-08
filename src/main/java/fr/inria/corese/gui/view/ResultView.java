package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.icon.IconButtonBarView;
import fr.inria.corese.gui.view.utils.TabPaneUtils;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class ResultView extends AbstractView {
    private final TabPane tabPane;
    private final Tab textTab;
    private final Tab visualTab;
    private final Tab tableTab;
    private final Tab graphTab;
    private final IconButtonBarView iconButtonBarView;

    public ResultView() {
        super(new BorderPane(), null);
        
        iconButtonBarView = new IconButtonBarView();
        
        tabPane = new TabPane();
        textTab = new Tab("Text");
        visualTab = new Tab("Visual");
        tableTab = new Tab("Table");
        graphTab = new Tab("Graph");
        
        // Default tabs
        tabPane.getTabs().addAll(textTab, visualTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    TabPaneUtils.enableFullWidthTabs(tabPane);

        BorderPane root = (BorderPane) getRoot();
        root.setCenter(tabPane);
        root.setRight(iconButtonBarView);
    }

    public TabPane getTabPane() { return tabPane; }
    public Tab getTextTab() { return textTab; }
    public Tab getVisualTab() { return visualTab; }
    public Tab getTableTab() { return tableTab; }
    public Tab getGraphTab() { return graphTab; }
    public IconButtonBarView getIconButtonBarView() { return iconButtonBarView; }
}
