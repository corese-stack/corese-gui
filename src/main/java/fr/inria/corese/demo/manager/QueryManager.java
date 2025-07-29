package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.demo.model.fileList.FileListModel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryManager {
    private static QueryManager instance;

    private Graph graph;
    private QueryProcess queryProcess;

    private final FileListModel fileListModel;
    private final List<File> loadedFiles;
    private final List<String> logEntries;
    private List<File> savedLoadedFiles;

    private final Map<Integer, TabCacheEntry> queryResultCache = new HashMap<>();
    private String lastGraphResult = null;

    private QueryManager() {
        this.fileListModel = new FileListModel();
        this.loadedFiles = new ArrayList<>();
        this.logEntries = new ArrayList<>();
        this.savedLoadedFiles = new ArrayList<>();
        initializeGraph();
    }

    public static synchronized QueryManager getInstance() {
        if (instance == null) {
            instance = new QueryManager();
        }
        return instance;
    }

    private void initializeGraph() {
        this.graph = Graph.create();
        this.queryProcess = QueryProcess.create(graph);
        this.fileListModel.clearFiles();
        this.loadedFiles.clear();
        addLogEntry("Graph cleared and initialized.");
    }

    public Graph getGraph() {
        return this.graph;
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
            fileListModel.addFile(file.getName());
            if (!loadedFiles.contains(file)) {
                loadedFiles.add(file);
            }
            addLogEntry("File loaded successfully: " + file.getName());
        } catch (Exception e) {
            addLogEntry("Error loading file: " + e.getMessage());
            throw e;
        }
    }

    public void clearGraphAndFiles() {
        initializeGraph();
    }

    public void reloadFiles() {
        List<File> filesToReload = new ArrayList<>(this.loadedFiles);
        initializeGraph();
        addLogEntry("Reloading all files...");
        for (File file : filesToReload) {
            try {
                loadFile(file);
            } catch (Exception e) {
                addLogEntry("Error reloading file " + file.getName() + ": " + e.getMessage());
            }
        }
        addLogEntry("All files reloaded.");
    }

    public void saveGraph(File targetFile) throws Exception {
        String baseName = targetFile.getName();
        if (!baseName.toLowerCase().endsWith(".ttl")) {
            baseName += ".ttl";
        }
        File graphFile = new File(targetFile.getParentFile(), baseName);
        try (FileOutputStream out = new FileOutputStream(graphFile)) {
            String turtleRepresentation = formatGraph(this.graph, ResultFormat.format.TURTLE_FORMAT);
            out.write(turtleRepresentation.getBytes());
        }
        addLogEntry("Graph saved to: " + graphFile.getAbsolutePath());
    }

    public void saveCurrentState() {
        this.savedLoadedFiles = new ArrayList<>(this.loadedFiles);
        addLogEntry("Current state snapshot saved (" + savedLoadedFiles.size() + " files).");
    }

    public void restoreState() {
        this.loadedFiles.clear();
        this.loadedFiles.addAll(this.savedLoadedFiles);
        reloadFiles();
        this.fileListModel.clearFiles();
        for (File file : this.loadedFiles) {
            this.fileListModel.addFile(file.getName());
        }
        addLogEntry("State restored from snapshot (" + loadedFiles.size() + " files).");
    }

    public void executeAndCacheQuery(String query, Integer tabId) throws Exception {
        try {
            this.queryProcess = QueryProcess.create(this.graph);
            Mappings mappings = this.queryProcess.query(query);
            String queryType = determineQueryType(query);
            TabCacheEntry cacheEntry;

            if (queryType.equals("CONSTRUCT") || queryType.equals("DESCRIBE")) {
                Graph resultGraph = (Graph) mappings.getGraph();
                this.lastGraphResult = formatGraph(resultGraph, ResultFormat.format.TURTLE_FORMAT);
                cacheEntry = new TabCacheEntry(queryType, resultGraph);
            } else {
                this.lastGraphResult = null;
                cacheEntry = new TabCacheEntry(queryType, mappings);
            }
            queryResultCache.put(tabId, cacheEntry);
            addLogEntry("Query executed and result cached for tab " + tabId);
        } catch (Exception e) {
            this.lastGraphResult = null;
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

    public FileListModel getFileListModel() {
        return fileListModel;
    }

    public List<File> getLoadedFiles() {
        return new ArrayList<>(loadedFiles);
    }

    public int getTripletCount() {
        return (this.graph != null) ? this.graph.size() : 0;
    }

    public int getSemanticElementsCount() {
        return getTripletCount();
    }

    public int getGraphCount() {
        return 1;
    }

    public String getLastGraphResult() {
        return this.lastGraphResult;
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