package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.ArrayList;
import java.util.List;

public final class Kruskal {
    private Kruskal() {
    }

    public static MstResult compute(Graph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph");
        }
        List<Edge> sorted = new ArrayList<>(graph.edges());
        WeightComparator comparator = new WeightComparator();
        sorted.sort(comparator);
        DSU dsu = new DSU(graph.vertexCount());
        List<Edge> forest = new ArrayList<>();
        long totalWeight = 0;
        long edgeScans = 0;
        for (Edge edge : sorted) {
            if (dsu.components() <= 1) {
                break;
            }
            edgeScans++;
            if (dsu.union(edge.from(), edge.to())) {
                forest.add(edge);
                totalWeight += edge.weight();
            }
        }
        return new MstResult(forest, totalWeight, dsu.components(),
                dsu.dsuOps(), edgeScans, comparator.comparisons());
    }
}
