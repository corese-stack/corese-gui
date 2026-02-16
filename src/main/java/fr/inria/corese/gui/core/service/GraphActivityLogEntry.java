package fr.inria.corese.gui.core.service;

import java.util.Objects;

/**
 * Immutable activity entry describing a user-visible operation that changed the
 * shared RDF graph.
 *
 * @param timestampMillis
 *            event timestamp in epoch milliseconds
 * @param source
 *            activity origin
 * @param action
 *            short action label
 * @param details
 *            optional additional details
 * @param insertedCount
 *            inserted triples count (best effort)
 * @param deletedCount
 *            deleted triples count (best effort)
 * @param totalTripleCount
 *            graph triple count snapshot after the operation
 * @param namedGraphCount
 *            named graph count snapshot after the operation
 */
public record GraphActivityLogEntry(long timestampMillis, Source source, String action, String details,
		int insertedCount, int deletedCount, int totalTripleCount, int namedGraphCount) {

	/**
	 * Logical origin of a graph activity entry.
	 */
	public enum Source {
		DATA_WORKSPACE, QUERY_SERVICE, REASONING_SERVICE, RULE_FILE_SERVICE, SYSTEM
	}

	public GraphActivityLogEntry {
		Objects.requireNonNull(source, "source must not be null");
		if (action == null || action.isBlank()) {
			throw new IllegalArgumentException("action must not be blank");
		}
		if (insertedCount < 0) {
			throw new IllegalArgumentException("insertedCount must be >= 0");
		}
		if (deletedCount < 0) {
			throw new IllegalArgumentException("deletedCount must be >= 0");
		}
		if (totalTripleCount < 0) {
			throw new IllegalArgumentException("totalTripleCount must be >= 0");
		}
		if (namedGraphCount < 0) {
			throw new IllegalArgumentException("namedGraphCount must be >= 0");
		}
		action = action.trim();
		details = details == null ? "" : details.trim();
		if (timestampMillis <= 0) {
			timestampMillis = System.currentTimeMillis();
		}
	}

	/**
	 * Creates an entry stamped with the current system time.
	 *
	 * @param source
	 *            activity origin
	 * @param action
	 *            short action label
	 * @param details
	 *            optional details
	 * @param insertedCount
	 *            inserted triples count
	 * @param deletedCount
	 *            deleted triples count
	 * @param totalTripleCount
	 *            graph triple count snapshot after operation
	 * @param namedGraphCount
	 *            named graph count snapshot after operation
	 * @return timestamped activity entry
	 */
	public static GraphActivityLogEntry now(Source source, String action, String details, int insertedCount,
			int deletedCount, int totalTripleCount, int namedGraphCount) {
		return new GraphActivityLogEntry(System.currentTimeMillis(), source, action, details, insertedCount,
				deletedCount, totalTripleCount, namedGraphCount);
	}

	/**
	 * Indicates whether the entry carries a non-zero triple delta.
	 *
	 * @return true when inserted or deleted counters are non-zero
	 */
	public boolean hasDelta() {
		return insertedCount > 0 || deletedCount > 0;
	}
}
