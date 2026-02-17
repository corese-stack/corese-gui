package fr.inria.corese.gui.feature.editor.tab;

import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.editor.CodeMirrorWidget;
import fr.inria.corese.gui.component.tabstrip.TabStripController;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.theme.ThemeManager;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * View component for the tabbed editor interface.
 *
 * <p>
 * Manages the visual representation of code editor tabs, including:
 *
 * <ul>
 * <li>Custom tab strip with "New tab" and "Open file" actions.
 * <li>The content area, decoupled from tab headers for flexibility.
 * <li>Visual indicators for modified tabs (dirty state).
 * <li>Empty state display when no tabs are open.
 * <li>Animation for showing/hiding result panes.
 * </ul>
 */
public class TabEditorView extends AbstractView {

	// ==============================================================================================
	// Constants
	// ==============================================================================================

	private static final String STYLESHEET = "/css/features/tab-editor.css";
	private static final String TAB_CONTENT_WRAPPER_ID = "tab-content-wrapper";
	private static final String EMPTY_STATE_VIEW_ID = "empty-state-widget";
	private static final String STYLE_CLASS_TAB_ACTIONS = "tab-header-toolbar";
	private static final String STYLE_CLASS_TAB_HEADER = "tab-header";
	private static final String STYLE_CLASS_FILE_DROP_OVERLAY = "file-drop-overlay";
	private static final String STYLE_CLASS_FILE_DROP_OVERLAY_ACTIVE = "file-drop-overlay-active";
	private static final double WORKSPACE_RADIUS = 8.0;

	// Result Pane Animation
	private static final Duration SPLIT_ANIMATION_DURATION = Duration.millis(300);
	private static final Duration EDITOR_CONTENT_FADE_IN_DURATION = Duration.millis(240);
	private static final double RESULT_PANE_VISIBLE_POSITION = 0.6;
	private static final double RESULT_PANE_HIDDEN_POSITION = 1.0;
	private static final double RESULT_DIVIDER_EPSILON = 0.001;

	// Modified Tab Icon
	private static final double MODIFIED_CIRCLE_RADIUS = 4.0;

	// Execution Button Layout
	private static final Insets EXECUTION_BUTTON_MARGIN = new Insets(0, 60, 40, 0);

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	private final ThemeManager themeManager;
	private final Map<Tab, Node> tabContentMap;
	private final Map<SplitPane, Timeline> resultPaneAnimations;
	private final Map<SplitPane, Boolean> resultPaneTargetVisibility;

	// TabPane is kept as the internal model (selection/list), not rendered as
	// header.
	private final TabPane tabPane;
	private final TabStripController tabStripController;
	private final IconButtonWidget newTabButton;
	private final IconButtonWidget openFileButton;
	private final IconButtonWidget templateButton;
	private final HBox headerBar;
	private final StackPane contentContainer;
	private final VBox mainContent;
	private final Region fileDropOverlay;
	private Consumer<List<File>> onFilesDropped;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	public TabEditorView() {
		super(new StackPane(), STYLESHEET);
		this.themeManager = ThemeManager.getInstance();
		this.tabContentMap = new HashMap<>();
		this.resultPaneAnimations = new WeakHashMap<>();
		this.resultPaneTargetVisibility = new WeakHashMap<>();

		this.tabPane = createTabPane();
		this.tabStripController = new TabStripController(tabPane, themeManager);
		this.tabStripController.setOnCloseRequest(this::requestCloseTab);
		this.newTabButton = createNewTabButton();
		this.openFileButton = createOpenFileButton();
		this.templateButton = createTemplateButton();
		this.headerBar = createTabHeader();
		this.contentContainer = new StackPane();
		this.mainContent = new VBox();
		this.fileDropOverlay = createFileDropOverlay();

		initializeLayout();
		setupListeners();
	}

	// ==============================================================================================
	// Initialization
	// ==============================================================================================

	private TabPane createTabPane() {
		TabPane pane = new TabPane();
		pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		return pane;
	}

	private IconButtonWidget createNewTabButton() {
		return createHeaderActionButton(ButtonFactory.newTab());
	}

	private IconButtonWidget createOpenFileButton() {
		IconButtonWidget button = createHeaderActionButton(ButtonFactory.openFile());
		button.setDisable(true);
		return button;
	}

	private IconButtonWidget createTemplateButton() {
		IconButtonWidget button = createHeaderActionButton(ButtonFactory.template());
		button.setVisible(false);
		button.setManaged(false);
		button.setDisable(true);
		return button;
	}

