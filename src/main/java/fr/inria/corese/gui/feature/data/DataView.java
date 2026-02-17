package fr.inria.corese.gui.feature.data;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.service.DataWorkspaceStatus;
import fr.inria.corese.gui.core.service.ReasoningProfile;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.feature.data.model.DataRuleFileItem;
import fr.inria.corese.gui.feature.data.support.DataRuleFileRowFactory;
import fr.inria.corese.gui.feature.data.support.DataStatusTooltipSupport;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Main view for the Data page.
 *
 * <p>
 * Layout:
 * <ul>
 * <li>Left pane: reasoning controls (built-in profiles + rule files area)</li>
 * <li>Right pane: graph visualization and toolbar actions</li>
 * </ul>
 */
public class DataView extends AbstractView {

	@SuppressWarnings("java:S1075") // Internal classpath stylesheet path
	private static final String STYLESHEET_PATH = "/css/features/data-view.css";

	@SuppressWarnings("java:S1075") // Internal classpath stylesheet path
	private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";
	private static final String STYLE_CLASS_GRAPH_EMPTY_STATE = "data-graph-empty-state";
	private static final String STYLE_CLASS_GRAPH_DROP_OVERLAY = "data-graph-drop-overlay";
	private static final String STYLE_CLASS_GRAPH_DROP_OVERLAY_ACTIVE = "data-graph-drop-overlay-active";
	private static final String STYLE_CLASS_GRAPH_BODY = "data-graph-body";
	private static final String STYLE_CLASS_PAGE_LAYOUT = "data-page-layout";
	private static final String STYLE_CLASS_REASONING_SECTION = "data-reasoning-section";
	private static final String STYLE_CLASS_STATUS_GROUP = "data-status-group";
	private static final String STYLE_CLASS_STATUS_GROUP_SECONDARY = "data-status-group-secondary";
	private static final String STYLE_CLASS_CUSTOM_RULES_DROP_OVERLAY = "data-custom-rules-drop-overlay";
	private static final String STYLE_CLASS_CUSTOM_RULES_DROP_OVERLAY_ACTIVE = "data-custom-rules-drop-overlay-active";
	private static final String TOOLTIP_TITLE_TRIPLES = "Triples";
	private static final String TOOLTIP_TITLE_SOURCES = "Sources";
	private static final String TOOLTIP_TITLE_NAMED_GRAPHS = "Named Graphs";
	private static final String TOOLTIP_TITLE_INFERRED = "Inferred";
	private static final double CARD_RADIUS = 8.0;

	private final GraphDisplayWidget graphWidget = new GraphDisplayWidget();
	private final ToolbarWidget toolbarWidget = new ToolbarWidget();
	private final StackPane graphContainer = new StackPane();
	private final Region graphDropOverlay = new Region();

	private final ToggleSwitch rdfsToggle = new ToggleSwitch();
	private final ToggleSwitch owlRlToggle = new ToggleSwitch();
	private final ToggleSwitch owlRlLiteToggle = new ToggleSwitch();
	private final ToggleSwitch owlRlExtToggle = new ToggleSwitch();
	private final Button rdfsViewButton = createBuiltInRuleViewButton("View RDFS profile rule file");
	private final Button owlRlViewButton = createBuiltInRuleViewButton("View OWL RL profile rule file");
	private final Button owlRlLiteViewButton = createBuiltInRuleViewButton("View OWL RL Lite profile rule file");
	private final Button owlRlExtViewButton = createBuiltInRuleViewButton("View OWL RL Ext profile rule file");
	private final VBox ruleFilesList = new VBox(8);
	private final ScrollPane ruleFilesScrollPane = new ScrollPane(ruleFilesList);
	private final StackPane ruleFilesContent = new StackPane();
	private final Region ruleFilesDropOverlay = new Region();
	private final ToolbarWidget ruleFilesToolbar = new ToolbarWidget(ToolbarWidget.Orientation.HORIZONTAL);

