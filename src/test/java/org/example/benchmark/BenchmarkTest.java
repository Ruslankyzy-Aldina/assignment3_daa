package org.example.benchmark;

import org.example.algorithms.DSU;
import org.example.algorithms.Kahn;
import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.metrics.OperationMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkTest {
    @TempDir
    Path temp;

    @Test
    void generatesReproducibleGraphsWithRequestedProperties() {
        for (GraphGenerator.Kind kind : GraphGenerator.Kind.values()) {
            Graph first = GraphGenerator.generate(100, 400, kind);
            Graph second = GraphGenerator.generate(100, 400, kind);
            assertEquals(first.edges(), second.edges());
            assertEquals(100, first.vertexCount());
            assertEquals(400, first.edgeCount());
            assertEquals(kind != GraphGenerator.Kind.UNDIRECTED, first.isDirected());
            DSU connectivity = new DSU(100);
            for (Edge edge : first.edges()) {
                assertNotEquals(edge.from(), edge.to());
                assertTrue(edge.weight() >= 1 && edge.weight() <= 1000);
                connectivity.union(edge.from(), edge.to());
            }
            if (kind == GraphGenerator.Kind.UNDIRECTED) {
                assertEquals(1, connectivity.components());
            }
            if (kind == GraphGenerator.Kind.DAG) {
                assertTrue(Kahn.isValidOrder(first, Kahn.compute(first).order()));
            }
        }
    }

    @Test
    void rejectsImpossibleGeneratorParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generate(0, 0, GraphGenerator.Kind.DAG));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generate(1, 1, GraphGenerator.Kind.DIRECTED));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generate(3, 1, GraphGenerator.Kind.UNDIRECTED));
        assertThrows(IllegalArgumentException.class,
                () -> GraphGenerator.generate(2, -1, GraphGenerator.Kind.DAG));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(2, 1, null));
        assertEquals(0, GraphGenerator.generate(1, 0, GraphGenerator.Kind.UNDIRECTED).edgeCount());
    }

    @Test
    void matchesSparseAndDenseReferencesForAllAlgorithms() {
        for (ReferenceValues reference : ReferenceValues.ALL.subList(0, 2)) {
            List<BenchmarkRow> rows = Benchmark.runCase(reference);
            assertEquals(List.of("Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"),
                    rows.stream().map(BenchmarkRow::algorithm).toList());
            assertEquals(List.of(reference.mstWeight(), reference.mstWeight(),
                            (long) reference.sccCount(), (long) reference.n(), reference.distanceSum()),
                    rows.stream().map(BenchmarkRow::result).toList());
            for (BenchmarkRow row : rows) {
                assertEquals(reference.n(), row.n());
                assertEquals(reference.m(), row.m());
                assertEquals(reference.m() == 4 * reference.n() ? "sparse" : "dense", row.density());
                assertTrue(row.toCsv().endsWith("," + row.density()));
                assertTrue(row.timeMs() >= 0);
            }
            assertEquals(2L * reference.m(), rows.get(2).metrics().edgeScans());
            assertEquals(reference.m(), rows.get(3).metrics().edgeScans());
            assertEquals(reference.m(), rows.get(4).metrics().edgeScans());
        }
    }

    @Test
    void failsLoudlyOnWrongReference() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Benchmark.runCase(new ReferenceValues(1_000, 4_000, -1, 27, 396_274)));
        assertTrue(exception.getMessage().contains("Kruskal n=1000 m=4000"));
        assertTrue(exception.getMessage().contains("expected -1, got 147970"));
    }

    @Test
    void takesMedianWithoutChangingMeasurements() {
        long[] nanos = {9_000_000, 1_000_000, 5_000_000, 3_000_000, 7_000_000};
        long[] original = nanos.clone();
        assertEquals(5.0, Benchmark.medianMillis(nanos));
        assertArrayEquals(original, nanos);
        assertThrows(IllegalArgumentException.class, () -> Benchmark.medianMillis(new long[4]));
    }

    @Test
    void writesCsvWithStableColumnsAndDecimalPoint() throws IOException {
        BenchmarkRow row = new BenchmarkRow("Kruskal", "undirected", 3, 4, 1.25,
                new OperationMetrics(7, 3, 5, 0), 42);
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("Kruskal,undirected,3,4,1.250000,7,3,5,0,42,custom", row.toCsv());
        } finally {
            Locale.setDefault(previous);
        }
        Path file = temp.resolve("results/results.csv");
        Benchmark.writeCsv(file, List.of(row));
        List<String> lines = Files.readAllLines(file);
        assertEquals(List.of(BenchmarkRow.HEADER, row.toCsv()), lines);
        assertEquals(11, lines.get(0).split(",").length);
        assertEquals(11, lines.get(1).split(",").length);
    }
}