	private IconButtonWidget createHeaderActionButton(ButtonConfig config) {
		return new IconButtonWidget(config);
	}

	private void initializeLayout() {
		mainContent.setMinHeight(0);
		mainContent.getChildren().addAll(headerBar, contentContainer);
		VBox.setVgrow(contentContainer, Priority.ALWAYS);

		StackPane rootStack = (StackPane) getRoot();
		rootStack.getChildren().add(mainContent);
		RoundedClipSupport.applyRoundedClip(rootStack, WORKSPACE_RADIUS);

		contentContainer.setId(TAB_CONTENT_WRAPPER_ID);
		contentContainer.getChildren().add(fileDropOverlay);
		refreshEditorPlaceholderBackgrounds();
	}

	private Region createFileDropOverlay() {
		Region overlay = new Region();
		overlay.getStyleClass().add(STYLE_CLASS_FILE_DROP_OVERLAY);
		overlay.setMouseTransparent(true);
		overlay.setVisible(false);
		overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		return overlay;
	}

	private HBox createTabHeader() {
		HBox header = new HBox();
		header.getStyleClass().add(STYLE_CLASS_TAB_HEADER);
		header.setAlignment(Pos.BOTTOM_LEFT);

		HBox actionToolbar = createActionToolbar();
		HBox.setHgrow(tabStripController.getView(), Priority.ALWAYS);
		header.getChildren().addAll(tabStripController.getView(), actionToolbar);
		return header;
	}

	private HBox createActionToolbar() {
		HBox toolbar = new HBox(6);
		toolbar.getStyleClass().add(STYLE_CLASS_TAB_ACTIONS);
		toolbar.setAlignment(Pos.CENTER_LEFT);
		toolbar.getChildren().addAll(newTabButton, openFileButton, templateButton);
		toolbar.setMinWidth(Region.USE_PREF_SIZE);
		return toolbar;
	}

	private void setupListeners() {
		setupTabSelectionListener();
		setupTabListChangeListener();
		setupThemeListener();
		setupFileDropListeners();
	}

