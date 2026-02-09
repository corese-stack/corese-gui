package fr.inria.corese.gui.core.model;

import fr.inria.corese.gui.core.enums.QueryType;

/**
 * Lightweight handle for a cached query result inside QueryService.
 */
public class QueryResultRef {
	private final String id;
	private final QueryType queryType;
	private final Boolean askResult;
	private final int insertedTriples;
	private final int deletedTriples;
	private final int resultCount;

	public QueryResultRef(String id, QueryType queryType) {
		this(id, queryType, null, 0, 0, 0);
	}

	public QueryResultRef(String id, QueryType queryType, Boolean askResult, int insertedTriples, int deletedTriples,
			int resultCount) {
		this.id = id;
		this.queryType = queryType;
		this.askResult = askResult;
		this.insertedTriples = insertedTriples;
		this.deletedTriples = deletedTriples;
		this.resultCount = Math.max(0, resultCount);
	}

	public String getId() {
		return id;
	}

	public QueryType getQueryType() {
		return queryType;
	}

	/**
	 * Returns ASK boolean result when the query type is {@link QueryType#ASK},
	 * otherwise {@code null}.
	 */
	public Boolean getAskResult() {
		return askResult;
	}

	/** Returns the number of triples inserted by an update query. */
	public int getInsertedTriples() {
		return insertedTriples;
	}

	/** Returns the number of triples deleted by an update query. */
	public int getDeletedTriples() {
		return deletedTriples;
	}

	/**
	 * Returns the number of produced result entries for SELECT / CONSTRUCT /
	 * DESCRIBE queries.
	 *
	 * <p>
	 * Returns 0 when no data result is available.
	 */
	public int getResultCount() {
		return resultCount;
	}
}
