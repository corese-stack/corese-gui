package fr.inria.corese.gui.feature.data.dialog;

import atlantafx.base.theme.Styles;
import fr.inria.corese.gui.core.dialog.DialogLayout;
import fr.inria.corese.gui.core.dialog.ModalService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Modal dialog displaying a short user-facing explanation for one reasoning
 * mode.
 */
public final class DataReasoningInfoDialog {

	private static final String DIALOG_TITLE = "Reasoning Mode - RDFS Subset";
	private static final String DIALOG_SUBTITLE = "Lightweight native RDFS entailment in Corese.";
	private static final String INTRO_TEXT = """
			RDFS Subset enables Corese's lightweight native RDFS entailment on the current graph.
			""";
	private static final String WHAT_IT_DOES_TEXT = """
			- infer rdf:type from rdfs:domain and rdfs:range
			- propagate triples through rdfs:subPropertyOf
			- use the rdfs:subClassOf hierarchy for rdf:type queries
			- infer reverse or symmetric triples from owl:inverseOf and owl:SymmetricProperty
			""";
	private static final String RESULT_TEXT = """
			- Some inferred triples are added to Corese's entailment graph (kg:entailment).
			- Not all consequences are materialized there.
			- Subclass-based rdf:type results may also be resolved at query time.
			""";
	private static final String SCOPE_TEXT = """
			This is an RDFS subset, not the full RDFS closure.
			""";

	private DataReasoningInfoDialog() {
		throw new AssertionError("Utility class");
	}

	public static void showRdfsSubsetInfo() {
		VBox content = new VBox(10, createMessageLabel(INTRO_TEXT), createSectionTitle("What it does"),
				createMessageLabel(WHAT_IT_DOES_TEXT), createSectionTitle("How results appear"),
				createMessageLabel(RESULT_TEXT), createSectionTitle("Scope"), createMessageLabel(SCOPE_TEXT));
		content.setPadding(new Insets(0));

		Button closeButton = new Button("Close");
		closeButton.setCancelButton(true);
		closeButton.setOnAction(event -> ModalService.getInstance().hide());

		ModalService.getInstance().show(new DialogLayout(DIALOG_TITLE, DIALOG_SUBTITLE, content, closeButton));
	}

	private static Label createSectionTitle(String text) {
		Label label = new Label(text);
		label.getStyleClass().addAll("dialog-message", Styles.TEXT_BOLD);
		label.setWrapText(true);
		return label;
	}

	private static Label createMessageLabel(String text) {
		Label label = new Label(text);
		label.getStyleClass().add("dialog-message");
		label.setWrapText(true);
		return label;
	}
}
