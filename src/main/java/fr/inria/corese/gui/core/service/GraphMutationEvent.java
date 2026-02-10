package fr.inria.corese.gui.core.service;

import java.util.Objects;

/**
 * Immutable event describing a mutation in the shared RDF graph.
 *
 * @param kind
 *            mutation category
 * @param source
 *            origin of the mutation notification
 * @param insertedCount
 *            number of inserted triples
 * @param deletedCount
 *            number of deleted triples
 * @param timestampMillis
 *            event timestamp in epoch milliseconds
 */
public record GraphMutationEvent(Kind kind, Source source, int insertedCount, int deletedCount, long timestampMillis) {

	/**
	 * Mutation kind.
	 */
	public enum Kind {
		INSERT, DELETE, INSERT_DELETE, BULK_REFRESH_REQUIRED, CLEAR_ALL
	}

	/**
	 * Event source.
	 */
	public enum Source {
		GRAPH_LISTENER, DATA_WORKSPACE, REASONING, UNKNOWN
	}

	public GraphMutationEvent {
		Objects.requireNonNull(kind, "kind must not be null");
		Objects.requireNonNull(source, "source must not be null");
		if (insertedCount < 0) {
			throw new IllegalArgumentException("insertedCount must be >= 0");
		}
		if (deletedCount < 0) {
			throw new IllegalArgumentException("deletedCount must be >= 0");
		}
	}

	/**
	 * Creates a delta event from insert/delete counters.
	 *
	 * @param source
	 *            event source
	 * @param insertedCount
	 *            number of inserted triples
	 * @param deletedCount
	 *            number of deleted triples
	 * @return a normalized mutation event
	 */
	public static GraphMutationEvent delta(Source source, int insertedCount, int deletedCount) {
		Kind kind;
		if (insertedCount > 0 && deletedCount > 0) {
			kind = Kind.INSERT_DELETE;
		} else if (insertedCount > 0) {
			kind = Kind.INSERT;
		} else if (deletedCount > 0) {
			kind = Kind.DELETE;
		} else {
			kind = Kind.BULK_REFRESH_REQUIRED;
		}
		return new GraphMutationEvent(kind, source, Math.max(0, insertedCount), Math.max(0, deletedCount),
				System.currentTimeMillis());
	}

	/**
	 * Creates a refresh-required event.
	 *
	 * @param source
	 *            event source
	 * @return bulk refresh event
	 */
	public static GraphMutationEvent bulkRefreshRequired(Source source) {
		return new GraphMutationEvent(Kind.BULK_REFRESH_REQUIRED, source, 0, 0, System.currentTimeMillis());
	}

	/**
	 * Creates a clear-all event.
	 *
	 * @param source
	 *            event source
	 * @return clear-all event
	 */
	public static GraphMutationEvent clearAll(Source source) {
		return new GraphMutationEvent(Kind.CLEAR_ALL, source, 0, 0, System.currentTimeMillis());
	}

	/**
	 * Indicates whether the event corresponds to a structural graph change.
	 *
	 * @return true when triples were inserted/deleted or when a clear event
	 *         occurred
	 */
	public boolean hasStructuralChange() {
		return insertedCount > 0 || deletedCount > 0 || kind == Kind.CLEAR_ALL;
	}
}
