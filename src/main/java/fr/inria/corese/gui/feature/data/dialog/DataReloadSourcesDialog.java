package fr.inria.corese.gui.feature.data.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.dialog.DialogLayout.ImpactTone;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import fr.inria.corese.gui.core.service.ModalService;
import fr.inria.corese.gui.core.theme.CssUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/**
 * Modal dialog that lets users select tracked sources to reload.
 */
public final class DataReloadSourcesDialog {

	private static final String STYLESHEET = "/css/features/data-reload-sources-dialog.css";
	private static final String STYLE_CLASS_DIALOG = "data-reload-dialog";
	private static final String STYLE_CLASS_CONTENT = "data-reload-dialog-content";
	private static final String STYLE_CLASS_SOURCE_SCROLL = "data-reload-dialog-source-scroll";
	private static final String STYLE_CLASS_SOURCE_LIST = "data-reload-dialog-source-list";
	private static final String STYLE_CLASS_SOURCE_ITEM = "data-reload-dialog-source-item";
	private static final String STYLE_CLASS_INLINE_ERROR = "data-reload-dialog-inline-error";

	private static final String DIALOG_TITLE = "Reload Sources";
	private static final String WARNING_TITLE = "Warning";
	private static final List<String> WARNING_ITEMS = List.of("SPARQL UPDATE modifications are discarded.",
			"Reasoning profiles are reset to OFF.", "Unchecked sources are excluded from future reloads.");
	private static final String EMPTY_SELECTION_MESSAGE = "Select at least one source to reload.";
	private static final int FILE_PATH_MAX_CHARS = 56;
	private static final int URI_MAX_CHARS = 72;
	private static final int SOURCE_VIEWPORT_HEIGHT = 240;
	private static final int SOURCE_MIN_VIEWPORT_HEIGHT = 120;
	private static final String VALIDATION_ANIMATION_KEY = "reloadSourcesValidationAnimation";
	private static final String BUTTON_ANIMATION_KEY = "reloadSourcesButtonAnimation";

	private DataReloadSourcesDialog() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Shows the source selection dialog for graph reload.
	 *
	 * @param trackedSources
	 *            currently tracked data sources
	 * @param onReloadRequested
	 *            callback invoked with selected sources when user confirms
	 */
	public static void show(List<DataSource> trackedSources, Consumer<List<DataSource>> onReloadRequested) {
		Objects.requireNonNull(onReloadRequested, "onReloadRequested must not be null");
		List<DataSource> safeSources = trackedSources == null ? List.of() : List.copyOf(trackedSources);

		VBox sourceList = new VBox(8);
		sourceList.getStyleClass().add(STYLE_CLASS_SOURCE_LIST);
		sourceList.setFillWidth(true);

		Map<CheckBox, DataSource> sourceSelections = new LinkedHashMap<>();
		for (DataSource source : safeSources) {
			CheckBox checkBox = createSourceCheckBox(source, sourceList);
			sourceSelections.put(checkBox, source);
			sourceList.getChildren().add(checkBox);
		}

		ScrollPane sourceScroll = new ScrollPane(sourceList);
		sourceScroll.getStyleClass().add(STYLE_CLASS_SOURCE_SCROLL);
		sourceScroll.setFitToWidth(true);
		sourceScroll.setPrefViewportHeight(SOURCE_VIEWPORT_HEIGHT);
		sourceScroll.setMinViewportHeight(SOURCE_MIN_VIEWPORT_HEIGHT);
		sourceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		sourceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		sourceScroll.setFitToHeight(false);

		VBox warningBox = DialogLayout.createImpactBlock(WARNING_TITLE, WARNING_ITEMS, ImpactTone.WARNING);

		VBox content = new VBox(10, warningBox, sourceScroll);
		content.getStyleClass().add(STYLE_CLASS_CONTENT);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button reloadButton = new Button("Reload");
		reloadButton.getStyleClass().add(Styles.DANGER);
		Label selectionErrorLabel = new Label(EMPTY_SELECTION_MESSAGE);
		selectionErrorLabel.getStyleClass().add(STYLE_CLASS_INLINE_ERROR);
		selectionErrorLabel.setWrapText(false);
		DialogSelectionStateSupport.prepareCollapsedValidationLabel(selectionErrorLabel);

		Runnable updateSelectionState = () -> {
			boolean anySelected = sourceSelections.keySet().stream().anyMatch(CheckBox::isSelected);
			DialogSelectionStateSupport.updateActionState(anySelected, reloadButton, selectionErrorLabel,
					EMPTY_SELECTION_MESSAGE, VALIDATION_ANIMATION_KEY, BUTTON_ANIMATION_KEY);
		};
		sourceSelections.keySet().forEach(checkBox -> checkBox.selectedProperty()
				.addListener((obs, oldValue, newValue) -> updateSelectionState.run()));
		updateSelectionState.run();

		reloadButton.setOnAction(event -> {
			List<DataSource> selectedSources = sourceSelections.entrySet().stream()
					.filter(entry -> entry.getKey().isSelected()).map(Map.Entry::getValue).toList();
			if (selectedSources.isEmpty()) {
				DialogSelectionStateSupport.showValidationMessage(selectionErrorLabel, EMPTY_SELECTION_MESSAGE,
						VALIDATION_ANIMATION_KEY);
				return;
			}
			ModalService.getInstance().hide();
			onReloadRequested.accept(selectedSources);
		});

		DialogLayout dialog = new DialogLayout(DIALOG_TITLE, content, selectionErrorLabel, cancelButton, reloadButton);
		dialog.getStyleClass().add(STYLE_CLASS_DIALOG);
		CssUtils.applyViewStyles(dialog, STYLESHEET);
		ModalService.getInstance().show(dialog);
	}

	private static String formatDisplayLabel(DataSource source) {
		if (source == null) {
			return "Unknown source";
		}
		int maxChars = source.type() == SourceType.FILE ? FILE_PATH_MAX_CHARS : URI_MAX_CHARS;
		return abbreviateKeepingTail(source.location(), maxChars);
	}

	private static CheckBox createSourceCheckBox(DataSource source, VBox sourceList) {
		CheckBox checkBox = new CheckBox(formatDisplayLabel(source));
		checkBox.setSelected(true);
		checkBox.setWrapText(false);
		checkBox.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
		checkBox.setMaxWidth(Double.MAX_VALUE);
		checkBox.prefWidthProperty().bind(sourceList.widthProperty());
		checkBox.getStyleClass().add(STYLE_CLASS_SOURCE_ITEM);
		checkBox.setTooltip(new Tooltip(formatTooltipText(source)));
		return checkBox;
	}

	private static String formatTooltipText(DataSource source) {
		if (source == null) {
			return "Unknown source";
		}
		if (source.type() == SourceType.FILE) {
			return "File: " + source.location();
		}
		return "URI: " + source.location();
	}

	private static String abbreviateKeepingTail(String value, int maxChars) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String normalized = value.trim();
		if (normalized.length() <= maxChars) {
			return normalized;
		}
		if (maxChars <= 3) {
			return normalized.substring(normalized.length() - maxChars);
		}
		return "..." + normalized.substring(normalized.length() - (maxChars - 3));
	}
}
