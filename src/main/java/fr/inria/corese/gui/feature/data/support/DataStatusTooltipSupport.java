package fr.inria.corese.gui.feature.data.support;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderMode;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderStatus;
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
public final class DataStatusTooltipSupport {

	private static final int TOOLTIP_MAX_WIDTH = 420;
	private static final int TOOLTIP_SHOW_DELAY_MS = 150;
	private static final int TOOLTIP_HIDE_DELAY_MS = 120;
	private static final int TOOLTIP_PREVIEW_LIMIT = 5;
	private static final int TOOLTIP_MAX_LINES = 6;
	private static final int TOOLTIP_MAX_LINE_LENGTH = 96;
	private static final int RENDER_TOOLTIP_MAX_LINES = 3;
	private static final int RENDER_DETAIL_PREVIEW_LIMIT = 2;
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.getDefault());

	private DataStatusTooltipSupport() {
		throw new AssertionError("Utility class");
	}

	public static void updateStatusMetric(Label label, String title, int value, List<String> tooltipLines) {
		label.setText(title + ": " + formatCount(value));
		applyTooltip(label, tooltipLines, title);
	}

	public static void updateStatusTextMetric(Label label, String title, String value, List<String> tooltipLines) {
		String safeTitle = (title == null || title.isBlank()) ? "Details" : title.trim();
		String safeValue = (value == null || value.isBlank()) ? "n/a" : value.trim();
		label.setText(safeTitle + ": " + safeValue);
		applyTooltip(label, tooltipLines, safeTitle);
	}

	public static List<String> buildTriplesTooltipLines(DataWorkspaceStatus status) {
		int namedGraphTriples = Math.max(0, status.tripleCount() - status.defaultGraphTripleCount());
		return List.of("Explicit: " + formatCount(status.explicitTripleCount()),
				"Default graph: " + formatCount(status.defaultGraphTripleCount()),
				"Named graphs: " + formatCount(namedGraphTriples));
	}

	public static List<String> buildSourcesTooltipLines(DataWorkspaceStatus status) {
		return List.of("Files: " + formatCount(status.fileSourceCount()),
				"URIs: " + formatCount(status.uriSourceCount()));
	}

	public static List<String> buildNamedGraphTooltipLines(DataWorkspaceStatus status) {
		if (status.namedGraphStats().isEmpty()) {
			return List.of("No named graph with triples.");
		}

		List<String> lines = new ArrayList<>();
		int displayed = Math.min(TOOLTIP_PREVIEW_LIMIT, status.namedGraphStats().size());
		for (int index = 0; index < displayed; index++) {
			DataWorkspaceStatus.NamedGraphStat stat = status.namedGraphStats().get(index);
			lines.add(shortenGraphName(stat.graphName()) + ": " + formatCount(stat.tripleCount()));
		}
		if (status.namedGraphStats().size() > displayed) {
			lines.add("+ " + formatCount(status.namedGraphStats().size() - displayed) + " more");
		}
		return lines;
	}

	public static List<String> buildReasoningTooltipLines(DataWorkspaceStatus status) {
		if (status.reasoningStats().isEmpty()) {
			return List.of("No inferred triples.");
		}
		List<String> lines = new ArrayList<>();
		for (DataWorkspaceStatus.ReasoningStat stat : status.reasoningStats()) {
			lines.add(stat.profileLabel() + ": " + formatCount(stat.tripleCount()));
		}
		return lines;
	}

	public static List<String> buildRenderTooltipLines(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		List<String> lines = new ArrayList<>();
		lines.add(simplifyRenderSummary(safeStatus.summary(), safeStatus.mode()));

		List<String> rawDetails = safeStatus.details().isEmpty()
				? List.of(defaultRenderDetail(safeStatus.mode()))
				: safeStatus.details();
		List<String> normalizedDetails = rawDetails.stream().map(DataStatusTooltipSupport::simplifyRenderDetail)
				.filter(line -> !line.isBlank()).distinct().toList();

		lines.addAll(selectRenderDetailPreview(safeStatus.mode(), normalizedDetails));
		return compactTooltipLines(lines, RENDER_TOOLTIP_MAX_LINES);
	}

	private static List<String> selectRenderDetailPreview(GraphRenderMode mode, List<String> normalizedDetails) {
		if (normalizedDetails.size() <= RENDER_DETAIL_PREVIEW_LIMIT) {
			return normalizedDetails;
		}
		if (mode == GraphRenderMode.PAUSED) {
			return buildPausedDetailPreview(normalizedDetails);
		}
		return List.of(normalizedDetails.getFirst(), "More optimizations active.");
	}

	private static List<String> buildPausedDetailPreview(List<String> normalizedDetails) {
		String actionLine = normalizedDetails.stream().filter(DataStatusTooltipSupport::isDisplayAnywayDetail)
				.findFirst().orElse("");
		String autoLimitLine = normalizedDetails.stream().filter(DataStatusTooltipSupport::isAutoLimitDetail)
				.findFirst().orElse("");
		if (actionLine.isBlank()) {
			if (!autoLimitLine.isBlank()) {
				return List.of(autoLimitLine, "More optimizations active.");
			}
			return List.of(normalizedDetails.getFirst(), "More optimizations active.");
		}
		if (!autoLimitLine.isBlank()) {
			return List.of(actionLine, autoLimitLine);
		}
		String secondaryLine = normalizedDetails.stream().filter(line -> !line.equals(actionLine))
				.filter(line -> line.startsWith("Detected ")).findFirst().orElseGet(() -> normalizedDetails.stream()
						.filter(line -> !line.equals(actionLine)).findFirst().orElse("Adjust limit in Settings."));
		return List.of(actionLine, secondaryLine);
	}

	private static boolean isDisplayAnywayDetail(String line) {
		if (line == null || line.isBlank()) {
			return false;
		}
		return line.toLowerCase(Locale.ROOT).contains("display anyway");
	}

	private static boolean isAutoLimitDetail(String line) {
		if (line == null || line.isBlank()) {
			return false;
		}
		return line.toLowerCase(Locale.ROOT).startsWith("auto limit:");
	}

	public static List<String> compactTooltipLines(List<String> lines, int maxLines) {
		int safeLimit = maxLines <= 0 ? TOOLTIP_MAX_LINES : maxLines;
		List<String> normalizedLines = lines == null
				? List.of()
				: lines.stream().map(DataStatusTooltipSupport::normalizeTooltipLine).filter(line -> !line.isBlank())
						.distinct().toList();
		if (normalizedLines.isEmpty()) {
			return List.of();
		}
		if (normalizedLines.size() <= safeLimit) {
			return normalizedLines;
		}
		if (safeLimit == 1) {
			return List.of("...");
		}
		List<String> compactLines = new ArrayList<>(normalizedLines.subList(0, safeLimit - 1));
		compactLines.add("...");
		return compactLines;
	}

	private static String simplifyRenderSummary(String summary, GraphRenderMode mode) {
		String safeSummary = normalizeTooltipLine(summary);
		if (!safeSummary.isBlank()) {
			return safeSummary;
		}
		return switch (mode) {
			case NORMAL -> "Standard rendering.";
			case DEGRADED -> "Adaptive rendering active.";
			case PAUSED -> "Preview paused.";
		};
	}

	private static String defaultRenderDetail(GraphRenderMode mode) {
		return switch (mode) {
			case NORMAL -> "Standard rendering profile.";
			case DEGRADED -> "Rendering detail reduced for responsiveness.";
			case PAUSED -> "Use \"Display anyway\" to force rendering.";
		};
	}

	private static String simplifyRenderDetail(String detail) {
		String safeDetail = normalizeTooltipLine(detail);
		if (safeDetail.isBlank()) {
			return "";
		}
		return switch (safeDetail) {
			case "Node labels hidden to keep rendering responsive." -> "Node labels reduced.";
			case "Edge labels hidden to reduce draw cost." -> "Edge labels reduced.";
			case "Link geometry simplified for dense graphs." -> "Simplified link geometry.";
			case "Parallel link offset layout disabled for dense edges." -> "Parallel edge offsets disabled.";
			case "Link motion sampling enabled to keep animation smooth." -> "Link motion sampling enabled.";
			case "Node tooltips disabled for very large graph." -> "Node tooltips disabled.";
			case "Force simulation paused for very dense graph." -> "Force simulation paused.";
			case "Labels temporarily hidden while interacting with the graph." -> "Labels hidden during interaction.";
			case "Node labels disabled for current graph size." -> "Node labels disabled.";
			case "Node labels hidden at current zoom level." -> "Node labels hidden at this zoom.";
			case "Edge labels disabled." -> "Edge labels disabled.";
			case "Edge labels disabled for current graph size." -> "Edge labels disabled.";
			case "Edge labels hidden by default for dense graph." -> "Edge labels hidden by default.";
			case "Edge labels hidden at current zoom level." -> "Edge labels hidden at this zoom.";
			case "Arrow heads hidden at low zoom for readability." -> "Arrow heads hidden at low zoom.";
			case "Adaptive performance calibration allows richer detail on this device." ->
				"Device allows richer detail.";
			case "Adaptive performance calibration tightened details to keep rendering smooth." ->
				"Detail reduced after calibration.";
			case "Offline layout budget reduced after runtime calibration." -> "Offline layout budget reduced.";
			case "Offline layout budget expanded after runtime calibration." -> "Offline layout budget expanded.";
			case "Threshold can be changed in Settings > Appearance > Graph Preview." -> "Adjust limit in Settings.";
			case "Use \"Display anyway\" to force rendering on demand." -> "Use \"Display anyway\" to render now.";
			case "Use \"Display anyway\" to force rendering." -> "Use \"Display anyway\" to render now.";
			case "Adjust base limit in Settings > Appearance > Graph Preview." -> "Adjust base limit in Settings.";
			case "Preview payload is skipped at this size to keep the UI responsive." ->
				"Preview payload skipped at this size.";
			case "Manual rendering can freeze the interface on very large graphs." ->
				"Manual render may freeze the UI.";
			default -> safeDetail.startsWith("Recent draw phase took about ")
					? "Rendering load detected; detail reduced."
					: safeDetail;
		};
	}

	private static String normalizeTooltipLine(String line) {
		if (line == null) {
			return "";
		}
		String normalized = line.replaceAll("\\s+", " ").trim();
		if (normalized.length() <= TOOLTIP_MAX_LINE_LENGTH) {
			return normalized;
		}
		return normalized.substring(0, TOOLTIP_MAX_LINE_LENGTH - 3) + "...";
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
		List<String> safeLines = compactTooltipLines(lines, TOOLTIP_MAX_LINES);
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
