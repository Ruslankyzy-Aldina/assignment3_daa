package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.graph.GraphReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    @Test
    void readsConnectedAndDisconnectedExamples() throws IOException {
        check(GraphReader.read(Path.of("data/connected.txt")), 9, 1);
        check(GraphReader.read(Path.of("data/disconnected.txt")), 13, 3);
    }

    @Test
    void handlesEmptyGraphsAndIsolatedVertices() {
        check(new Graph(0, false), 0, 0);
        check(new Graph(1, false), 0, 1);
        check(new Graph(5, false), 0, 5);
    }

    @Test
    void handlesLoopsParallelEdgesNegativeAndEqualWeights() {
        Graph graph = new Graph(5, false);
        graph.addEdge(0, 0, Integer.MIN_VALUE);
        graph.addEdge(0, 1, 8);
        graph.addEdge(0, 1, -4);
        graph.addEdge(1, 2, 0);
        graph.addEdge(0, 2, 0);
        graph.addEdge(3, 4, -2);
        graph.addEdge(4, 4, -100);
        check(graph, -6, 2);
    }

    @Test
    void usesLongForTotalWeightAndSafeWeightComparison() {
        Graph positive = new Graph(3, false);
        positive.addEdge(0, 1, Integer.MAX_VALUE);
        positive.addEdge(1, 2, Integer.MAX_VALUE);
        check(positive, 2L * Integer.MAX_VALUE, 1);
        Graph negative = new Graph(3, false);
        negative.addEdge(0, 1, Integer.MIN_VALUE);
        negative.addEdge(1, 2, Integer.MIN_VALUE);
        negative.addEdge(0, 2, Integer.MAX_VALUE);
        check(negative, 2L * Integer.MIN_VALUE, 1);
    }

    @Test
    void rejectsDirectedGraphs() {
        Graph graph = new Graph(2, true);
        graph.addEdge(0, 1, 3);
        assertThrows(IllegalArgumentException.class, () -> Kruskal.compute(graph));
        assertThrows(IllegalArgumentException.class, () -> Prim.compute(graph));
    }

    @Test
    void agreesOnOneHundredRandomConnectedGraphs() {
        Random random = new Random(42);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(49);
            Graph graph = new Graph(n, false);
            for (int i = 1; i < n; i++) {
                graph.addEdge(random.nextInt(i), i, random.nextInt(201) - 100);
            }
            for (int i = 0; i < 4 * n; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(201) - 100);
            }
            check(graph, Kruskal.compute(graph).totalWeight(), 1);
        }
    }

    @Test
    void agreesWithExhaustiveSearchOnSmallRandomGraphs() {
        Random random = new Random(17);
        for (int trial = 0; trial < 100; trial++) {
            int n = 1 + random.nextInt(6);
            Graph graph = new Graph(n, false);
            int m = random.nextInt(11);
            DSU connectivity = new DSU(n);
            for (int i = 0; i < m; i++) {
                int from = random.nextInt(n);
                int to = random.nextInt(n);
                graph.addEdge(from, to, random.nextInt(21) - 10);
                connectivity.union(from, to);
            }
            int components = connectivity.components();
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << m); mask++) {
                if (Integer.bitCount(mask) != n - components) {
                    continue;
                }
                DSU candidate = new DSU(n);
                long weight = 0;
                boolean valid = true;
                for (int i = 0; i < m; i++) {
                    if ((mask & (1 << i)) != 0) {
                        Edge edge = graph.edges().get(i);
                        if (!candidate.union(edge.from(), edge.to())) {
                            valid = false;
                            break;
                        }
                        weight += edge.weight();
                    }
                }
                if (valid && candidate.components() == components) {
                    best = Math.min(best, weight);
                }
            }
            check(graph, best, components);
        }
    }

    @Test
    void countsOperationsAndKeepsRunsIndependent() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 3);
        List<Edge> original = List.copyOf(graph.edges());
        MstResult kruskal = Kruskal.compute(graph);
        MstResult prim = Prim.compute(graph);
        assertEquals(2, kruskal.edgeScans());
        assertEquals(6, kruskal.dsuOps());
        assertEquals(6, prim.edgeScans());
        assertEquals(0, prim.dsuOps());
        assertTrue(kruskal.comparisons() > 0);
        assertTrue(prim.comparisons() > 0);
        assertEquals(kruskal, Kruskal.compute(graph));
        assertEquals(prim, Prim.compute(graph));
        assertEquals(original, graph.edges());
        assertThrows(UnsupportedOperationException.class, () -> kruskal.edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> prim.edges().clear());
    }

    private void check(Graph graph, long weight, int components) {
        for (MstResult result : List.of(Kruskal.compute(graph), Prim.compute(graph))) {
            assertEquals(weight, result.totalWeight());
            assertEquals(components, result.components());
            assertEquals(graph.vertexCount() - components, result.edges().size());
            DSU forest = new DSU(graph.vertexCount());
            long sum = 0;
            for (Edge edge : result.edges()) {
                assertTrue(graph.neighbors(edge.from()).contains(edge));
                assertTrue(forest.union(edge.from(), edge.to()));
                sum += edge.weight();
            }
            assertEquals(weight, sum);
            assertEquals(components, forest.components());
            for (Edge edge : graph.edges()) {
                assertEquals(forest.find(edge.from()), forest.find(edge.to()));
            }
        }
    }
}
