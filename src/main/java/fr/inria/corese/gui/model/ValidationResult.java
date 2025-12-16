package fr.inria.corese.gui.model;

import fr.inria.corese.core.Graph;

/**
 * Represents the result of a SHACL validation execution.
 */
public class ValidationResult {
    private final boolean conforms;
    private final Graph reportGraph;
    private final String errorMessage;

    public ValidationResult(boolean conforms, Graph reportGraph, String errorMessage) {
        this.conforms = conforms;
        this.reportGraph = reportGraph;
        this.errorMessage = errorMessage;
    }

    public boolean isConforms() {
        return conforms;
    }

    public Graph getReportGraph() {
        return reportGraph;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
