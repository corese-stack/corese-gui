package fr.inria.corese.gui.feature.data.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.DataSource;
import fr.inria.corese.gui.core.service.DataSourceRegistryService.SourceType;
import fr.inria.corese.gui.core.service.ModalService;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Modal dialog that lets users select tracked sources to reload.
 */
public final class DataReloadSourcesDialog {

	private static final String DIALOG_TITLE = "Reload Sources";
	private static final String WARNING_TEXT = "Reload rebuilds the graph from selected sources.\n"
			+ "All modifications made via SPARQL UPDATE queries will be lost.\n"
			+ "Reasoning toggles will be reset to OFF.";
	private static final String SOURCE_SELECTION_LABEL = "Select sources to reload:";

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
		sourceList.setFillWidth(true);

		Map<CheckBox, DataSource> sourceSelections = new LinkedHashMap<>();
		for (DataSource source : safeSources) {
			CheckBox checkBox = new CheckBox(formatSourceLabel(source));
			checkBox.setSelected(true);
			checkBox.setWrapText(true);
			sourceSelections.put(checkBox, source);
			sourceList.getChildren().add(checkBox);
		}

		ScrollPane sourceScroll = new ScrollPane(sourceList);
		sourceScroll.setFitToWidth(true);
		sourceScroll.setPrefViewportHeight(180);
		sourceScroll.setMinViewportHeight(120);

		Label warningLabel = new Label(WARNING_TEXT);
		warningLabel.setWrapText(true);

		Label sourceLabel = new Label(SOURCE_SELECTION_LABEL);
		sourceLabel.getStyleClass().add(Styles.TEXT_BOLD);

		VBox content = new VBox(10, warningLabel, sourceLabel, sourceScroll);
		content.setPadding(new Insets(0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction(event -> ModalService.getInstance().hide());

		Button reloadButton = new Button("Reload");
		reloadButton.getStyleClass().add(Styles.ACCENT);
		reloadButton.setOnAction(event -> {
			List<DataSource> selectedSources = sourceSelections.entrySet().stream()
					.filter(entry -> entry.getKey().isSelected()).map(Map.Entry::getValue).toList();
			ModalService.getInstance().hide();
			onReloadRequested.accept(selectedSources);
		});

		ModalService.getInstance().show(new DialogLayout(DIALOG_TITLE, content, cancelButton, reloadButton));
	}

	private static String formatSourceLabel(DataSource source) {
		if (source == null) {
			return "Unknown source";
		}
		if (source.type() == SourceType.FILE) {
			String fileName = new File(source.location()).getName();
			return "File: " + fileName + "  (" + source.location() + ")";
		}
		return "URI: " + source.location();
	}
}
