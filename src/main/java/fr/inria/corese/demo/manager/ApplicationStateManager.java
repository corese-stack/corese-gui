package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.demo.model.FormattedResult;
import fr.inria.corese.demo.model.fileList.FileListModel;
import fr.inria.corese.demo.model.graph.CoreseGraph;
import fr.inria.corese.demo.model.graph.SemanticGraph;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationStateManager {
    private static ApplicationStateManager instance;

    // Core graph components
    private Graph graph;
    private QueryProcess queryProcess;
    // RESTORED: SemanticGraph to support older methods and avoid breaking other
    // classes.
    private final SemanticGraph semanticGraph;

    // Rule management
    private final RuleManager ruleManager;

    // File management
    private final FileListModel fileListModel;
    private final List<File> loadedFiles;
    private String projectPath;

    private final List<String> logEntries;

    // Caching mechanism
    private final Map<Integer, TabCacheEntry> queryResultCache = new HashMap<>();
    private String lastGraphResult = null;

    private ApplicationStateManager() {
        this.semanticGraph = new CoreseGraph();
        this.fileListModel = new FileListModel();
        this.loadedFiles = new ArrayList<>();
        this.logEntries = new ArrayList<>();
        this.ruleManager = new RuleManager(this);
        initializeGraph();
    }

    public static synchronized ApplicationStateManager getInstance() {
        if (instance == null) {
            instance = new ApplicationStateManager();
        }
        return instance;
    }

    private void initializeGraph() {
        this.graph = Graph.create();
        this.queryProcess = QueryProcess.create(graph);
    }

    public void loadFile(File file) throws Exception {
        try {
            Load loader = Load.create(graph);
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".ttl")) {
                loader.parse(file.getAbsolutePath(), Load.format.TURTLE_FORMAT);
            } else if (fileName.endsWith(".rdf") || fileName.endsWith(".xml")) {
                loader.parse(file.getAbsolutePath(), Load.format.RDFXML_FORMAT);
            } else if (fileName.endsWith(".jsonld")) {
                loader.parse(file.getAbsolutePath(), Load.format.JSONLD_FORMAT);
            } else {
                loader.parse(file.getAbsolutePath(), Load.format.TURTLE_FORMAT);
            }
            semanticGraph.loadFile(file);
            fileListModel.addFile(file.getName());
            if (!loadedFiles.contains(file)) {
                loadedFiles.add(file);
            }
            addLogEntry("File loaded successfully: " + file.getName());
            ruleManager.applyRules(this.graph);
        } catch (Exception e) {
            addLogEntry("Error loading file: " + e.getMessage());
            throw e;
        }
    }

    public Mappings executeQuery(String queryString) throws Exception {
        ruleManager.applyRules(this.graph);
        this.queryProcess = QueryProcess.create(this.graph);
        return this.queryProcess.query(queryString);
    }

    public String getFormattedCachedQuery(Integer tabId, String format) throws Exception {
        TabCacheEntry cachedEntry = getCachedResult(tabId);
        if (cachedEntry == null) {
            return "";
        }

        String upperCaseFormat = format.toUpperCase();

        if (cachedEntry.getMappingsResult() != null) {
            switch (upperCaseFormat) {
                case "CSV":
                    return formatMappings(cachedEntry.getMappingsResult(), ResultFormat.format.CSV_FORMAT);
                case "XML":
                    return formatMappings(cachedEntry.getMappingsResult(), ResultFormat.format.XML_FORMAT);
                case "JSON":
                    return formatMappings(cachedEntry.getMappingsResult(), ResultFormat.format.JSON_FORMAT);
                case "TSV":
                    return formatMappings(cachedEntry.getMappingsResult(), ResultFormat.format.TSV_FORMAT);
                case "MARKDOWN":
                    return formatMappings(cachedEntry.getMappingsResult(), ResultFormat.format.MARKDOWN_FORMAT);
                default:
                    return "Format '" + format + "' is not applicable to this query type.";
            }
        }

        if (cachedEntry.getGraphResult() != null) {
            Graph resultGraph = cachedEntry.getGraphResult();
            this.lastGraphResult = "Graph result is being processed...";
            switch (upperCaseFormat) {
                case "TURTLE":
                    this.lastGraphResult = formatGraph(resultGraph, ResultFormat.format.TURTLE_FORMAT);
                    return this.lastGraphResult;
                case "RDF_XML":
                    return formatGraph(resultGraph, ResultFormat.format.RDF_XML_FORMAT);
                case "JSON-LD":
                    return formatGraph(resultGraph, ResultFormat.format.JSONLD_FORMAT);
                case "N-TRIPLES":
                    return formatGraph(resultGraph, ResultFormat.format.NTRIPLES_FORMAT);
                case "N-QUADS":
                    return formatGraph(resultGraph, ResultFormat.format.NQUADS_FORMAT);
                case "TRIG":
                    return formatGraph(resultGraph, ResultFormat.format.TRIG_FORMAT);
                default:
                    return "Format '" + format + "' is not applicable to this query type.";
            }
        }

        return "";
    }
    // In ApplicationStateManager.java

    public void executeAndCacheQuery(String query, Integer tabId) throws Exception {
        try {
            Mappings mappings = executeQuery(query);
            String queryType = determineQueryType(query);
            if (queryType.equals("CONSTRUCT") || queryType.equals("DESCRIBE")) {
                Graph resultGraph = (Graph) mappings.getGraph();
                cacheTabResult(tabId, new TabCacheEntry(queryType, resultGraph));
            } else {
                cacheTabResult(tabId, new TabCacheEntry(queryType, mappings));
            }
        } catch (Exception e) {
            addLogEntry("Error executing or caching query: " + e.getMessage());
            throw e;
        }
    }

    public FormattedResult executeAndFormatSelectQuery(String query, Integer tabId) throws Exception {
        var mappings = executeQuery(query);
        String queryType = determineQueryType(query);

        TabCacheEntry cacheEntry = new TabCacheEntry(queryType, mappings);
        cacheTabResult(tabId, cacheEntry);

        String primaryResult = formatMappings(mappings, ResultFormat.format.CSV_FORMAT);
        Map<String, String> allFormats = new HashMap<>();
        allFormats.put("XML", formatMappings(mappings, ResultFormat.format.XML_FORMAT));
        allFormats.put("JSON", formatMappings(mappings, ResultFormat.format.JSON_FORMAT));
        allFormats.put("CSV", primaryResult);
        allFormats.put("TSV", formatMappings(mappings, ResultFormat.format.TSV_FORMAT));
        allFormats.put("MARKDOWN", formatMappings(mappings, ResultFormat.format.MARKDOWN_FORMAT));

        this.lastGraphResult = null;
        return new FormattedResult(queryType, primaryResult, allFormats);
    }

    public FormattedResult executeAndFormatConstructQuery(String query, Integer tabId) throws Exception {
        var mappings = executeQuery(query);
        Graph resultGraph = (Graph) mappings.getGraph();
        String queryType = determineQueryType(query);

        TabCacheEntry cacheEntry = new TabCacheEntry(queryType, resultGraph);
        cacheTabResult(tabId, cacheEntry);
        String primaryResult = formatGraph(resultGraph, ResultFormat.format.TURTLE_FORMAT);
        Map<String, String> allFormats = Map.of("TURTLE", primaryResult);
        this.lastGraphResult = primaryResult;
        return new FormattedResult(queryType, primaryResult, allFormats);
    }

    public void cacheTabResult(Integer tabHashCode, TabCacheEntry result) {
        queryResultCache.put(tabHashCode, result);
    }

    public TabCacheEntry getCachedResult(Integer tabHashCode) {
        return queryResultCache.get(tabHashCode);
    }

    public void clearCacheForTab(Integer tabHashCode) {
        queryResultCache.remove(tabHashCode);
    }

    public String formatMappings(Mappings mappings, ResultFormat.format format) {
        if (mappings == null || mappings.isEmpty()) {
            return "";
        }
        try {
            return ResultFormat.create(mappings, format).toString();
        } catch (Exception e) {
            addLogEntry("Error formatting query results: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public String formatGraph(Graph graph, ResultFormat.format format) {
        if (graph == null || graph.size() == 0) {
            return "";
        }
        try {
            return ResultFormat.create(graph, format).toString();
        } catch (Exception e) {
            addLogEntry("Error formatting graph results: " + e.getMessage());
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

    public void clearGraph() {
        initializeGraph();
        semanticGraph.clearGraph();
        addLogEntry("Graph cleared");
    }

    public void clearFiles() {
        fileListModel.clearFiles();
        loadedFiles.clear();
    }

    public void reloadFiles() {
        clearGraph();
        for (File file : loadedFiles) {
            try {
                loadFile(file);
            } catch (Exception e) {
                addLogEntry("Error reloading file " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void loadProject(File directory) {
        // Placeholder for future implementation
        addLogEntry("Project loading not fully implemented.");
    }

    public void saveProject(File targetFile) throws Exception {
        String baseName = targetFile.getName();
        if (!baseName.endsWith(".ttl")) {
            baseName += ".ttl";
        }
        File graphFile = new File(targetFile.getParentFile(), baseName);
        try (FileOutputStream out = new FileOutputStream(graphFile)) {
            out.write(graph.toString().getBytes());
        }
        addLogEntry("Graph saved to: " + graphFile.getAbsolutePath());
    }

    public void saveCurrentState() {
        try {
            semanticGraph.saveContext();
            addLogEntry("Current state saved");
        } catch (Exception e) {
            addLogEntry("Error saving current state: " + e.getMessage());
        }
    }

    public void restoreState() {
        try {
            loadedFiles.clear();
            loadedFiles.addAll(semanticGraph.getLoadedFiles());
            fileListModel.clearFiles();
            for (File file : loadedFiles) {
                fileListModel.addFile(file.getName());
            }
            for (File ruleFile : semanticGraph.getLoadedRules()) {
                ruleManager.loadRuleFile(ruleFile);
            }
            ruleManager.applyRules(graph);
            addLogEntry("State restored and rules applied.");
        } catch (Exception e) {
            addLogEntry("Error restoring state: " + e.getMessage());
        }
    }

    public SemanticGraph getSemanticGraph() {
        return this.semanticGraph;
    }

    public int getTripletCount() {
        return semanticGraph.getTripletCount();
    }

    public int getSemanticElementsCount() {
        return semanticGraph.getSemanticElementsCount();
    }

    public int getGraphCount() {
        return semanticGraph.getGraphCount();
    }

    public String getLastGraphResult() {
        return this.lastGraphResult;
    }

    // --- GETTERS ---
    public Graph getGraph() {
        return this.graph;
    }

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    public FileListModel getFileListModel() {
        return fileListModel;
    }

    public List<File> getLoadedFiles() {
        return new ArrayList<>(loadedFiles);
    }

    public List<String> getLogEntries() {
        return new ArrayList<>(logEntries);
    }

    public void addLogEntry(String entry) {
        logEntries.add(entry);
        semanticGraph.addLogEntry(entry);
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