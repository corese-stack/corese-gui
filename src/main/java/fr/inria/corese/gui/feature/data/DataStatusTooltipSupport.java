package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.core.service.DataWorkspaceStatus;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Shared formatter/helper for Data status bar labels and tooltips.
 */
final class DataStatusTooltipSupport {

	private static final int TOOLTIP_MAX_WIDTH = 420;
	private static final int TOOLTIP_SHOW_DELAY_MS = 150;
	private static final int TOOLTIP_HIDE_DELAY_MS = 120;
	private static final int TOOLTIP_PREVIEW_LIMIT = 8;
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.getDefault());

	private DataStatusTooltipSupport() {
		throw new AssertionError("Utility class");
	}

	static void updateStatusMetric(Label label, String title, int value, List<String> tooltipLines) {
		label.setText(title + ": " + formatCount(value));
		applyTooltip(label, tooltipLines, title);
	}

	static List<String> buildTriplesTooltipLines(DataWorkspaceStatus status) {
		int namedGraphTriples = Math.max(0, status.tripleCount() - status.defaultGraphTripleCount());
		return List.of("Explicit triples: " + formatCount(status.explicitTripleCount()),
				"Default graph triples: " + formatCount(status.defaultGraphTripleCount()),
				"Triples in named graphs: " + formatCount(namedGraphTriples));
	}

	static List<String> buildSourcesTooltipLines(DataWorkspaceStatus status) {
		return List.of("File sources: " + formatCount(status.fileSourceCount()),
				"URI sources: " + formatCount(status.uriSourceCount()));
	}

	static List<String> buildNamedGraphTooltipLines(DataWorkspaceStatus status) {
		if (status.namedGraphStats().isEmpty()) {
			return List.of("No named graph currently contains triples.");
		}

		List<String> lines = new ArrayList<>();
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

	static List<String> buildReasoningTooltipLines(DataWorkspaceStatus status) {
		List<String> lines = new ArrayList<>();
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
