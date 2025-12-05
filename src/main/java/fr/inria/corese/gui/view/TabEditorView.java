package fr.inria.corese.gui.view;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import fr.inria.corese.gui.view.codeEditor.CodeEditorView;

public class TabEditorView extends VBox {
  private TabPane tabPane;
  private Tab addTab;
  private SplitMenuButton addTabButton;
  private MenuItem newFileItem;
  private MenuItem openFileItem;
  private MenuItem templatesItem;
  private EmptyStateView emptyStateView;
  private StackPane mainContainer;

  public TabEditorView() {
    this.getStylesheets().add(getClass().getResource("/styles/buttons.css").toExternalForm());
    tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    tabPane.setTabMaxWidth(150);

    addTabButton = new SplitMenuButton();
    newFileItem = new MenuItem("New File");
    openFileItem = new MenuItem("Open File");
    templatesItem = new MenuItem("Templates");
    addTabButton.getItems().addAll(newFileItem, openFileItem, templatesItem);

    FontIcon addIcon = new FontIcon(MaterialDesignP.PLUS);
    addIcon.setIconSize(18);

    ScaleTransition scaleIn = new ScaleTransition(Duration.millis(150), addIcon);
    scaleIn.setToX(1.1);
    scaleIn.setToY(1.1);

    ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), addIcon);
    scaleOut.setToX(1.0);
    scaleOut.setToY(1.0);

    addIcon.setOnMouseEntered(
        e -> {
          addIcon.setCursor(Cursor.HAND);
          scaleIn.playFromStart();
        });

    addIcon.setOnMouseExited(
        e -> {
          addIcon.setCursor(Cursor.DEFAULT);
          scaleOut.playFromStart();
        });
    addTabButton.setGraphic(addIcon);
    addTabButton.getStyleClass().add("add-tab-button");

    addTab = new Tab();
    addTab.setGraphic(addTabButton);
    addTab.setClosable(false);
    addTab.getStyleClass().add("add-tab");

    tabPane.getTabs().add(addTab);

    emptyStateView = new EmptyStateView(MaterialDesignC.CODE_TAGS, "No file opened\nOpen a file from the file explorer to display its content in the code editor");
    emptyStateView.setMaxWidth(Double.MAX_VALUE);
    emptyStateView.setMaxHeight(Double.MAX_VALUE);
    VBox.setVgrow(emptyStateView, Priority.ALWAYS);
    emptyStateView.setAlignment(Pos.CENTER);

    mainContainer = new StackPane();
    mainContainer.getChildren().addAll(tabPane);
    VBox.setVgrow(mainContainer, Priority.ALWAYS);
    getChildren().addAll(mainContainer);
  }

  /**
   * Adds a floating node (e.g., a button) to the editor view.
   *
   * @param node The node to add.
   * @param position The position of the node within the stack pane.
   * @param margin The margin to apply to the node.
   */
  public void addFloatingNode(javafx.scene.Node node, Pos position, javafx.geometry.Insets margin) {
    StackPane.setAlignment(node, position);
    StackPane.setMargin(node, margin);
    mainContainer.getChildren().add(node);
  }

  /**
   * Sets the empty state view to be displayed when no tabs are open.
   *
   * @param emptyStateView The empty state view node.
   */
  public void setEmptyStateView(javafx.scene.Node emptyStateView) {
    if (this.emptyStateView != null) {
      mainContainer.getChildren().remove(this.emptyStateView);
    }
    // Add at index 0 to be behind the tab pane (or manage visibility)
    // Actually, usually empty state replaces the content or sits on top if content is hidden.
    // Let's add it to the stack.
    mainContainer.getChildren().add(0, emptyStateView);
    
    // Bind visibility: Show empty state only when TabPane is hidden or empty?
    // The controller will manage visibility.
  }

  /**
   * Creates a Tab with the given title and code editor view without adding it to the TabPane
   *
   * @param title The title of the tab
   * @param codeEditorView The code editor view to be displayed in the tab
   * @return The created tab
   */
  public Tab createEditorTab(String title, CodeEditorView codeEditorView) {
    Tab tab = new Tab(title);
    codeEditorView.setMaxWidth(Double.MAX_VALUE);
    codeEditorView.setMaxHeight(Double.MAX_VALUE);
    tab.setContent(codeEditorView);

    return tab;
  }

  public Tab addNewEditorTab(String title, CodeEditorView codeEditorView) {
    Tab tab = createEditorTab(title, codeEditorView);
    tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
    tabPane.getSelectionModel().select(tab);
    return tab;
  }

  public void updateTabIcon(Tab tab, boolean isModified) {
    if (isModified) {
      Circle circle = new Circle(4, Color.web("#2196F3"));
      tab.setGraphic(circle);
    } else {
      tab.setGraphic(null);
    }
  }

  public Tab getAddTab() {
    return addTab;
  }

  public SplitMenuButton getAddTabButton() {
    return addTabButton;
  }

  public MenuItem getNewFileItem() {
    return newFileItem;
  }

  public MenuItem getOpenFileItem() {
    return openFileItem;
  }

  public MenuItem getTemplatesItem() {
    return templatesItem;
  }

  public TabPane getTabPane() {
    return tabPane;
  }
}
