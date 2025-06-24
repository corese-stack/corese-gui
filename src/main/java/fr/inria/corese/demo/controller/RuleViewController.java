package fr.inria.corese.demo.controller;

import fr.inria.corese.demo.enums.icon.IconButtonType;
import fr.inria.corese.demo.manager.ApplicationStateManager;
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
 */
public class RuleViewController {
    private final ApplicationStateManager stateManager;
    private final RuleManager ruleManager;
    private RuleView view;
    private final Map<String, RuleItem> ruleItems;
    private final PopupFactory popupFactory;
    private Runnable owlRLAction;
    private Runnable owlRLExtendedAction;

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
     */
    public RuleViewController() {
        this.view = new RuleView();
        this.ruleItems = new HashMap<>();
        this.stateManager = ApplicationStateManager.getInstance();
        this.ruleManager = stateManager.getRuleManager();
        this.popupFactory = PopupFactory.getInstance();
    }

    /**
     * Initializes the controller.
     * Sets up the rule view and containers.
     */
    @FXML
    public void initialize() {
        view = new RuleView();
        initializeRules();

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
     * Initializes rules.
     * Sets up predefined rules and updates the view.
     */
    public void initializeRules() {
        if (rdfsRulesContainer != null && owlRulesContainer != null) {
            rdfsRulesContainer.getChildren().clear();
            owlRulesContainer.getChildren().clear();

            // RDFS rules
            addRuleItem(rdfsRulesContainer, "RDFS Subset", true);
            addRuleItem(rdfsRulesContainer, "RDFS RL", true);

            // OWL rules
            addRuleItem(owlRulesContainer, "OWL RL", true);
            addRuleItem(owlRulesContainer, "OWL RL Extended", true);
            addRuleItem(owlRulesContainer, "OWL RL Test", true);
            addRuleItem(owlRulesContainer, "OWL Clean", true);
        }

        updateView();
    }

    /**
     * Updates the view.
     * Updates rule states and custom rules.
     */
    public void updateView() {
        // Update predefined rule states
        updatePredefinedRuleStates();

        // Update custom rules
        if (customRulesContainer != null) {
            // Get custom rules from state manager
            List<File> customRules = new ArrayList<>(ruleManager.getLoadedRuleFiles());
            // Filter out predefined rules
            customRules.removeIf(rule -> rule.getName().equals("RDFS Subset") ||
                    rule.getName().equals("RDFS RL") ||
                    rule.getName().equals("OWL RL") ||
                    rule.getName().equals("OWL RL Extended") ||
                    rule.getName().equals("OWL RL Test") ||
                    rule.getName().equals("OWL Clean"));

            // Display custom rules
            displayCustomRules(customRules);
        }
    }

    /**
     * Updates predefined rule states.
     */
    private void updatePredefinedRuleStates() {
        for (Map.Entry<String, RuleItem> entry : ruleItems.entrySet()) {
            String ruleName = entry.getKey();
            RuleItem item = entry.getValue();

            // Get rule state from state manager
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
     * Adds a rule item to a container.
     *
     * @param container  The container to add the rule item to
     * @param ruleName   The name of the rule
     * @param predefined Whether the rule is predefined
     */
    private void addRuleItem(VBox container, String ruleName, boolean predefined) {
        RuleItem ruleItem = new RuleItem(ruleName);
        ruleItems.put(ruleName, ruleItem);

        ruleItem.getDocumentationButton().setOnAction(e -> handleShowDocumentation(ruleName));
        ruleItem.getCheckBox().setOnAction(e -> handleRuleToggle(ruleName, ruleItem.getCheckBox().isSelected()));

        container.getChildren().add(ruleItem);
    }

    /**
     * Handles loading a rule file.
     */
    @FXML
    public void handleLoadRuleFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Rule files (*.rul)", "*.rul"));

        File selectedFile = fileChooser.showOpenDialog(rdfsRulesContainer.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Log loading
                stateManager.addLogEntry("Starting to load rule file: " + selectedFile.getName());

                // Load rule file
                ruleManager.loadRuleFile(selectedFile);
                stateManager.addLogEntry("Rule file loaded successfully: " + selectedFile.getName());

                // Show success notification
                if (popupFactory != null) {
                    IPopup successPopup = popupFactory.createPopup(PopupFactory.TOAST_NOTIFICATION);
                    successPopup.setMessage("Rule file '" + selectedFile.getName() + "' has been successfully loaded!");
                    successPopup.displayPopup();
                } else {
                    // Fallback if popupFactory is not available
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Rule file '" + selectedFile.getName() + "' has been successfully loaded!");
                    alert.showAndWait();
                }

                // Update the view
                updateView();

            } catch (Exception e) {
                String errorMessage = "Error loading rule file: " + e.getMessage();
                stateManager.addLogEntry("ERROR: " + errorMessage);

                // Show error notification
                if (popupFactory != null) {
                    IPopup errorPopup = popupFactory.createPopup(PopupFactory.WARNING_POPUP);
                    errorPopup.setMessage(errorMessage);
                    ((WarningPopup) errorPopup).getResult();
                } else {
                    // Fallback if popupFactory is not available
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error Loading Rule");
                    alert.setContentText(errorMessage);
                    alert.showAndWait();
                }
            }
        }
    }

    /**
     * Handles showing documentation for a rule.
     *
     * @param ruleName The name of the rule
     */
    private void handleShowDocumentation(String ruleName) {
        // TODO: Open documentation with external link
    }

    /**
     * Handles toggling a rule.
     *
     * @param ruleName The name of the rule
     * @param selected Whether the rule is selected
     */
    private void handleRuleToggle(String ruleName, boolean selected) {
        switch (ruleName) {
            case "RDFS Subset":
                ruleManager.setRDFSSubsetEnabled(selected);
                break;
            case "RDFS RL":
                ruleManager.setRDFSRLEnabled(selected);
                break;
            case "OWL RL":
                if (selected && owlRLAction != null) {
                    owlRLAction.run();
                }
                ruleManager.setOWLRLEnabled(selected);
                // Save current state
                stateManager.saveCurrentState();
                break;
            case "OWL RL Extended":
                if (selected && owlRLExtendedAction != null) {
                    owlRLExtendedAction.run();
                }
                ruleManager.setOWLRLExtendedEnabled(selected);
                // Save current state
                stateManager.saveCurrentState();
                break;
            case "OWL RL Test":
                ruleManager.setOWLRLTestEnabled(selected);
                break;
            case "OWL Clean":
                ruleManager.setOWLCleanEnabled(selected);
                break;
            default:
                // Custom rule handling
                ruleManager.setCustomRuleEnabled(ruleName, selected);
                break;
        }

        // Update the view
        updateView();
    }

    /**
     * Displays custom rules.
     *
     * @param customRules The list of custom rules
     */
    private void displayCustomRules(List<File> customRules) {
        // Check that the container exists
        if (customRulesContainer == null) {
            stateManager.addLogEntry("Custom rules container not found in FXML");
            return;
        }

        // Clear the container
        customRulesContainer.getChildren().clear();

        if (customRules.isEmpty()) {
            // Show message if no custom rules are loaded
            Label noRulesLabel = new Label("No custom rules loaded");
            noRulesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
            customRulesContainer.getChildren().add(noRulesLabel);
        } else {
            // Add each custom rule to the container
            for (File ruleFile : customRules) {
                RuleItem ruleItem = new RuleItem(ruleFile);
                String ruleName = ruleFile.getName();

                // Add to map
                ruleItems.put(ruleName, ruleItem);

                // Set checkbox state from state manager
                boolean isEnabled = ruleManager.isCustomRuleEnabled(ruleName);
                ruleItem.getCheckBox().setSelected(isEnabled);

                // Configure checkbox to update state when changed
                ruleItem.getCheckBox()
                        .setOnAction(e -> handleRuleToggle(ruleName, ruleItem.getCheckBox().isSelected()));

                // Replace documentation button with delete button
                Button deleteButton = ruleItem.getDocumentationButton();
                // Change button to delete icon
                IconButtonView deleteIconButton = new IconButtonView(IconButtonType.DELETE);

                if (deleteButton instanceof IconButtonView) {
                    ((IconButtonView) deleteButton).setType(IconButtonType.DELETE);
                } else {
                    // Replace existing button with new button
                    HBox parent = (HBox) deleteButton.getParent();
                    int index = parent.getChildren().indexOf(deleteButton);
                    parent.getChildren().remove(deleteButton);
                    parent.getChildren().add(index, deleteIconButton);
                    deleteButton = deleteIconButton;
                }

                // Configure delete button action
                final String finalRuleName = ruleName;
                deleteButton.setOnAction(e -> handleDeleteCustomRule(finalRuleName));

                // Add the rule item to the container
                customRulesContainer.getChildren().add(ruleItem);
            }
        }
    }

    /**
     * Handles deleting a custom rule.
     *
     * @param ruleName The name of the rule to delete
     */
    private void handleDeleteCustomRule(String ruleName) {
        // Confirm before deletion
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Rule");
        confirmDialog.setHeaderText("Delete Custom Rule");
        confirmDialog.setContentText("Are you sure you want to delete the rule: " + ruleName + "?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Disable the rule first
                ruleManager.setCustomRuleEnabled(ruleName, false);

                // Remove the rule from the model
                ruleManager.removeRule(ruleName);

                // Save current state
                stateManager.saveCurrentState();

                // Update the view
                updateView();

                // Show success notification
                if (popupFactory != null) {
                    IPopup successPopup = popupFactory.createPopup(PopupFactory.TOAST_NOTIFICATION);
                    successPopup.setMessage("Rule '" + ruleName + "' has been deleted successfully!");
                    successPopup.displayPopup();
                } else {
                    // Fallback to Alert
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Rule Deleted");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Rule '" + ruleName + "' has been deleted successfully.");
                    successAlert.showAndWait();
                }
            } catch (Exception ex) {
                // Show error notification
                if (popupFactory != null) {
                    IPopup errorPopup = popupFactory.createPopup(PopupFactory.WARNING_POPUP);
                    errorPopup.setMessage("Failed to delete rule: " + ex.getMessage());
                    ((WarningPopup) errorPopup).getResult();
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Error Deleting Rule");
                    errorAlert.setContentText("Failed to delete rule: " + ex.getMessage());
                    errorAlert.showAndWait();
                }
            }
        }
    }

    /**
     * Sets the action to execute when OWL RL rules are activated.
     *
     * @param action The action to execute
     */
    public void setOWLRLAction(Runnable action) {
        this.owlRLAction = action;
    }

    /**
     * Sets the action to execute when OWL RL Extended rules are activated.
     *
     * @param action The action to execute
     */
    public void setOWLRLExtendedAction(Runnable action) {
        this.owlRLExtendedAction = action;
    }
}