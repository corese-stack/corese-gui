package fr.inria.corese.demo.view;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * View for managing rules (RDFS, OWL, Personal).
 * Replaces rule-view.fxml.
 */
public class RuleView extends VBox {

    private final VBox rdfsRulesContainer;
    private final VBox owlRulesContainer;
    private final VBox customRulesContainer;
    private final Button loadRuleButton;

    public RuleView() {
        this.rdfsRulesContainer = new VBox(5);
        this.owlRulesContainer = new VBox(5);
        this.customRulesContainer = new VBox(5);
        this.loadRuleButton = new Button("Load Rule");

        initializeLayout();
    }

    private void initializeLayout() {
        setSpacing(4);
        setPadding(new Insets(4));
        getStyleClass().addAll("section-container", "rules-container");

        // RDFS Rules
        TitledPane rdfsPane = new TitledPane("RDFS Rules", new VBox(5, rdfsRulesContainer));
        rdfsPane.setExpanded(true);

        // OWL Rules
        TitledPane owlPane = new TitledPane("OWL Rules", new VBox(5, owlRulesContainer));
        owlPane.setExpanded(true);

        // Personal Rules
        HBox personalRulesHeader = new HBox(10);
        personalRulesHeader.setAlignment(Pos.CENTER_LEFT);
        Label personalRulesLabel = new Label("Personal Rules");
        personalRulesLabel.getStyleClass().add("rules-title-text");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        loadRuleButton.getStyleClass().add("blue-button"); // Keeping original style class for now, maybe update to AtlantaFX later
        loadRuleButton.getStyleClass().add(Styles.ACCENT); // AtlantaFX accent style

        personalRulesHeader.getChildren().addAll(personalRulesLabel, spacer, loadRuleButton);

        TitledPane personalPane = new TitledPane();
        personalPane.setGraphic(personalRulesHeader);
        personalPane.setContent(new VBox(5, customRulesContainer));
        personalPane.setExpanded(true);
        // TitledPane graphic consumes the whole header area, so we set text to empty or null if we use graphic for everything
        personalPane.setText(""); 

        getChildren().addAll(rdfsPane, owlPane, personalPane);
    }

    public VBox getRdfsRulesContainer() {
        return rdfsRulesContainer;
    }

    public VBox getOwlRulesContainer() {
        return owlRulesContainer;
    }

    public VBox getCustomRulesContainer() {
        return customRulesContainer;
    }

    public Button getLoadRuleButton() {
        return loadRuleButton;
    }
}
