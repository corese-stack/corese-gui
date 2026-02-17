package fr.inria.corese.gui.core.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.gui.core.enums.QueryType;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.model.QueryResultRef;

/**
 * Service for executing SPARQL queries against the shared Corese graph.
 *
 * <p>
 * This service manages the lifecycle of query execution and result caching,
 * providing:
 * <ul>
 * <li>Execution of SELECT, ASK, CONSTRUCT, DESCRIBE, and UPDATE queries</li>
 * <li>Automatic query type detection</li>
 * <li>Result caching for pagination, formatting, and export without
 * re-execution</li>
 * <li>Memory-efficient result management with explicit release</li>
 * </ul>
 *
 * <p>
 * The Singleton pattern is justified here because:
 * <ul>
 * <li>Query execution must be coordinated to prevent conflicts</li>
 * <li>Result cache should be centrally managed for the entire application</li>
 * <li>Ensures consistent query processing and result formatting</li>
 * </ul>
 *
 * <p>
 * Example usage:
 *
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

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryService.class);
	private static final QueryService INSTANCE = new QueryService();
	private static final int QUERY_PREVIEW_MAX_CHARS = 180;

	private final Map<String, CacheEntry> resultCache;
	private final GraphActivityLogService activityLogService;

	// ==============================================================================================
	// Constructor
	// ==============================================================================================

	private QueryService() {
		this.resultCache = new ConcurrentHashMap<>();
		this.activityLogService = GraphActivityLogService.getInstance();
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
	 * The query type is automatically detected, and results are cached for
	 * subsequent formatting. The returned reference contains a unique ID that can
	 * be used to access formatted results.
	 *
	 * @param queryString
	 *            The SPARQL query string.
	 * @return A {@link QueryResultRef} containing the result ID and detected query
	 *         type.
	 * @throws IllegalArgumentException
	 *             if the query string is null or empty.
	 * @throws QueryExecutionException
	 *             if the query is invalid or execution fails.
	 */
	@SuppressWarnings("java:S2139")
	public QueryResultRef executeQuery(String queryString) {
		validateQueryString(queryString);
		LOGGER.debug("Executing SPARQL query...");

		try {
			QueryExecutionSnapshot snapshot = executeAndBuildSnapshot(queryString);
			cacheQueryResult(snapshot);
			logQueryExecutionSuccess(snapshot);
			return snapshot.toResultRef();
		} catch (EngineException | RuntimeException e) {
			String errorMsg = String.format("Query execution failed: %s", e.getMessage());
			LOGGER.error(errorMsg, e);
			throw new QueryExecutionException(errorMsg, e);
		}
	}

	private static void validateQueryString(String queryString) {
		if (queryString == null || queryString.isBlank()) {
			throw new IllegalArgumentException("Query string cannot be empty.");
		}
	}

	private QueryExecutionSnapshot executeAndBuildSnapshot(String queryString) throws EngineException {
		Graph graph = GraphStoreService.getInstance().getGraph();
		int graphSizeBefore = graph.size();
		QueryProcess exec = QueryProcess.create(graph);
		Mappings mappings = executeWithLoadFallback(exec, queryString);
		QueryType type = detectType(mappings.getAST());
		Graph resultGraph = resolveResultGraph(type, mappings);
		Boolean askResult = type == QueryType.ASK ? resolveAskResult(mappings) : null;
		int[] updateDelta = resolveUpdateDelta(type, mappings, graph, graphSizeBefore, queryString);
		int resultCount = computeResultCount(type, mappings, resultGraph);
		return new QueryExecutionSnapshot(UUID.randomUUID().toString(), type, mappings, resultGraph, askResult,
				updateDelta[0], updateDelta[1], resultCount);
	}

	private void cacheQueryResult(QueryExecutionSnapshot snapshot) {
		CacheEntry entry = new CacheEntry(snapshot.type(), snapshot.mappings(), snapshot.resultGraph());
		resultCache.put(snapshot.id(), entry);
	}

	private void logQueryExecutionSuccess(QueryExecutionSnapshot snapshot) {
		LOGGER.info("Query executed successfully. Type: {}, ID: {}, Results: {}", snapshot.type(), snapshot.id(),
				snapshot.mappings().size());
	}

	/**
	 * Formats the result associated with the given ID.
	 *
	 * @param resultId
	 *            The ID of the cached result.
	 * @param format
	 *            The desired serialization format.
	 * @return The formatted result string, or an error message.
	 */
	public String formatResult(String resultId, SerializationFormat format) {
		CacheEntry entry = resultCache.get(resultId);
		if (entry == null) {
			return "Error: Result expired or not found (ID: " + resultId + ")";
		}

		if (entry.type == QueryType.CONSTRUCT || entry.type == QueryType.DESCRIBE) {
			if (entry.graph == null)
				return "Error: No graph result available.";
			return ResultFormatter.getInstance().formatGraph(entry.graph, format);
		} else {
			return ResultFormatter.getInstance().formatMappings(entry.mappings, format);
		}
	}

	/**
	 * Releases a cached result to free memory.
	 *
	 * <p>
	 * This should be called when the result is no longer needed to prevent memory
	 * leaks.
	 *
	 * @param resultId
	 *            The ID of the result to release.
	 */
	public void releaseResult(String resultId) {
		if (resultId != null) {
			resultCache.remove(resultId);
			LOGGER.debug("Released result ID: {}", resultId);
		}
	}

	// ==============================================================================================
	// Private Methods
	// ==============================================================================================

	/**
	 * Detects the query type from the AST.
	 */
	private QueryType detectType(ASTQuery ast) {
		if (ast == null)
			return QueryType.UNKNOWN;
		if (ast.isSelect())
			return QueryType.SELECT;
		if (ast.isAsk())
			return QueryType.ASK;
		if (ast.isConstruct())
			return QueryType.CONSTRUCT;
		if (ast.isDescribe())
			return QueryType.DESCRIBE;
		if (ast.isUpdate())
			return QueryType.UPDATE;
		return QueryType.UNKNOWN;
	}

	private int computeResultCount(QueryType type, Mappings mappings, Graph resultGraph) {
		if (type == QueryType.SELECT) {
			return Math.max(0, mappings.size());
		}
		if (type == QueryType.CONSTRUCT || type == QueryType.DESCRIBE) {
			if (resultGraph != null) {
				return Math.max(0, resultGraph.size());
			}
			return Math.max(0, mappings.size());
		}
		return 0;
	}

	private static boolean resolveAskResult(Mappings mappings) {
		// In Corese ASK is represented as non-empty mappings=true, empty=false.
		return mappings != null && mappings.size() > 0;
	}

	private int[] resolveUpdateDelta(QueryType type, Mappings mappings, Graph graph, int graphSizeBefore,
			String query) {
		if (type != QueryType.UPDATE || mappings == null || graph == null) {
			return new int[]{0, 0};
		}
		int insertedTriples = Math.max(0, mappings.nbInsert());
		int deletedTriples = Math.max(0, mappings.nbDelete());
		if (insertedTriples == 0 && deletedTriples == 0) {
			int delta = graph.size() - graphSizeBefore;
			if (delta > 0) {
				insertedTriples = delta;
			} else if (delta < 0) {
				deletedTriples = -delta;
			}
		}
		logUpdateActivity(query, insertedTriples, deletedTriples);
		return new int[]{insertedTriples, deletedTriples};
	}

	private static Graph resolveResultGraph(QueryType type, Mappings mappings) {
		if (type != QueryType.CONSTRUCT && type != QueryType.DESCRIBE) {
			return null;
		}
		if (mappings == null) {
			return null;
		}
		Object graphValue = mappings.getGraph();
		return graphValue instanceof Graph constructedGraph ? constructedGraph : null;
	}

	private Mappings executeWithLoadFallback(QueryProcess exec, String queryString) throws EngineException {
		String preprocessedQuery = DemoHttpFallbackSupport.rewriteLoadUrisToHttp(queryString);
		boolean usingPreprocessedLoadUris = preprocessedQuery != null && !preprocessedQuery.equals(queryString);
		if (usingPreprocessedLoadUris) {
			LOGGER.info("Applying HTTP fallback to known demo LOAD URIs before query execution.");
		}

		try {
			return exec.query(preprocessedQuery);
		} catch (EngineException primaryFailure) {
			if (usingPreprocessedLoadUris) {
				throw primaryFailure;
			}
			if (!DemoHttpFallbackSupport.isSslHandshakeFailure(primaryFailure)) {
				throw primaryFailure;
			}
			String fallbackQuery = DemoHttpFallbackSupport.rewriteLoadUrisToHttp(queryString);
			if (fallbackQuery == null || fallbackQuery.equals(queryString)) {
				throw primaryFailure;
			}
			LOGGER.warn("TLS validation failed during query LOAD. Retrying known demo URIs with HTTP fallback.");
			try {
				return exec.query(fallbackQuery);
			} catch (EngineException fallbackFailure) {
				fallbackFailure.addSuppressed(primaryFailure);
				throw fallbackFailure;
			}
		}
	}

	private void logUpdateActivity(String queryString, int insertedTriples, int deletedTriples) {
		String action = "Executed SPARQL update";
		String details = previewQuery(queryString);
		activityLogService.log(GraphActivityLogEntry.Source.QUERY_SERVICE, action, details, insertedTriples,
				deletedTriples);
	}

	private static String previewQuery(String queryString) {
		if (queryString == null || queryString.isBlank()) {
			return "";
		}
		String normalized = queryString.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim()
				.replaceAll("\\s{2,}", " ");
		if (normalized.length() <= QUERY_PREVIEW_MAX_CHARS) {
			return normalized;
		}
		return normalized.substring(0, QUERY_PREVIEW_MAX_CHARS - 3) + "...";
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
	private record CacheEntry(QueryType type, Mappings mappings, Graph graph) {
	}

	private record QueryExecutionSnapshot(String id, QueryType type, Mappings mappings, Graph resultGraph,
			Boolean askResult, int insertedTriples, int deletedTriples, int resultCount) {
		private QueryResultRef toResultRef() {
			return new QueryResultRef(id, type, askResult, insertedTriples, deletedTriples, resultCount);
		}
	}
}
