package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central manager for executing and caching SPARQL queries per UI tab over the Corese Graph.
 * Implements a thread-safe singleton that: - runs queries via QueryProcess and stores a
 * TabCacheEntry with either Mappings (SELECT/ASK) or a result Graph (CONSTRUCT/DESCRIBE); - formats
 * cached results using ResultFormat, enforcing mapping vs RDF format compatibility (custom CSV
 * supported for mappings); - converts external format names to Corese formats and logs operations;
 * - provides cache access and clearing by tab id. Typical flow: executeAndCacheQuery(query, tabId)
 * -> getFormattedCachedQuery(tabId, format) -> clearCacheForTab(tabId).
 */
public class QueryManager {
  private static final Logger logger = LoggerFactory.getLogger(QueryManager.class);
  private static QueryManager instance;

  private final GraphManager graphManager;
  private QueryProcess queryProcess;

  private final List<String> logEntries;

  private final Map<Integer, TabCacheEntry> queryResultCache = new HashMap<>();

  private static final Set<ResultFormat.format> RDF_FORMATS =
      Set.of(
          ResultFormat.format.TURTLE_FORMAT,
          ResultFormat.format.RDF_XML_FORMAT,
          ResultFormat.format.JSONLD_FORMAT,
          ResultFormat.format.NTRIPLES_FORMAT,
          ResultFormat.format.NQUADS_FORMAT,
          ResultFormat.format.TRIG_FORMAT);

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
   * ResultFormat with validation of mapping vs RDF formats; includes a custom CSV formatter for
   * mappings. - Utilities to retrieve and clear cached results, plus lightweight logging.
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

  public String getFormattedCachedQuery(Integer tabId, String format) {
    TabCacheEntry cachedEntry = getCachedResult(tabId);
    if (cachedEntry == null) return "";

    ASTQuery ast = cachedEntry.getAst();
    if (ast == null) return "Error: Query AST not found in cache.";

    ResultFormat.format coreseFormat = getCoreseFormat(format);
    if (coreseFormat == null) return "Unsupported format: " + format;

    boolean isRdfFormat = RDF_FORMATS.contains(coreseFormat);
    if ((ast.isConstruct() || ast.isDescribe() || ast.isUpdate()) && !isRdfFormat) {
      return String.format("Error: %s is not a valid RDF format for this query type.", format);
    }
    if ((ast.isSelect() || ast.isAsk()) && isRdfFormat) {
      return String.format("Error: %s is not a valid mapping format for this query type.", format);
    }

    if (cachedEntry.getGraphResult() != null) {
      return formatGraph(cachedEntry.getGraphResult(), coreseFormat);
    }
    if (cachedEntry.getMappingsResult() != null) {
      return formatMappings(cachedEntry.getMappingsResult(), coreseFormat);
    }

    return "";
  }

  private ResultFormat.format getCoreseFormat(String formatName) {
    return switch (formatName.toUpperCase().replace("-", "_")) {
      case "XML" -> ResultFormat.format.XML_FORMAT;
      case "JSON" -> ResultFormat.format.JSON_FORMAT;
      case "CSV" -> ResultFormat.format.CSV_FORMAT;
      case "TSV" -> ResultFormat.format.TSV_FORMAT;
      case "MARKDOWN" -> ResultFormat.format.MARKDOWN_FORMAT;
      case "TURTLE" -> ResultFormat.format.TURTLE_FORMAT;
      case "RDF_XML" -> ResultFormat.format.RDF_XML_FORMAT;
      case "JSON_LD" -> ResultFormat.format.JSONLD_FORMAT;
      case "N_TRIPLES" -> ResultFormat.format.NTRIPLES_FORMAT;
      case "N_QUADS" -> ResultFormat.format.NQUADS_FORMAT;
      case "TRIG" -> ResultFormat.format.TRIG_FORMAT;
      default -> null;
    };
  }

  public String formatMappings(Mappings mappings, ResultFormat.format format) {
    if (mappings == null) return "";
    if (format == ResultFormat.format.CSV_FORMAT) {
      return formatMappingsAsCSV(mappings);
    }
    if (mappings.isEmpty()) return "";
    try {
      return ResultFormat.create(mappings, format).toString();
    } catch (Exception e) {
      addLogEntry("Error formatting Mappings: " + e.getMessage());
      return "Error: " + e.getMessage();
    }
  }

  private String formatMappingsAsCSV(Mappings mappings) {
    if (mappings == null || mappings.getAST() == null) {
      return "";
    }
    StringBuilder csv = new StringBuilder();
    List<Variable> variables = mappings.getAST().getSelect();

    // Header
    for (int i = 0; i < variables.size(); i++) {
      csv.append(variables.get(i).getName().substring(1)); // remove leading '?'
      if (i < variables.size() - 1) {
        csv.append(',');
      }
    }
    csv.append("\n");

    // Rows
    mappings.forEach(
        mapping -> {
          for (int i = 0; i < variables.size(); i++) {
            Variable var = variables.get(i);
            fr.inria.corese.core.kgram.api.core.Node node = mapping.getNode(var);
            if (node != null) {
              csv.append(node.getLabel());
            }
            if (i < variables.size() - 1) {
              csv.append(',');
            }
          }
          csv.append("\n");
        });

    return csv.toString();
  }

  public String formatGraph(Graph graph, ResultFormat.format format) {
    if (graph == null || graph.size() == 0) return "";
    try {
      return ResultFormat.create(graph, format).toString();
    } catch (Exception e) {
      addLogEntry("Error formatting Graph: " + e.getMessage());
      return "Error: " + e.getMessage();
    }
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
    logger.debug("[QueryManager] {}", entry);
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
