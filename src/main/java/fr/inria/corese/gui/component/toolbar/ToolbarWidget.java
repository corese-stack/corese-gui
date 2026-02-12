package fr.inria.corese.gui.component.toolbar;

import fr.inria.corese.gui.component.button.IconButtonWidget;
import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A reusable toolbar widget containing icon buttons.
 *
 * <p>
 * The widget supports vertical and horizontal layouts to keep one shared
 * component for all toolbar surfaces.
 */
public class ToolbarWidget extends StackPane {

	/** Orientation variants supported by the toolbar widget. */
	public enum Orientation {
		VERTICAL, HORIZONTAL
	}

	// ==============================================================================================
	// Constants
	// ==============================================================================================

	private static final String STYLESHEET = "/css/features/toolbar.css";
	private static final String STYLE_CLASS = "app-toolbar";
	private static final String STYLE_CLASS_VERTICAL = "app-toolbar-vertical";
	private static final String STYLE_CLASS_HORIZONTAL = "app-toolbar-horizontal";
	private static final String STYLE_CLASS_SEPARATOR = "app-toolbar-separator";

	// ==============================================================================================
	// Fields
	// ==============================================================================================

	/** Map for quick access to buttons by their icon type. */
	private final Map<ButtonIcon, IconButtonWidget> buttonMap = new LinkedHashMap<>();
	private final Orientation orientation;
	private final Pane buttonContainer;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	/** Creates a new generic toolbar widget. */
	public ToolbarWidget() {
		this(Orientation.VERTICAL);
	}

	/**
	 * Creates a new toolbar widget with the requested orientation.
	 *
	 * @param orientation
	 *            toolbar orientation
	 */
	public ToolbarWidget(Orientation orientation) {
		this.orientation = orientation == null ? Orientation.VERTICAL : orientation;
		this.buttonContainer = createContainer(this.orientation);
		initialize();
	}

	// ==============================================================================================
	// Initialization
	// ==============================================================================================

	private void initialize() {
		CssUtils.applyViewStyles(this, STYLESHEET);
		getStyleClass().add(STYLE_CLASS);
		if (orientation == Orientation.HORIZONTAL) {
			getStyleClass().add(STYLE_CLASS_HORIZONTAL);
			StackPane.setAlignment(buttonContainer, Pos.CENTER_LEFT);
		} else {
			getStyleClass().add(STYLE_CLASS_VERTICAL);
			StackPane.setAlignment(buttonContainer, Pos.TOP_CENTER);
		}
		getChildren().setAll(buttonContainer);
	}

	private Pane createContainer(Orientation layoutOrientation) {
		if (layoutOrientation == Orientation.HORIZONTAL) {
			HBox container = new HBox(6);
			container.setAlignment(Pos.CENTER_LEFT);
			return container;
		}
		VBox container = new VBox(10);
		container.setAlignment(Pos.TOP_CENTER);
		return container;
	}

	// ==============================================================================================
	// Public API
	// ==============================================================================================

	/**
	 * Sets the buttons to be displayed in the toolbar.
	 *
	 * @param configs
	 *            The list of button configurations.
	 */
	public void setButtons(List<ButtonConfig> configs) {
		buttonContainer.getChildren().clear();
		buttonMap.clear();

		if (configs == null) {
			return;
		}

		for (ButtonConfig config : configs) {
			if (config.getIcon() == null) {
				continue;
			}

			IconButtonWidget button = new IconButtonWidget(config);
			buttonMap.put(config.getIcon(), button);
			buttonContainer.getChildren().add(button);
		}
	}

	/**
	 * Sets the disabled state of a specific button.
	 *
	 * @param type
	 *            The button icon type.
	 * @param disabled
	 *            true to disable, false to enable.
	 */
	public void setButtonDisabled(ButtonIcon type, boolean disabled) {
		IconButtonWidget button = buttonMap.get(type);
		if (button != null) {
			button.setDisable(disabled);
		}
	}

	/**
	 * Retrieves a button instance directly. Prefer using setButtonDisabled for
	 * simple state changes.
	 *
	 * @param type
	 *            The button icon type.
	 * @return The button widget, or null if not found.
	 */
	public IconButtonWidget getButton(ButtonIcon type) {
		return buttonMap.get(type);
	}

	/**
	 * Inserts a visual separator immediately after the given button icon, if
	 * present.
	 *
	 * @param type
	 *            The button icon after which the separator should be inserted.
	 */
	public void insertSeparatorAfter(ButtonIcon type) {
		IconButtonWidget button = buttonMap.get(type);
		if (button == null) {
			return;
		}

		int buttonIndex = buttonContainer.getChildren().indexOf(button);
		if (buttonIndex < 0) {
			return;
		}

		int insertionIndex = buttonIndex + 1;
		if (insertionIndex < buttonContainer.getChildren().size()
				&& buttonContainer.getChildren().get(insertionIndex).getStyleClass().contains(STYLE_CLASS_SEPARATOR)) {
			return;
		}

		Region separator = new Region();
		separator.getStyleClass().add(STYLE_CLASS_SEPARATOR);
		buttonContainer.getChildren().add(insertionIndex, separator);
	}
}
