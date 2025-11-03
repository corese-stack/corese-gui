package fr.inria.corese.demo.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.RuleLoad;
import fr.inria.corese.core.rule.RuleEngine;

/**
 * Manages the loading, configuration, and application of inference rules.
 * This class is now decoupled from the old ApplicationStateManager and depends
 * on the QueryManager to access the main data graph.
 */
public class RuleManager {

    private final QueryManager queryManager;
    private final GraphManager graphManager;

    private final List<File> loadedRuleFiles;
    private final Map<String, Boolean> customRuleStates;
    private boolean rdfsSubsetEnabled;
    private boolean rdfsRLEnabled;
    private boolean owlRLEnabled;
    private boolean owlRLExtendedEnabled;
    private boolean owlRLTestEnabled;
    private boolean owlCleanEnabled;

    /**
     * Constructor that injects the required QueryManager dependency.
     * 
     * @param queryManager The central QueryManager instance.
     */
    public RuleManager(QueryManager queryManager) {
        this.queryManager = queryManager;
        this.graphManager = GraphManager.getInstance();
        this.loadedRuleFiles = new ArrayList<>();
        this.customRuleStates = new HashMap<>();
    }

    /**
     * Loads a custom rule file and marks it as active.
     * After loading, it automatically re-applies all rules.
     *
     * @param file The rule file (.rul) to load.
     * @throws Exception if loading fails.
     */
    public void loadRuleFile(File file) throws Exception {
        if (!loadedRuleFiles.contains(file)) {
            loadedRuleFiles.add(file);
        }
        customRuleStates.put(file.getName(), true);
        queryManager.addLogEntry("Rule file loaded and enabled: " + file.getName());
        applyRules();
    }

    /**
     * Disables and removes a custom rule file from the active set.
     * After removing, it automatically re-applies the remaining rules.
     * Note: This requires reloading the original data to get a clean state.
     *
     * @param ruleName The name of the rule to remove/disable.
     */
    public void removeRule(String ruleName) {
        customRuleStates.put(ruleName, false);

        loadedRuleFiles.removeIf(file -> file.getName().equals(ruleName));

        queryManager.addLogEntry("Rule disabled and removed: " + ruleName);
        applyRules();
    }

    /**
     * The core method to apply all configured rules to the main graph.
     * It fetches the graph from the QueryManager, configures a RuleEngine,
     * loads custom rules, and processes them.
     */
    public void applyRules() {
        Graph graph = graphManager.getGraph();
        if (graph == null) {
            queryManager.addLogEntry("ERROR: Cannot apply rules, graph is null.");
            return;
        }

        queryManager.addLogEntry("Applying inference rules...");
        int initialSize = graph.size();

        try {
            RuleEngine engine = RuleEngine.create(graph);

            if (isOWLRLEnabled()) {
                engine.setProfile(RuleEngine.OWL_RL);
            }
            if (isOWLRLExtendedEnabled()) {
                engine.setProfile(RuleEngine.OWL_RL_EXT);
            }
            if (isOWLRLTestEnabled()) {
                engine.setProfile(RuleEngine.OWL_RL_TEST);
            }

            RuleLoad ruleLoader = RuleLoad.create(engine);
            for (File ruleFile : loadedRuleFiles) {
                if (isCustomRuleEnabled(ruleFile.getName())) {
                    queryManager.addLogEntry("-> Loading custom rule: " + ruleFile.getName());
                    ruleLoader.parse(ruleFile.getAbsolutePath());
                }
            }

            engine.process();
            int finalSize = graph.size();
            queryManager
                    .addLogEntry("Rule processing complete. " + (finalSize - initialSize) + " new triples inferred.");

        } catch (Exception e) {
            queryManager.addLogEntry("ERROR during rule application: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Calculates the total number of currently active rule sets.
     * This includes both built-in profiles and enabled custom rule files.
     *
     * @return The count of active rule sets.
     */
    public int getLoadedRulesCount() {
        int count = 0;
        if (rdfsSubsetEnabled)
            count++;
        if (rdfsRLEnabled)
            count++;
        if (owlRLEnabled)
            count++;
        if (owlRLExtendedEnabled)
            count++;
        if (owlRLTestEnabled)
            count++;
        if (owlCleanEnabled)
            count++;

        for (boolean enabled : customRuleStates.values()) {
            if (enabled) {
                count++;
            }
        }
        return count;
    }

    private void updateAndApplyRules() {
        applyRules();
    }

    public void setRDFSSubsetEnabled(boolean enabled) {
        this.rdfsSubsetEnabled = enabled;
        queryManager.addLogEntry("RDFS Subset rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setRDFSRLEnabled(boolean enabled) {
        this.rdfsRLEnabled = enabled;
        queryManager.addLogEntry("RDFS RL rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLRLEnabled(boolean enabled) {
        this.owlRLEnabled = enabled;
        queryManager.addLogEntry("OWL RL rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLRLExtendedEnabled(boolean enabled) {
        this.owlRLExtendedEnabled = enabled;
        queryManager.addLogEntry("OWL RL Extended rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLRLTestEnabled(boolean enabled) {
        this.owlRLTestEnabled = enabled;
        queryManager.addLogEntry("OWL RL Test rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLCleanEnabled(boolean enabled) {
        this.owlCleanEnabled = enabled;
        queryManager.addLogEntry("OWL Clean rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setCustomRuleEnabled(String ruleName, boolean enabled) {
        customRuleStates.put(ruleName, enabled);
        queryManager.addLogEntry("Custom rule " + ruleName + " " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public boolean isRDFSSubsetEnabled() {
        return rdfsSubsetEnabled;
    }

    public boolean isRDFSRLEnabled() {
        return rdfsRLEnabled;
    }

    public boolean isOWLRLEnabled() {
        return owlRLEnabled;
    }

    public boolean isOWLRLExtendedEnabled() {
        return owlRLExtendedEnabled;
    }

    public boolean isOWLRLTestEnabled() {
        return owlRLTestEnabled;
    }

    public boolean isOWLCleanEnabled() {
        return owlCleanEnabled;
    }

    public boolean isCustomRuleEnabled(String ruleName) {
        return customRuleStates.getOrDefault(ruleName, false);
    }

    public List<File> getLoadedRuleFiles() {
        return loadedRuleFiles;
    }

    public Map<String, Boolean> getCustomRuleStates() {
        return customRuleStates;
    }
}