package fr.inria.corese.gui.feature.systemlog;

import fr.inria.corese.gui.core.service.GraphActivityLogEntry;

/**
 * Immutable row model for the System Logs table.
 */
record SystemLogTableRow(long timestampMillis, String time, String type, String action, String details,
		int insertedCount, int deletedCount, int totalTripleCount, int namedGraphCount) {

	static SystemLogTableRow from(GraphActivityLogEntry entry, String formattedTime, String typeLabel) {
		if (entry == null) {
			return new SystemLogTableRow(0L, "", "", "", "", 0, 0, 0, 0);
		}
		return new SystemLogTableRow(entry.timestampMillis(), formattedTime, typeLabel, entry.action(), entry.details(),
				entry.insertedCount(), entry.deletedCount(), entry.totalTripleCount(), entry.namedGraphCount());
	}

	int netDelta() {
		return insertedCount - deletedCount;
	}

	String diffLabel() {
		if (insertedCount > 0 && deletedCount > 0) {
			return "+" + insertedCount + " / -" + deletedCount;
		}
		if (insertedCount > 0) {
			return "+" + insertedCount;
		}
		if (deletedCount > 0) {
			return "-" + deletedCount;
		}
		return "0";
	}

	String stateLabel() {
		return "Triples: " + totalTripleCount + " | Named Graphs: " + namedGraphCount;
	}
}
