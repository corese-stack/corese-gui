package fr.inria.corese.demo.manager;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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

    public synchronized void loadGraph(String content) {
        initializeGraph();
        if (content != null && !content.trim().isEmpty()) {
            try {
                Load.create(graph).parse(
                        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                        Load.format.TURTLE_FORMAT
                );
            } catch (LoadException e) {
                e.printStackTrace();
            }
        }
    }
}
