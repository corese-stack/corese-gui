package fr.inria.corese.demo.view;

import fr.inria.corese.demo.view.codeEditor.CodeEditorView;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

public class TabEditorView extends VBox {
  private TabPane tabPane;
  private Tab addTab;
  private Button addTabButton;
  private FontIcon modifiedFile;
  private EmptyStateView emptyStateView;
  private StackPane mainContainer;

  public TabEditorView() {
    tabPane = new TabPane();
    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    tabPane.setTabMaxWidth(150);

    addTabButton = new Button();
    FontIcon addIcon = new FontIcon(MaterialDesignP.PLUS);
    addIcon.setIconSize(10);

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
    addTabButton.setStyle(
        """
        -fx-background-color: transparent;
        -fx-min-height: 30;
        -fx-max-height: 30;
        """);

    addTab = new Tab();
    addTab.setStyle(
        """
        -fx-background-color: transparent;
        -fx-border-color: #E0E0E0;
        -fx-border-radius: 5 5 0 0;
        -fx-border-width: 1 1 0 1;
        -fx-padding: 0;
        """);
    addTab.setGraphic(addTabButton);
    addTab.setClosable(false);

    tabPane.getTabs().add(addTab);

    modifiedFile = new FontIcon(MaterialDesignC.CHECKBOX_BLANK_CIRCLE);
    modifiedFile.setIconSize(5);
    modifiedFile.setIconColor(Color.web("#2196F3"));

    Label emptyStateTitle = new Label("No file opened");
    Label emptyStateMessage =
        new Label("Open a file from the file explorer to display\nits content in the code editor");
    String emptyStateImage =
        "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9"
            + " 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z";

    emptyStateView = new EmptyStateView(emptyStateTitle, emptyStateMessage, emptyStateImage);
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
   * Creates a Tab with the given title and code editor view without adding it to the TabPane
   *
   * @param title The title of the tab
   * @param codeEditorView The code editor view to be displayed in the tab
   * @return The created tab
   */
  public Tab createEditorTab(String title, CodeEditorView codeEditorView) {
    Tab tab = new Tab(title);

    tab.setStyle(
        """
            -fx-background-color: transparent;
            -fx-border-color: #E0E0E0;
            -fx-border-width: 0 1 0 1;
            -fx-font-size: 12px;
        """);
    tab.setOnSelectionChanged(
        e -> {
          if (tab.isSelected()) {
            tab.setStyle(
                """
                    -fx-border-color: #2196F3;
                    -fx-border-width: 0 0 1 0;
                """);
          } else {
            tab.setStyle(
                """
                    -fx-border-color: #E0E0E0;
                    -fx-border-width: 0 1 0 1;
                """);
          }
        });
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
    FontIcon icon = new FontIcon(MaterialDesignR.RECORD);
    icon.setIconSize(2);
    if (isModified) {
      tab.setGraphic(icon);
    } else {
      tab.setGraphic(null);
    }
  }

  public Tab getAddTab() {
    return addTab;
  }

  public Button getAddTabButton() {
    return addTabButton;
  }

  public TabPane getTabPane() {
    return tabPane;
  }
}
