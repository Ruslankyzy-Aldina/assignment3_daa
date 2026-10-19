package org.example.benchmark;

import org.example.algorithms.DagShortestPath;
import org.example.algorithms.Kahn;
import org.example.algorithms.Kosaraju;
import org.example.algorithms.Kruskal;
import org.example.algorithms.MstResult;
import org.example.algorithms.Prim;
import org.example.graph.Graph;
import org.example.metrics.OperationMetrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Benchmark {
    private static final int RUNS = 5;

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: Benchmark [output.csv]");
        }
        Path output = Path.of(args.length == 0 ? "results/results.csv" : args[0]);
        List<BenchmarkRow> rows = new ArrayList<>();
        for (ReferenceValues reference : ReferenceValues.ALL) {
            rows.addAll(runCase(reference));
            System.out.printf("n=%d m=%d: MST=%d SCC=%d DAG sum=%d OK%n",
                    reference.n(), reference.m(), reference.mstWeight(),
                    reference.sccCount(), reference.distanceSum());
        }
        writeCsv(output, rows);
        System.out.println("Saved " + rows.size() + " rows to " + output);
    }

    public static List<BenchmarkRow> runCase(ReferenceValues reference) {
        List<BenchmarkRow> rows = new ArrayList<>(5);
        rows.addAll(measureMst(reference));
        rows.add(measureScc(reference));
        rows.addAll(measureDag(reference));
        return List.copyOf(rows);
    }

    private static List<BenchmarkRow> measureMst(ReferenceValues reference) {
        Graph graph = GraphGenerator.generate(reference.n(), reference.m(), GraphGenerator.Kind.UNDIRECTED);
        Function<MstResult, RunResult> summarize = result -> {
            require(result.components() == 1, "MST graph must be connected");
            require(result.edges().size() == reference.n() - 1, "Invalid MST edge count");
            require(result.totalWeight() == reference.mstWeight(),
                    "MST reference mismatch: expected " + reference.mstWeight() + ", got " + result.totalWeight());
            return new RunResult(result.totalWeight(), new OperationMetrics(result.dsuOps(),
                    result.edgeScans(), result.comparisons(), 0));
        };
        BenchmarkRow kruskal = measure("Kruskal", "undirected", reference,
                () -> Kruskal.compute(graph), summarize);
        BenchmarkRow prim = measure("Prim", "undirected", reference,
                () -> Prim.compute(graph), summarize);
        require(kruskal.result() == prim.result(), "Kruskal and Prim disagree");
        return List.of(kruskal, prim);
    }

    private static BenchmarkRow measureScc(ReferenceValues reference) {
        Graph graph = GraphGenerator.generate(reference.n(), reference.m(), GraphGenerator.Kind.DIRECTED);
        return measure("Kosaraju", "directed", reference, () -> Kosaraju.compute(graph), result -> {
            require(result.components() == reference.sccCount(),
                    "SCC reference mismatch: expected " + reference.sccCount() + ", got " + result.components());
            require(result.edgeScans() == 2L * reference.m(), "Kosaraju must scan 2m edges");
            return new RunResult(result.components(), new OperationMetrics(0, result.edgeScans(),
                    0, result.maxDepth()));
        });
    }

    private static List<BenchmarkRow> measureDag(ReferenceValues reference) {
        Graph graph = GraphGenerator.generate(reference.n(), reference.m(), GraphGenerator.Kind.DAG);
        BenchmarkRow kahn = measure("Kahn", "dag", reference, () -> Kahn.compute(graph), result -> {
            require(!result.hasCycle() && Kahn.isValidOrder(graph, result.order()), "Invalid topological order");
            require(result.edgeScans() == reference.m(), "Kahn must scan m edges");
            return new RunResult(result.order().size(), new OperationMetrics(0, result.edgeScans(), 0, 0));
        });
        BenchmarkRow paths = measure("DagShortestPath", "dag", reference,
                () -> DagShortestPath.compute(graph), result -> {
                    require(result.sumDistances() == reference.distanceSum(),
                            "DAG distance reference mismatch: expected " + reference.distanceSum()
                                    + ", got " + result.sumDistances());
                    require(result.edgeScans() == reference.m(), "DAG relaxation must scan m edges");
                    return new RunResult(result.sumDistances(), new OperationMetrics(0, result.edgeScans(), 0, 0));
                });
        return List.of(kahn, paths);
    }

    private static <T> BenchmarkRow measure(String algorithm, String kind, ReferenceValues reference,
                                           Supplier<T> operation, Function<T, RunResult> summarize) {
        long[] times = new long[RUNS];
        RunResult first = null;
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            T result = operation.get();
            times[i] = System.nanoTime() - start;
            RunResult current;
            try {
                current = summarize.apply(result);
            } catch (IllegalStateException exception) {
                throw new IllegalStateException(algorithm + " n=" + reference.n() + " m=" + reference.m()
                        + ": " + exception.getMessage(), exception);
            }
            if (first == null) {
                first = current;
            } else {
                require(first.equals(current), algorithm + ": counters or result changed between runs");
            }
        }
        return new BenchmarkRow(algorithm, kind, reference.n(), reference.m(), medianMillis(times),
                first.metrics(), first.value());
    }

    public static double medianMillis(long[] nanos) {
        if (nanos.length != RUNS) {
            throw new IllegalArgumentException("Expected five measurements");
        }
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        return sorted[RUNS / 2] / 1_000_000.0;
    }

    public static void writeCsv(Path output, List<BenchmarkRow> rows) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        List<String> lines = new ArrayList<>(rows.size() + 1);
        lines.add(BenchmarkRow.HEADER);
        for (BenchmarkRow row : rows) {
            lines.add(row.toCsv());
        }
        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record RunResult(long value, OperationMetrics metrics) {
    }
}
