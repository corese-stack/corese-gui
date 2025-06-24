// RuleManager.java
package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.RuleLoad;
import fr.inria.corese.core.rule.RuleEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages the loading, configuration, and application of inference rules.
 * This class isolates all rule-related logic from the main
 * ApplicationStateManager.
 */
public class RuleManager {

    private final ApplicationStateManager stateManager;

    // Rule state
    private final List<File> loadedRuleFiles;
    private final Map<String, Boolean> customRuleStates;
    private boolean rdfsSubsetEnabled;
    private boolean rdfsRLEnabled;
    private boolean owlRLEnabled;
    private boolean owlRLExtendedEnabled;
    private boolean owlRLTestEnabled;
    private boolean owlCleanEnabled;

    public RuleManager(ApplicationStateManager stateManager) {
        this.stateManager = stateManager;
        this.loadedRuleFiles = new ArrayList<>();
        this.customRuleStates = new HashMap<>();
    }

    /**
     * Loads a custom rule file and marks it as active.
     * 
     * @param file The rule file (.rul) to load.
     * @throws Exception if loading fails.
     */
    public void loadRuleFile(File file) throws Exception {
        if (!loadedRuleFiles.contains(file)) {
            loadedRuleFiles.add(file);
        }
        customRuleStates.put(file.getName(), true);
        stateManager.addLogEntry("Rule file loaded and enabled: " + file.getName());
        applyRules(stateManager.getGraph());
    }

    /**
     * The core method to apply all configured rules to a given graph.
     * It creates a new RuleEngine, configures it based on the current state,
     * loads custom rules, and processes them.
     *
     * @param graph The graph on which to apply the rules.
     */
    public void applyRules(Graph graph) {
        stateManager.addLogEntry("Applying inference rules...");
        int initialSize = graph.size();

        try {
            RuleEngine engine = RuleEngine.create(graph);

            // 1. Configure built-in rule profiles
            if (isOWLRLEnabled()) {
                engine.setProfile(RuleEngine.OWL_RL);
            }
            if (isOWLRLExtendedEnabled()) {
                engine.setProfile(RuleEngine.OWL_RL_EXT);
            }
            if (isOWLRLTestEnabled()) {
                engine.setProfile(RuleEngine.OWL_RL_TEST);
            }
            // Note: Setting a profile like OWL_RL includes RDFS entailments.
            // Other profiles like RDFS_SUBSET or OWL_CLEAN would need custom
            // implementations
            // if they are not part of the standard RuleEngine profiles.

            // 2. Load and apply enabled custom rules
            RuleLoad ruleLoader = RuleLoad.create(engine);
            for (File ruleFile : loadedRuleFiles) {
                if (isCustomRuleEnabled(ruleFile.getName())) {
                    stateManager.addLogEntry("-> Loading custom rule: " + ruleFile.getName());
                    ruleLoader.parse(ruleFile.getAbsolutePath());
                }
            }

            // 3. Process the rules
            engine.process();
            int finalSize = graph.size();
            stateManager
                    .addLogEntry("Rule processing complete. " + (finalSize - initialSize) + " new triples inferred.");

        } catch (Exception e) {
            stateManager.addLogEntry("ERROR during rule application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Disables a custom rule and re-applies the remaining rules.
     * 
     * @param ruleName The name of the rule to remove/disable.
     */
    public void removeRule(String ruleName) {
        customRuleStates.put(ruleName, false);
        stateManager.addLogEntry("Rule disabled: " + ruleName);
        applyRules(stateManager.getGraph());
    }

    public List<File> getLoadedRuleFiles() {
        return loadedRuleFiles;
    }

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
            if (enabled)
                count++;
        }
        return count;
    }

    public Map<String, Boolean> getCustomRuleStates() {
        return customRuleStates;
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

    private void updateAndApplyRules() {
        applyRules(stateManager.getGraph());
    }

    public void setRDFSSubsetEnabled(boolean enabled) {
        this.rdfsSubsetEnabled = enabled;
        stateManager.addLogEntry("RDFS Subset rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setRDFSRLEnabled(boolean enabled) {
        this.rdfsRLEnabled = enabled;
        stateManager.addLogEntry("RDFS RL rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLRLEnabled(boolean enabled) {
        this.owlRLEnabled = enabled;
        stateManager.addLogEntry("OWL RL rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLRLExtendedEnabled(boolean enabled) {
        this.owlRLExtendedEnabled = enabled;
        stateManager.addLogEntry("OWL RL Extended rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLRLTestEnabled(boolean enabled) {
        this.owlRLTestEnabled = enabled;
        stateManager.addLogEntry("OWL RL Test rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setOWLCleanEnabled(boolean enabled) {
        this.owlCleanEnabled = enabled;
        stateManager.addLogEntry("OWL Clean rules " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void setCustomRuleEnabled(String ruleName, boolean enabled) {
        customRuleStates.put(ruleName, enabled);
        stateManager.addLogEntry("Custom rule " + ruleName + " " + (enabled ? "enabled" : "disabled"));
        updateAndApplyRules();
    }

    public void saveRulesConfiguration(Path projectDir) throws Exception {
        Path rulesDir = projectDir.resolve("rules");
        Files.createDirectories(rulesDir);

        File configFile = new File(projectDir.toFile(), "rules.config");
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            Properties props = new Properties();

            // Use local fields/methods directly, not a 'ruleManager' variable
            props.setProperty("RDFS_SUBSET", String.valueOf(isRDFSSubsetEnabled()));
            props.setProperty("RDFS_RL", String.valueOf(isRDFSRLEnabled()));
            props.setProperty("OWL_RL", String.valueOf(isOWLRLEnabled()));
            props.setProperty("OWL_RL_EXTENDED", String.valueOf(isOWLRLExtendedEnabled()));
            props.setProperty("OWL_RL_TEST", String.valueOf(isOWLRLTestEnabled()));
            props.setProperty("OWL_CLEAN", String.valueOf(isOWLCleanEnabled()));

            for (Map.Entry<String, Boolean> entry : getCustomRuleStates().entrySet()) {
                props.setProperty("CUSTOM_RULE_" + entry.getKey(), String.valueOf(entry.getValue()));
            }
            props.store(out, "Rules configuration");
        }

        // Use the stateManager reference for logging
        stateManager.addLogEntry("Rules configuration saved to: " + configFile.getAbsolutePath());
    }
}