	private final Label tripleCountLabel = new Label();
	private final Label sourceCountLabel = new Label();
	private final Label namedGraphCountLabel = new Label();
	private final Label inferredTripleCountLabel = new Label();
	private DataViewController controller;
	private EmptyStateWidget graphEmptyStateWidget;
	private EmptyStateWidget ruleFilesEmptyStateWidget;
	private Consumer<List<File>> onGraphFilesDropped;
	private Consumer<List<File>> onRuleFilesDropped;

	/**
	 * Creates the Data page view.
	 */
	public DataView() {
		super(new BorderPane(), STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		initializeLayout();
		updateStatus(DataWorkspaceStatus.empty());
		updateRuleFiles(List.of(), (id, enabled) -> {
		}, id -> {
		}, id -> {
		}, id -> {
		});
	}

	public void setController(DataViewController controller) {
		this.controller = controller;
	}

	public DataViewController getController() {
		return controller;
	}

	private void initializeLayout() {
		BorderPane root = (BorderPane) getRoot();
		root.getStyleClass().addAll("data-page-root", "app-workspace-root");

		VBox reasoningPane = createReasoningPane();
		BorderPane graphPane = createGraphPane();
		HBox layout = new HBox(reasoningPane, graphPane);
		layout.getStyleClass().addAll(STYLE_CLASS_PAGE_LAYOUT, "app-workspace-layout");
		HBox.setHgrow(graphPane, Priority.ALWAYS);

		root.setCenter(layout);
	}

	private VBox createReasoningPane() {
		VBox pane = new VBox(10);
		pane.getStyleClass().addAll("data-left-pane", "app-card", "app-card-subtle");
		RoundedClipSupport.applyRoundedClip(pane, CARD_RADIUS);

		Label titleLabel = new Label("Reasoning");
		titleLabel.getStyleClass().add("data-pane-title");

		Label builtInTitle = new Label("Built-in Profiles");
		builtInTitle.getStyleClass().add("data-section-title");
		VBox builtInRules = new VBox(8, createBuiltInRuleRow("RDFS", rdfsToggle, rdfsViewButton),
				createBuiltInRuleRow("OWL RL", owlRlToggle, owlRlViewButton),
				createBuiltInRuleRow("OWL RL Lite", owlRlLiteToggle, owlRlLiteViewButton),
				createBuiltInRuleRow("OWL RL Ext", owlRlExtToggle, owlRlExtViewButton));
		builtInRules.getStyleClass().addAll("data-rule-list", "app-card", "app-card-default");
		RoundedClipSupport.applyRoundedClip(builtInRules, CARD_RADIUS);

		VBox builtInCard = new VBox(8, builtInTitle, builtInRules);
		builtInCard.getStyleClass().add(STYLE_CLASS_REASONING_SECTION);

		Label customTitle = new Label("Rule Files");
		customTitle.getStyleClass().add("data-section-title");
		HBox customHeader = new HBox(customTitle);
		customHeader.getStyleClass().add("data-custom-rules-header");

		initializeRuleFilesContent();
		VBox.setVgrow(ruleFilesContent, Priority.ALWAYS);
		ruleFilesToolbar.getStyleClass().add("data-custom-rules-toolbar");

		VBox customRulesSurface = new VBox(ruleFilesContent, ruleFilesToolbar);
		customRulesSurface.getStyleClass().addAll("data-custom-rules-surface", "app-card", "app-card-default");
		customRulesSurface.setMaxHeight(Double.MAX_VALUE);
		RoundedClipSupport.applyRoundedClip(customRulesSurface, CARD_RADIUS);
		VBox.setVgrow(customRulesSurface, Priority.ALWAYS);

		VBox customCard = new VBox(8, customHeader, customRulesSurface);
		customCard.getStyleClass().addAll(STYLE_CLASS_REASONING_SECTION, "data-custom-rules-section");
		VBox.setVgrow(customRulesSurface, Priority.ALWAYS);

		pane.getChildren().addAll(titleLabel, builtInCard, customCard);
		VBox.setVgrow(customCard, Priority.ALWAYS);
		return pane;
	}

	private static Button createBuiltInRuleViewButton(String tooltipText) {
		FontIcon icon = new FontIcon(ButtonIcon.VIEW.getIkon());
		icon.getStyleClass().add("data-custom-rule-menu-button-icon");
		icon.setIconSize(16);

		Button button = new Button();
		button.setGraphic(icon);
		button.getStyleClass().addAll(Styles.BUTTON_OUTLINED, "data-custom-rule-menu-button");
		button.setFocusTraversable(false);
		button.setDisable(true);
		button.setTooltip(new Tooltip(tooltipText));
		return button;
	}

	private HBox createBuiltInRuleRow(String labelText, ToggleSwitch toggle, Button viewButton) {
		Label label = new Label(labelText);
		label.getStyleClass().add("data-rule-toggle-label");

		toggle.getStyleClass().add("data-rule-toggle-switch");
		toggle.setFocusTraversable(false);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox row = new HBox(8, label, spacer, viewButton, toggle);
		row.getStyleClass().addAll("data-rule-row", "app-card-row");
		row.setAlignment(Pos.CENTER_LEFT);
		return row;
	}

	private void initializeRuleFilesContent() {
		ruleFilesContent.getStyleClass().add("data-custom-rules-content");
		ruleFilesContent.setMaxHeight(Double.MAX_VALUE);
		ruleFilesContent.getChildren().setAll(ruleFilesScrollPane, ruleFilesDropOverlay);
		RoundedClipSupport.applyRoundedClip(ruleFilesContent, 8);

		ruleFilesList.getStyleClass().add("data-custom-rules-list");

		ruleFilesScrollPane.getStyleClass().add("data-custom-rules-scroll");
		ruleFilesScrollPane.setFitToWidth(true);
		ruleFilesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		ruleFilesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		ruleFilesScrollPane.setVisible(false);
		ruleFilesScrollPane.setManaged(false);
		ruleFilesScrollPane.setMinHeight(0);
		ruleFilesScrollPane.setMaxHeight(Double.MAX_VALUE);
		ruleFilesScrollPane.setPrefViewportHeight(240);

		ruleFilesDropOverlay.getStyleClass().add(STYLE_CLASS_CUSTOM_RULES_DROP_OVERLAY);
		ruleFilesDropOverlay.setManaged(false);
		ruleFilesDropOverlay.setVisible(false);
		ruleFilesDropOverlay.setMouseTransparent(true);
		ruleFilesDropOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		ruleFilesEmptyStateWidget = new EmptyStateWidget(ButtonIcon.TEMPLATE, "No rule files", FileTypeSupport
				.withAcceptedExtensions("Load a rule file or drop one here.", FileTypeSupport.ruleExtensions()));
		ruleFilesEmptyStateWidget.getStyleClass().add("data-custom-rules-empty-state");
		ruleFilesEmptyStateWidget.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		ruleFilesContent.getChildren().add(1, ruleFilesEmptyStateWidget);

		setupRuleFilesDropListeners();
	}

	private BorderPane createGraphPane() {
		BorderPane graphPane = new BorderPane();
		graphPane.getStyleClass().add("data-right-pane");
		toolbarWidget.getStyleClass().add("data-graph-toolbar");

		initializeGraphContainer();

		HBox statusBar = createStatusBar();
		initializeStatusMetricLabels();

		HBox graphBody = new HBox(graphContainer, toolbarWidget);
		graphBody.getStyleClass().add(STYLE_CLASS_GRAPH_BODY);
		HBox.setHgrow(graphContainer, Priority.ALWAYS);

		VBox graphCard = new VBox(graphBody, statusBar);
		graphCard.getStyleClass().addAll("data-graph-card", "app-card", "app-card-default");
		VBox.setVgrow(graphBody, Priority.ALWAYS);

		graphPane.setCenter(graphCard);

		return graphPane;
	}

	private HBox createStatusBar() {
		HBox primaryStatusGroup = createStatusGroup(tripleCountLabel, sourceCountLabel);
		HBox secondaryStatusGroup = createStatusGroup(namedGraphCountLabel, inferredTripleCountLabel);
		secondaryStatusGroup.getStyleClass().add(STYLE_CLASS_STATUS_GROUP_SECONDARY);

		Region statusSpacer = new Region();
		HBox.setHgrow(statusSpacer, Priority.ALWAYS);

		HBox statusBar = new HBox(10, primaryStatusGroup, statusSpacer, secondaryStatusGroup);
		statusBar.getStyleClass().add("data-status-bar");
		return statusBar;
	}

	private static HBox createStatusGroup(Label... labels) {
		HBox group = new HBox(14, labels);
		group.getStyleClass().add(STYLE_CLASS_STATUS_GROUP);
		return group;
	}

	private void initializeStatusMetricLabels() {
		for (Label label : List.of(tripleCountLabel, sourceCountLabel, namedGraphCountLabel,
				inferredTripleCountLabel)) {
			label.getStyleClass().add("data-status-label");
			label.setFocusTraversable(false);
		}
	}

	private void initializeGraphContainer() {
		graphContainer.getStyleClass().add("data-graph-container");
		graphWidget.setBorderVisible(false);
		graphContainer.getChildren().setAll(graphWidget, graphDropOverlay);

		graphDropOverlay.getStyleClass().add(STYLE_CLASS_GRAPH_DROP_OVERLAY);
		graphDropOverlay.setManaged(false);
		graphDropOverlay.setVisible(false);
		graphDropOverlay.setMouseTransparent(true);
		graphDropOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		setupGraphDropListeners();
	}

	private void setupGraphDropListeners() {
		graphContainer.addEventFilter(DragEvent.DRAG_OVER, this::handleGraphDragOver);
		graphContainer.addEventFilter(DragEvent.DRAG_DROPPED, this::handleGraphDropped);
		graphContainer.addEventFilter(DragEvent.DRAG_ENTERED, this::handleGraphDragEntered);
		graphContainer.addEventFilter(DragEvent.DRAG_EXITED, this::handleGraphDragExited);
	}

	private void handleGraphDragOver(DragEvent event) {
		handleDragOver(event, onGraphFilesDropped);
	}

	private void handleGraphDropped(DragEvent event) {
		handleDropped(event, onGraphFilesDropped, this::setGraphDropActive);
	}

	private void handleGraphDragEntered(DragEvent event) {
		handleDragEntered(event, onGraphFilesDropped, this::setGraphDropActive);
	}

	private void handleGraphDragExited(DragEvent event) {
		handleDragExited(onGraphFilesDropped, this::setGraphDropActive);
	}

	private void setupRuleFilesDropListeners() {
		ruleFilesContent.addEventFilter(DragEvent.DRAG_OVER, this::handleRuleFilesDragOver);
		ruleFilesContent.addEventFilter(DragEvent.DRAG_DROPPED, this::handleRuleFilesDropped);
		ruleFilesContent.addEventFilter(DragEvent.DRAG_ENTERED, this::handleRuleFilesDragEntered);
		ruleFilesContent.addEventFilter(DragEvent.DRAG_EXITED, this::handleRuleFilesDragExited);
	}

	private void handleRuleFilesDragOver(DragEvent event) {
		handleDragOver(event, onRuleFilesDropped);
	}

	private void handleRuleFilesDropped(DragEvent event) {
		handleDropped(event, onRuleFilesDropped, this::setRuleFilesDropActive);
	}

	private void handleRuleFilesDragEntered(DragEvent event) {
		handleDragEntered(event, onRuleFilesDropped, this::setRuleFilesDropActive);
	}

	private void handleRuleFilesDragExited(DragEvent event) {
		handleDragExited(onRuleFilesDropped, this::setRuleFilesDropActive);
	}

	private boolean hasFilesInDragboard(DragEvent event) {
		Dragboard dragboard = event.getDragboard();
		return dragboard != null && dragboard.hasFiles();
	}

	private void handleDragOver(DragEvent event, Consumer<List<File>> dropHandler) {
		if (!isDropEnabled(dropHandler)) {
			return;
		}
		if (hasFilesInDragboard(event)) {
			event.acceptTransferModes(TransferMode.COPY);
			event.consume();
		}
	}

	private void handleDropped(DragEvent event, Consumer<List<File>> dropHandler,
			Consumer<Boolean> overlayStateSetter) {
		if (!isDropEnabled(dropHandler)) {
			return;
		}
		boolean completed = false;
		if (hasFilesInDragboard(event)) {
			Dragboard dragboard = event.getDragboard();
			dropHandler.accept(List.copyOf(dragboard.getFiles()));
			completed = true;
		}
		overlayStateSetter.accept(false);
		event.setDropCompleted(completed);
		event.consume();
	}

	private void handleDragEntered(DragEvent event, Consumer<List<File>> dropHandler,
			Consumer<Boolean> overlayStateSetter) {
		if (!isDropEnabled(dropHandler)) {
			return;
		}
		if (hasFilesInDragboard(event)) {
			overlayStateSetter.accept(true);
			event.consume();
		}
	}

	private void handleDragExited(Consumer<List<File>> dropHandler, Consumer<Boolean> overlayStateSetter) {
		if (!isDropEnabled(dropHandler)) {
			return;
		}
		overlayStateSetter.accept(false);
	}

	private static boolean isDropEnabled(Consumer<List<File>> dropHandler) {
		return dropHandler != null;
	}

	private void setGraphDropActive(boolean active) {
		setDropOverlayActive(graphDropOverlay, STYLE_CLASS_GRAPH_DROP_OVERLAY_ACTIVE, active);
	}

	private void setRuleFilesDropActive(boolean active) {
		setDropOverlayActive(ruleFilesDropOverlay, STYLE_CLASS_CUSTOM_RULES_DROP_OVERLAY_ACTIVE, active);
	}

	private static void setDropOverlayActive(Region overlay, String activeStyleClass, boolean active) {
		if (active) {
			if (!overlay.getStyleClass().contains(activeStyleClass)) {
				overlay.getStyleClass().add(activeStyleClass);
			}
			overlay.setManaged(true);
			overlay.setVisible(true);
			overlay.toFront();
			return;
		}
		overlay.getStyleClass().remove(activeStyleClass);
		overlay.setManaged(false);
		overlay.setVisible(false);
	}

	/**
	 * Sets the right-side toolbar button configuration.
	 *
	 * @param buttons
	 *            toolbar buttons
	 */
	public void setToolbarActions(List<ButtonConfig> buttons) {
		toolbarWidget.setButtons(buttons);
	}

	/**
	 * Inserts a separator after a specific toolbar button.
	 *
	 * @param buttonIcon
	 *            toolbar button icon key
	 */
	public void insertToolbarSeparatorAfter(ButtonIcon buttonIcon) {
		toolbarWidget.insertSeparatorAfter(buttonIcon);
	}

	/**
	 * Enables or disables a specific toolbar button.
	 *
	 * @param buttonIcon
	 *            toolbar button icon key
	 * @param disabled
	 *            true to disable, false to enable
	 */
	public void setToolbarButtonDisabled(ButtonIcon buttonIcon, boolean disabled) {
		toolbarWidget.setButtonDisabled(buttonIcon, disabled);
	}

	/**
	 * Sets the rule-files toolbar button configuration.
	 *
	 * @param buttons
	 *            toolbar buttons
	 */
	public void setRuleFilesToolbarActions(List<ButtonConfig> buttons) {
		ruleFilesToolbar.setButtons(buttons);
	}

	/**
	 * Enables or disables a rule-files toolbar button.
	 *
	 * @param buttonIcon
	 *            toolbar button icon key
	 * @param disabled
	 *            true to disable, false to enable
	 */
	public void setRuleFilesToolbarButtonDisabled(ButtonIcon buttonIcon, boolean disabled) {
		ruleFilesToolbar.setButtonDisabled(buttonIcon, disabled);
	}

	/**
	 * Configures the graph empty-state actions.
	 *
	 * @param onLoadFileAction
	 *            action for file loading
	 * @param onLoadUriAction
	 *            action for URI loading
	 */
	public void configureGraphEmptyState(Runnable onLoadFileAction, Runnable onLoadUriAction) {
		if (graphEmptyStateWidget != null) {
			graphContainer.getChildren().remove(graphEmptyStateWidget);
		}

		graphEmptyStateWidget = new EmptyStateWidget(ButtonIcon.NAV_DATA, "No data loaded",
				FileTypeSupport.withAcceptedExtensions("Load RDF from a file or URI, or drop RDF files here.",
						FileTypeSupport.rdfExtensions()),
				EmptyStateWidget.createAction("Load RDF File", ButtonIcon.EMPTY_ACTION_OPEN, onLoadFileAction),
				EmptyStateWidget.createAction("Load from URI", ButtonIcon.OPEN_URI, onLoadUriAction));
		graphEmptyStateWidget.getStyleClass().add(STYLE_CLASS_GRAPH_EMPTY_STATE);
		graphEmptyStateWidget.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		graphEmptyStateWidget.setManaged(false);
		graphEmptyStateWidget.setVisible(false);

		int dropOverlayIndex = graphContainer.getChildren().indexOf(graphDropOverlay);
		int insertIndex = dropOverlayIndex < 0 ? graphContainer.getChildren().size() : dropOverlayIndex;
		graphContainer.getChildren().add(insertIndex, graphEmptyStateWidget);
	}

	/**
	 * Shows or hides the graph empty state.
	 *
	 * @param visible
	 *            true to show empty state, false to hide it
	 */
	public void setGraphEmptyStateVisible(boolean visible) {
		if (graphEmptyStateWidget == null) {
			return;
		}
		graphEmptyStateWidget.setVisible(visible);
		graphEmptyStateWidget.setManaged(visible);
		graphWidget.setVisible(!visible);
	}

	/**
	 * Renders the rule file list.
	 *
	 * @param rules
	 *            rule files to render
	 * @param onToggle
	 *            callback called when a rule toggle changes
	 * @param onReload
	 *            callback called when a rule reload is requested
	 * @param onView
	 *            callback called when a rule preview is requested
	 * @param onRemove
	 *            callback called when a rule is removed
	 */
	public void updateRuleFiles(List<DataRuleFileItem> rules, BiConsumer<String, Boolean> onToggle,
			Consumer<String> onReload, Consumer<String> onView, Consumer<String> onRemove) {
		List<DataRuleFileItem> safeRules = rules == null
				? List.of()
				: rules.stream().filter(rule -> rule != null && rule.id() != null && !rule.id().isBlank()).toList();
		BiConsumer<String, Boolean> safeOnToggle = onToggle == null ? (id, enabled) -> {
		} : onToggle;
		Consumer<String> safeOnReload = onReload == null ? id -> {
		} : onReload;
		Consumer<String> safeOnView = onView == null ? id -> {
		} : onView;
		Consumer<String> safeOnRemove = onRemove == null ? id -> {
		} : onRemove;

		ruleFilesList.getChildren().clear();
		for (DataRuleFileItem rule : safeRules) {
			ruleFilesList.getChildren()
					.add(DataRuleFileRowFactory.createRow(rule, safeOnToggle, safeOnReload, safeOnView, safeOnRemove));
		}

		boolean hasRules = !safeRules.isEmpty();
		ruleFilesScrollPane.setVisible(hasRules);
		ruleFilesScrollPane.setManaged(hasRules);
		ruleFilesEmptyStateWidget.setVisible(!hasRules);
		ruleFilesEmptyStateWidget.setManaged(!hasRules);
	}

	/**
	 * Registers a callback for files dropped on the graph area.
	 *
	 * @param onGraphFilesDropped
	 *            callback receiving dropped files
	 */
	public void setOnGraphFilesDropped(Consumer<List<File>> onGraphFilesDropped) {
		this.onGraphFilesDropped = onGraphFilesDropped;
		if (onGraphFilesDropped == null) {
			setGraphDropActive(false);
		}
	}

	/**
	 * Registers a callback for files dropped on the rule files area.
	 *
	 * @param onRuleFilesDropped
	 *            callback receiving dropped files
	 */
	public void setOnRuleFilesDropped(Consumer<List<File>> onRuleFilesDropped) {
		this.onRuleFilesDropped = onRuleFilesDropped;
		if (onRuleFilesDropped == null) {
			setRuleFilesDropActive(false);
		}
	}

	/**
	 * Marks a toolbar button as dangerous.
	 *
	 * @param buttonIcon
	 *            toolbar button icon key
	 */
	public void markToolbarButtonDanger(ButtonIcon buttonIcon) {
		markDanger(toolbarWidget.getButton(buttonIcon));
	}

	/**
	 * Marks a rule-files toolbar button as dangerous.
	 *
	 * @param buttonIcon
	 *            toolbar button icon key
	 */
	public void markRuleFilesToolbarButtonDanger(ButtonIcon buttonIcon) {
		markDanger(ruleFilesToolbar.getButton(buttonIcon));
	}

	private static void markDanger(Button button) {
		if (button != null && !button.getStyleClass().contains(Styles.DANGER)) {
			button.getStyleClass().add(Styles.DANGER);
		}
	}

	/**
	 * Returns the graph visualization widget.
	 *
	 * @return graph widget
	 */
	public GraphDisplayWidget getGraphWidget() {
		return graphWidget;
	}

	/**
	 * Returns built-in reasoning toggles.
	 *
	 * @return list of rule toggles
	 */
	public List<ToggleSwitch> getBuiltInRuleToggles() {
		return List.of(rdfsToggle, owlRlToggle, owlRlLiteToggle, owlRlExtToggle);
	}

	/**
	 * Sets the view action for one built-in profile source.
	 *
	 * @param profile
	 *            target built-in profile
	 * @param action
	 *            action to run on click
	 */
	public void setBuiltInRuleViewAction(ReasoningProfile profile, Runnable action) {
		Button targetButton = switch (profile) {
			case RDFS -> rdfsViewButton;
			case OWL_RL -> owlRlViewButton;
			case OWL_RL_LITE -> owlRlLiteViewButton;
			case OWL_RL_EXT -> owlRlExtViewButton;
		};
		if (action == null) {
			targetButton.setOnAction(null);
			targetButton.setDisable(true);
			return;
		}
		targetButton.setOnAction(event -> action.run());
		targetButton.setDisable(false);
	}

	/**
	 * Resets all built-in reasoning toggles to OFF.
	 */
	public void resetBuiltInRuleToggles() {
		for (ToggleSwitch toggle : getBuiltInRuleToggles()) {
			toggle.setSelected(false);
		}
	}

	/**
	 * Updates graph workspace counters and details in the footer.
	 *
	 * @param status
	 *            current workspace status
	 */
	public void updateStatus(DataWorkspaceStatus status) {
		DataWorkspaceStatus safeStatus = status == null ? DataWorkspaceStatus.empty() : status;

		DataStatusTooltipSupport.updateStatusMetric(tripleCountLabel, TOOLTIP_TITLE_TRIPLES, safeStatus.tripleCount(),
				DataStatusTooltipSupport.buildTriplesTooltipLines(safeStatus));
		DataStatusTooltipSupport.updateStatusMetric(sourceCountLabel, TOOLTIP_TITLE_SOURCES, safeStatus.sourceCount(),
				DataStatusTooltipSupport.buildSourcesTooltipLines(safeStatus));
		DataStatusTooltipSupport.updateStatusMetric(namedGraphCountLabel, TOOLTIP_TITLE_NAMED_GRAPHS,
				safeStatus.namedGraphCount(), DataStatusTooltipSupport.buildNamedGraphTooltipLines(safeStatus));
		DataStatusTooltipSupport.updateStatusMetric(inferredTripleCountLabel, TOOLTIP_TITLE_INFERRED,
				safeStatus.inferredTripleCount(), DataStatusTooltipSupport.buildReasoningTooltipLines(safeStatus));
	}

}
