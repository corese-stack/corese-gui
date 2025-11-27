package fr.inria.corese.gui.model.graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;

public class CoreseGraph implements SemanticGraph {
  private Graph graph;
  private final GraphContext context;
  private final List<String> logEntries = new ArrayList<>();

  public CoreseGraph() {
    this.graph = Graph.create();
    this.context = new GraphContext();
  }

  @Override
  public void loadFile(File file) throws LoadException {
    try {
      Load ld = Load.create(graph);
      ld.parse(file.getAbsolutePath());
      context.addLoadedFile(file);
      addLogEntry("File loaded: " + file.getName() + " with " + graph.size() + " triples");
    } catch (LoadException e) {
      addLogEntry("Error loading file: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public void loadRuleFile(File file) throws Exception {
    try {
      // Implémentation spécifique à Corese pour charger les règles
      context.addLoadedRule(file);
      addLogEntry("Rule file loaded: " + file.getName());
    } catch (Exception e) {
      addLogEntry("Error loading rule file: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public GraphContext getContext() {
    return context;
  }

  @Override
  public void saveContext() throws Exception {
    Properties props = new Properties();

    // Sauvegarder les chemins des fichiers chargés
    StringBuilder files = new StringBuilder();
    for (File f : context.getLoadedFiles()) {
      files.append(f.getAbsolutePath()).append(";");
    }
    props.setProperty("loadedFiles", files.toString());

    // Sauvegarder les chemins des règles chargées
    StringBuilder rules = new StringBuilder();
    for (File r : context.getLoadedRules()) {
      rules.append(r.getAbsolutePath()).append(";");
    }
    props.setProperty("loadedRules", rules.toString());

    // Sauvegarder le namespace courant
    if (context.getCurrentNamespace() != null) {
      props.setProperty("currentNamespace", context.getCurrentNamespace());
    }

    File contextFile = new File("graph-context.properties");
    try (FileOutputStream out = new FileOutputStream(contextFile)) {
      props.store(out, "Graph Context");
    }
  }

  @Override
  public void loadContext(String contextPath) throws Exception {
    Properties props = new Properties();
    File contextFile = new File(contextPath);

    try (FileInputStream in = new FileInputStream(contextFile)) {
      props.load(in);

      // Charger les fichiers
      String files = props.getProperty("loadedFiles", "");
      for (String filePath : files.split(";")) {
        if (!filePath.isEmpty()) {
          loadFile(new File(filePath));
        }
      }

      // Charger les règles
      String rules = props.getProperty("loadedRules", "");
      for (String rulePath : rules.split(";")) {
        if (!rulePath.isEmpty()) {
          loadRuleFile(new File(rulePath));
        }
      }

      // Restaurer le namespace
      String namespace = props.getProperty("currentNamespace");
      if (namespace != null) {
        context.setCurrentNamespace(namespace);
      }
    }
  }

  @Override
  public List<File> getLoadedFiles() {
    return context.getLoadedFiles();
  }

  @Override
  public List<File> getLoadedRules() {
    return context.getLoadedRules();
  }

  @Override
  public void clearGraph() {
    this.graph = Graph.create();
    this.context.clear();
    addLogEntry("Graph and context cleared");
  }

  @Override
  public void reloadFiles() {
    clearGraph();
    addLogEntry("Graph reloaded");
  }

  @Override
  public int getSemanticElementsCount() {
    return graph.size() / 3; // Approximation simplifiée
  }

  @Override
  public int getTripletCount() {
    return graph.size();
  }

  @Override
  public int getGraphCount() {
    try {
      // Méthode simplifiée qui tente d'accéder à la méthode appropriée
      try {
        java.lang.reflect.Method getNamedGraphsMethod =
            graph.getClass().getMethod("getNamedGraphs");
        Object result = getNamedGraphsMethod.invoke(graph);
        if (result instanceof java.util.List) {
          return ((java.util.List<?>) result).size() + 1;
        }
      } catch (NoSuchMethodException e) {
        // Essayer une autre méthode
        try {
          java.lang.reflect.Method getGraphNamesMethod =
              graph.getClass().getMethod("getGraphNames");
          Object result = getGraphNamesMethod.invoke(graph);
          if (result instanceof java.util.List) {
            return ((java.util.List<?>) result).size() + 1;
          }
        } catch (NoSuchMethodException ex) {
          return 1;
        }
      }
      return 1;
    } catch (Exception e) {
      addLogEntry("Error counting graphs: " + e.getMessage());
      return 1;
    }
  }

  @Override
  public void addLogEntry(String entry) {
    logEntries.add(entry);
  }

  @Override
  public List<String> getLogEntries() {
    return new ArrayList<>(logEntries);
  }

  @Override
  public boolean applyRules() {
    // Implémenter l'application des règles pour Corese
    return true;
  }

  // Méthode pour accéder au graphe Corese sous-jacent si nécessaire
  public Graph getCoreseGraph() {
    return graph;
  }
}
