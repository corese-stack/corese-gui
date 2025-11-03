package fr.inria.corese.demo.model;

import java.util.Map;

public class FormattedResult {
  private final String queryType;
  private final String primaryResult;
  private final Map<String, String> allFormats;

  public FormattedResult(String queryType, String primaryResult, Map<String, String> allFormats) {
    this.queryType = queryType;
    this.primaryResult = primaryResult;
    this.allFormats = allFormats;
  }

  public String getQueryType() {
    return queryType;
  }

  public String getPrimaryResult() {
    return primaryResult;
  }

  public String getFroamattedResult(String format) {
    return allFormats.getOrDefault(format.toUpperCase(), "format not supported");
  }
}
