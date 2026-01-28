package fr.inria.corese.gui.feature.rule;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import atlantafx.base.theme.Styles;

/**
 * Vue représentant une liste de règles dans l'interface utilisateur.
 */
public class RuleView extends VBox {
    private final VBox customRulesContainer;
    private final VBox rdfsRulesContainer;
    private final VBox owlRulesContainer;
    private final Button loadRuleButton;

    public RuleView() {
        setSpacing(20);
        setPadding(new Insets(20));

        loadRuleButton = new Button("Load Rule File");
        loadRuleButton.getStyleClass().add(Styles.ACCENT);

        customRulesContainer = new VBox(10);
        rdfsRulesContainer = new VBox(10);
        owlRulesContainer = new VBox(10);

        Label customTitle = new Label("Custom Rules");
        customTitle.getStyleClass().add(Styles.TITLE_4);

        Label rdfsTitle = new Label("RDFS Rules");
        rdfsTitle.getStyleClass().add(Styles.TITLE_4);

        Label owlTitle = new Label("OWL Rules");
        owlTitle.getStyleClass().add(Styles.TITLE_4);

        getChildren().addAll(
            loadRuleButton,
            customTitle, customRulesContainer,
            rdfsTitle, rdfsRulesContainer,
            owlTitle, owlRulesContainer
        );
    }

    public VBox getCustomRulesContainer() {
        return customRulesContainer;
    }

    public VBox getRdfsRulesContainer() {
        return rdfsRulesContainer;
    }

    public VBox getOwlRulesContainer() {
        return owlRulesContainer;
    }

    public Button getLoadRuleButton() {
        return loadRuleButton;
    }
}