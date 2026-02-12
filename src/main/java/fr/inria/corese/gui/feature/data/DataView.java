package fr.inria.corese.gui.feature.data;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.service.DataWorkspaceStatus;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.view.AbstractView;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
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
import javafx.util.Duration;

/**
 * Main view for the Data page.
 *
 * <p>
 * Layout:
 * <ul>
 * <li>Left pane: reasoning controls (built-in profiles + custom rules
 * area)</li>
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
	private static final String TOOLTIP_TITLE_TRIPLES = "Triples";
	private static final String TOOLTIP_TITLE_SOURCES = "Sources";
	private static final String TOOLTIP_TITLE_NAMED_GRAPHS = "Named Graphs";
	private static final String TOOLTIP_TITLE_INFERRED = "Inferred";
	private static final int TOOLTIP_MAX_WIDTH = 420;
	private static final int TOOLTIP_SHOW_DELAY_MS = 150;
	private static final int TOOLTIP_HIDE_DELAY_MS = 120;
	private static final int TOOLTIP_PREVIEW_LIMIT = 8;
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.getDefault());

	private final GraphDisplayWidget graphWidget = new GraphDisplayWidget();
	private final ToolbarWidget toolbarWidget = new ToolbarWidget();
	private final StackPane graphContainer = new StackPane();
	private final Region graphDropOverlay = new Region();

	private final CheckBox rdfsToggle = new CheckBox("RDFS entailments");
	private final CheckBox owlRlToggle = new CheckBox("OWL RL entailments");
	private final CheckBox owlRlLiteToggle = new CheckBox("OWL RL Lite entailments");
	private final CheckBox owlRlExtToggle = new CheckBox("OWL RL Ext entailments");
	private final Button loadCustomRuleButton = new Button("Load .rul rule");

	private final Label tripleCountLabel = new Label();
	private final Label sourceCountLabel = new Label();
	private final Label namedGraphCountLabel = new Label();
	private final Label inferredTripleCountLabel = new Label();
	private EmptyStateWidget graphEmptyStateWidget;
	private Consumer<List<File>> onGraphFilesDropped;

	/**
	 * Creates the Data page view.
	 */
	public DataView() {
		super(new BorderPane(), STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		initializeLayout();
		updateStatus(DataWorkspaceStatus.empty());
	}

	private void initializeLayout() {
		BorderPane root = (BorderPane) getRoot();
		root.getStyleClass().add("data-page-root");

		SplitPane splitPane = new SplitPane();
		splitPane.getStyleClass().add("data-page-split");

		VBox reasoningPane = createReasoningPane();
		BorderPane graphPane = createGraphPane();
		SplitPane.setResizableWithParent(reasoningPane, false);
		splitPane.getItems().addAll(reasoningPane, graphPane);
		splitPane.setDividerPositions(0.27);

		root.setCenter(splitPane);
	}

	private VBox createReasoningPane() {
		VBox pane = new VBox(14);
		pane.getStyleClass().add("data-left-pane");

		Label titleLabel = new Label("Reasoning");
		titleLabel.getStyleClass().add("data-pane-title");

		Label subtitleLabel = new Label("Manage entailment profiles and custom rules.");
		subtitleLabel.getStyleClass().add("data-pane-subtitle");
		subtitleLabel.setWrapText(true);

		Label builtInTitle = new Label("Built-in Profiles");
		builtInTitle.getStyleClass().add("data-section-title");

		VBox builtInRules = new VBox(8, rdfsToggle, owlRlToggle, owlRlLiteToggle, owlRlExtToggle);
		builtInRules.getStyleClass().add("data-rule-list");
		for (CheckBox toggle : List.of(rdfsToggle, owlRlToggle, owlRlLiteToggle, owlRlExtToggle)) {
			toggle.getStyleClass().add("data-rule-toggle");
		}

		Label customTitle = new Label("Custom Rules (.rul)");
		customTitle.getStyleClass().add("data-section-title");

		Label customHint = new Label("Load custom rule files to apply domain-specific entailments.");
		customHint.getStyleClass().add("data-pane-subtitle");
		customHint.setWrapText(true);

		loadCustomRuleButton.getStyleClass().add(Styles.ACCENT);

		VBox builtInCard = new VBox(10, builtInTitle, builtInRules);
		builtInCard.getStyleClass().addAll("data-card", "floating-panel");

		VBox customCard = new VBox(10, customTitle, customHint, loadCustomRuleButton);
		customCard.getStyleClass().addAll("data-card", "floating-panel");

		Region spacer = new Region();
		VBox.setVgrow(spacer, Priority.ALWAYS);

		pane.getChildren().addAll(titleLabel, subtitleLabel, builtInCard, customCard, spacer);
		return pane;
	}

	private BorderPane createGraphPane() {
		BorderPane graphPane = new BorderPane();
		graphPane.getStyleClass().add("data-right-pane");

		initializeGraphContainer();

		HBox statusBar = new HBox(10, tripleCountLabel, sourceCountLabel, namedGraphCountLabel,
				inferredTripleCountLabel);
		statusBar.getStyleClass().add("data-status-bar");
		initializeStatusMetricLabels();

		HBox graphBody = new HBox(graphContainer, toolbarWidget);
		HBox.setHgrow(graphContainer, Priority.ALWAYS);

		VBox graphCard = new VBox(10, graphBody, statusBar);
		graphCard.getStyleClass().addAll("data-card", "data-graph-card", "floating-panel");
		VBox.setVgrow(graphBody, Priority.ALWAYS);

		graphPane.setCenter(graphCard);

		return graphPane;
	}

	private void initializeStatusMetricLabels() {
		for (Label label : List.of(tripleCountLabel, sourceCountLabel, namedGraphCountLabel, inferredTripleCountLabel)) {
			label.getStyleClass().add("data-status-label");
			label.setFocusTraversable(false);
		}
	}

	private void initializeGraphContainer() {
		graphContainer.getStyleClass().add("data-graph-container");
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
		if (onGraphFilesDropped == null) {
			return;
		}
		if (hasFilesInDragboard(event)) {
			event.acceptTransferModes(TransferMode.COPY);
			event.consume();
		}
	}

	private void handleGraphDropped(DragEvent event) {
		if (onGraphFilesDropped == null) {
			return;
		}
		boolean completed = false;
		if (hasFilesInDragboard(event)) {
			Dragboard dragboard = event.getDragboard();
			onGraphFilesDropped.accept(List.copyOf(dragboard.getFiles()));
			completed = true;
		}
		setGraphDropActive(false);
		event.setDropCompleted(completed);
		event.consume();
	}

	private void handleGraphDragEntered(DragEvent event) {
		if (onGraphFilesDropped == null) {
			return;
		}
		if (hasFilesInDragboard(event)) {
			setGraphDropActive(true);
			event.consume();
		}
	}

	private void handleGraphDragExited(DragEvent event) {
		if (onGraphFilesDropped == null) {
			return;
		}
		setGraphDropActive(false);
	}

	private boolean hasFilesInDragboard(DragEvent event) {
		Dragboard dragboard = event.getDragboard();
		return dragboard != null && dragboard.hasFiles();
	}

	private void setGraphDropActive(boolean active) {
		if (active) {
			if (!graphDropOverlay.getStyleClass().contains(STYLE_CLASS_GRAPH_DROP_OVERLAY_ACTIVE)) {
				graphDropOverlay.getStyleClass().add(STYLE_CLASS_GRAPH_DROP_OVERLAY_ACTIVE);
			}
			graphDropOverlay.setManaged(true);
			graphDropOverlay.setVisible(true);
			graphDropOverlay.toFront();
			return;
		}
		graphDropOverlay.getStyleClass().remove(STYLE_CLASS_GRAPH_DROP_OVERLAY_ACTIVE);
		graphDropOverlay.setManaged(false);
		graphDropOverlay.setVisible(false);
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
				"Load RDF from a file or URI, or drop RDF files here.",
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
	 * Marks a toolbar button as dangerous.
	 *
	 * @param buttonIcon
	 *            toolbar button icon key
	 */
	public void markToolbarButtonDanger(ButtonIcon buttonIcon) {
		var button = toolbarWidget.getButton(buttonIcon);
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
	public List<CheckBox> getBuiltInRuleToggles() {
		return List.of(rdfsToggle, owlRlToggle, owlRlLiteToggle, owlRlExtToggle);
	}

	/**
	 * Resets all built-in reasoning toggles to OFF.
	 */
	public void resetBuiltInRuleToggles() {
		for (CheckBox toggle : getBuiltInRuleToggles()) {
			toggle.setSelected(false);
		}
	}

	/**
	 * Returns the custom rule load button.
	 *
	 * @return button instance
	 */
	public Button getLoadCustomRuleButton() {
		return loadCustomRuleButton;
	}

	/**
	 * Updates graph workspace counters and details in the footer.
	 *
	 * @param status
	 *            current workspace status
	 */
	public void updateStatus(DataWorkspaceStatus status) {
		DataWorkspaceStatus safeStatus = status == null ? DataWorkspaceStatus.empty() : status;

		updateStatusMetric(tripleCountLabel, TOOLTIP_TITLE_TRIPLES, safeStatus.tripleCount(),
				buildTriplesTooltipLines(safeStatus));
		updateStatusMetric(sourceCountLabel, TOOLTIP_TITLE_SOURCES, safeStatus.sourceCount(),
				buildSourcesTooltipLines(safeStatus));
		updateStatusMetric(namedGraphCountLabel, TOOLTIP_TITLE_NAMED_GRAPHS, safeStatus.namedGraphCount(),
				buildNamedGraphTooltipLines(safeStatus));
		updateStatusMetric(inferredTripleCountLabel, TOOLTIP_TITLE_INFERRED, safeStatus.inferredTripleCount(),
				buildReasoningTooltipLines(safeStatus));
	}

	private static void updateStatusMetric(Label label, String title, int value, List<String> tooltipLines) {
		label.setText(title + ": " + formatCount(value));
		applyTooltip(label, tooltipLines, title);
	}

	private static List<String> buildTriplesTooltipLines(DataWorkspaceStatus status) {
		return """
				Total triples: %s
				Explicit triples: %s
				Inferred triples: %s
				Default graph triples: %s
				""".formatted(formatCount(status.tripleCount()), formatCount(status.explicitTripleCount()),
				formatCount(status.inferredTripleCount()), formatCount(status.defaultGraphTripleCount())).lines().toList();
	}

	private static List<String> buildSourcesTooltipLines(DataWorkspaceStatus status) {
		return """
				Tracked sources: %s
				File sources: %s
				URI sources: %s
				""".formatted(formatCount(status.sourceCount()), formatCount(status.fileSourceCount()),
				formatCount(status.uriSourceCount())).lines().toList();
	}

	private static List<String> buildNamedGraphTooltipLines(DataWorkspaceStatus status) {
		if (status.namedGraphStats().isEmpty()) {
			return List.of("No named graph currently contains triples.");
		}

		List<String> lines = new ArrayList<>();
		lines.add("Named graphs with triples: " + formatCount(status.namedGraphCount()));
		int displayed = Math.min(TOOLTIP_PREVIEW_LIMIT, status.namedGraphStats().size());
		for (int index = 0; index < displayed; index++) {
			DataWorkspaceStatus.NamedGraphStat stat = status.namedGraphStats().get(index);
			lines.add(shortenGraphName(stat.graphName()) + ": " + formatCount(stat.tripleCount()) + " triples");
		}
		if (status.namedGraphStats().size() > displayed) {
			lines.add("... and " + formatCount(status.namedGraphStats().size() - displayed) + " more named graphs.");
		}
		return lines;
	}

	private static List<String> buildReasoningTooltipLines(DataWorkspaceStatus status) {
		List<String> lines = new ArrayList<>();
		lines.add("Inferred triples: " + formatCount(status.inferredTripleCount()));
		for (DataWorkspaceStatus.ReasoningStat stat : status.reasoningStats()) {
			lines.add(stat.profileLabel() + ": " + formatCount(stat.tripleCount()) + " triples");
		}
		return lines;
	}

	private static String shortenGraphName(String graphName) {
		if (graphName == null || graphName.isBlank()) {
			return "(unnamed)";
		}
		if (graphName.length() <= 56) {
			return graphName;
		}
		return graphName.substring(0, 53) + "...";
	}

	private static String formatCount(int value) {
		return INTEGER_FORMAT.format(Math.max(0, value));
	}

	private static void applyTooltip(Label label, List<String> lines, String title) {
		List<String> safeLines = lines == null
				? List.of()
				: lines.stream().filter(line -> line != null && !line.isBlank()).toList();
		if (safeLines.isEmpty()) {
			label.setTooltip(null);
			return;
		}
		String safeTitle = (title == null || title.isBlank()) ? "Details" : title;
		String tooltipText = safeTitle + "\n\n" + String.join("\n", safeLines);

		Tooltip tooltip = label.getTooltip();
		if (tooltip == null) {
			tooltip = new Tooltip();
			label.setTooltip(tooltip);
		}
		if (!tooltipText.equals(tooltip.getText())) {
			tooltip.setText(tooltipText);
		}
		tooltip.setGraphic(null);
		tooltip.setWrapText(false);
		tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
		tooltip.setShowDelay(Duration.millis(TOOLTIP_SHOW_DELAY_MS));
		tooltip.setHideDelay(Duration.millis(TOOLTIP_HIDE_DELAY_MS));
		tooltip.setShowDuration(Duration.INDEFINITE);
	}
}
