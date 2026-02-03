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
 * <p>
 * This service manages the lifecycle of query execution and result caching, providing:
 * <ul>
 *   <li>Execution of SELECT, ASK, CONSTRUCT, DESCRIBE, and UPDATE queries</li>
 *   <li>Automatic query type detection</li>
 *   <li>Result caching for pagination, formatting, and export without re-execution</li>
 *   <li>Memory-efficient result management with explicit release</li>
 * </ul>
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 *   <li>Query execution must be coordinated to prevent conflicts</li>
 *   <li>Result cache should be centrally managed for the entire application</li>
 *   <li>Ensures consistent query processing and result formatting</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * QueryService service = QueryService.getInstance();
 * QueryResultRef result = service.executeQuery("SELECT * WHERE { ?s ?p ?o }");
 * String formatted = service.formatResult(result.id(), SerializationFormat.JSON);
 * service.releaseResult(result.id()); // Free memory when done
 * }</pre>
 */
@SuppressWarnings("java:S6548") // Singleton pattern is justified for query coordination
public class QueryService {

    // ==============================================================================================
    // Fields
    // ==============================================================================================

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    private static final QueryService INSTANCE = new QueryService();

    private final Map<String, CacheEntry> resultCache;

    // ==============================================================================================
    // Constructor
    // ==============================================================================================

    private QueryService() {
        this.resultCache = new ConcurrentHashMap<>();
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    /**
     * Returns the singleton instance of the query service.
     *
     * @return The QueryService instance.
     */
    public static QueryService getInstance() {
        return INSTANCE;
    }

    /**
     * Executes a SPARQL query against the shared graph.
     *
     * <p>
     * The query type is automatically detected, and results are cached for subsequent formatting.
     * The returned reference contains a unique ID that can be used to access formatted results.
     *
     * @param queryString The SPARQL query string.
     * @return A {@link QueryResultRef} containing the result ID and detected query type.
     * @throws IllegalArgumentException if the query string is null or empty.
     * @throws QueryExecutionException if the query is invalid or execution fails.
     */
    @SuppressWarnings("java:S2139")
    public QueryResultRef executeQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            throw new IllegalArgumentException("Query string cannot be empty.");
        }

        logger.debug("Executing SPARQL query...");
        
        try {
            Graph graph = GraphStore.getInstance().getGraph();
            QueryProcess exec = QueryProcess.create(graph);
            
            Mappings mappings = exec.query(queryString);
            ASTQuery ast = mappings.getAST();
            
            QueryType type = detectType(ast);
            String id = UUID.randomUUID().toString();
            
            Graph resultGraph = null;
            if (type == QueryType.CONSTRUCT || type == QueryType.DESCRIBE) {
                Object g = mappings.getGraph();
                if (g instanceof Graph constructedGraph) {
                    resultGraph = constructedGraph;
                }
            }
            
            CacheEntry entry = new CacheEntry(type, mappings, resultGraph);
            resultCache.put(id, entry);
            
            logger.info("Query executed successfully. Type: {}, ID: {}, Results: {}", type, id, mappings.size());
            return new QueryResultRef(id, type);
            
        } catch (Exception e) { // Generic catch is justified: Corese can throw various exception types
            String errorMsg = String.format("Query execution failed: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new QueryExecutionException(errorMsg, e);
        }
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
     * <p>
     * This should be called when the result is no longer needed to prevent memory leaks.
     *
     * @param resultId The ID of the result to release.
     */
    public void releaseResult(String resultId) {
        if (resultId != null) {
            resultCache.remove(resultId);
            logger.debug("Released result ID: {}", resultId);
        }
    }

    // ==============================================================================================
    // Private Methods
    // ==============================================================================================

    /**
     * Detects the query type from the AST.
     */
    private QueryType detectType(ASTQuery ast) {
        if (ast == null) return QueryType.UNKNOWN;
        if (ast.isSelect()) return QueryType.SELECT;
        if (ast.isAsk()) return QueryType.ASK;
        if (ast.isConstruct()) return QueryType.CONSTRUCT;
        if (ast.isDescribe()) return QueryType.DESCRIBE;
        if (ast.isUpdate()) return QueryType.UPDATE;
        return QueryType.UNKNOWN;
    }

    // ==============================================================================================
    // Exception Classes
    // ==============================================================================================

    /**
     * Exception thrown when SPARQL query execution fails.
     */
    public static class QueryExecutionException extends RuntimeException {
        public QueryExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ==============================================================================================
    // Inner Classes
    // ==============================================================================================

    /**
     * Internal cache entry holder.
     */
    private record CacheEntry(QueryType type, Mappings mappings, Graph graph) {}
}