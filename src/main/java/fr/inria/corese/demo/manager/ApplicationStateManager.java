package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.load.RuleLoad;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.rule.RuleEngine;
import fr.inria.corese.demo.model.fileList.FileItem;
import fr.inria.corese.demo.model.fileList.FileListModel;
import fr.inria.corese.demo.model.graph.CoreseGraph;
import fr.inria.corese.demo.model.graph.SemanticGraph;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Application state manager that centralizes all state management.
 * This class follows the Singleton pattern and serves as the single source of truth
 * for application data and state.
 */
public class ApplicationStateManager {
    private static ApplicationStateManager instance;

    // Core graph components
    private Graph graph;
    private QueryProcess queryProcess;
    private final SemanticGraph semanticGraph;
    private RuleEngine ruleEngine;

    // File management
    private final FileListModel fileListModel;
    private final List<File> loadedFiles;
    private final List<File> loadedRuleFiles;
    private String projectPath;

    // Rule states
    private boolean rdfsSubsetEnabled;
    private boolean rdfsRLEnabled;
    private boolean owlRLEnabled;
    private boolean owlRLExtendedEnabled;
    private boolean owlRLTestEnabled;
    private boolean owlCleanEnabled;
    private final Map<String, Boolean> customRuleStates;

    // Logging
    private final List<String> logEntries;
    
    private fr.inria.corese.core.kgram.core.Mappings lastSelectQueryMappings;

    /**
     * Private constructor to enforce singleton pattern.
     * Initializes all necessary components and state.
     */
    private ApplicationStateManager() {
        this.semanticGraph = new CoreseGraph();
        this.fileListModel = new FileListModel();
        this.loadedFiles = new ArrayList<>();
        this.loadedRuleFiles = new ArrayList<>();
        this.customRuleStates = new HashMap<>();
        this.logEntries = new ArrayList<>();

        // Initialize the graph and related components
        initializeGraph();
    }

    /**
     * Gets the singleton instance of ApplicationStateManager.
     *
     * @return The singleton instance
     */
    public static synchronized ApplicationStateManager getInstance() {
        if (instance == null) {
            instance = new ApplicationStateManager();
        }
        return instance;
    }

    /**
     * Initializes the graph and associated query and rule engines.
     */
    private void initializeGraph() {
        this.graph = Graph.create();
        this.queryProcess = QueryProcess.create(graph);
        this.ruleEngine = RuleEngine.create(graph);
    }

