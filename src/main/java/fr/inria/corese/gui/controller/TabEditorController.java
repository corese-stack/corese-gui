package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.enums.icon.IconButtonType;
import fr.inria.corese.gui.model.TabEditorModel;
import fr.inria.corese.gui.view.FloatingButton;
import fr.inria.corese.gui.view.TabEditorView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class TabEditorController {
  private final TabEditorView view;
  private final TabEditorModel model;
  private final List<IconButtonType> buttons;
  private Runnable onExecutionRequest;
  private javafx.scene.Node emptyStateNode;
  private Function<Tab, ResultController> resultControllerFactory;
  private String executionButtonTooltip;

  private final Map<Tab, FloatingButton> tabExecutionButtons = new HashMap<>();

  public TabEditorController(List<IconButtonType> buttons) {
    this.view = new TabEditorView();
    this.model = new TabEditorModel();
    this.buttons = buttons;
    initializeTabPane();
    initializeKeyboardShortcuts();
  }

  /**
   * Sets the action to be executed when the user triggers the execution command (e.g., pressing
   * Ctrl+Enter in the editor).
   *
   * @param action The action to execute.
   */
  public void setOnExecutionRequest(Runnable action) {
    this.onExecutionRequest = action;
  }

  /**
   * Sets the execution state for the current tab.
   *
   * @param loading True if execution is in progress, false otherwise.
   */
  public void setExecutionState(boolean loading) {
    Tab selectedTab = view.getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
        FloatingButton button = tabExecutionButtons.get(selectedTab);
        if (button != null) {
            button.setLoading(loading);
        }
    }
  }

  /**
   * Shows the result pane for the current tab with an animation.
   */
  public void showResultPane() {
    Tab selectedTab = view.getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
        Node content = view.getTabContent(selectedTab);
        if (content instanceof SplitPane) {
            SplitPane splitPane = (SplitPane) content;
            if (splitPane.getItems().size() < 2) {
                ResultController resultController = model.getResultControllerForTab(selectedTab);
                if (resultController != null) {
                    Node resultRoot = resultController.getView().getRoot();
                    splitPane.getItems().add(resultRoot);
                    
                    // Animate the divider from bottom (1.0) to visible (0.6)
                    splitPane.setDividerPositions(1.0);
                    
                    Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(300), 
                            new KeyValue(splitPane.getDividers().get(0).positionProperty(), 0.6))
                    );
                    timeline.play();
                }
            }
        }
    }
  }

  /**
   * Hides the result pane for the current tab with an animation.
   */
  public void hideResultPane() {
    Tab selectedTab = view.getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
        Node content = view.getTabContent(selectedTab);
        if (content instanceof SplitPane) {
            SplitPane splitPane = (SplitPane) content;
            if (splitPane.getItems().size() > 1) {
                // Animate the divider from current position to bottom (1.0)
                Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(300), 
                        new KeyValue(splitPane.getDividers().get(0).positionProperty(), 1.0))
                );
                timeline.setOnFinished(e -> {
                    if (splitPane.getItems().size() > 1) {
                        splitPane.getItems().remove(1);
                    }
                });
                timeline.play();
            }
        }
    }
  }

  /**
   * Sets the empty state view to display when no tabs are open. Automatically manages the
   * visibility of the tab pane and the empty state.
   *
   * @param emptyStateNode The node to display.
   */
  public void setEmptyState(javafx.scene.Node emptyStateNode) {
    this.emptyStateNode = emptyStateNode;
    view.setEmptyStateView(emptyStateNode);
    updateEmptyStateVisibility();
  }

  /**
   * Sets the factory to create a ResultController for each new tab.
   * If set, each tab will contain a SplitPane with the editor and the result view.
   *
   * @param factory The factory function.
   */
  public void setResultControllerFactory(Function<Tab, ResultController> factory) {
    this.resultControllerFactory = factory;
  }

  /**
   * Returns the ResultController for the currently selected tab.
   *
   * @return The ResultController, or null if none.
   */
  public ResultController getCurrentResultController() {
    Tab selectedTab = view.getTabPane().getSelectionModel().getSelectedItem();
    return model.getResultControllerForTab(selectedTab);
  }

  /**
   * Sets the action to be executed when the "Open File" menu item is clicked.
   *
   * @param action The action to execute.
   */
  public void setOnOpenFileAction(javafx.event.EventHandler<javafx.event.ActionEvent> action) {
    view.getOpenFileItem().setOnAction(action);
  }

  /**
   * Adds a floating action node (e.g. a button) to the editor view.
   *
   * @param node The node to add.
   * @param position The position.
   * @param margin The margin.
   */
  public void addFloatingNode(
      javafx.scene.Node node, javafx.geometry.Pos position, javafx.geometry.Insets margin) {
    view.addFloatingNode(node, position, margin);
  }

  /**
   * Adds a standard "Run/Execute" floating button to the editor. The button is automatically bound
   * to the execution request action and enabled/disabled based on the tab selection.
   *
   * @param tooltipText The text to display in the tooltip.
   */
  public void addExecutionButton(String tooltipText) {
    this.executionButtonTooltip = tooltipText;
  }

  private FloatingButton createExecutionButton(Tab tab) {
    FloatingButton runButton = new FloatingButton(MaterialDesignP.PLAY, executionButtonTooltip);

    runButton.setOnAction(
        e -> {
          if (onExecutionRequest != null) {
            onExecutionRequest.run();
          }
        });
    
    CodeEditorController controller = model.getControllerForTab(tab);
    if (controller != null) {
      BooleanBinding isEmpty =
          Bindings.createBooleanBinding(
              () -> {
                String c = controller.getModel().getContent();
                return c == null || c.trim().isEmpty();
              },
              controller.getModel().contentProperty());
      runButton.disableProperty().bind(isEmpty.or(runButton.loadingProperty()));
    }

    return runButton;
  }

  /**
   * Returns the root node of the view.
   *
   * @return The root Parent node.
   */
  public javafx.scene.Parent getViewRoot() {
    return getView();
  }

  private void initializeTabPane() {
    view.getTabPane()
        .getTabs()
        .addListener(
            (ListChangeListener<Tab>) c -> Platform.runLater(this::updateEmptyStateVisibility));

    // Handle SplitMenuButton actions
    view.getAddTabButton().setOnAction(e -> addNewTab("Untitled", ""));
    view.getNewFileItem().setOnAction(e -> addNewTab("Untitled", ""));
    view.getOpenFileItem().setOnAction(e -> openFile());
    view.getTemplatesItem().setOnAction(e -> openTemplates());
  }

  private void openFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open File");
    File file = fileChooser.showOpenDialog(view.getScene().getWindow());
    if (file != null) {
      addNewTab(file);
    }
  }

  private void openTemplates() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Templates");
    alert.setHeaderText("Templates not implemented yet");
    alert.setContentText("This feature will be available soon.");
    alert.showAndWait();
  }

  private Tab addNewTabHelper(String title, String content, String filePath) {
    CodeEditorController codeEditorController = new CodeEditorController(buttons, content);

    // Wrap editor in StackPane to add floating button
    StackPane editorWrapper = new StackPane(codeEditorController.getView());
    
    Node tabContent = editorWrapper;
    ResultController resultController = null;

    if (resultControllerFactory != null) {
      resultController = resultControllerFactory.apply(null);
      if (resultController != null) {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        // Initially only add the editor wrapper. Result view will be added on demand.
        splitPane.getItems().add(editorWrapper);
        tabContent = splitPane;
      }
    }

    Tab tab = view.createEditorTab(title, tabContent);
    model.addTabModel(tab, codeEditorController);
    if (resultController != null) {
      model.addTabResultController(tab, resultController);
    }
    
    // Create and add execution button if configured
    if (executionButtonTooltip != null) {
        FloatingButton runButton = createExecutionButton(tab);
        tabExecutionButtons.put(tab, runButton);
        StackPane.setAlignment(runButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(runButton, new Insets(0, 40, 40, 0));
        editorWrapper.getChildren().add(runButton);
    }

    tab.textProperty().bind(codeEditorController.getModel().displayNameProperty());

    // Bind modified property to tab icon
    codeEditorController
        .getModel()
        .modifiedProperty()
        .addListener((obs, oldVal, newVal) -> view.updateTabIcon(tab, newVal));
    // Initialize icon state
    view.updateTabIcon(tab, codeEditorController.getModel().isModified());

    view.getTabPane().getTabs().add(tab);
    view.getTabPane().getSelectionModel().select(tab);

    if (filePath != null) {
      codeEditorController.getModel().setFilePath(filePath);
      codeEditorController.getModel().markAsSaved();
    }
    tab.setOnCloseRequest(
        event -> {
          event.consume();
          handleCloseFile(tab);
        });

    // Bind execution shortcut (Ctrl+Enter)
    codeEditorController
        .getView()
        .setOnKeyPressed(
            event -> {
              if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)
                  && onExecutionRequest != null) {
                onExecutionRequest.run();
                event.consume();
              }
            });

    return tab;
  }

  private void updateEmptyStateVisibility() {
    if (emptyStateNode == null) return;

    long realTabCount = view.getTabPane().getTabs().size();
    boolean noTabsOpen = (realTabCount == 0);

    emptyStateNode.setVisible(noTabsOpen);
    emptyStateNode.setManaged(noTabsOpen);

    view.getTabPane().setVisible(!noTabsOpen);
    view.getTabPane().setManaged(!noTabsOpen);
  }

  public Tab addNewTab(String title, String content) {
    return addNewTabHelper(title, content, null);
  }

  public Tab addNewTab(File file) {
    // Check if file is already open
    for (Tab tab : view.getTabPane().getTabs()) {
      CodeEditorController controller = model.getControllerForTab(tab);
      if (controller != null
          && file.getAbsolutePath().equals(controller.getModel().getFilePath())) {
        view.getTabPane().getSelectionModel().select(tab);
        return tab;
      }
    }

    try {
      String content = Files.readString(file.toPath());
      return addNewTabHelper(file.getName(), content, file.getPath());
    } catch (IOException e) {
      showError("Could not read file: " + e.getMessage());
      return null;
    }
  }

  private void initializeKeyboardShortcuts() {
    view.addEventHandler(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
            handleSaveShortcut();
            event.consume();
          }
        });
  }

  private void handleSaveShortcut() {
    Tab selectedTab = view.getTabPane().getSelectionModel().getSelectedItem();
    if (selectedTab != null) {
      CodeEditorController activeController = model.getControllerForTab(selectedTab);
      if (activeController != null) activeController.saveFile();
    }
  }

  public boolean handleCloseFile(Tab tab) {
    if (tab == null) return false;
    CodeEditorController controller = model.getControllerForTab(tab);

    if (controller == null || !controller.getModel().isModified()) {
      closeTab(tab);
      return true;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Unsaved Changes");
    alert.setHeaderText("Save changes to " + controller.getModel().getDisplayName() + "?");
    ButtonType save = new ButtonType("Save");
    ButtonType dontSave = new ButtonType("Don't Save");
    ButtonType cancel = new ButtonType("Cancel");
    alert.getButtonTypes().setAll(save, dontSave, cancel);

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent()) {
      if (result.get() == save) {
        controller.saveFile();
        if (!controller.getModel().isModified()) {
          closeTab(tab);
          return true;
        }
      } else if (result.get() == dontSave) {
        closeTab(tab);
        return true;
      }
    }
    return false;
  }

  private void closeTab(Tab tab) {
    view.getTabPane().getTabs().remove(tab);
    model.removeTabModel(tab);
  }

  private void showError(String content) {
    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, content).showAndWait());
  }

  public TabEditorView getView() {
    return view;
  }

  public TabEditorModel getModel() {
    return model;
  }

  public List<IconButtonType> getButtons() {
    return buttons;
  }

  /**
   * Returns the currently selected tab.
   * @return The selected Tab, or null if none.
   */
  public Tab getSelectedTab() {
      return view.getTabPane().getSelectionModel().getSelectedItem();
  }

  /**
   * Adds a listener to the list of tabs.
   * @param listener The listener to add.
   */
  public void addTabListener(ListChangeListener<Tab> listener) {
      view.getTabPane().getTabs().addListener(listener);
  }

  /**
   * Retrieves the text content of the editor for a specific tab.
   * @param tab The tab to get content from.
   * @return The text content, or null if the tab is not valid.
   */
  public String getEditorContent(Tab tab) {
      CodeEditorController controller = model.getControllerForTab(tab);
      if (controller != null) {
          // Prefer getting text from the view to ensure we have the latest edits
          return controller.getView().getText();
      }
      return null;
  }
}
