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

  public QueryResultRef(String id, QueryType queryType) {
    this(id, queryType, null, 0, 0);
  }

  public QueryResultRef(
      String id,
      QueryType queryType,
      Boolean askResult,
      int insertedTriples,
      int deletedTriples) {
    this.id = id;
    this.queryType = queryType;
    this.askResult = askResult;
    this.insertedTriples = insertedTriples;
    this.deletedTriples = deletedTriples;
  }

  public String getId() {
    return id;
  }

  public QueryType getQueryType() {
    return queryType;
  }

  /**
   * Returns ASK boolean result when the query type is {@link QueryType#ASK}, otherwise {@code null}.
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
}
