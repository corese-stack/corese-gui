package fr.inria.corese.gui.component.graph;

import fr.inria.corese.gui.component.graph.GraphDisplayWidget.GraphStats;
import java.util.ArrayList;
import java.util.List;
import netscape.javascript.JSObject;

/**
 * Parsing helpers used by the JavaScript bridge of {@link GraphDisplayWidget}.
 */
@SuppressWarnings({"java:S5738", "removal"})
final class GraphBridgeParsing {

	private GraphBridgeParsing() {
		// Utility class
	}

	static boolean isCurrentRenderRequest(String requestId, long currentRequestId) {
		if (requestId == null || requestId.isBlank()) {
			return false;
		}
		try {
			return Long.parseLong(requestId) == currentRequestId;
		} catch (NumberFormatException _) {
			return false;
		}
	}

	static List<GraphStats.NamedGraphStat> parseNamedGraphStats(Object value) {
		if (!(value instanceof JSObject statsArray)) {
			return List.of();
		}
		int size = parseArrayLength(statsArray);
		if (size <= 0) {
			return List.of();
		}

		List<GraphStats.NamedGraphStat> stats = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			try {
				GraphStats.NamedGraphStat stat = parseNamedGraphStat(statsArray.getSlot(index));
				if (stat != null && !stat.graphId().isBlank()) {
					stats.add(stat);
				}
			} catch (Exception _) {
				// Ignore malformed entry and keep the remaining stats.
			}
		}
		return stats;
	}

	static int parseNonNegativeInt(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number number) {
			return Math.max(0, number.intValue());
		}
		return parseNonNegativeInt(String.valueOf(value));
	}

	static int parseNonNegativeInt(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		String normalized = value.trim();
		try {
			return Math.max(0, Integer.parseInt(normalized));
		} catch (NumberFormatException _) {
			try {
				return Math.max(0, (int) Math.floor(Double.parseDouble(normalized)));
			} catch (NumberFormatException _) {
				return 0;
			}
		}
	}

	static double parseNonNegativeDouble(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number number) {
			return Math.max(0, number.doubleValue());
		}
		return parseNonNegativeDouble(String.valueOf(value));
	}

	static double parseNonNegativeDouble(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		String normalized = value.trim();
		try {
			return Math.max(0, Double.parseDouble(normalized));
		} catch (NumberFormatException _) {
			return 0;
		}
	}

	static String parseTrimmedString(Object value) {
		if (value == null) {
			return "";
		}
		return String.valueOf(value).trim();
	}

	static List<String> parseStringList(Object value) {
		if (value == null) {
			return List.of();
		}
		if (value instanceof JSObject arrayObject) {
			int size = parseArrayLength(arrayObject);
			if (size <= 0) {
				return List.of();
			}
			List<String> lines = new ArrayList<>(size);
			for (int index = 0; index < size; index++) {
				try {
					String line = parseTrimmedString(arrayObject.getSlot(index));
					if (!line.isBlank()) {
						lines.add(line);
					}
				} catch (Exception _) {
					// Ignore malformed entry and continue with remaining values.
				}
			}
			return lines;
		}
		String singleLine = parseTrimmedString(value);
		if (singleLine.isBlank()) {
			return List.of();
		}
		if (singleLine.indexOf('\n') >= 0 || singleLine.indexOf('\r') >= 0) {
			return singleLine.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
		}
		return List.of(singleLine);
	}

	private static int parseArrayLength(JSObject arrayObject) {
		try {
			return parseNonNegativeInt(arrayObject.getMember("length"));
		} catch (Exception _) {
			return 0;
		}
	}

	private static GraphStats.NamedGraphStat parseNamedGraphStat(Object value) {
		if (!(value instanceof JSObject statObject)) {
			return null;
		}
		String graphId = parseStringMember(statObject, "id");
		if (graphId.isBlank()) {
			return null;
		}
		int tripleCount = parseNonNegativeInt(readMember(statObject, "linkCount"));
		return new GraphStats.NamedGraphStat(graphId, tripleCount);
	}

	private static String parseStringMember(JSObject object, String memberName) {
		Object value = readMember(object, memberName);
		return value == null ? "" : String.valueOf(value).trim();
	}

	private static Object readMember(JSObject object, String memberName) {
		try {
			return object.getMember(memberName);
		} catch (Exception _) {
			return null;
		}
	}
}
