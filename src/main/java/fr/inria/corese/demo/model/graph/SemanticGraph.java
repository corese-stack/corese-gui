package fr.inria.corese.demo.model.graph;

import java.io.File;
import java.util.List;

public interface SemanticGraph {
  void loadFile(File file) throws Exception;

  void clearGraph();

  void reloadFiles();

  // Nouvelles méthodes pour gérer le contexte
  GraphContext getContext();

  void saveContext() throws Exception;

  void loadContext(String contextPath) throws Exception;

  List<File> getLoadedFiles();

  List<File> getLoadedRules();

  // Méthodes existantes
  int getSemanticElementsCount();

  int getTripletCount();

  int getGraphCount();

  void addLogEntry(String entry);

  List<String> getLogEntries();

  boolean applyRules();

  void loadRuleFile(File file) throws Exception;
}
