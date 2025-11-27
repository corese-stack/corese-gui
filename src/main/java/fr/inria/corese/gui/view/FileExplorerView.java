package fr.inria.corese.gui.view;

import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.view.icon.IconButtonView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FileExplorerView extends HBox {
  private TreeView<String> treeView;
  private IconButtonView newFileButton;
  private IconButtonView newFolderButton;
  private IconButtonView openFolderButton;
  private EmptyStateView emptyStateView;
  private HBox buttonBar;
  private VBox mainContent;

  public FileExplorerView() {
    this.setSpacing(0);

    initializeComponents();
    initializeButtonBar();
    initializeMainContent();

    VBox.setVgrow(this, Priority.ALWAYS);

    getChildren().addAll(mainContent);
  }

  private void initializeComponents() {
    Label emptyStateTitle = new Label("No files loaded");
    Label emptyStateMessage = new Label("Open a folder or load a file\nin the file explorer");
    String emptyStateImage =
        "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9"
            + " 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z";

    emptyStateView = new EmptyStateView(emptyStateTitle, emptyStateMessage, emptyStateImage);
    emptyStateView.setMaxWidth(Double.MAX_VALUE);
    emptyStateView.setMaxHeight(Double.MAX_VALUE);
    VBox.setVgrow(emptyStateView, Priority.ALWAYS);
    emptyStateView.setAlignment(Pos.TOP_CENTER);

    treeView = new TreeView<>();

    treeView.setCellFactory(
        tv ->
            new TreeCell<String>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                  setText(null);
                  setGraphic(null);
                } else {
                  setText(item);
                  setStyle("-fx-font-size: 12px;");

                  if (getTreeItem().getGraphic() != null) {
                    setGraphic(getTreeItem().getGraphic());
                  }

                  setOnMouseEntered(
                      e -> setStyle("-fx-background-color: #CCE8FF; -fx-font-size: 12px"));

                  setOnMouseExited(
                      e -> {
                        if (!isSelected()) {
                          setStyle("-fx-font-size: 12px");
                        }
                      });

                  selectedProperty()
                      .addListener(
                          (obs, wasSelected, isNowSelected) -> {
                            if (isNowSelected) {
                              setStyle("-fx-background-color: #CCE8FF;");
                            } else {
                              setStyle("");
                            }
                          });
                }
              }
            });

    treeView.setStyle("-fx-background-color: transparent;" + "-fx-border-color: transparent;");

    treeView.setMinWidth(120);

    newFileButton = new IconButtonView(IconButtonType.NEW_FILE);
    newFolderButton = new IconButtonView(IconButtonType.NEW_FOLDER);
    openFolderButton = new IconButtonView(IconButtonType.OPEN_FOLDER);
  }

  private void initializeButtonBar() {
    buttonBar = new HBox(5);
    buttonBar.setStyle("-fx-background-color: #eef5ff");
    buttonBar.setAlignment(Pos.CENTER);
    buttonBar.getChildren().addAll(openFolderButton, newFileButton, newFolderButton);
  }

  private void initializeMainContent() {
    mainContent = new VBox(5);
    mainContent.setPadding(new Insets(0, 0, 5, 0));

    mainContent.setMaxHeight(Double.MAX_VALUE);
    HBox.setHgrow(mainContent, Priority.ALWAYS);
    VBox.setVgrow(mainContent, Priority.ALWAYS);

    VBox.setVgrow(treeView, Priority.ALWAYS);
    treeView.setMaxHeight(Double.MAX_VALUE);

    openFolderButton.setAlignment(Pos.CENTER);
    mainContent.getChildren().add(buttonBar);
    if (treeView.getRoot() != null) {
      mainContent.getChildren().add(treeView);
    } else {
      mainContent.getChildren().add(emptyStateView);
    }
  }

  public void switchView(boolean hasRoot) {
    if (mainContent.getChildren().size() > 1) {
      mainContent.getChildren().remove(1);
    }
    if (hasRoot) {
      mainContent.getChildren().add(treeView);
    } else {
      VBox emptyStateContainer = new VBox();
      emptyStateContainer.setAlignment(Pos.CENTER);
      emptyStateContainer.getChildren().add(emptyStateView);
      VBox.setVgrow(emptyStateContainer, Priority.ALWAYS);
      mainContent.getChildren().add(emptyStateContainer);
    }
  }

  public TreeView<String> getTreeView() {
    return treeView;
  }

  public void setTreeView(TreeView<String> treeView) {
    this.treeView = treeView;
  }

  public Button getNewFileButton() {
    return newFileButton;
  }

  public Button getNewFolderButton() {
    return newFolderButton;
  }

  public Button getOpenFolderButton() {
    return openFolderButton;
  }

  public VBox getMainContent() {
    return mainContent;
  }
}
