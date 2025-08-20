package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.demo.model.fileList.FileListModel;

import java.io.File;
import java.io.FileOutputStream;
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

    public void loadFile(File file) throws Exception {
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
            fileListModel.addFile(file.getName());
            if (!loadedFiles.contains(file)) {
                loadedFiles.add(file);
            }
            queryManager.addLogEntry("File loaded successfully: " + file.getName());
        } catch (Exception e) {
            queryManager.addLogEntry("Error loading file: " + e.getMessage());
            throw e;
        }
    }

    public void reloadFiles() {
        List<File> filesToReload = new ArrayList<>(this.loadedFiles);
        graphManager.initializeGraph();
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

    public void saveGraph(File targetFile) throws Exception {
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

    public void saveCurrentState() {
        this.savedLoadedFiles = new ArrayList<>(this.loadedFiles);
        queryManager.addLogEntry("Current state snapshot saved (" + savedLoadedFiles.size() + " files).");
    }

    public void restoreState() {
        this.loadedFiles.clear();
        this.loadedFiles.addAll(this.savedLoadedFiles);
        reloadFiles();
        this.fileListModel.clearFiles();
        for (File file : this.loadedFiles) {
            this.fileListModel.addFile(file.getName());
        }
        queryManager.addLogEntry("State restored from snapshot (" + loadedFiles.size() + " files).");
    }

    public FileListModel getFileListModel() {
        return fileListModel;
    }

    public List<File> getLoadedFiles() {
        return new ArrayList<>(loadedFiles);
    }

    public void clearGraphAndFiles() {
        graphManager.initializeGraph();
        this.fileListModel.clearFiles();
        this.loadedFiles.clear();
        queryManager.addLogEntry("Graph cleared and files reloaded.");
    }
}