	private void setupTabSelectionListener() {
		tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
			showContentForTab(newTab, false);
			requestFocusForTab(newTab);
		});
	}

	private void setupTabListChangeListener() {
		tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					for (Tab tab : change.getRemoved()) {
						tabContentMap.remove(tab);
					}
				}

				if (change.wasAdded()) {
					Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
					if (selectedTab != null && change.getAddedSubList().contains(selectedTab)) {
						Platform.runLater(() -> showContentForTab(selectedTab, false));
					}
				}
			}

			if (tabPane.getTabs().isEmpty()) {
				showContentForTab(null, false);
			}
		});
	}

	private void setupThemeListener() {
		themeManager.accentColorProperty().addListener((obs, oldColor, newColor) -> tabStripController.refresh());
		themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
			tabStripController.refresh();
			refreshEditorPlaceholderBackgrounds();
		});
	}

	private void setupFileDropListeners() {
		contentContainer.addEventFilter(DragEvent.DRAG_OVER, this::handleFileDragOver);
		contentContainer.addEventFilter(DragEvent.DRAG_DROPPED, this::handleFileDropped);
		contentContainer.addEventFilter(DragEvent.DRAG_ENTERED, this::handleFileDragEntered);
		contentContainer.addEventFilter(DragEvent.DRAG_EXITED, this::handleFileDragExited);
	}

	private void requestCloseTab(Tab tab) {
		Event closeEvent = new Event(tab, tab, Tab.TAB_CLOSE_REQUEST_EVENT);
		Event.fireEvent(tab, closeEvent);
		if (!closeEvent.isConsumed()) {
			tabPane.getTabs().remove(tab);
		}
	}

	// ==============================================================================================
	// Content Management
	// ==============================================================================================

	private void showContentForTab(Tab selectedTab, boolean animateOnOpen) {
		contentContainer.getChildren().removeIf(node -> TAB_CONTENT_WRAPPER_ID.equals(node.getId()));

		if (selectedTab != null) {
			Node content = tabContentMap.get(selectedTab);
			if (content != null) {
				StackPane wrapper = new StackPane(content);
				wrapper.setId(TAB_CONTENT_WRAPPER_ID);
				applyEditorPlaceholderBackground(wrapper);
				contentContainer.getChildren().add(0, wrapper);
				if (animateOnOpen) {
					playEditorContentFadeIn(resolveEditorFadeTarget(selectedTab, content));
				}
			}
		}
	}

	private void refreshEditorPlaceholderBackgrounds() {
		applyEditorPlaceholderBackground(contentContainer);
		contentContainer.getChildren().stream()
				.filter(node -> TAB_CONTENT_WRAPPER_ID.equals(node.getId()) && node instanceof Region)
				.map(Region.class::cast).forEach(this::applyEditorPlaceholderBackground);
	}

	private void applyEditorPlaceholderBackground(Region region) {
		Color backgroundColor = Color.web(resolveEditorPlaceholderBackground());
		region.setBackground(new Background(new BackgroundFill(backgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
	}

	private String resolveEditorPlaceholderBackground() {
		return themeManager.getEditorBackgroundHex();
	}

	private Node resolveEditorFadeTarget(Tab tab, Node tabContent) {
		TabContext context = TabContext.get(tab);
		if (context != null && context.getEditorController() != null) {
			Node editorRoot = context.getEditorController().getViewRoot();
			Node codeMirror = findCodeMirrorNode(editorRoot);
			if (codeMirror != null) {
				return codeMirror;
			}
		}

		return findCodeMirrorNode(tabContent);
	}

	private Node findCodeMirrorNode(Node node) {
		if (node == null) {
			return null;
		}
		if (node instanceof CodeMirrorWidget) {
			return node;
		}
		if (node instanceof SplitPane splitPane) {
			for (Node item : splitPane.getItems()) {
				Node found = findCodeMirrorNode(item);
				if (found != null) {
					return found;
				}
			}
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				Node found = findCodeMirrorNode(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private void playEditorContentFadeIn(Node editorTarget) {
		if (editorTarget == null) {
			return;
		}
		editorTarget.setOpacity(0.0);
		FadeTransition fadeIn = new FadeTransition(EDITOR_CONTENT_FADE_IN_DURATION, editorTarget);
		fadeIn.setFromValue(0.0);
		fadeIn.setToValue(1.0);
		fadeIn.play();
	}

	private Node getTabContent(Tab tab) {
		return tabContentMap.get(tab);
	}

	private void requestFocusForTab(Tab tab) {
		if (tab == null) {
			return;
		}
		Platform.runLater(() -> {
			TabContext context = TabContext.get(tab);
			if (context != null) {
				context.getEditorController().requestEditorFocus();
				return;
			}
			Node content = tabContentMap.get(tab);
			if (content == null) {
				return;
			}
			Node focusTarget = resolveEditorFadeTarget(tab, content);
			if (focusTarget instanceof CodeMirrorWidget codeMirrorWidget) {
				codeMirrorWidget.requestEditorFocus();
			} else if (focusTarget != null) {
				focusTarget.requestFocus();
			}
		});
	}

	// ==============================================================================================
	// Public API - Tabs
	// ==============================================================================================

	/** Creates a configured Tab with the associated content. */
	public Tab createEditorTab(String title, Node content) {
		Tab tab = new Tab(title);
		if (content instanceof Region region) {
			region.setMaxWidth(Double.MAX_VALUE);
			region.setMaxHeight(Double.MAX_VALUE);
		}
		tabContentMap.put(tab, content);
		return tab;
	}

	public void addNewEditorTab(Tab tab) {
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		showContentForTab(tab, true);
	}

	public void selectTab(Tab tab) {
		tabPane.getSelectionModel().select(tab);
	}

	public Tab getSelectedTab() {
		return tabPane.getSelectionModel().getSelectedItem();
	}

	public ObservableList<Tab> getTabs() {
		return tabPane.getTabs();
	}

	// ==============================================================================================
	// Public API - State & UI
	// ==============================================================================================

	public void updateTabIcon(Tab tab, boolean isModified) {
		if (isModified) {
			Circle circle = new Circle(MODIFIED_CIRCLE_RADIUS, themeManager.getAccentColor());
			tab.setGraphic(circle);
		} else {
			tab.setGraphic(null);
		}
		tabStripController.refresh();
	}

	public void setTabsVisible(boolean visible) {
		headerBar.setVisible(visible);
		headerBar.setManaged(visible);
		if (visible) {
			// First display can occur before layout metrics are stable.
			// Refresh again on next pulses to get correct full-width tab sizing.
			Platform.runLater(tabStripController::refresh);
			Platform.runLater(() -> Platform.runLater(tabStripController::refresh));
		}
	}

	public void setEmptyStateWidget(Node emptyStateWidget) {
		emptyStateWidget.setId(EMPTY_STATE_VIEW_ID);
		contentContainer.getChildren().removeIf(node -> EMPTY_STATE_VIEW_ID.equals(node.getId()));
		contentContainer.getChildren().add(0, emptyStateWidget);
	}

	public void updateEmptyStateVisibility(boolean visible) {
		contentContainer.getChildren().stream().filter(node -> EMPTY_STATE_VIEW_ID.equals(node.getId())).findFirst()
				.ifPresent(node -> {
					node.setVisible(visible);
					node.setManaged(visible);
				});
	}

	public void addFloatingNode(Node node, Pos position, Insets margin) {
		StackPane.setAlignment(node, position);
		StackPane.setMargin(node, margin);
		contentContainer.getChildren().add(node);
		fileDropOverlay.toFront();
	}

	// ==============================================================================================
	// Public API - Dialogs
	// ==============================================================================================

	public void showError(String title, String message) {
		ModalService.getInstance().showError(title, message);
	}

	public void showError(String title, String message, String details) {
		ModalService.getInstance().showError(title, message, details);
	}

	public void showUnsavedChangesDialog(String fileName, Consumer<ModalService.UnsavedChangesResult> callback) {
		ModalService.getInstance().showUnsavedChangesDialog(fileName, callback);
	}

	// ==============================================================================================
	// Public API - Results Pane
	// ==============================================================================================

	public void showResultPane(Node resultNode) {
		Tab selectedTab = getSelectedTab();
		if (selectedTab == null || resultNode == null)
			return;

		Node content = getTabContent(selectedTab);
		if (!(content instanceof SplitPane splitPane)) {
			return;
		}

		stopResultPaneAnimation(splitPane);
		if (Boolean.TRUE.equals(resultPaneTargetVisibility.get(splitPane))) {
			splitPane.setDividerPositions(RESULT_PANE_VISIBLE_POSITION);
			return;
		}
		resultPaneTargetVisibility.put(splitPane, true);
		prepareResultNodeForSlide(resultNode);

		if (splitPane.getItems().size() < 2) {
			splitPane.getItems().add(resultNode);
		}

		resultNode.setVisible(false);
		splitPane.setDividerPositions(RESULT_PANE_HIDDEN_POSITION);
		splitPane.applyCss();
		splitPane.layout();

		Platform.runLater(() -> {
			if (!splitPane.getItems().contains(resultNode)) {
				return;
			}
			resultNode.setVisible(true);
			splitPane.setDividerPositions(RESULT_PANE_HIDDEN_POSITION);

			Timeline timeline = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(splitPane.getDividers().get(0).positionProperty(),
									splitPane.getDividers().get(0).getPosition(), Interpolator.EASE_BOTH)),
					new KeyFrame(SPLIT_ANIMATION_DURATION,
							new KeyValue(splitPane.getDividers().get(0).positionProperty(),
									RESULT_PANE_VISIBLE_POSITION, Interpolator.EASE_BOTH)));
			timeline.setOnFinished(e -> resultPaneAnimations.remove(splitPane));
			resultPaneAnimations.put(splitPane, timeline);
			timeline.play();
		});
	}

	public void hideResultPane() {
		Tab selectedTab = getSelectedTab();
		if (selectedTab == null)
			return;

		Node content = getTabContent(selectedTab);
		if (!(content instanceof SplitPane splitPane)) {
			return;
		}
		if (splitPane.getItems().size() <= 1) {
			resultPaneTargetVisibility.put(splitPane, false);
			return;
		}

		stopResultPaneAnimation(splitPane);
		if (Boolean.FALSE.equals(resultPaneTargetVisibility.get(splitPane))) {
			// Already hidden (or already animating towards hidden): avoid replaying
			// collapse animation.
			return;
		}
		resultPaneTargetVisibility.put(splitPane, false);

		double currentPosition = splitPane.getDividers().get(0).getPosition();
		if (Math.abs(currentPosition - RESULT_PANE_HIDDEN_POSITION) <= RESULT_DIVIDER_EPSILON) {
			splitPane.getItems().remove(1);
			return;
		}
		Timeline timeline = new Timeline(
				new KeyFrame(Duration.ZERO,
						new KeyValue(splitPane.getDividers().get(0).positionProperty(),
								splitPane.getDividers().get(0).getPosition(), Interpolator.EASE_BOTH)),
				new KeyFrame(SPLIT_ANIMATION_DURATION, new KeyValue(splitPane.getDividers().get(0).positionProperty(),
						RESULT_PANE_HIDDEN_POSITION, Interpolator.EASE_BOTH)));
		timeline.setOnFinished(e -> {
			if (splitPane.getItems().size() > 1) {
				splitPane.getItems().remove(1);
			}
			resultPaneTargetVisibility.put(splitPane, false);
			resultPaneAnimations.remove(splitPane);
		});
		resultPaneAnimations.put(splitPane, timeline);
		timeline.play();
	}

	public boolean isResultPaneVisibleForSelectedTab() {
		Tab selectedTab = getSelectedTab();
		if (selectedTab == null) {
			return false;
		}
		Node content = getTabContent(selectedTab);
		if (!(content instanceof SplitPane splitPane)) {
			return false;
		}
		return splitPane.getItems().size() > 1;
	}

	private void stopResultPaneAnimation(SplitPane splitPane) {
		Timeline activeAnimation = resultPaneAnimations.remove(splitPane);
		if (activeAnimation != null) {
			activeAnimation.stop();
		}
	}

	private void prepareResultNodeForSlide(Node resultNode) {
		if (resultNode instanceof Region region) {
			region.setMinHeight(0);
		}
		SplitPane.setResizableWithParent(resultNode, Boolean.TRUE);
	}

	// ==============================================================================================
	// Public API - Actions
	// ==============================================================================================

	public void setOnNewTabAction(EventHandler<ActionEvent> action) {
		newTabButton.setOnAction(action);
	}

	public void setOnOpenFileAction(EventHandler<ActionEvent> action) {
		openFileButton.setOnAction(action);
		openFileButton.setDisable(action == null);
	}

	public void setOnTemplateAction(EventHandler<ActionEvent> action) {
		boolean enabled = action != null;
		templateButton.setOnAction(action);
		templateButton.setDisable(!enabled);
		templateButton.setVisible(enabled);
		templateButton.setManaged(enabled);
	}

	/**
	 * Registers the callback invoked when one or more files are dropped over the
	 * editor content area.
	 *
	 * <p>
	 * Passing {@code null} disables file drop handling and removes the active
	 * overlay state.
	 */
	public void setOnFilesDropped(Consumer<List<File>> onFilesDropped) {
		this.onFilesDropped = onFilesDropped;
		if (onFilesDropped == null) {
			setFileDropActive(false);
		}
	}

	public void subscribeToTabChanges(ListChangeListener<Tab> listener) {
		tabPane.getTabs().addListener(listener);
	}

	public void addSelectionListener(ChangeListener<Tab> listener) {
		tabPane.getSelectionModel().selectedItemProperty().addListener(listener);
	}

	public static Insets getExecutionButtonMargin() {
		return EXECUTION_BUTTON_MARGIN;
	}

	private void handleFileDragOver(DragEvent event) {
		if (onFilesDropped == null) {
			return;
		}
		if (hasFilesInDragboard(event)) {
			event.acceptTransferModes(TransferMode.COPY);
			event.consume();
		}
	}

	private void handleFileDropped(DragEvent event) {
		if (onFilesDropped == null) {
			return;
		}
		boolean completed = false;
		if (hasFilesInDragboard(event)) {
			Dragboard dragboard = event.getDragboard();
			onFilesDropped.accept(List.copyOf(dragboard.getFiles()));
			completed = true;
		}
		setFileDropActive(false);
		event.setDropCompleted(completed);
		event.consume();
	}

	private void handleFileDragEntered(DragEvent event) {
		if (onFilesDropped == null) {
			return;
		}
		if (hasFilesInDragboard(event)) {
			setFileDropActive(true);
			event.consume();
		}
	}

	private void handleFileDragExited(DragEvent event) {
		if (onFilesDropped == null) {
			return;
		}
		setFileDropActive(false);
	}

	private boolean hasFilesInDragboard(DragEvent event) {
		Dragboard dragboard = event.getDragboard();
		return dragboard != null && dragboard.hasFiles();
	}

	private void setFileDropActive(boolean active) {
		if (active) {
			if (!fileDropOverlay.getStyleClass().contains(STYLE_CLASS_FILE_DROP_OVERLAY_ACTIVE)) {
				fileDropOverlay.getStyleClass().add(STYLE_CLASS_FILE_DROP_OVERLAY_ACTIVE);
			}
			fileDropOverlay.setVisible(true);
			fileDropOverlay.toFront();
			return;
		}
		fileDropOverlay.getStyleClass().remove(STYLE_CLASS_FILE_DROP_OVERLAY_ACTIVE);
		fileDropOverlay.setVisible(false);
	}
}
