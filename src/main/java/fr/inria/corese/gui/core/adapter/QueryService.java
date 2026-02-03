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
 * Service for executing and caching SPARQL queries over the Corese Graph.
 *
 * <p>Implements a thread-safe singleton that: - runs queries via QueryProcess and stores a
 * QueryCacheEntry with either Mappings (SELECT/ASK) or a result Graph (CONSTRUCT/DESCRIBE); -
 * formats cached results using ResultFormatter; - converts external format names to Corese formats
 * and logs operations; - exposes results via opaque ids so the GUI never sees corese-core types.
 */
public class QueryService {
  private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

  private static QueryService instance;

  private final GraphStore graphStore;

  private final Map<String, QueryCacheEntry> queryResultCache = new ConcurrentHashMap<>();

  private QueryService() {
    this.graphStore = GraphStore.getInstance();
  }

  public static synchronized QueryService getInstance() {
    if (instance == null) {
      instance = new QueryService();
    }
    return instance;
  }

  /**
   * Executes a query and caches its result internally.
   *
   * <p>Returns a lightweight handle that the GUI can store without exposing corese-core types.
   */
  public QueryResultRef executeQuery(String query) throws Exception {
    try {
      QueryProcess queryProcess = QueryProcess.create(graphStore.getGraph());
      Mappings mappings = queryProcess.query(query);
      ASTQuery ast = mappings.getAST();
      QueryType queryType = determineQueryTypeFromAST(ast);

      QueryCacheEntry cacheEntry;
      if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
        Graph resultGraph = (Graph) mappings.getGraph();
        cacheEntry = new QueryCacheEntry(queryType, resultGraph);
      } else {
        cacheEntry = new QueryCacheEntry(queryType, mappings);
      }
      String resultId = UUID.randomUUID().toString();
      queryResultCache.put(resultId, cacheEntry);
      logger.info("Query executed and result cached with id {}", resultId);
      return new QueryResultRef(resultId, queryType);
    } catch (Exception e) {
      logger.error("Error executing or caching query", e);
      throw e;
    }
  }

  public String formatResult(String resultId, String formatString) {
    QueryCacheEntry cachedEntry = queryResultCache.get(resultId);
    if (cachedEntry == null) return "";

    SerializationFormat format = SerializationFormat.fromString(formatString);
    boolean isRdfFormat = isRdfFormat(format);
    QueryType queryType = cachedEntry.getQueryType();

    if ((queryType == QueryType.CONSTRUCT
            || queryType == QueryType.DESCRIBE
            || queryType == QueryType.UPDATE)
        && !isRdfFormat) {
      return String.format(
          "Error: %s is not a valid RDF format for this query type.", formatString);
    }
    if ((queryType == QueryType.SELECT || queryType == QueryType.ASK) && isRdfFormat) {
      return String.format(
          "Error: %s is not a valid mapping format for this query type.", formatString);
    }

    if (cachedEntry.getGraphResult() != null) {
      return ResultFormatter.getInstance().formatGraph(cachedEntry.getGraphResult(), format);
    }
    if (cachedEntry.getMappingsResult() != null) {
      return ResultFormatter.getInstance().formatMappings(cachedEntry.getMappingsResult(), format);
    }

    return "";
  }

  private boolean isRdfFormat(SerializationFormat format) {
    return Arrays.asList(SerializationFormat.rdfFormats()).contains(format);
  }

  private QueryType determineQueryTypeFromAST(ASTQuery ast) {
    if (ast == null) return QueryType.UNKNOWN;
    if (ast.isUpdate()) return QueryType.UPDATE;
    if (ast.isSelect()) return QueryType.SELECT;
    if (ast.isConstruct()) return QueryType.CONSTRUCT;
    if (ast.isAsk()) return QueryType.ASK;
    if (ast.isDescribe()) return QueryType.DESCRIBE;
    return QueryType.UNKNOWN;
  }

  private static class QueryCacheEntry {
    private final QueryType queryType;
    private final Mappings mappingsResult;
    private final Graph graphResult;

    public QueryCacheEntry(QueryType queryType, Mappings mappings) {
      this.queryType = queryType;
      this.mappingsResult = mappings;
      this.graphResult = null;
    }

    public QueryCacheEntry(QueryType queryType, Graph graph) {
      this.queryType = queryType;
      this.mappingsResult = null;
      this.graphResult = graph;
    }

    public QueryType getQueryType() {
      return queryType;
    }

    public Mappings getMappingsResult() {
      return mappingsResult;
    }

    public Graph getGraphResult() {
      return graphResult;
    }

  }

  public void releaseResult(String resultId) {
    if (resultId != null) {
      queryResultCache.remove(resultId);
    }
  }
}
