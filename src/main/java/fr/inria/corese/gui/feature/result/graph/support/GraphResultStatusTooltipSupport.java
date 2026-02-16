package fr.inria.corese.gui.feature.result.graph.support;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphStats;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formatter/helper for Graph Result status bar tooltips.
 */
public final class GraphResultStatusTooltipSupport {

	private static final int TOOLTIP_PREVIEW_LIMIT = 8;
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.getDefault());

	private GraphResultStatusTooltipSupport() {
		throw new AssertionError("Utility class");
	}

	public static List<String> buildTriplesTooltipLines(GraphStats stats) {
		GraphStats safeStats = safeStats(stats);
		int namedGraphTriples = sanitizedNamedGraphStats(safeStats).stream()
				.mapToInt(GraphStats.NamedGraphStat::tripleCount)
				.sum();
		namedGraphTriples = Math.min(namedGraphTriples, safeStats.tripleCount());
		int defaultGraphTriples = Math.max(0, safeStats.tripleCount() - namedGraphTriples);

		return List.of("Explicit triples: " + formatCount(safeStats.tripleCount()),
				"Default graph triples: " + formatCount(defaultGraphTriples),
				"Triples in named graphs: " + formatCount(namedGraphTriples));
	}

	public static List<String> buildNamedGraphTooltipLines(GraphStats stats) {
		GraphStats safeStats = safeStats(stats);
		List<GraphStats.NamedGraphStat> namedGraphStats = sanitizedNamedGraphStats(safeStats);
		if (namedGraphStats.isEmpty()) {
			return List.of("No named graph currently contains triples.");
		}

		List<String> lines = new ArrayList<>();
		int displayed = Math.min(TOOLTIP_PREVIEW_LIMIT, namedGraphStats.size());
		for (int index = 0; index < displayed; index++) {
			GraphStats.NamedGraphStat stat = namedGraphStats.get(index);
			lines.add(shortenGraphName(stat.graphId()) + ": " + formatCount(stat.tripleCount()) + " triples");
		}
		if (namedGraphStats.size() > displayed) {
			lines.add("... and " + formatCount(namedGraphStats.size() - displayed) + " more named graphs.");
		}
		return lines;
	}

	private static List<GraphStats.NamedGraphStat> sanitizedNamedGraphStats(GraphStats stats) {
		return stats.namedGraphStats().stream()
				.filter(stat -> stat != null && !stat.graphId().isBlank())
				.toList();
	}

	private static GraphStats safeStats(GraphStats stats) {
		if (stats == null) {
			return new GraphStats(0, 0, List.of());
		}
		return stats;
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
}
