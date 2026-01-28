package fr.inria.corese.gui.feature.rule;

import fr.inria.corese.gui.core.manager.RuleManager;
import fr.inria.corese.gui.core.factory.popup.IPopup;
import fr.inria.corese.gui.core.factory.popup.PopupFactory;
import fr.inria.corese.gui.core.factory.popup.WarningPopup;
import fr.inria.corese.gui.feature.data.DataViewController;






import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Controller for the rule view. Manages rule display, activation, and loading. This controller is
 * now decoupled and relies on an injected RuleManager.
 */
public class RuleViewController {
  private static final Logger logger = LoggerFactory.getLogger(RuleViewController.class);

  private RuleManager ruleManager;
  private final RuleView view;

  private final PopupFactory popupFactory;
  private final Map<String, RuleItem> ruleItems;

  private boolean rulesInitialized = false;
  private Runnable onRuleToggled;
  private DataViewController parentController;

  /** Constructor for the rule view controller. It no longer creates its own managers. */
  public RuleViewController(RuleView view) {
    this.view = view;
    this.ruleItems = new HashMap<>();
    this.popupFactory = PopupFactory.getInstance();
    initialize();
  }

  /**
   * Sets the RuleManager instance for this controller to use. This method MUST be called by the
   * parent controller (DataViewController) after loading the FXML.
   *
   * @param ruleManager The active RuleManager instance for the application.
   */
  public void setRuleManager(RuleManager ruleManager) {
    this.ruleManager = ruleManager;
  }

  public void setParentController(DataViewController parentController) {
    this.parentController = parentController;
  }

  private void initialize() {
    if (view.getCustomRulesContainer() != null) {
      Label noRulesLabel = new Label("No custom rules loaded");
      noRulesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
      view.getCustomRulesContainer().getChildren().clear();
      view.getCustomRulesContainer().getChildren().add(noRulesLabel);
    }

    if (view.getLoadRuleButton() != null) {
      view.getLoadRuleButton().setOnAction(e -> handleLoadRuleFile());
    }
  }

  /** Initializes the rule displays. This must be called AFTER setRuleManager has been called. */
  public void initializeRules() {
    if (this.ruleManager == null) {
      logger.error(
          "FATAL: RuleViewController.initializeRules() called before RuleManager was set.");
      return;
    }
    if (rulesInitialized) {
      updateView();
      return;
    }

    if (view.getRdfsRulesContainer() != null && view.getOwlRulesContainer() != null) {
      view.getRdfsRulesContainer().getChildren().clear();
      view.getOwlRulesContainer().getChildren().clear();

      // RDFS rules
      addRuleItem(view.getRdfsRulesContainer(), "RDFS Subset");
      addRuleItem(view.getRdfsRulesContainer(), "RDFS RL");

      // OWL rules
      addRuleItem(view.getOwlRulesContainer(), "OWL RL");
      addRuleItem(view.getOwlRulesContainer(), "OWL RL Extended");
      addRuleItem(view.getOwlRulesContainer(), "OWL RL Test");
      addRuleItem(view.getOwlRulesContainer(), "OWL Clean");

      rulesInitialized = true;
    }
    updateView();
  }

  /** Updates the entire view based on the current state of the injected RuleManager. */
  public void updateView() {
    if (this.ruleManager == null) return;
    updatePredefinedRuleStates();
    displayCustomRules();
  }

  /** Updates the checkboxes for the predefined RDFS and OWL rules. */
  private void updatePredefinedRuleStates() {
    for (Map.Entry<String, RuleItem> entry : ruleItems.entrySet()) {
      String ruleName = entry.getKey();
      RuleItem item = entry.getValue();

      boolean isEnabled =
          switch (ruleName) {
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

  /** Creates a UI element for a predefined rule and adds it to the container. */
  private void addRuleItem(VBox container, String ruleName) {
    RuleItem ruleItem = new RuleItem(ruleName);
    ruleItems.put(ruleName, ruleItem);

    ruleItem.getDocumentationButton().setOnAction(e -> handleShowDocumentation(ruleName));
    ruleItem
        .getCheckBox()
        .setOnAction(e -> handleRuleToggle(ruleName, ruleItem.getCheckBox().isSelected()));

    container.getChildren().add(ruleItem);
  }

  /** Opens a FileChooser to load a custom .rul file. */
  public void handleLoadRuleFile() {
    if (this.ruleManager == null) {
      logger.error("Cannot load rule file: RuleManager is not initialized.");
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Rule files (*.rul)", "*.rul"));
    File selectedFile = fileChooser.showOpenDialog(view.getLoadRuleButton().getScene().getWindow());

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
    logger.debug("Documentation for {} requested.", ruleName);
  }

  /** Toggles the state of a rule (both predefined and custom). */
  private void handleRuleToggle(String ruleName, boolean isSelected) {
    if (this.ruleManager == null) return;

    switch (ruleName) {
      case "RDFS Subset" -> ruleManager.setRDFSSubsetEnabled(isSelected);
      case "RDFS RL" -> ruleManager.setRDFSRLEnabled(isSelected);
      case "OWL RL" -> ruleManager.setOWLRLEnabled(isSelected);
      case "OWL RL Extended" -> ruleManager.setOWLRLExtendedEnabled(isSelected);
      case "OWL RL Test" -> ruleManager.setOWLRLTestEnabled(isSelected);
      case "OWL Clean" -> ruleManager.setOWLCleanEnabled(isSelected);
      default -> ruleManager.setCustomRuleEnabled(ruleName, isSelected);
    }

    if (onRuleToggled != null) {
      onRuleToggled.run();
    }
    refreshGraph();
  }

  private void refreshGraph() {
    if (parentController != null) {
      parentController.loadGraphData(parentController.getCurrentContent());
    }
  }

  /** Clears and re-populates the list of custom rules in the UI. */
  private void displayCustomRules() {
    if (view.getCustomRulesContainer() == null) return;
    view.getCustomRulesContainer().getChildren().clear();

    List<File> customRules = ruleManager.getLoadedRuleFiles();

    if (customRules.isEmpty()) {
      Label noRulesLabel = new Label("No custom rules loaded");
      noRulesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
      view.getCustomRulesContainer().getChildren().add(noRulesLabel);
    } else {
      for (File ruleFile : customRules) {
        String ruleName = ruleFile.getName();
        RuleItem ruleItem = new RuleItem(ruleFile);
        ruleItems.put(ruleName, ruleItem);

        ruleItem.getCheckBox().setSelected(ruleManager.isCustomRuleEnabled(ruleName));
        ruleItem
            .getCheckBox()
            .setOnAction(e -> handleRuleToggle(ruleName, ruleItem.getCheckBox().isSelected()));

        view.getCustomRulesContainer().getChildren().add(ruleItem);
      }
    }
  }

  public void setOnRuleToggled(Runnable onRuleToggled) {
    this.onRuleToggled = onRuleToggled;
  }
}

