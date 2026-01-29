package fr.inria.corese.gui.core.manager;

import fr.inria.corese.gui.core.model.ValidationReportItem;
import fr.inria.corese.gui.feature.validation.ValidationResult;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.shacl.Shacl;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.kgram.core.Mappings;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for SHACL validation operations.
 * Handles the interaction with Corese SHACL validator and result formatting.
 */
public class ShaclManager {
    private static ShaclManager instance;

    private ShaclManager() {}

    public static synchronized ShaclManager getInstance() {
        if (instance == null) {
            instance = new ShaclManager();
        }
        return instance;
    }

    /**
     * Validates the current data graph against the provided shapes content.
     *
     * @param shapesContent The SHACL shapes in Turtle format.
     * @return The result of the validation.
     */
    public ValidationResult validate(String shapesContent) {
        try {
            // Get the data graph from the manager
            Graph dataGraph = CoreseGraphManager.getInstance().getGraph();

            Graph shapeGraph = Graph.create();
            Load.create(shapeGraph)
                .parse(
                    new ByteArrayInputStream(shapesContent.getBytes(StandardCharsets.UTF_8)),
                    Load.format.TURTLE_FORMAT);

            Shacl validator = new Shacl(dataGraph, shapeGraph);

            Graph reportGraph = validator.eval();

            boolean conforms = validator.conform(reportGraph);

            return new ValidationResult(conforms, reportGraph, null);

        } catch (Throwable e) {
            e.printStackTrace();
            return new ValidationResult(false, null, "Validation Error: " + e.getMessage());
        }
    }

    /**
     * Extracts validation report items from the report graph.
     *
     * @param result The validation result containing the report graph.
     * @return A list of ValidationReportItem objects.
     */
    public List<ValidationReportItem> extractReportItems(ValidationResult result) {
        List<ValidationReportItem> items = new ArrayList<>();
        if (result == null || result.getReportGraph() == null) {
            return items;
        }

        try {
            QueryProcess exec = QueryProcess.create(result.getReportGraph());
                        String query = "SELECT ?severity ?focusNode ?resultPath ?value ?message WHERE { " + 
                                "?r a <http://www.w3.org/ns/shacl#ValidationResult> ; " +
                                "<http://www.w3.org/ns/shacl#resultSeverity> ?severity ; " +
                                "<http://www.w3.org/ns/shacl#focusNode> ?focusNode . " +
                                "OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultPath> ?resultPath } " +
                                "OPTIONAL { ?r <http://www.w3.org/ns/shacl#value> ?value } " +
                                "OPTIONAL { ?r <http://www.w3.org/ns/shacl#resultMessage> ?message } " +
                                "}";
            Mappings map = exec.query(query);
            for (var mapping : map) {
                String severity = mapping.getValue("?severity") != null ? mapping.getValue("?severity").getLabel() : "";
                String focusNode = mapping.getValue("?focusNode") != null ? mapping.getValue("?focusNode").getLabel() : "";
                String resultPath = mapping.getValue("?resultPath") != null ? mapping.getValue("?resultPath").getLabel() : "";
                String value = mapping.getValue("?value") != null ? mapping.getValue("?value").getLabel() : "";
                String message = mapping.getValue("?message") != null ? mapping.getValue("?message").getLabel() : "";

                // Simplify severity URI
                if (severity.contains("#")) severity = severity.substring(severity.lastIndexOf("#") + 1);

                items.add(new ValidationReportItem(severity, focusNode, resultPath, value, message));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
}
