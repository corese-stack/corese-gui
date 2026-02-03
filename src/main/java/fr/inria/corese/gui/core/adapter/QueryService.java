package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResultRef;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton service for executing and managing SPARQL queries.
 *
 * <p>This service:
 * <ul>
 *   <li>Executes SPARQL queries (SELECT, ASK, CONSTRUCT, DESCRIBE, UPDATE) on the Corese graph
 *   <li>Caches query results internally with unique identifiers
 *   <li>Formats cached results in various serialization formats
 *   <li>Provides opaque result references to GUI layer (no corese-core type exposure)
 * </ul>
 *
 * <p>Thread-safe implementation with concurrent cache management.
 *
 * @see QueryResultRef
 * @see ResultFormatter
 */
public class QueryService {

  private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

  private static QueryService instance;

  private final GraphStore graphStore;
  private final Map<String, QueryCacheEntry> queryResultCache;

  private QueryService() {
    this.graphStore = GraphStore.getInstance();
    this.queryResultCache = new ConcurrentHashMap<>();
  }

  /**
   * Returns the singleton instance of QueryService.
   *
   * @return the singleton QueryService instance
   */
  public static synchronized QueryService getInstance() {
    if (instance == null) {
      instance = new QueryService();
    }
    return instance;
  }

  // ============================================================================================
  // Query Execution
  // ============================================================================================

  /**
   * Executes a SPARQL query and caches the result.
   *
   * <p>The query is executed against the current graph store. Results are cached
   * internally and a lightweight reference is returned to avoid exposing corese-core types.
   *
   * @param query the SPARQL query string
   * @return a {@link QueryResultRef} containing the result ID and query type
   * @throws IllegalArgumentException if the query is null or blank
   * @throws Exception if query execution fails
   */
  public QueryResultRef executeQuery(String query) throws Exception {
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("Query cannot be null or blank");
    }

    QueryProcess queryProcess = QueryProcess.create(graphStore.getGraph());
    Mappings mappings = queryProcess.query(query);
    ASTQuery ast = mappings.getAST();
    QueryType queryType = determineQueryType(ast);

    QueryCacheEntry cacheEntry = createCacheEntry(queryType, mappings);
    String resultId = UUID.randomUUID().toString();
    queryResultCache.put(resultId, cacheEntry);

    logger.info("Query executed successfully (type: {}, result ID: {})", queryType, resultId);
    return new QueryResultRef(resultId, queryType);
  }

  // ============================================================================================
  // Result Formatting
  // ============================================================================================

  /**
   * Formats a cached query result in the specified serialization format.
   *
   * <p>Validates that the format is compatible with the query type:
   * <ul>
   *   <li>CONSTRUCT/DESCRIBE/UPDATE require RDF formats (Turtle, RDF/XML, JSON-LD, etc.)
   *   <li>SELECT/ASK require mapping formats (XML, JSON, CSV, TSV, Markdown)
   * </ul>
   *
   * @param resultId the result identifier from {@link #executeQuery(String)}
   * @param formatString the target format as a string
   * @return the formatted result, or an error message if validation fails
   */
  public String formatResult(String resultId, String formatString) {
    QueryCacheEntry cachedEntry = queryResultCache.get(resultId);
    if (cachedEntry == null) {
      return "Error: Result not found for ID: " + resultId;
    }

    SerializationFormat format = SerializationFormat.fromString(formatString);
    if (format == null) {
      return "Error: Unknown serialization format: " + formatString;
    }

    // Validate format compatibility with query type
    String validationError = validateFormatCompatibility(cachedEntry.getQueryType(), format);
    if (validationError != null) {
      return validationError;
    }

    // Delegate formatting to ResultFormatter
    if (cachedEntry.getGraphResult() != null) {
      return ResultFormatter.getInstance().formatGraph(cachedEntry.getGraphResult(), format);
    }
    if (cachedEntry.getMappingsResult() != null) {
      return ResultFormatter.getInstance().formatMappings(cachedEntry.getMappingsResult(), format);
    }

    return "Error: No result data available";
  }

  /**
   * Releases a cached query result to free memory.
   *
   * <p>Should be called when the result is no longer needed by the GUI.
   *
   * @param resultId the result identifier to release
   */
  public void releaseResult(String resultId) {
    if (resultId != null) {
      queryResultCache.remove(resultId);
      logger.debug("Released cached result: {}", resultId);
    }
  }

  // ============================================================================================
  // Private Helpers
  // ============================================================================================

  /**
   * Creates a cache entry based on query type and result.
   */
  private QueryCacheEntry createCacheEntry(QueryType queryType, Mappings mappings) {
    if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
      Graph resultGraph = (Graph) mappings.getGraph();
      return new QueryCacheEntry(queryType, resultGraph);
    } else {
      return new QueryCacheEntry(queryType, mappings);
    }
  }

  /**
   * Determines the query type from the AST.
   */
  private QueryType determineQueryType(ASTQuery ast) {
    if (ast == null) {
      return QueryType.UNKNOWN;
    }
    if (ast.isUpdate()) {
      return QueryType.UPDATE;
    }
    if (ast.isSelect()) {
      return QueryType.SELECT;
    }
    if (ast.isConstruct()) {
      return QueryType.CONSTRUCT;
    }
    if (ast.isAsk()) {
      return QueryType.ASK;
    }
    if (ast.isDescribe()) {
      return QueryType.DESCRIBE;
    }
    return QueryType.UNKNOWN;
  }

  /**
   * Validates that the serialization format is compatible with the query type.
   *
   * @return error message if incompatible, null if valid
   */
  private String validateFormatCompatibility(QueryType queryType, SerializationFormat format) {
    boolean isRdfFormat = Arrays.asList(SerializationFormat.rdfFormats()).contains(format);

    if ((queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE || queryType == QueryType.UPDATE)
        && !isRdfFormat) {
      return String.format("Error: %s is not a valid RDF format for query type %s", format, queryType);
    }

    if ((queryType == QueryType.SELECT || queryType == QueryType.ASK) && isRdfFormat) {
      return String.format("Error: %s is not a valid mapping format for query type %s", format, queryType);
    }

    return null;
  }

  // ============================================================================================
  // Inner Classes
  // ============================================================================================

  /**
   * Internal cache entry for storing query results.
   *
   * <p>Stores either Mappings (for SELECT/ASK) or a Graph (for CONSTRUCT/DESCRIBE).
   */
  private static final class QueryCacheEntry {

    private final QueryType queryType;
    private final Mappings mappingsResult;
    private final Graph graphResult;

    /**
     * Constructor for mapping results (SELECT/ASK).
     */
    QueryCacheEntry(QueryType queryType, Mappings mappings) {
      this.queryType = queryType;
      this.mappingsResult = mappings;
      this.graphResult = null;
    }

    /**
     * Constructor for graph results (CONSTRUCT/DESCRIBE).
     */
    QueryCacheEntry(QueryType queryType, Graph graph) {
      this.queryType = queryType;
      this.mappingsResult = null;
      this.graphResult = graph;
    }

    QueryType getQueryType() {
      return queryType;
    }

    Mappings getMappingsResult() {
      return mappingsResult;
    }

    Graph getGraphResult() {
      return graphResult;
    }
  }
}
