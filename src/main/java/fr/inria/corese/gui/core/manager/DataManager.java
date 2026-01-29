package fr.inria.corese.gui.core.manager;

import fr.inria.corese.core.load.Load;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DataManager {

  //===============================================================================================
  // Constants
  //===============================================================================================

  private static DataManager instance;
  
  private final List<File> loadedFiles;
  private final QueryManager queryManager;
  private final GraphManager graphManager;

  private DataManager() {
    this.loadedFiles = new ArrayList<>();
    this.queryManager = QueryManager.getInstance();
    this.graphManager = GraphManager.getInstance();
  }

  /**
   * Returns the singleton instance of DataManager.
   * 
   * @return the DataManager instance
   */
  public static synchronized DataManager getInstance() {
    if (instance == null) {
      instance = new DataManager();
    }
    return instance;
  }

  // ========== Public API - File Loading ==========

  /**
   * Loads an RDF file into the graph. Supports Turtle, RDF/XML, and JSON-LD formats.
   * The format is automatically detected based on the file extension.
   * 
   * @param file the RDF file to load
   * @throws Exception if the file cannot be loaded
   */
  public synchronized void loadFile(File file) throws Exception {
    try {
      Load loader = Load.create(graphManager.getGraph());
      Load.format format = detectFormat(file);
      loader.parse(file.getAbsolutePath(), format);
      
      if (!loadedFiles.contains(file)) {
        loadedFiles.add(file);
      }
      
      log("File loaded successfully: " + file.getName());
    } catch (Exception e) {
      log("Error loading file: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Removes a file from the loaded files list and reloads the graph.
   * 
   * @param file the file to remove
   */
  public synchronized void removeFile(File file) {
    if (loadedFiles.remove(file)) {
      reloadFiles();
      log("File removed and graph reloaded: " + file.getName());
    } else {
      log("File not found in loaded list: " + file.getName());
    }
  }

  /**
   * Clears all loaded files and reinitializes the graph.
   */
  public synchronized void clearGraphAndFiles() {
    graphManager.initializeGraph();
    loadedFiles.clear();
    log("Graph and loaded files cleared.");
  }

  /**
   * Reloads all previously loaded files into a fresh graph.
   */
  public synchronized void reloadFiles() {
    List<File> filesToReload = new ArrayList<>(loadedFiles);
    graphManager.initializeGraph();
    
    log("Reloading all files...");
    for (File file : filesToReload) {
      try {
        loadFile(file);
      } catch (Exception e) {
        log("Error reloading file " + file.getName() + ": " + e.getMessage());
      }
    }
    log("All files reloaded.");
  }

  // ========== Public API - File Saving ==========

  /**
   * Saves the current graph to a Turtle file.
   * If the target file doesn't have a .ttl extension, it will be added.
   * 
   * @param targetFile the destination file
   * @throws Exception if the graph cannot be saved
   */
  public synchronized void saveGraph(File targetFile) throws Exception {
    File graphFile = ensureTurtleExtension(targetFile);
    
    try (FileOutputStream out = new FileOutputStream(graphFile)) {
      String turtleRepresentation = ExportManager.getInstance()
          .formatGraph(graphManager.getGraph(), SerializationFormat.TURTLE);
      out.write(turtleRepresentation.getBytes());
    }
    
    log("Graph saved to: " + graphFile.getAbsolutePath());
  }

  // ========== Public API - Getters ==========

  /**
   * Returns a copy of the list of loaded files.
   * 
   * @return a new list containing all loaded files
   */
  public List<File> getLoadedFiles() {
    return new ArrayList<>(loadedFiles);
  }

  /**
   * Returns the content of the most recently loaded file.
   * Returns an empty string if no files are loaded or if reading fails.
   * 
   * @return the content of the last loaded file, or empty string
   */
  public String getCurrentContent() {
    if (loadedFiles.isEmpty()) {
      return "";
    }
    
    File lastFile = loadedFiles.get(loadedFiles.size() - 1);
    try {
      return Files.readString(lastFile.toPath());
    } catch (IOException e) {
      log("Error reading file content: " + e.getMessage());
      return "";
    }
  }

  // ========== Private Helper Methods ==========

  private Load.format detectFormat(File file) {
    String fileName = file.getName().toLowerCase();
    if (fileName.endsWith(".ttl")) {
      return Load.format.TURTLE_FORMAT;
    } else if (fileName.endsWith(".rdf") || fileName.endsWith(".xml")) {
      return Load.format.RDFXML_FORMAT;
    } else if (fileName.endsWith(".jsonld")) {
      return Load.format.JSONLD_FORMAT;
    } else {
      return Load.format.TURTLE_FORMAT;
    }
  }

  private File ensureTurtleExtension(File file) {
    String baseName = file.getName();
    if (!baseName.toLowerCase().endsWith(".ttl")) {
      baseName += ".ttl";
    }
    return new File(file.getParentFile(), baseName);
  }

  private void log(String message) {
    queryManager.addLogEntry(message);
  }
}