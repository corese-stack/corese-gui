package fr.inria.corese.gui.feature.validation;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.core.io.FileTypeSupport;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * View for the Validation screen.
 *
 * <p>
 * Displays a tabbed interface where each tab contains a code editor for SHACL
 * shapes and a results pane for validation reports.
 */
public class ValidationView extends AbstractView {

	// ==============================================================================================
	// Constants
	// ==============================================================================================

	@SuppressWarnings("java:S1075") // Hardcoded URI - not relevant for internal CSS resources
	private static final String STYLESHEET_PATH = "/css/features/validation-view.css";
	@SuppressWarnings("java:S1075")
	private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";

	private final StackPane workspaceCard = new StackPane();
	private ValidationController controller;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	/** Creates the ValidationView. */
	public ValidationView() {
		super(new BorderPane(), STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		initializeLayout();
	}

	private void initializeLayout() {
		BorderPane root = (BorderPane) getRoot();
		root.getStyleClass().addAll("validation-page-root", "app-workspace-root");
		workspaceCard.getStyleClass().addAll("app-workspace-card", "app-card", "app-card-default");
		workspaceCard.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		RoundedClipSupport.applyRoundedClip(workspaceCard, 8);
		root.setCenter(workspaceCard);
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Sets the main content (typically the TabEditor).
	 *
	 * @param node
	 *            The content node.
	 */
	public void setMainContent(Node node) {
		if (node instanceof Region region) {
			region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		}
		workspaceCard.getChildren().setAll(node);
	}

	public void setController(ValidationController controller) {
		this.controller = controller;
	}

	public ValidationController getController() {
		return controller;
	}

	/**
	 * Returns the list of buttons to be displayed in the result view toolbar.
	 *
	 * @return A list of ButtonConfig.
	 */
	public List<ButtonConfig> getResultToolbarButtons() {
		return List.of(ButtonFactory.copy(), ButtonFactory.export());
	}

	public String getRunValidationLabel() {
		return "Run Validation";
	}

	/**
	 * Creates the empty state view for the validation screen.
	 *
	 * @param onNewAction
	 *            Action for "New Shapes File".
	 * @param onLoadAction
	 *            Action for "Load Shapes File".
	 * @param onTemplateAction
	 *            Action for "Template" (optional).
	 * @return The configured EmptyStateWidget.
	 */
	public Node createEmptyState(Runnable onNewAction, Runnable onLoadAction, Runnable onTemplateAction) {
		java.util.List<Node> actions = new java.util.ArrayList<>();
		actions.add(EmptyStateWidget.createAction("New Shapes File", ButtonIcon.EMPTY_ACTION_NEW, onNewAction));
		actions.add(EmptyStateWidget.createAction("Load Shapes File", ButtonIcon.EMPTY_ACTION_OPEN, onLoadAction));
		if (onTemplateAction != null) {
			actions.add(EmptyStateWidget.createAction("Template", ButtonIcon.EMPTY_ACTION_TEMPLATE, onTemplateAction));
		}

		return new EmptyStateWidget(ButtonIcon.EMPTY_VALIDATION, "No shapes files open",
				FileTypeSupport.withAcceptedExtensions("Create a new SHACL shapes file, load one, or drop it here.",
						FileTypeSupport.rdfExtensions()),
				actions.toArray(Node[]::new));
	}
}
