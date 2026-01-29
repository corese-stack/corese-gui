package fr.inria.corese.gui.core.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central manager for executing and caching SPARQL queries per UI tab over the Corese Graph.
 * Implements a thread-safe singleton that: - runs queries via QueryProcess and stores a
 * TabCacheEntry with either Mappings (SELECT/ASK) or a result Graph (CONSTRUCT/DESCRIBE); - formats
 * cached results using ExportManager; - converts external format names to Corese formats and logs
 * operations; - provides cache access and clearing by tab id. Typical flow:
 * executeAndCacheQuery(query, tabId) -> getFormattedCachedQuery(tabId, format) ->
 * clearCacheForTab(tabId).
 */
public class QueryManager {
  private static QueryManager instance;

  private final GraphManager graphManager;
  private QueryProcess queryProcess;

  private final List<String> logEntries;

  private final Map<Integer, TabCacheEntry> queryResultCache = new HashMap<>();

  private QueryManager() {
    this.graphManager = GraphManager.getInstance();
    this.logEntries = new ArrayList<>();
  }

  public static synchronized QueryManager getInstance() {
    if (instance == null) {
      instance = new QueryManager();
    }
    return instance;
  }

  /**
   * Central manager that executes, caches, and formats SPARQL queries per UI tab over a Corese
   * Graph.
   *
   * <p>Features: - Thread-safe singleton (getInstance) using GraphManager and QueryProcess to run
   * queries. - Per-tab caching of results as either Mappings (SELECT/ASK) or Graph
   * (CONSTRUCT/DESCRIBE), with query type derived from the AST. - Result formatting via
   * ExportManager with validation of mapping vs RDF formats. - Utilities to retrieve and clear
   * cached results, plus lightweight logging.
   *
   * <p>Typical flow: executeAndCacheQuery(query, tabId) -> getFormattedCachedQuery(tabId, format)
   * -> clearCacheForTab(tabId).
   */
  public void executeAndCacheQuery(String query, Integer tabId) throws Exception {
    try {
      this.queryProcess = QueryProcess.create(graphManager.getGraph());
      Mappings mappings = this.queryProcess.query(query);
      ASTQuery ast = mappings.getAST();
      String queryType = determineQueryTypeFromAST(ast);

      TabCacheEntry cacheEntry;
      if (ast.isConstruct() || ast.isDescribe()) {
        Graph resultGraph = (Graph) mappings.getGraph();
        cacheEntry = new TabCacheEntry(queryType, resultGraph, ast);
      } else {
        cacheEntry = new TabCacheEntry(queryType, mappings, ast);
      }
      queryResultCache.put(tabId, cacheEntry);
      addLogEntry("Query executed and result cached for tab " + tabId);
    } catch (Exception e) {
      addLogEntry("Error executing or caching query: " + e.getMessage());
      throw e;
    }
  }

  public TabCacheEntry getCachedResult(Integer tabId) {
    return queryResultCache.get(tabId);
  }

  public void clearCacheForTab(Integer tabId) {
    queryResultCache.remove(tabId);
  }

  public String getFormattedCachedQuery(Integer tabId, String formatString) {
    TabCacheEntry cachedEntry = getCachedResult(tabId);
    if (cachedEntry == null) return "";

    ASTQuery ast = cachedEntry.getAst();
    if (ast == null) return "Error: Query AST not found in cache.";

    SerializationFormat format = SerializationFormat.fromString(formatString);
    boolean isRdfFormat = isRdfFormat(format);

    if ((ast.isConstruct() || ast.isDescribe() || ast.isUpdate()) && !isRdfFormat) {
      return String.format(
          "Error: %s is not a valid RDF format for this query type.", formatString);
    }
    if ((ast.isSelect() || ast.isAsk()) && isRdfFormat) {
      return String.format(
          "Error: %s is not a valid mapping format for this query type.", formatString);
    }

    if (cachedEntry.getGraphResult() != null) {
      return ExportManager.getInstance().formatGraph(cachedEntry.getGraphResult(), format);
    }
    if (cachedEntry.getMappingsResult() != null) {
      return ExportManager.getInstance().formatMappings(cachedEntry.getMappingsResult(), format);
    }

    return "";
  }

  private boolean isRdfFormat(SerializationFormat format) {
    return Arrays.asList(SerializationFormat.rdfFormats()).contains(format);
  }

  private String determineQueryTypeFromAST(ASTQuery ast) {
    if (ast.isSelect()) return "SELECT";
    if (ast.isConstruct()) return "CONSTRUCT";
    if (ast.isAsk()) return "ASK";
    if (ast.isDescribe()) return "DESCRIBE";
    return "UNKNOWN";
  }

  public void addLogEntry(String entry) {
    logEntries.add(entry);
  }

  public List<String> getLogEntries() {
    return new ArrayList<>(logEntries);
  }

  public static class TabCacheEntry {
    private final String queryType;
    private final Mappings mappingsResult;
    private final Graph graphResult;
    private final ASTQuery ast;

    public TabCacheEntry(String queryType, Mappings mappings, ASTQuery ast) {
      this.queryType = queryType;
      this.mappingsResult = mappings;
      this.graphResult = null;
      this.ast = ast;
    }

    public TabCacheEntry(String queryType, Graph graph, ASTQuery ast) {
      this.queryType = queryType;
      this.mappingsResult = null;
      this.graphResult = graph;
      this.ast = ast;
    }

    public String getQueryType() {
      return queryType;
    }

    public Mappings getMappingsResult() {
      return mappingsResult;
    }

    public Graph getGraphResult() {
      return graphResult;
    }

    public ASTQuery getAst() {
      return ast;
    }
  }
}
