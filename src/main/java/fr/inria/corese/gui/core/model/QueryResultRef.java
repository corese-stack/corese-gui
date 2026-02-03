package fr.inria.corese.gui.core.model;

import fr.inria.corese.gui.core.enums.QueryType;

/**
 * Lightweight handle for a cached query result inside QueryManager.
 */
public class QueryResultRef {
  private final String id;
  private final QueryType queryType;

  public QueryResultRef(String id, QueryType queryType) {
    this.id = id;
    this.queryType = queryType;
  }

  public String getId() {
    return id;
  }

  public QueryType getQueryType() {
    return queryType;
  }
}
