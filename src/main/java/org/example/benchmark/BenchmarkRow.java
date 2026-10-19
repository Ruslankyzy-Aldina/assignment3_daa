package org.example.benchmark;

import org.example.metrics.OperationMetrics;

import java.util.Locale;

public record BenchmarkRow(String algorithm, String graph, int n, int m, double timeMs,
                           OperationMetrics metrics, long result) {
    public static final String HEADER =
            "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";

    public String toCsv() {
        return String.format(Locale.ROOT, "%s,%s,%d,%d,%.6f,%d.00,%d,%d,%d,%d",
                algorithm, graph, n, m, timeMs, metrics.dsuOps(), metrics.edgeScans(),
                metrics.comparisons(), metrics.maxDepth(), result);
    }
}