    /**
     * Loads a file into the graph and updates the state.
     *
     * @param file The file to load
     * @throws Exception If an error occurs during loading
     */
    public void loadFile(File file) throws Exception {
        try {
            // Create a loader with the appropriate format based on file extension
            Load loader = Load.create(graph);
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(".ttl")) {
                loader.parse(file.getAbsolutePath(), Load.format.TURTLE_FORMAT);
            } else if (fileName.endsWith(".rdf") || fileName.endsWith(".xml")) {
                loader.parse(file.getAbsolutePath(), Load.format.RDFXML_FORMAT);
            } else if (fileName.endsWith(".jsonld")) {
                loader.parse(file.getAbsolutePath(), Load.format.JSONLD_FORMAT);
            } else {
                // Default to Turtle
                loader.parse(file.getAbsolutePath(), Load.format.TURTLE_FORMAT);
            }

            // Add to the semantic graph and file list
            semanticGraph.loadFile(file);
            fileListModel.addFile(file.getName());

            // Update the loaded files list
            if (!loadedFiles.contains(file)) {
                loadedFiles.add(file);
            }

            addLogEntry("File loaded successfully: " + file.getName());

            // Apply rules after loading
            processRules();
            addLogEntry("Applied " + getLoadedRulesCount() + " rules to the graph");

        } catch (Exception e) {
            addLogEntry("Error loading file: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Loads a rule file into the rule engine and updates the state.
     *
     * @param file The rule file to load
     * @throws Exception If an error occurs during loading
     */
    public void loadRuleFile(File file) throws Exception {
        try {
            // Load the rule file into the rule engine
            RuleLoad ruleLoader = RuleLoad.create(ruleEngine);
            ruleLoader.parse(file.getAbsolutePath());

            // Update the semantic graph
            semanticGraph.loadRuleFile(file);

            // Update the loaded rule files list
            if (!loadedRuleFiles.contains(file)) {
                loadedRuleFiles.add(file);
            }

            // Mark this rule as enabled
            customRuleStates.put(file.getName(), true);

            addLogEntry("Rule file loaded successfully: " + file.getName());

            // Process rules
            ruleEngine.process();

        } catch (Exception e) {
            addLogEntry("Error loading rule file: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a SPARQL query on the graph.
     *
     * @param queryString The SPARQL query to execute
     * @return An array containing the formatted result, the query type, and for SELECT queries, the mappings object
     * @throws Exception If an error occurs during query execution
     */
    public Object[] executeQuery(String queryString) throws Exception {
        String queryType = determineQueryType(queryString);
        fr.inria.corese.core.kgram.core.Mappings mappings = queryProcess.query(queryString);
        Object formattedResult;

        switch (queryType) {
            case "SELECT" -> {
                this.lastSelectQueryMappings = mappings;
                formattedResult = ResultFormat.create(mappings,
                        ResultFormat.format.CSV_FORMAT).toString();
                addLogEntry("SELECT query executed successfully");
                // Return mappings as third element for SELECT
                return new Object[]{formattedResult, queryType, mappings};
            }
            case "CONSTRUCT" -> {
                Graph resultGraph = (Graph) mappings.getGraph();
                formattedResult = resultGraph != null ?
                        fr.inria.corese.core.print.ResultFormat.create(resultGraph,
                                fr.inria.corese.core.print.ResultFormat.format.TRIG_FORMAT).toString() :
                        "No results.";
                addLogEntry("CONSTRUCT query executed successfully");
            }
            case "ASK" -> {
                formattedResult = !mappings.isEmpty();
                addLogEntry("ASK query executed successfully");
            }
            case "DESCRIBE" -> {
                Graph resultGraph = (Graph) mappings.getGraph();
                formattedResult = resultGraph != null ?
                        fr.inria.corese.core.print.ResultFormat.create(resultGraph,
                                fr.inria.corese.core.print.ResultFormat.format.TRIG_FORMAT).toString() :
                        "No results.";
                addLogEntry("DESCRIBE query executed successfully");
            }
            default -> throw new Exception("Unsupported query type: " + queryType);
        }

        return new Object[]{formattedResult, queryType};
    }

    /**
     * Determines the type of a SPARQL query.
     *
     * @param queryString The query string to analyze
     * @return The query type (SELECT, CONSTRUCT, ASK, or DESCRIBE)
     */
    private String determineQueryType(String queryString) {
        // Remove prefixes and clean up the query string
        String cleanedQuery = queryString.replaceAll("^\\s*PREFIX\\s+[^:]+:\\s*<[^>]+>\\s*", "")
                .replaceAll("^\\s*PREFIX\\s+[^:]+:\\s*<[^>]+>\\s*", "")
                .trim()
                .toUpperCase();

        if (cleanedQuery.startsWith("SELECT")) return "SELECT";
        if (cleanedQuery.startsWith("CONSTRUCT")) return "CONSTRUCT";
        if (cleanedQuery.startsWith("ASK")) return "ASK";
        if (cleanedQuery.startsWith("DESCRIBE")) return "DESCRIBE";
        return "UNKNOWN";
    }

    /**
     * Clears the graph and resets rule engines.
     */
    public void clearGraph() {
        initializeGraph();
        semanticGraph.clearGraph();
        addLogEntry("Graph cleared");
    }

    /**
     * Clears the file list.
     */
    public void clearFiles() {
        fileListModel.clearFiles();
        loadedFiles.clear();
    }

    /**
     * Reloads all files in the project.
     */
    public void reloadFiles() {
        clearGraph();
        for (File file : loadedFiles) {
            try {
                loadFile(file);
                addLogEntry("Reloaded file: " + file.getName());
            } catch (Exception e) {
                addLogEntry("Error reloading file " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Loads a project from a directory.
     *
     * @param directory The project directory
     */
    public void loadProject(File directory) {
        clearGraph();
        clearFiles();
        this.projectPath = directory.getAbsolutePath();
        addLogEntry("Loading project from: " + directory.getAbsolutePath());

        // Try to load the context first
        try {
            File contextFile = new File(directory, "project.context");
            if (contextFile.exists()) {
                semanticGraph.loadContext(contextFile.getAbsolutePath());
                addLogEntry("Project context restored");

                // Reload the state from the semantic graph
                restoreState();
            }
        } catch (Exception e) {
            addLogEntry("Could not restore project context: " + e.getMessage());
            // Continue with normal loading
            loadProjectFiles(directory);
            loadProjectRules(directory);
        }
    }

    /**
     * Loads all TTL files from a project directory.
     *
     * @param directory The project directory
     */
    private void loadProjectFiles(File directory) {
        File[] ttlFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttl"));
        if (ttlFiles != null) {
            for (File file : ttlFiles) {
                try {
                    loadFile(file);
                } catch (Exception e) {
                    addLogEntry("Error loading file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Loads all rule files from a project directory.
     *
     * @param directory The project directory
     */
    private void loadProjectRules(File directory) {
        File rulesDir = new File(directory, "rules");
        if (rulesDir.exists() && rulesDir.isDirectory()) {
            File[] ruleFiles = rulesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".rul"));
            if (ruleFiles != null) {
                for (File ruleFile : ruleFiles) {
                    try {
                        loadRuleFile(ruleFile);
                    } catch (Exception e) {
                        addLogEntry("Error loading rule file " + ruleFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Saves the current project to a file.
     *
     * @param targetFile The target file
     */
    public void saveProject(File targetFile) {
        try {
            Path projectDir = Paths.get(targetFile.getParent());
            Files.createDirectories(projectDir);

            // Save the context
            semanticGraph.saveContext();

            // Save rules configuration
            saveRulesConfiguration(projectDir);

            // Save project configuration
            saveProjectConfiguration(targetFile);

            this.projectPath = projectDir.toString();
            addLogEntry("Project and context saved successfully to: " + projectDir);
        } catch (Exception e) {
            addLogEntry("Error saving project: " + e.getMessage());
        }
    }

    /**
     * Saves the rules configuration to a file.
     *
     * @param projectDir The project directory
     * @throws Exception If an error occurs during saving
     */
    private void saveRulesConfiguration(Path projectDir) throws Exception {
        Path rulesDir = projectDir.resolve("rules");
        Files.createDirectories(rulesDir);

        StringBuilder configContent = createRulesConfigContent();
        File configFile = new File(projectDir.toFile(), "rules.config");
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            Properties props = new Properties();
            props.setProperty("RDFS_SUBSET", String.valueOf(rdfsSubsetEnabled));
            props.setProperty("RDFS_RL", String.valueOf(rdfsRLEnabled));
            props.setProperty("OWL_RL", String.valueOf(owlRLEnabled));
            props.setProperty("OWL_RL_EXTENDED", String.valueOf(owlRLExtendedEnabled));
            props.setProperty("OWL_RL_TEST", String.valueOf(owlRLTestEnabled));
            props.setProperty("OWL_CLEAN", String.valueOf(owlCleanEnabled));

            // Save custom rule states
            for (Map.Entry<String, Boolean> entry : customRuleStates.entrySet()) {
                props.setProperty("CUSTOM_RULE_" + entry.getKey(), String.valueOf(entry.getValue()));
            }

            props.store(out, "Rules configuration");
        }
        addLogEntry("Rules configuration saved to: " + configFile.getAbsolutePath());
    }

    /**
     * Creates the content for the rules configuration file.
     *
     * @return The rules configuration content
     */
    private StringBuilder createRulesConfigContent() {
        StringBuilder configContent = new StringBuilder();
        configContent.append("# Rules configuration\n");
        configContent.append("RDFS_SUBSET=").append(rdfsSubsetEnabled).append("\n");
        configContent.append("RDFS_RL=").append(rdfsRLEnabled).append("\n");
        configContent.append("OWL_RL=").append(owlRLEnabled).append("\n");
        configContent.append("OWL_RL_EXTENDED=").append(owlRLExtendedEnabled).append("\n");
        configContent.append("OWL_RL_TEST=").append(owlRLTestEnabled).append("\n");
        configContent.append("OWL_CLEAN=").append(owlCleanEnabled).append("\n");
        return configContent;
    }

    /**
     * Saves the project configuration to a file.
     *
     * @param targetFile The target file
     * @throws Exception If an error occurs during saving
     */
    private void saveProjectConfiguration(File targetFile) throws Exception {
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

    /**
     * Processes rules on the current graph.
     */
    public void processRules() {
        try {
            addLogEntry("Starting rule processing");

            int tripletsBefore = graph.size();

            // Process rules
            ruleEngine.process();

            int tripletsAfter = graph.size();
            addLogEntry("Rules processing completed. Added " +
                    (tripletsAfter - tripletsBefore) + " new triples.");

        } catch (Exception e) {
            addLogEntry("Error processing rules: " + e.getMessage());
        }
    }

    /**
     * Reloads all rules.
     */
    public void reloadRules() {
        // Save custom rules
        Set<String> customRules = new HashSet<>(customRuleStates.keySet());

        // Reset rule engine
        this.ruleEngine = RuleEngine.create(graph);

        // Reload predefined rules
        if (rdfsSubsetEnabled) loadRDFSSubset();
        if (rdfsRLEnabled) loadRDFSRL();
        if (owlRLEnabled) loadOWLRL();
        if (owlRLExtendedEnabled) loadOWLRLExtended();
        if (owlRLTestEnabled) loadOWLRLTest();
        if (owlCleanEnabled) loadOWLClean();

        // Reload custom rules
        for (String ruleName : customRules) {
            if (customRuleStates.getOrDefault(ruleName, false)) {
                for (File ruleFile : loadedRuleFiles) {
                    if (ruleFile.getName().equals(ruleName)) {
                        try {
                            RuleLoad ruleLoader = RuleLoad.create(ruleEngine);
                            ruleLoader.parse(ruleFile.getAbsolutePath());
                        } catch (Exception e) {
                            addLogEntry("Error reloading custom rule " + ruleName + ": " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        }

        // Apply all rules
        try {
            ruleEngine.process();
        } catch (Exception e) {
            addLogEntry("Error processing rules: " + e.getMessage());
        }
    }

    /**
     * Loads and applies RDFS Subset rules.
     */
    public void loadRDFSSubset() {
        // Implementation for RDFS Subset
        addLogEntry("RDFS Subset rules " + (rdfsSubsetEnabled ? "enabled" : "disabled"));
    }

    /**
     * Loads and applies RDFS RL rules.
     */
    public void loadRDFSRL() {
        if (rdfsRLEnabled) {
            try {
                RuleEngine engine = RuleEngine.create(graph);
                engine.setProfile(RuleEngine.OWL_RL);
                engine.process();
                addLogEntry("RDFS RL rules enabled");
            } catch (Exception e) {
                addLogEntry("Error loading RDFS RL: " + e.getMessage());
            }
        } else {
            addLogEntry("RDFS RL rules disabled");
        }
    }

    /**
     * Loads and applies OWL RL rules.
     */
    public void loadOWLRL() {
        if (owlRLEnabled) {
            try {
                ruleEngine.setProfile(RuleEngine.OWL_RL);
                ruleEngine.process();
                addLogEntry("OWL RL rules enabled");
            } catch (Exception e) {
                addLogEntry("Error loading OWL RL: " + e.getMessage());
            }
        } else {
            addLogEntry("OWL RL rules disabled");
        }
    }

    /**
     * Loads and applies OWL RL Extended rules.
     */
    public void loadOWLRLExtended() {
        if (owlRLExtendedEnabled) {
            try {
                ruleEngine.setProfile(RuleEngine.OWL_RL_EXT);
                ruleEngine.process();
                addLogEntry("OWL RL Extended rules enabled");
            } catch (Exception e) {
                addLogEntry("Error loading OWL RL Extended: " + e.getMessage());
            }
        } else {
            addLogEntry("OWL RL Extended rules disabled");
        }
    }

    /**
     * Loads and applies OWL RL Test rules.
     */
    public void loadOWLRLTest() {
        if (owlRLTestEnabled) {
            try {
                ruleEngine.setProfile(RuleEngine.OWL_RL_TEST);
                ruleEngine.process();
                addLogEntry("OWL RL Test rules enabled");
            } catch (Exception e) {
                addLogEntry("Error loading OWL RL Test: " + e.getMessage());
            }
        } else {
            addLogEntry("OWL RL Test rules disabled");
        }
    }

    /**
     * Loads and applies OWL Clean rules.
     */
    public void loadOWLClean() {
        // Implementation for OWL Clean
        addLogEntry("OWL Clean rules " + (owlCleanEnabled ? "enabled" : "disabled"));
    }

    /**
     * Removes a rule.
     *
     * @param ruleName The name of the rule to remove
     */
    public void removeRule(String ruleName) {
        customRuleStates.put(ruleName, false);
        reloadRules();
        addLogEntry("Rule removed: " + ruleName);
    }

    /**
     * Saves the current state.
     */
    public void saveCurrentState() {
        try {
            // Save the context of the graph
            semanticGraph.saveContext();

            addLogEntry("Current state saved");
        } catch (Exception e) {
            addLogEntry("Error saving current state: " + e.getMessage());
        }
    }

    /**
     * Restores the state from the saved context.
     */
    public void restoreState() {
        try {
            // Load files and rules from the semantic graph
            loadedFiles.clear();
            loadedFiles.addAll(semanticGraph.getLoadedFiles());

            loadedRuleFiles.clear();
            loadedRuleFiles.addAll(semanticGraph.getLoadedRules());

            // Clear file list model and reload it
            fileListModel.clearFiles();
            for (File file : loadedFiles) {
                fileListModel.addFile(file.getName());
            }

            // Reload rules if OWL RL is enabled
            if (owlRLEnabled) {
                ruleEngine = RuleEngine.create(graph);
                ruleEngine.setProfile(RuleEngine.OWL_RL);
                ruleEngine.process();
                addLogEntry("OWL RL rules reapplied after state restoration");
            }

            addLogEntry("State restored");
        } catch (Exception e) {
            addLogEntry("Error restoring state: " + e.getMessage());
        }
    }

    /**
     * Returns the file list model.
     *
     * @return The file list model
     */
    public FileListModel getFileListModel() {
        return fileListModel;
    }

    /**
     * Returns the loaded files.
     *
     * @return The list of loaded files
     */
    public List<File> getLoadedFiles() {
        return new ArrayList<>(loadedFiles);
    }

    /**
     * Returns the loaded rule files.
     *
     * @return The list of loaded rule files
     */
    public List<File> getLoadedRules() {
        return new ArrayList<>(loadedRuleFiles);
    }

    /**
     * Returns the number of semantic elements.
     *
     * @return The number of semantic elements
     */
    public int getSemanticElementsCount() {
        return semanticGraph.getSemanticElementsCount();
    }

    /**
     * Returns the number of triplets.
     *
     * @return The number of triplets
     */
    public int getTripletCount() {
        return semanticGraph.getTripletCount();
    }

    /**
     * Returns the number of graphs.
     *
     * @return The number of graphs
     */
    public int getGraphCount() {
        return semanticGraph.getGraphCount();
    }

    /**
     * Returns the number of loaded rules.
     *
     * @return The number of loaded rules
     */
    public int getLoadedRulesCount() {
        int count = 0;
        if (rdfsSubsetEnabled) count++;
        if (rdfsRLEnabled) count++;
        if (owlRLEnabled) count++;
        if (owlRLExtendedEnabled) count++;
        if (owlRLTestEnabled) count++;
        if (owlCleanEnabled) count++;

        // Add custom rules that are enabled
        for (boolean enabled : customRuleStates.values()) {
            if (enabled) count++;
        }

        return count;
    }

    /**
     * Adds a log entry.
     *
     * @param entry The log entry to add
     */
    public void addLogEntry(String entry) {
        logEntries.add(entry);
        semanticGraph.addLogEntry(entry);
    }

    /**
     * Returns the log entries.
     *
     * @return The list of log entries
     */
    public List<String> getLogEntries() {
        return new ArrayList<>(logEntries);
    }

    /**
     * Returns the semantic graph.
     *
     * @return The semantic graph
     */
    public SemanticGraph getSemanticGraph() {
        return semanticGraph;
    }

    /**
     * Returns the state of RDFS Subset rules.
     *
     * @return True if RDFS Subset rules are enabled
     */
    public boolean isRDFSSubsetEnabled() {
        return rdfsSubsetEnabled;
    }

    /**
     * Sets the state of RDFS Subset rules.
     *
     * @param enabled The new state
     */
    public void setRDFSSubsetEnabled(boolean enabled) {
        this.rdfsSubsetEnabled = enabled;
        if (enabled) {
            loadRDFSSubset();
        } else {
            reloadRules();
        }
    }

    /**
     * Returns the state of RDFS RL rules.
     *
     * @return True if RDFS RL rules are enabled
     */
    public boolean isRDFSRLEnabled() {
        return rdfsRLEnabled;
    }

    /**
     * Sets the state of RDFS RL rules.
     *
     * @param enabled The new state
     */
    public void setRDFSRLEnabled(boolean enabled) {
        this.rdfsRLEnabled = enabled;
        if (enabled) {
            loadRDFSRL();
        } else {
            reloadRules();
        }
    }

    /**
     * Returns the state of OWL RL rules.
     *
     * @return True if OWL RL rules are enabled
     */
    public boolean isOWLRLEnabled() {
        return owlRLEnabled;
    }

    /**
     * Sets the state of OWL RL rules.
     *
     * @param enabled The new state
     */
    public void setOWLRLEnabled(boolean enabled) {
        this.owlRLEnabled = enabled;
        if (enabled) {
            loadOWLRL();
        } else {
            reloadRules();
        }
    }

    /**
     * Returns the state of OWL RL Extended rules.
     *
     * @return True if OWL RL Extended rules are enabled
     */
    public boolean isOWLRLExtendedEnabled() {
        return owlRLExtendedEnabled;
    }

    /**
     * Sets the state of OWL RL Extended rules.
     *
     * @param enabled The new state
     */
    public void setOWLRLExtendedEnabled(boolean enabled) {
        this.owlRLExtendedEnabled = enabled;
        if (enabled) {
            loadOWLRLExtended();
        } else {
            reloadRules();
        }
    }

    /**
     * Returns the state of OWL RL Test rules.
     *
     * @return True if OWL RL Test rules are enabled
     */
    public boolean isOWLRLTestEnabled() {
        return owlRLTestEnabled;
    }

    /**
     * Sets the state of OWL RL Test rules.
     *
     * @param enabled The new state
     */
    public void setOWLRLTestEnabled(boolean enabled) {
        this.owlRLTestEnabled = enabled;
        if (enabled) {
            loadOWLRLTest();
        } else {
            reloadRules();
        }
    }

    /**
     * Returns the state of OWL Clean rules.
     *
     * @return True if OWL Clean rules are enabled
     */
    public boolean isOWLCleanEnabled() {
        return owlCleanEnabled;
    }

    /**
     * Sets the state of OWL Clean rules.
     *
     * @param enabled The new state
     */
    public void setOWLCleanEnabled(boolean enabled) {
        this.owlCleanEnabled = enabled;
        if (enabled) {
            loadOWLClean();
        } else {
            reloadRules();
        }
    }

    /**
     * Returns the state of a custom rule.
     *
     * @param ruleName The name of the rule
     * @return True if the rule is enabled
     */
    public boolean isCustomRuleEnabled(String ruleName) {
        return customRuleStates.getOrDefault(ruleName, false);
    }

    /**
     * Sets the state of a custom rule.
     *
     * @param ruleName The name of the rule
     * @param enabled The new state
     */
    public void setCustomRuleEnabled(String ruleName, boolean enabled) {
        customRuleStates.put(ruleName, enabled);
        reloadRules();
    }
    
    /**
     * Formats the last SELECT query result in the specified format.
     * 
     * @param format The desired result format (XML, JSON, CSV, TSV, MARKDOWN)
     * @return The formatted result as a String, or null if no SELECT query has been executed
     */
    public String formatLastSelectResult(ResultFormat.format format) {
        if (lastSelectQueryMappings == null) {
            addLogEntry("No SELECT query results available to format");
            return null;
        }
        
        try {
            String formattedResult = ResultFormat.create(lastSelectQueryMappings, format).toString();
            addLogEntry("Query results formatted as " + format.name());
            return formattedResult;
        } catch (Exception e) {
            addLogEntry("Error formatting query results: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Formats any SELECT query result in the specified format.
     *
     * @param mappings The Mappings object from a SELECT query
     * @param format The desired result format (XML, JSON, CSV, TSV, MARKDOWN)
     * @return The formatted result as a String, or null if mappings is null
     */
    public String formatSelectResult(fr.inria.corese.core.kgram.core.Mappings mappings, ResultFormat.format format) {
        if (mappings == null) {
            addLogEntry("No SELECT query results available to format");
            return null;
        }
        try {
            String formattedResult = ResultFormat.create(mappings, format).toString();
            addLogEntry("Query results formatted as " + format.name());
            return formattedResult;
        } catch (Exception e) {
            addLogEntry("Error formatting query results: " + e.getMessage());
            return null;
        }
    }
}
