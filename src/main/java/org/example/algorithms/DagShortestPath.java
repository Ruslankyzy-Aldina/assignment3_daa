package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DagShortestPath {
    private DagShortestPath() {
    }

    public static ShortestPathResult compute(Graph graph) {
        TopologicalResult topological = Kahn.compute(graph);
        if (topological.hasCycle()) {
            throw new IllegalArgumentException("Shortest paths require a DAG");
        }
        int n = graph.vertexCount();
        long[] distance = new long[n];
        int[] parent = new int[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(parent, -1);
        for (int vertex = 0; vertex < n; vertex++) {
            if (graph.inDegree(vertex) == 0) {
                distance[vertex] = 0;
            }
        }
        long edgeScans = 0;
        for (int vertex : topological.order()) {
            for (Edge edge : graph.neighbors(vertex)) {
                edgeScans++;
                long candidate = distance[vertex] + edge.weight();
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    parent[edge.to()] = vertex;
                }
            }
        }
        List<Long> distances = new ArrayList<>(n);
        List<Integer> parents = new ArrayList<>(n);
        for (int vertex = 0; vertex < n; vertex++) {
            distances.add(distance[vertex]);
            parents.add(parent[vertex]);
        }
        return new ShortestPathResult(distances, parents, edgeScans);
    }
}
