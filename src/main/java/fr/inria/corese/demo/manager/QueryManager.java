package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.demo.model.fileList.FileListModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    
    
    public void executeAndCacheQuery(String query, Integer tabId) throws Exception {
        try {
            this.queryProcess = QueryProcess.create(graphManager.getGraph());
            Mappings mappings = this.queryProcess.query(query);
            String queryType = determineQueryType(query);
            TabCacheEntry cacheEntry;

            if (queryType.equals("CONSTRUCT") || queryType.equals("DESCRIBE")) {
                Graph resultGraph = (Graph) mappings.getGraph();
                cacheEntry = new TabCacheEntry(queryType, resultGraph);
            } else {
                cacheEntry = new TabCacheEntry(queryType, mappings);
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
        if (cachedEntry == null)
            return "";

        String upperCaseFormat = format.toUpperCase().replace("-", "_");

        ResultFormat.format coreseFormat;

        if (cachedEntry.getMappingsResult() != null) {
            coreseFormat = switch (upperCaseFormat) {
                case "XML" -> ResultFormat.format.XML_FORMAT;
                case "JSON" -> ResultFormat.format.JSON_FORMAT;
                case "CSV" -> ResultFormat.format.CSV_FORMAT;
                case "TSV" -> ResultFormat.format.TSV_FORMAT;
                case "MARKDOWN" -> ResultFormat.format.MARKDOWN_FORMAT;
                default -> null;
            };
            return (coreseFormat != null) ? formatMappings(cachedEntry.getMappingsResult(), coreseFormat)
                    : "Unsupported format for SELECT/ASK.";
        }

        if (cachedEntry.getGraphResult() != null) {
            coreseFormat = switch (upperCaseFormat) {
                case "TURTLE" -> ResultFormat.format.TURTLE_FORMAT;
                case "RDF_XML" -> ResultFormat.format.RDF_XML_FORMAT;
                case "JSON_LD" -> ResultFormat.format.JSONLD_FORMAT;
                case "N_TRIPLES" -> ResultFormat.format.NTRIPLES_FORMAT;
                case "N_QUADS" -> ResultFormat.format.NQUADS_FORMAT;
                case "TRIG" -> ResultFormat.format.TRIG_FORMAT;
                default -> null;
            };
            return (coreseFormat != null) ? formatGraph(cachedEntry.getGraphResult(), coreseFormat)
                    : "Unsupported format for CONSTRUCT/DESCRIBE.";
        }
        return "";
    }

    public String formatMappings(Mappings mappings, ResultFormat.format format) {
        if (mappings == null || mappings.isEmpty())
            return "";
        try {
            return ResultFormat.create(mappings, format).toString();
        } catch (Exception e) {
            addLogEntry("Error formatting Mappings: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public String formatGraph(Graph graph, ResultFormat.format format) {
        if (graph == null || graph.size() == 0)
            return "";
        try {
            return ResultFormat.create(graph, format).toString();
        } catch (Exception e) {
            addLogEntry("Error formatting Graph: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public String determineQueryType(String queryString) {
        String cleanedQuery = queryString.trim().toUpperCase();
        while (cleanedQuery.startsWith("PREFIX")) {
            int endOfPrefix = cleanedQuery.indexOf(">");
            if (endOfPrefix != -1) {
                cleanedQuery = cleanedQuery.substring(endOfPrefix + 1).trim();
            } else {
                break;
            }
        }
        if (cleanedQuery.startsWith("SELECT"))
            return "SELECT";
        if (cleanedQuery.startsWith("CONSTRUCT"))
            return "CONSTRUCT";
        if (cleanedQuery.startsWith("ASK"))
            return "ASK";
        if (cleanedQuery.startsWith("DESCRIBE"))
            return "DESCRIBE";
        return "UNKNOWN";
    }

    
    
    public void addLogEntry(String entry) {
        logEntries.add(entry);
        System.out.println("[LOG] " + entry);
    }

    public List<String> getLogEntries() {
        return new ArrayList<>(logEntries);
    }

    public static class TabCacheEntry {
        private final String queryType;
        private final Mappings mappingsResult;
        private final Graph graphResult;

        public TabCacheEntry(String queryType, Mappings mappings) {
            this.queryType = queryType;
            this.mappingsResult = mappings;
            this.graphResult = null;
        }

        public TabCacheEntry(String queryType, Graph graph) {
            this.queryType = queryType;
            this.mappingsResult = null;
            this.graphResult = graph;
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
    }
}