package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {
    private Prim() {
    }

    public static MstResult compute(Graph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph");
        }
        boolean[] visited = new boolean[graph.vertexCount()];
        WeightComparator comparator = new WeightComparator();
        PriorityQueue<Edge> queue = new PriorityQueue<>(comparator);
        List<Edge> forest = new ArrayList<>();
        long totalWeight = 0;
        long edgeScans = 0;
        int components = 0;
        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visited[start] = true;
            for (Edge edge : graph.neighbors(start)) {
                edgeScans++;
                if (!visited[edge.to()]) {
                    queue.add(edge);
                }
            }
            while (!queue.isEmpty()) {
                Edge next = queue.remove();
                if (visited[next.to()]) {
                    continue;
                }
                visited[next.to()] = true;
                forest.add(next);
                totalWeight += next.weight();
                for (Edge edge : graph.neighbors(next.to())) {
                    edgeScans++;
                    if (!visited[edge.to()]) {
                        queue.add(edge);
                    }
                }
            }
        }
        return new MstResult(forest, totalWeight, components,
                0, edgeScans, comparator.comparisons());
    }
}
