package org.example.algorithms;

import java.util.List;

public record SccResult(List<Integer> componentOf, int components, long edgeScans, int maxDepth) {
    public SccResult {
        componentOf = List.copyOf(componentOf);
    }
}
