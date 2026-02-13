package fr.inria.corese.gui.feature.data.dialog;

import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import fr.inria.corese.gui.core.service.ReasoningService.RuleFileState;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/**
 * Shared label/tooltip formatting for reload dialogs.
 */
final class DataReloadDialogLabelSupport {

	private static final double CHECKBOX_INDICATOR_AND_GAP_WIDTH = 56d;
	private static final double MIN_TEXT_WIDTH = 120d;

	private DataReloadDialogLabelSupport() {
		throw new AssertionError("Utility class");
	}

	record DisplayText(String primaryText, String secondaryText, String tooltipText) {
	}

	static DisplayText describeDataSource(DataSource source) {
		if (source == null) {
			return new DisplayText("Unknown source", null, "Unknown source");
		}
		if (source.type() == SourceType.FILE) {
			return describeFilePath(source.location(), "File");
		}
		String uri = nonBlank(source.location(), "Unknown URI");
		return new DisplayText(uri, null, "URI: " + uri);
	}

	static DisplayText describeRuleFile(RuleFileState rule) {
		if (rule == null) {
			return new DisplayText("Unknown rule file", null, "Unknown rule file");
		}
		String sourcePath = rule.sourcePath();
		if (sourcePath != null && !sourcePath.isBlank()) {
			return describeFilePath(sourcePath, "File");
		}
		String label = rule.label();
		if (label == null || label.isBlank()) {
			return new DisplayText("(unnamed rule file)", null, "Unknown rule file");
		}
		return new DisplayText(label, null, "Rule file: " + label);
	}

	static void applyCheckBoxDisplay(CheckBox checkBox, VBox listContainer, DisplayText displayText) {
		if (checkBox == null || listContainer == null || displayText == null) {
			return;
		}

		checkBox.setText("");
		checkBox.setWrapText(false);

		Label primaryLabel = createPrimaryLabel(displayText.primaryText());
		Label secondaryLabel = createSecondaryLabel(displayText.secondaryText());

		VBox textContainer = new VBox(1);
		textContainer.getStyleClass().add("data-reload-dialog-item-text");
		textContainer.getChildren().add(primaryLabel);
		if (secondaryLabel != null) {
			textContainer.getChildren().add(secondaryLabel);
		}

		DoubleBinding textMaxWidth = Bindings.max(MIN_TEXT_WIDTH,
				listContainer.widthProperty().subtract(CHECKBOX_INDICATOR_AND_GAP_WIDTH));
		primaryLabel.maxWidthProperty().bind(textMaxWidth);
		if (secondaryLabel != null) {
			secondaryLabel.maxWidthProperty().bind(textMaxWidth);
		}
		textContainer.prefWidthProperty().bind(textMaxWidth);
		textContainer.maxWidthProperty().bind(textMaxWidth);

		checkBox.setGraphic(textContainer);
		checkBox.setGraphicTextGap(14);
		checkBox.setTooltip(new Tooltip(nonBlank(displayText.tooltipText(), displayText.primaryText())));
		checkBox.setAccessibleText(nonBlank(displayText.primaryText(), "Reload item"));
	}

	private static DisplayText describeFilePath(String path, String tooltipPrefix) {
		String normalizedPath = nonBlank(path, "Unknown file");
		PathParts pathParts = splitPath(normalizedPath);
		String tooltipText = nonBlank(tooltipPrefix, "File") + ": " + normalizedPath;
		return new DisplayText(pathParts.fileName(), pathParts.parentPath(), tooltipText);
	}

	private static PathParts splitPath(String fullPath) {
		String normalized = nonBlank(fullPath, "");
		int slashIndex = normalized.lastIndexOf('/');
		int backslashIndex = normalized.lastIndexOf('\\');
		int splitIndex = Math.max(slashIndex, backslashIndex);
		if (splitIndex < 0 || splitIndex >= normalized.length() - 1) {
			return new PathParts(nonBlank(normalized, "Unknown file"), null);
		}
		String parentPath = normalized.substring(0, splitIndex + 1);
		String fileName = normalized.substring(splitIndex + 1);
		if (fileName.isBlank()) {
			return new PathParts(nonBlank(normalized, "Unknown file"), null);
		}
		return new PathParts(fileName, parentPath);
	}

	private static Label createPrimaryLabel(String value) {
		Label label = new Label(nonBlank(value, "Unknown item"));
		label.getStyleClass().add("data-reload-dialog-item-primary");
		label.setWrapText(false);
		label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
		return label;
	}

	private static Label createSecondaryLabel(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		Label label = new Label(value);
		label.getStyleClass().add("data-reload-dialog-item-secondary");
		label.setWrapText(false);
		label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
		return label;
	}

	private static String nonBlank(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}

	private record PathParts(String fileName, String parentPath) {
	}
}
