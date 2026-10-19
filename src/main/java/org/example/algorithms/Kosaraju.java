package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Kosaraju {
    private Kosaraju() {
    }

    public static SccResult compute(Graph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("SCC requires a directed graph");
        }
        int n = graph.vertexCount();
        Graph reversed = new Graph(n, true);
        for (Edge edge : graph.edges()) {
            reversed.addEdge(edge.to(), edge.from(), edge.weight());
        }
        boolean[] visited = new boolean[n];
        int[] stack = new int[n];
        int[] nextEdge = new int[n];
        int[] finishOrder = new int[n];
        int finished = 0;
        int maxDepth = 0;
        long edgeScans = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            int depth = 1;
            stack[0] = start;
            visited[start] = true;
            maxDepth = Math.max(maxDepth, depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                List<Edge> neighbors = graph.neighbors(vertex);
                if (nextEdge[vertex] < neighbors.size()) {
                    Edge edge = neighbors.get(nextEdge[vertex]++);
                    edgeScans++;
                    if (!visited[edge.to()]) {
                        visited[edge.to()] = true;
                        stack[depth++] = edge.to();
                        maxDepth = Math.max(maxDepth, depth);
                    }
                } else {
                    finishOrder[finished++] = vertex;
                    depth--;
                }
            }
        }
        int[] componentOf = new int[n];
        Arrays.fill(componentOf, -1);
        Arrays.fill(nextEdge, 0);
        int components = 0;
        for (int i = finished - 1; i >= 0; i--) {
            int start = finishOrder[i];
            if (componentOf[start] != -1) {
                continue;
            }
            int depth = 1;
            stack[0] = start;
            componentOf[start] = components;
            maxDepth = Math.max(maxDepth, depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                List<Edge> neighbors = reversed.neighbors(vertex);
                if (nextEdge[vertex] < neighbors.size()) {
                    Edge edge = neighbors.get(nextEdge[vertex]++);
                    edgeScans++;
                    if (componentOf[edge.to()] == -1) {
                        componentOf[edge.to()] = components;
                        stack[depth++] = edge.to();
                        maxDepth = Math.max(maxDepth, depth);
                    }
                } else {
                    depth--;
                }
            }
            components++;
        }
        List<Integer> ids = new ArrayList<>(n);
        for (int id : componentOf) {
            ids.add(id);
        }
        return new SccResult(ids, components, edgeScans, maxDepth);
    }
}
