package fr.inria.corese.gui.feature.data.support;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderMode;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderCapabilities;
import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphRenderStatus;
import fr.inria.corese.gui.core.service.data.DataWorkspaceStatus;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
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
	private static final int TOOLTIP_MAX_LINES = 7;
	private static final int TOOLTIP_MAX_LINE_LENGTH = 96;
	private static final int RENDER_TOOLTIP_MAX_LINES = 7;
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.getDefault());

	private DataStatusTooltipSupport() {
		throw new AssertionError("Utility class");
	}

	public enum RenderStatusBadge {
		STANDARD("Standard", GraphRenderMode.NORMAL), ADAPTIVE("Adaptive", GraphRenderMode.DEGRADED),
		LOCKED("Locked", GraphRenderMode.DEGRADED), PAUSED("Paused", GraphRenderMode.PAUSED),
		FAILED("Failed", GraphRenderMode.PAUSED);

		private final String label;
		private final GraphRenderMode styleMode;

		RenderStatusBadge(String label, GraphRenderMode styleMode) {
			this.label = label;
			this.styleMode = styleMode;
		}

		public String label() {
			return label;
		}

		public GraphRenderMode styleMode() {
			return styleMode;
		}
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
		List<String> lines = new ArrayList<>();
		if (status != null && status.nativeRdfsSubsetEnabled()) {
			lines.add("RDFS Subset: active");
			lines.add("Some inferred triples are added to kg:entailment.");
			lines.add("Subclass-based rdf:type results may also be resolved at query time.");
		}
		if (status == null || status.reasoningStats().isEmpty()) {
			if (lines.isEmpty()) {
				return List.of("No inferred triples.");
			}
			return lines;
		}
		for (DataWorkspaceStatus.ReasoningStat stat : status.reasoningStats()) {
			lines.add(stat.profileLabel() + ": " + formatCount(stat.tripleCount()));
		}
		return lines;
	}

	public static List<String> buildRenderTooltipLines(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		List<String> lines = new ArrayList<>();
		String normalizedSummary = simplifyRenderSummary(safeStatus.summary(), safeStatus.mode());
		lines.add(normalizedSummary);

		List<String> capabilityDetails = buildCapabilityDetails(safeStatus.capabilities());
		List<String> rawDetails = safeStatus.details().isEmpty()
				? capabilityDetails.isEmpty() ? List.of(defaultRenderDetail(safeStatus.mode(), normalizedSummary))
						: capabilityDetails
				: safeStatus.details();
		List<String> normalizedDetails = rawDetails.stream().map(DataStatusTooltipSupport::simplifyRenderDetail)
				.filter(line -> !line.isBlank()).distinct().toList();

		lines.addAll(prioritizeRenderDetails(safeStatus.mode(), normalizedDetails));
		return compactTooltipLines(lines, RENDER_TOOLTIP_MAX_LINES);
	}

	public static RenderStatusBadge resolveRenderStatusBadge(GraphRenderStatus status) {
		GraphRenderStatus safeStatus = status == null ? GraphRenderStatus.normal() : status;
		String summary = normalizeTooltipLine(safeStatus.summary()).toLowerCase(Locale.ROOT);
		GraphRenderCapabilities capabilities = safeStatus.capabilities();
		boolean hasLockSignal = summaryIndicatesLocked(summary) || safeStatus.details().stream()
				.map(DataStatusTooltipSupport::normalizeTooltipLine)
				.map(line -> line.toLowerCase(Locale.ROOT))
				.anyMatch(DataStatusTooltipSupport::detailIndicatesLocked);
		boolean capabilityLockSignal = capabilities != null && !capabilities.interactionsEnabled();
		boolean hasFailureSignal = summaryIndicatesFailure(summary) || safeStatus.details().stream()
				.map(DataStatusTooltipSupport::normalizeTooltipLine).map(line -> line.toLowerCase(Locale.ROOT))
				.anyMatch(DataStatusTooltipSupport::detailIndicatesFailure);
		boolean hasAdaptiveSignal = summaryIndicatesAdaptive(summary);
		boolean capabilityAdaptiveSignal = capabilities != null && capabilities.hasAnyRestriction();

		if (hasFailureSignal) {
			return RenderStatusBadge.FAILED;
		}
		if (safeStatus.mode() == GraphRenderMode.PAUSED) {
			return RenderStatusBadge.PAUSED;
		}
		if (hasLockSignal || capabilityLockSignal) {
			return RenderStatusBadge.LOCKED;
		}
		if (safeStatus.mode() == GraphRenderMode.DEGRADED || hasAdaptiveSignal || capabilityAdaptiveSignal
				|| !safeStatus.details().isEmpty()) {
			return RenderStatusBadge.ADAPTIVE;
		}
		return RenderStatusBadge.STANDARD;
	}

	private static List<String> buildCapabilityDetails(GraphRenderCapabilities capabilities) {
		if (capabilities == null) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		if (!capabilities.interactionsEnabled()) {
			lines.add("Graph interactions disabled for very large graph.");
		}
		if (!capabilities.nodeLabelsVisible()) {
			lines.add("Node labels hidden at current zoom level.");
		}
		if (!capabilities.edgeLabelsVisible()) {
			lines.add("Edge labels hidden at current zoom level.");
		}
		if (!capabilities.tooltipsEnabled()) {
			lines.add("Node tooltips disabled for very large graph.");
		}
		if (!capabilities.hoverFocusEnabled()) {
			lines.add("Node hover focus disabled for very large graph.");
		}
		if (!capabilities.arrowsVisible()) {
			lines.add("Arrow heads hidden at low zoom for readability.");
		}
		return lines;
	}

	private static List<String> prioritizeRenderDetails(GraphRenderMode mode, List<String> normalizedDetails) {
		if (normalizedDetails.isEmpty()) {
			return List.of(defaultRenderDetail(mode, ""));
		}
		if (mode == GraphRenderMode.PAUSED) {
			return buildPausedDetailPreview(normalizedDetails);
		}

		List<String> ordered = new ArrayList<>();
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Interactions:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Node labels:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Edge labels:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Node tooltips:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Hover focus:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Arrows:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Interaction:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Layout:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Animation:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Runtime calibration:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Runtime:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Warning:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Action:"));
		addMatchingDetails(ordered, normalizedDetails, line -> true);
		return ordered;
	}

	private static List<String> buildPausedDetailPreview(List<String> normalizedDetails) {
		List<String> ordered = new ArrayList<>();
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Detected "));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Current auto-preview limit:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Serialized graph size:"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Action: adjust preview limit"));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Action: use \"Display anyway\""));
		addMatchingDetails(ordered, normalizedDetails, line -> line.startsWith("Warning:"));
		addMatchingDetails(ordered, normalizedDetails, line -> true);
		return ordered;
	}

	private static void addMatchingDetails(List<String> target, List<String> source, Predicate<String> predicate) {
		for (String line : source) {
			if (line == null || line.isBlank() || target.contains(line)) {
				continue;
			}
			if (predicate.test(line)) {
				target.add(line);
			}
		}
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
		int hiddenCount = Math.max(1, normalizedLines.size() - Math.max(0, safeLimit - 1));
		if (safeLimit == 1) {
			return List.of("+ " + formatCount(hiddenCount) + " more");
		}
		List<String> compactLines = new ArrayList<>(normalizedLines.subList(0, safeLimit - 1));
		compactLines.add("+ " + formatCount(hiddenCount) + " more");
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

	private static String defaultRenderDetail(GraphRenderMode mode, String summary) {
		String normalizedSummary = normalizeTooltipLine(summary).toLowerCase(Locale.ROOT);
		return switch (mode) {
			case NORMAL -> summaryIndicatesLocked(normalizedSummary)
					? "Interactions: zoom/pan/drag/reset disabled for this graph size."
					: summaryIndicatesAdaptive(normalizedSummary)
							? "Rendering detail is currently adapted to keep the interface responsive."
							: "All graph interactions and labels are available.";
			case DEGRADED -> "Rendering detail is reduced to keep the interface responsive.";
			case PAUSED -> "Preview is paused until manual rendering is requested.";
		};
	}

	private static boolean summaryIndicatesLocked(String summary) {
		if (summary == null || summary.isBlank()) {
			return false;
		}
		return summary.contains("interaction") && (summary.contains("disabled") || summary.contains("locked"));
	}

	private static boolean summaryIndicatesFailure(String summary) {
		if (summary == null || summary.isBlank()) {
			return false;
		}
		return summary.contains("failed") || summary.contains("error") || summary.contains("exception");
	}

	private static boolean summaryIndicatesAdaptive(String summary) {
		if (summary == null || summary.isBlank()) {
			return false;
		}
		return summary.contains("adaptive") || summary.contains("performance")
				|| summary.contains("optimization") || summary.contains("pressure");
	}

	private static boolean detailIndicatesLocked(String detail) {
		if (detail == null || detail.isBlank()) {
			return false;
		}
		return detail.contains("interaction") && (detail.contains("disabled") || detail.contains("locked"));
	}

	private static boolean detailIndicatesFailure(String detail) {
		if (detail == null || detail.isBlank()) {
			return false;
		}
		return detail.contains("failed") || detail.contains("error") || detail.contains("exception");
	}

	private static String simplifyRenderDetail(String detail) {
		String safeDetail = normalizeTooltipLine(detail);
		if (safeDetail.isBlank()) {
			return "";
		}
		return switch (safeDetail) {
			case "Node labels hidden to keep rendering responsive.", "Node labels disabled for current graph size." ->
				"Node labels: disabled for this graph size.";
			case "Edge labels hidden to reduce draw cost.", "Edge labels disabled for current graph size." ->
				"Edge labels: disabled for this graph size.";
			case "Link geometry simplified for dense graphs." -> "Layout: link geometry simplified for dense graphs.";
			case "Parallel link offset layout disabled for dense edges." -> "Layout: parallel edge offsets disabled.";
			case "Link motion sampling enabled to keep animation smooth." -> "Animation: link motion sampling enabled.";
			case "Node tooltips disabled for very large graph." -> "Node tooltips: disabled for this graph size.";
			case "Node hover focus disabled for very large graph." -> "Hover focus: disabled for this graph size.";
			case "Graph interactions disabled for very large graph." ->
				"Interactions: zoom/pan/drag/reset disabled for this graph size.";
			case "Force simulation paused for very dense graph." -> "Layout: force simulation paused after pre-layout.";
			case "Labels temporarily hidden while interacting with the graph." ->
				"Interaction: labels hidden while moving the graph.";
			case "Node labels hidden at current zoom level." -> "Node labels: hidden at current zoom level.";
			case "Edge labels disabled." -> "Edge labels: disabled by user.";
			case "Edge labels hidden by default for dense graph." -> "Edge labels: hidden by default on dense graph.";
			case "Edge labels hidden at current zoom level." -> "Edge labels: hidden at current zoom level.";
			case "Arrow heads hidden at low zoom for readability." -> "Arrows: hidden at low zoom.";
			case "Adaptive performance calibration allows richer detail on this device." ->
				"Runtime calibration: device can handle richer detail.";
			case "Adaptive performance calibration tightened details to keep rendering smooth." ->
				"Runtime calibration: detail reduced to keep FPS stable.";
			case "Offline layout budget reduced after runtime calibration." ->
				"Runtime calibration: offline layout budget reduced.";
			case "Offline layout budget expanded after runtime calibration." ->
				"Runtime calibration: offline layout budget expanded.";
			case "Threshold can be changed in Settings > Appearance > Graph Preview.",
					"Base limit can be changed in Settings > Appearance > Graph Preview.",
					"Adjust base limit in Settings > Appearance > Graph Preview.",
					"Limit can be changed in Settings > Appearance > Graph Preview." ->
				"Action: adjust preview limit in Settings > Appearance > Graph Preview.";
			case "Use \"Display anyway\" to force rendering on demand.", "Use \"Display anyway\" to force rendering." ->
				"Action: use \"Display anyway\" to force rendering now.";
			case "Preview payload is skipped at this size to keep the UI responsive." ->
				"Preview payload skipped at this size to protect responsiveness.";
			case "Manual rendering can freeze the interface on very large graphs." ->
				"Warning: manual rendering may freeze the interface on very large graphs.";
			default -> safeDetail.startsWith("Recent draw phase took about ")
					? "Runtime: recent draw phase was heavy; conservative detail enabled."
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
