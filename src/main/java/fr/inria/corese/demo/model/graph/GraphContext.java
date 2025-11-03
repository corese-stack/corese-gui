package fr.inria.corese.demo.model.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphContext {
  private final List<File> loadedFiles;
  private final List<File> loadedRules;
  private final Map<String, Object> graphProperties;
  private String currentNamespace;

  public GraphContext() {
    this.loadedFiles = new ArrayList<>();
    this.loadedRules = new ArrayList<>();
    this.graphProperties = new HashMap<>();
  }

  public void addLoadedFile(File file) {
    loadedFiles.add(file);
  }

  public void addLoadedRule(File rule) {
    loadedRules.add(rule);
  }

  public void setProperty(String key, Object value) {
    graphProperties.put(key, value);
  }

  public Object getProperty(String key) {
    return graphProperties.get(key);
  }

  public List<File> getLoadedFiles() {
    return new ArrayList<>(loadedFiles);
  }

  public List<File> getLoadedRules() {
    return new ArrayList<>(loadedRules);
  }

  public void setCurrentNamespace(String namespace) {
    this.currentNamespace = namespace;
  }

  public String getCurrentNamespace() {
    return currentNamespace;
  }

  public void clear() {
    loadedFiles.clear();
    loadedRules.clear();
    graphProperties.clear();
    currentNamespace = null;
  }
}
