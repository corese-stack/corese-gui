package fr.inria.corese.gui.core.model;

/**
 * Represents the result of a SHACL validation execution.
 *
 * <p>This DTO is GUI-facing and must not expose corese-core types.
 */
public class ValidationResult {
    private final boolean conforms;
    private final String reportId;
    private final String errorMessage;
    private final String errorDetails;

    public ValidationResult(boolean conforms, String reportId, String errorMessage) {
        this(conforms, reportId, errorMessage, null);
    }

    public ValidationResult(boolean conforms, String reportId, String errorMessage, String errorDetails) {
        this.conforms = conforms;
        this.reportId = reportId;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
    }

    public boolean isConforms() {
        return conforms;
    }

    /**
     * Identifier for the cached SHACL report, if available.
     */
    public String getReportId() {
        return reportId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
}
