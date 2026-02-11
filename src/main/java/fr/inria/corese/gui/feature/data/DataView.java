package fr.inria.corese.gui.feature.data;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.view.AbstractView;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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

	private final GraphDisplayWidget graphWidget = new GraphDisplayWidget();
	private final ToolbarWidget toolbarWidget = new ToolbarWidget();

	private final CheckBox rdfsToggle = new CheckBox("RDFS entailments");
	private final CheckBox owlRlToggle = new CheckBox("OWL RL entailments");
	private final CheckBox owlRlLiteToggle = new CheckBox("OWL RL Lite entailments");
	private final CheckBox owlRlExtToggle = new CheckBox("OWL RL Ext entailments");
	private final Button loadCustomRuleButton = new Button("Load .rul rule");

	private final Label tripleCountLabel = new Label();
	private final Label sourceCountLabel = new Label();

	/**
	 * Creates the Data page view.
	 */
	public DataView() {
		super(new BorderPane(), STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		initializeLayout();
		updateStatus(0, 0);
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

		StackPane graphContainer = new StackPane(graphWidget);
		graphContainer.getStyleClass().add("data-graph-container");

		HBox statusBar = new HBox(16, tripleCountLabel, sourceCountLabel);
		statusBar.getStyleClass().add("data-status-bar");
		tripleCountLabel.getStyleClass().add("data-status-label");
		sourceCountLabel.getStyleClass().add("data-status-label");

		HBox graphBody = new HBox(graphContainer, toolbarWidget);
		HBox.setHgrow(graphContainer, Priority.ALWAYS);

		VBox graphCard = new VBox(10, graphBody, statusBar);
		graphCard.getStyleClass().addAll("data-card", "data-graph-card", "floating-panel");
		VBox.setVgrow(graphBody, Priority.ALWAYS);

		graphPane.setCenter(graphCard);

		return graphPane;
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
	 * Updates graph/source counters in the footer.
	 *
	 * @param tripleCount
	 *            number of triples in graph
	 * @param sourceCount
	 *            number of tracked load sources
	 */
	public void updateStatus(int tripleCount, int sourceCount) {
		tripleCountLabel.setText("Triples: " + Math.max(0, tripleCount));
		sourceCountLabel.setText("Sources: " + Math.max(0, sourceCount));
	}
}
