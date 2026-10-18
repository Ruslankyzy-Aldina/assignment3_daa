package org.example.algorithms;

import org.example.graph.Edge;

import java.util.List;

public record MstResult(List<Edge> edges, long totalWeight, int components,
                        long dsuOps, long edgeScans, long comparisons) {
    public MstResult {
        edges = List.copyOf(edges);
    }
}
