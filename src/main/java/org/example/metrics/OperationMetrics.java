package org.example.metrics;

public record OperationMetrics(long dsuOps, long edgeScans, long comparisons, int maxDepth) {
}
