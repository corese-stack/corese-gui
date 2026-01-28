package fr.inria.corese.gui.core.model;



public class ValidationReportItem {
    private String severity;
    private String focusNode;
    private String resultPath;
    private String value;
    private String message;

    public ValidationReportItem(String severity, String focusNode, String resultPath, String value, String message) {
        this.severity = severity;
        this.focusNode = focusNode;
        this.resultPath = resultPath;
        this.value = value;
        this.message = message;
    }

    public String getSeverity() { return severity; }
    public String getFocusNode() { return focusNode; }
    public String getResultPath() { return resultPath; }
    public String getValue() { return value; }
    public String getMessage() { return message; }
}
