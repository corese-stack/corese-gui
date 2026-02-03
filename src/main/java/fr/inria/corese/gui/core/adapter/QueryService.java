package fr.inria.corese.gui.core.adapter;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResultRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for executing SPARQL queries against the shared Corese graph.
 *
 * <p>This service manages the lifecycle of query execution and result caching.
 * It provides:
 * <ul>
 *   <li>Execution of SELECT, ASK, CONSTRUCT, DESCRIBE, and UPDATE queries.</li>
 *   <li>Automatic detection of query types.</li>
 *   <li>Caching of results to support pagination, formatting, and export without re-execution.</li>
 *   <li>Access to formatted results via opaque result IDs.</li>
 * </ul>
 */
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    private static final QueryService INSTANCE = new QueryService();

    private final Map<String, CacheEntry> resultCache;

    private QueryService() {
        this.resultCache = new ConcurrentHashMap<>();
    }

    public static QueryService getInstance() {
        return INSTANCE;
    }

    // ============================================================================================
    // Public API
    // ============================================================================================

    /**
     * Executes a SPARQL query.
     *
     * @param queryString The SPARQL query string.
     * @return A {@link QueryResultRef} containing the result ID and detected query type.
     * @throws Exception If the query is invalid or execution fails.
     */
    public QueryResultRef executeQuery(String queryString) throws Exception {
        if (queryString == null || queryString.isBlank()) {
            throw new IllegalArgumentException("Query string cannot be empty.");
        }

        logger.debug("Executing query...");
        Graph graph = GraphStore.getInstance().getGraph();
        QueryProcess exec = QueryProcess.create(graph);
        
        Mappings mappings = exec.query(queryString);
        ASTQuery ast = mappings.getAST();
        
        QueryType type = detectType(ast);
        String id = UUID.randomUUID().toString();
        
        Graph resultGraph = null;
        if (type == QueryType.CONSTRUCT || type == QueryType.DESCRIBE) {
            Object g = mappings.getGraph();
            if (g instanceof Graph) {
                resultGraph = (Graph) g;
            }
        }
        
        CacheEntry entry = new CacheEntry(type, mappings, resultGraph);
        resultCache.put(id, entry);
        
        logger.info("Query executed. Type: {}, ID: {}, Results: {}", type, id, mappings.size());
        return new QueryResultRef(id, type);
    }

    /**
     * Formats the result associated with the given ID.
     *
     * @param resultId The ID of the cached result.
     * @param format   The desired serialization format.
     * @return The formatted result string, or an error message.
     */
    public String formatResult(String resultId, SerializationFormat format) {
        CacheEntry entry = resultCache.get(resultId);
        if (entry == null) {
            return "Error: Result expired or not found (ID: " + resultId + ")";
        }

        if (entry.type == QueryType.CONSTRUCT || entry.type == QueryType.DESCRIBE) {
            if (entry.graph == null) return "Error: No graph result available.";
            return ResultFormatter.getInstance().formatGraph(entry.graph, format);
        } else {
            return ResultFormatter.getInstance().formatMappings(entry.mappings, format);
        }
    }

    /**
     * Releases a cached result to free memory.
     *
     * @param resultId The ID of the result to release.
     */
    public void releaseResult(String resultId) {
        if (resultId != null) {
            resultCache.remove(resultId);
            logger.debug("Released result ID: {}", resultId);
        }
    }

    // ============================================================================================
    // Internal Helpers
    // ============================================================================================

    private QueryType detectType(ASTQuery ast) {
        if (ast == null) return QueryType.UNKNOWN;
        if (ast.isSelect()) return QueryType.SELECT;
        if (ast.isAsk()) return QueryType.ASK;
        if (ast.isConstruct()) return QueryType.CONSTRUCT;
        if (ast.isDescribe()) return QueryType.DESCRIBE;
        if (ast.isUpdate()) return QueryType.UPDATE;
        return QueryType.UNKNOWN;
    }

    /**
     * Internal cache holder.
     */
    private record CacheEntry(QueryType type, Mappings mappings, Graph graph) {}
}