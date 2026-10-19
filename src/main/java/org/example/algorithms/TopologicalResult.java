package org.example.algorithms;

import java.util.List;

public record TopologicalResult(List<Integer> order, boolean hasCycle, long edgeScans) {
    public TopologicalResult {
        order = List.copyOf(order);
    }
}
