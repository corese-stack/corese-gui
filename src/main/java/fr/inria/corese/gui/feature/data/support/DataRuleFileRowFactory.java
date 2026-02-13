package fr.inria.corese.gui.feature.data.support;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.feature.data.model.DataRuleFileItem;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Builds rule-file rows rendered in the Data reasoning panel.
 */
public final class DataRuleFileRowFactory {

	private DataRuleFileRowFactory() {
		throw new AssertionError("Utility class");
	}

	public static HBox createRow(DataRuleFileItem rule, BiConsumer<String, Boolean> onToggle, Consumer<String> onReload,
			Consumer<String> onView, Consumer<String> onRemove) {
		Label nameLabel = new Label(nonBlank(rule.label(), "Unnamed rule file"));
		nameLabel.getStyleClass().add("data-custom-rule-name");

		String sourcePath = nonBlank(rule.sourcePath(), "(unknown source)");
		nameLabel.setTooltip(new Tooltip(sourcePath));

		ToggleSwitch toggleSwitch = new ToggleSwitch();
		toggleSwitch.getStyleClass().add("data-rule-toggle-switch");
		toggleSwitch.setSelected(rule.enabled());
		toggleSwitch.setFocusTraversable(false);
		toggleSwitch.selectedProperty().addListener(
				(observable, previous, selected) -> onToggle.accept(rule.id(), Boolean.TRUE.equals(selected)));

		MenuItem reloadItem = createRuleMenuItem("Reload", ButtonIcon.RELOAD, () -> onReload.accept(rule.id()));
		MenuItem viewItem = createRuleMenuItem("View", ButtonIcon.VIEW, () -> onView.accept(rule.id()));
		MenuItem removeItem = createRuleMenuItem("Remove", ButtonIcon.CLEAR, () -> onRemove.accept(rule.id()));

		ContextMenu actionMenu = new ContextMenu(reloadItem, viewItem, removeItem);
		actionMenu.getStyleClass().add("data-custom-rule-menu");

		Button actionButton = new Button();
		actionButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, "data-custom-rule-menu-button");
		actionButton.setGraphic(createMenuIcon(ButtonIcon.MORE_ACTIONS, "data-custom-rule-menu-button-icon", 16));
		actionButton.setTooltip(new Tooltip("Rule actions"));
		actionButton.setFocusTraversable(false);
		actionButton.setOnAction(event -> actionMenu.show(actionButton, Side.BOTTOM, 0, 0));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox controls = new HBox(14, actionButton, toggleSwitch);
		controls.getStyleClass().add("data-custom-rule-controls");
		controls.setAlignment(Pos.CENTER_RIGHT);

		HBox row = new HBox(8, nameLabel, spacer, controls);
		row.getStyleClass().addAll("data-custom-rule-item", "app-card-row");
		row.setAlignment(Pos.CENTER_LEFT);
		row.setMaxWidth(Double.MAX_VALUE);
		Tooltip.install(row, new Tooltip(sourcePath));
		return row;
	}

	private static MenuItem createRuleMenuItem(String label, ButtonIcon icon, Runnable action) {
		MenuItem item = new MenuItem(label);
		item.setGraphic(createMenuIcon(icon, "data-custom-rule-menu-item-icon", 14));
		if (action != null) {
			item.setOnAction(event -> action.run());
		}
		return item;
	}

	private static FontIcon createMenuIcon(ButtonIcon icon, String styleClass, int iconSize) {
		FontIcon fontIcon = new FontIcon(icon.getIkon());
		fontIcon.setIconSize(iconSize);
		fontIcon.getStyleClass().add(styleClass);
		return fontIcon;
	}

	private static String nonBlank(String value, String fallback) {
		return (value == null || value.isBlank()) ? fallback : value;
	}
}
