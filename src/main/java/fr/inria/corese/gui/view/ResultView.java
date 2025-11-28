package fr.inria.corese.gui.view;

import fr.inria.corese.gui.view.base.AbstractView;
import fr.inria.corese.gui.view.icon.IconButtonBarView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ResultView extends AbstractView {
    private final TabPane tabPane;
    private final Tab textTab;
    private final Tab visualTab;
    private final IconButtonBarView iconButtonBarView;

    public ResultView() {
        super(new BorderPane(), null);
        
        iconButtonBarView = new IconButtonBarView();
        
        tabPane = new TabPane();
        textTab = new Tab("Text");
        visualTab = new Tab("Visual");
        
        tabPane.getTabs().addAll(textTab, visualTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = (BorderPane) getRoot();
        root.setCenter(tabPane);
    }

    public TabPane getTabPane() { return tabPane; }
    public Tab getTextTab() { return textTab; }
    public Tab getVisualTab() { return visualTab; }
    public IconButtonBarView getIconButtonBarView() { return iconButtonBarView; }
}
