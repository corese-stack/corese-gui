package fr.inria.corese.demo.model;

public class QueryResult {
    private final String queryType;
    private final String tableResult;
    private final String graphResult;
    private final String xmlResult;
    private final String jsonResult;
    private final String csvResult;
    private final String tsvResult;
    private final String markdownResult;

    public QueryResult(String queryType, String tableResult, String graphResult,
        String xmlResult, String jsonResult, String csvResult,
        String tsvResult, String markdownResult) {
        this.queryType = queryType;
        this.tableResult = tableResult;
        this.graphResult = graphResult;
        this.xmlResult = xmlResult;
        this.jsonResult = jsonResult;
        this.csvResult = csvResult;
        this.tsvResult = tsvResult;
        this.markdownResult = markdownResult;
    }

    public String getQueryType() {
        return queryType;
    }

    public String getTableResult() {
        return tableResult;
    }

    public String getGraphResult() {
        return graphResult;
    }

    public String getXmlResult() {
        return xmlResult;
    }

    public String getJsonResult() {
        return jsonResult;
    }

    public String getCsvResult() {
        return csvResult;
    }

    public String getTsvResult() {
        return tsvResult;
    }

    public String getMarkdownResult() {
        return markdownResult;
    }
}