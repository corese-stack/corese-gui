package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.model.fileList.FileListModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static DataManager instance;
    private final FileListModel fileListModel;
    private final List<File> loadedFiles;
    private List<File> savedLoadedFiles;
    private final QueryManager queryManager;
    private final GraphManager graphManager;

    private DataManager() {
        this.fileListModel = new FileListModel();
        this.loadedFiles = new ArrayList<>();
        this.savedLoadedFiles = new ArrayList<>();
        this.queryManager = QueryManager.getInstance();
        this.graphManager = GraphManager.getInstance();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public synchronized void loadFile(File file) throws Exception {
        try {
            Load loader = Load.create(graphManager.getGraph());
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".ttl")) {
                loader.parse(file.getAbsolutePath(), Load.format.TURTLE_FORMAT);
            } else if (fileName.endsWith(".rdf") || fileName.endsWith(".xml")) {
                loader.parse(file.getAbsolutePath(), Load.format.RDFXML_FORMAT);
            } else if (fileName.endsWith(".jsonld")) {
                loader.parse(file.getAbsolutePath(), Load.format.JSONLD_FORMAT);
            } else {
                loader.parse(file.getAbsolutePath(), Load.format.TURTLE_FORMAT);
            }
            fileListModel.addFile(file);
            if (!loadedFiles.contains(file)) {
                loadedFiles.add(file);
            }
            queryManager.addLogEntry("File loaded successfully: " + file.getName());
        } catch (Exception e) {
            queryManager.addLogEntry("Error loading file: " + e.getMessage());
            throw e;
        }
    }

    public synchronized void reloadFiles() {
        List<File> filesToReload = new ArrayList<>(this.loadedFiles);
        graphManager.initializeGraph();
        fileListModel.clearFiles();
        queryManager.addLogEntry("Reloading all files...");
        for (File file : filesToReload) {
            try {
                loadFile(file);
            } catch (Exception e) {
                queryManager.addLogEntry("Error reloading file " + file.getName() + ": " + e.getMessage());
            }
        }
        queryManager.addLogEntry("All files reloaded.");
    }

    public synchronized void saveGraph(File targetFile) throws Exception {
        String baseName = targetFile.getName();
        if (!baseName.toLowerCase().endsWith(".ttl")) {
            baseName += ".ttl";
        }
        File graphFile = new File(targetFile.getParentFile(), baseName);
        try (FileOutputStream out = new FileOutputStream(graphFile)) {
            String turtleRepresentation = queryManager.formatGraph(graphManager.getGraph(), ResultFormat.format.TURTLE_FORMAT);
            out.write(turtleRepresentation.getBytes());
        }
        queryManager.addLogEntry("Graph saved to: " + graphFile.getAbsolutePath());
    }

    public synchronized void saveCurrentState() {
        this.savedLoadedFiles = new ArrayList<>(this.loadedFiles);
        queryManager.addLogEntry("Current state snapshot saved (" + savedLoadedFiles.size() + " files).");
    }

    public synchronized void restoreState() {
        this.loadedFiles.clear();
        this.loadedFiles.addAll(this.savedLoadedFiles);
        reloadFiles();
        queryManager.addLogEntry("State restored from snapshot (" + loadedFiles.size() + " files).");
    }

    public FileListModel getFileListModel() {
        return fileListModel;
    }

    public List<File> getLoadedFiles() {
        return new ArrayList<>(loadedFiles);
    }

    public synchronized void clearGraphAndFiles() {
        graphManager.initializeGraph();
        this.fileListModel.clearFiles();
        this.loadedFiles.clear();
        queryManager.addLogEntry("Graph and loaded files cleared.");
    }

    public synchronized void removeFile(File file) {
        if (loadedFiles.remove(file)) {
            fileListModel.removeFile(file.getName());
            reloadFiles();
            queryManager.addLogEntry("File removed and graph reloaded: " + file.getName());
        } else {
            queryManager.addLogEntry("File not found in loaded list: " + file.getName());
        }
    }

    public String getCurrentContent() {
        if (loadedFiles.isEmpty()) {
            return "";
        }
        File lastFile = loadedFiles.get(loadedFiles.size() - 1);
        try {
            return Files.readString(lastFile.toPath());
        } catch (IOException e) {
            queryManager.addLogEntry("Error reading file content: " + e.getMessage());
            return "";
        }
    }
}
