package org.example.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ShortestPathResult(List<Long> distances, List<Integer> parents, long edgeScans) {
    public ShortestPathResult {
        distances = List.copyOf(distances);
        parents = List.copyOf(parents);
        if (distances.size() != parents.size()) {
            throw new IllegalArgumentException("Distances and parents must have equal sizes");
        }
    }

    public long distanceTo(int vertex) {
        checkVertex(vertex);
        return distances.get(vertex);
    }

    public List<Integer> pathTo(int vertex) {
        checkVertex(vertex);
        List<Integer> path = new ArrayList<>();
        int current = vertex;
        while (current != -1) {
            if (current < 0 || current >= parents.size() || path.size() >= parents.size()) {
                throw new IllegalStateException("Invalid parent chain");
            }
            path.add(current);
            current = parents.get(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    public long sumDistances() {
        long sum = 0;
        for (long distance : distances) {
            sum += distance;
        }
        return sum;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= distances.size()) {
            throw new IllegalArgumentException("Invalid vertex: " + vertex);
        }
    }
}
