package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;

import java.io.File;

public class GraphManager {
    private static GraphManager instance;

    private Graph graph;

    private GraphManager() {
        this.graph = Graph.create();
    }

    public static synchronized GraphManager getInstance() {
        if (instance == null) {
            instance = new GraphManager();
        }
        return instance;
    }

    public synchronized void initializeGraph() {
        if (this.graph != null) {
            this.graph.clear();
        } else {
            this.graph = Graph.create();
        }
    }

    public synchronized Graph getGraph() {
        return this.graph;
    }

    public synchronized int getTripletCount() {
        return (this.graph != null) ? this.graph.size() : 0;
    }
}
