package org.example.algorithms;

import org.example.graph.Edge;

import java.util.Comparator;

final class WeightComparator implements Comparator<Edge> {
    private long comparisons;

    @Override
    public int compare(Edge first, Edge second) {
        comparisons++;
        return Integer.compare(first.weight(), second.weight());
    }

    long comparisons() {
        return comparisons;
    }
}
