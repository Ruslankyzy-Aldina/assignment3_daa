package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Kahn {
    private Kahn() {
    }

    public static TopologicalResult compute(Graph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Topological ordering requires a directed graph");
        }
        int n = graph.vertexCount();
        int[] inDegree = new int[n];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < n; vertex++) {
            inDegree[vertex] = graph.inDegree(vertex);
            if (inDegree[vertex] == 0) {
                queue.add(vertex);
            }
        }
        List<Integer> order = new ArrayList<>(n);
        long edgeScans = 0;
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order.add(vertex);
            for (Edge edge : graph.neighbors(vertex)) {
                edgeScans++;
                if (--inDegree[edge.to()] == 0) {
                    queue.add(edge.to());
                }
            }
        }
        boolean hasCycle = order.size() != n;
        return new TopologicalResult(hasCycle ? List.of() : order, hasCycle, edgeScans);
    }

    public static boolean isValidOrder(Graph graph, List<Integer> order) {
        if (!graph.isDirected() || order.size() != graph.vertexCount()) {
            return false;
        }
        int[] position = new int[graph.vertexCount()];
        Arrays.fill(position, -1);
        for (int i = 0; i < order.size(); i++) {
            Integer vertex = order.get(i);
            if (vertex == null || vertex < 0 || vertex >= position.length || position[vertex] != -1) {
                return false;
            }
            position[vertex] = i;
        }
        for (Edge edge : graph.edges()) {
            if (position[edge.from()] >= position[edge.to()]) {
                return false;
            }
        }
        return true;
    }
}
