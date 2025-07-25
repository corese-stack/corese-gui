package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.manager.RuleManager;
import fr.inria.corese.demo.view.icon.IconButtonView;
import fr.inria.corese.demo.view.rule.RuleItem;
import fr.inria.corese.demo.view.rule.RuleView;
import fr.inria.corese.demo.factory.popup.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

/**
 * Controller for the rule view.
 * Manages rule display, activation, and loading.
 * This controller is now decoupled and relies on an injected RuleManager.
 */
public class RuleViewController {

    private RuleManager ruleManager;

    private final PopupFactory popupFactory;
    private final Map<String, RuleItem> ruleItems;

    private Runnable owlRLAction;
    private Runnable owlRLExtendedAction;

    // FXML bindings
    @FXML
    private VBox rdfsRulesContainer;
    @FXML
    private VBox owlRulesContainer;
    @FXML
    private VBox customRulesContainer;
    @FXML
    private Button loadRuleButton;

    /**
     * Constructor for the rule view controller.
     * It no longer creates its own managers.
     */
    public RuleViewController() {
        this.ruleItems = new HashMap<>();
        this.popupFactory = PopupFactory.getInstance();
    }

    /**
     * Sets the RuleManager instance for this controller to use.
     * This method MUST be called by the parent controller (DataViewController)
     * after loading the FXML.
     * 
     * @param ruleManager The active RuleManager instance for the application.
     */
    public void setRuleManager(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    @FXML
    public void initialize() {
        if (customRulesContainer != null) {
            Label noRulesLabel = new Label("No custom rules loaded");
            noRulesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
            customRulesContainer.getChildren().clear();
            customRulesContainer.getChildren().add(noRulesLabel);
        }

        if (loadRuleButton != null) {
            loadRuleButton.setOnAction(e -> handleLoadRuleFile());
        }
    }

    /**
     * Initializes the rule displays. This must be called AFTER setRuleManager has
     * been called.
     */
    public void initializeRules() {
        if (this.ruleManager == null) {
            System.err.println("FATAL: RuleViewController.initializeRules() called before RuleManager was set.");
            return;
        }
        if (rdfsRulesContainer != null && owlRulesContainer != null) {
            rdfsRulesContainer.getChildren().clear();
            owlRulesContainer.getChildren().clear();

            // RDFS rules
            addRuleItem(rdfsRulesContainer, "RDFS Subset");
            addRuleItem(rdfsRulesContainer, "RDFS RL");

            // OWL rules
            addRuleItem(owlRulesContainer, "OWL RL");
            addRuleItem(owlRulesContainer, "OWL RL Extended");
            addRuleItem(owlRulesContainer, "OWL RL Test");
            addRuleItem(owlRulesContainer, "OWL Clean");
        }
        updateView();
    }

    /**
     * Updates the entire view based on the current state of the injected
     * RuleManager.
     */
    public void updateView() {
        if (this.ruleManager == null)
            return; 
        updatePredefinedRuleStates();
        displayCustomRules();
    }

    /**
     * Updates the checkboxes for the predefined RDFS and OWL rules.
     */
    private void updatePredefinedRuleStates() {
        for (Map.Entry<String, RuleItem> entry : ruleItems.entrySet()) {
            String ruleName = entry.getKey();
            RuleItem item = entry.getValue();

            boolean isEnabled = switch (ruleName) {
                case "RDFS Subset" -> ruleManager.isRDFSSubsetEnabled();
                case "RDFS RL" -> ruleManager.isRDFSRLEnabled();
                case "OWL RL" -> ruleManager.isOWLRLEnabled();
                case "OWL RL Extended" -> ruleManager.isOWLRLExtendedEnabled();
                case "OWL RL Test" -> ruleManager.isOWLRLTestEnabled();
                case "OWL Clean" -> ruleManager.isOWLCleanEnabled();
                default -> ruleManager.isCustomRuleEnabled(ruleName);
            };
            item.getCheckBox().setSelected(isEnabled);
        }
    }

    /**
     * Creates a UI element for a predefined rule and adds it to the container.
     */
    private void addRuleItem(VBox container, String ruleName) {
        RuleItem ruleItem = new RuleItem(ruleName);
        ruleItems.put(ruleName, ruleItem);

        ruleItem.getDocumentationButton().setOnAction(e -> handleShowDocumentation(ruleName));
        ruleItem.getCheckBox().setOnAction(e -> handleRuleToggle(ruleName, ruleItem.getCheckBox().isSelected()));

        container.getChildren().add(ruleItem);
    }

    /**
     * Opens a FileChooser to load a custom .rul file.
     */
    @FXML
    public void handleLoadRuleFile() {
        if (this.ruleManager == null) {
            System.err.println("Cannot load rule file: RuleManager is not initialized.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Rule files (*.rul)", "*.rul"));
        File selectedFile = fileChooser.showOpenDialog(loadRuleButton.getScene().getWindow());

        if (selectedFile != null) {
            try {
                ruleManager.loadRuleFile(selectedFile);

                IPopup successPopup = popupFactory.createPopup(PopupFactory.TOAST_NOTIFICATION);
                successPopup.setMessage("Rule file '" + selectedFile.getName() + "' loaded!");
                successPopup.displayPopup();

                updateView(); 
            } catch (Exception e) {
                e.printStackTrace();
                String errorMessage = "Error loading rule file: " + e.getMessage();
                IPopup errorPopup = popupFactory.createPopup(PopupFactory.WARNING_POPUP);
                errorPopup.setMessage(errorMessage);
                ((WarningPopup) errorPopup).getResult();
            }
        }
    }

    private void handleShowDocumentation(String ruleName) {
        System.out.println("Documentation for " + ruleName + " requested.");
    }

    /**
     * Toggles the state of a rule (both predefined and custom).
     */
    private void handleRuleToggle(String ruleName, boolean isSelected) {
        if (this.ruleManager == null)
            return;

        switch (ruleName) {
            case "RDFS Subset" -> ruleManager.setRDFSSubsetEnabled(isSelected);
            case "RDFS RL" -> ruleManager.setRDFSRLEnabled(isSelected);
            case "OWL RL" -> ruleManager.setOWLRLEnabled(isSelected);
            case "OWL RL Extended" -> ruleManager.setOWLRLExtendedEnabled(isSelected);
            case "OWL RL Test" -> ruleManager.setOWLRLTestEnabled(isSelected);
            case "OWL Clean" -> ruleManager.setOWLCleanEnabled(isSelected);
            default -> {
                ruleManager.setCustomRuleEnabled(ruleName, isSelected);
            }
        }
    }

    /**
     * Clears and re-populates the list of custom rules in the UI.
     */
    private void displayCustomRules() {
        if (customRulesContainer == null)
            return;
        customRulesContainer.getChildren().clear();

        List<File> customRules = ruleManager.getLoadedRuleFiles();

        if (customRules.isEmpty()) {
            Label noRulesLabel = new Label("No custom rules loaded");
            noRulesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
            customRulesContainer.getChildren().add(noRulesLabel);
        } else {
            for (File ruleFile : customRules) {
                String ruleName = ruleFile.getName();
                RuleItem ruleItem = new RuleItem(ruleFile);
                ruleItems.put(ruleName, ruleItem);

                ruleItem.getCheckBox().setSelected(ruleManager.isCustomRuleEnabled(ruleName));
                ruleItem.getCheckBox()
                        .setOnAction(e -> handleRuleToggle(ruleName, ruleItem.getCheckBox().isSelected()));

                customRulesContainer.getChildren().add(ruleItem);
            }
        }
    }

    public void setOWLRLAction(Runnable action) {
        this.owlRLAction = action;
    }

    public void setOWLRLExtendedAction(Runnable action) {
        this.owlRLExtendedAction = action;
    }
}