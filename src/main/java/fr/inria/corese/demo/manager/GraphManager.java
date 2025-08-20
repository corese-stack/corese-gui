package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;

import java.io.File;

public class GraphManager {
    private static GraphManager instance;

    private Graph originalGraph; 
    private Graph workingGraph; 

    private GraphManager() {
        initializeGraph();
    }

    public static synchronized GraphManager getInstance() {
        if (instance == null) {
            instance = new GraphManager();
        }
        return instance;
    }
    public void initializeGraph() {
        this.originalGraph = Graph.create();
        this.workingGraph = Graph.create();
    }

    /**
     * Loads data from a file into the original graph and copies it to the working graph.
     * @param file The data file to load.
     * @throws LoadException if loading fails.
     */
    public void loadData(File file) throws LoadException {
        initializeGraph(); // Clear previous data
        Load loader = Load.create(originalGraph);
        loader.parse(file.getAbsolutePath());
        resetGraphToOriginal(); // Copy to working graph
    }
    
    /**
     * Loads data from a graph into the original graph and copies it to the working graph.
     * @param graph The graph to load.
     */
    public void loadData(Graph graph) {
        initializeGraph(); // Clear previous data
        originalGraph.copy(graph);
        resetGraphToOriginal(); // Copy to working graph
    }

    /**
     * Returns the working graph for operations like querying and validation.
     * @return The working Graph instance.
     */
    public Graph getGraph() {
        return this.workingGraph;
    }

    /**
     * Resets the working graph to the state of the original loaded data.
     * This is useful for re-applying rules or running queries on a clean slate.
     */
    public void resetGraphToOriginal() {
        if (originalGraph != null) {
            this.workingGraph = originalGraph.copy();
        }
    }

    public int getTripletCount() {
        return (this.workingGraph != null) ? this.workingGraph.size() : 0;
    }
}
