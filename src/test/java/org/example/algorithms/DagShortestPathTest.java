package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.graph.GraphReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DagShortestPathTest {
    @Test
    void computesExampleAndRestoresPath() throws IOException {
        Graph graph = GraphReader.read(Path.of("data/dag.txt"));
        ShortestPathResult result = DagShortestPath.compute(graph);
        assertEquals(List.of(0L, 0L, -1L, 4L, 1L, 6L), result.distances());
        assertEquals(List.of(1, 2, 4, 5), result.pathTo(5));
        assertEquals(10, result.sumDistances());
        assertEquals(6, result.edgeScans());
        verifyPaths(graph, result);
    }

    @Test
    void startsOnlySourcesAtZeroAndIncludesDisconnectedComponents() {
        Graph graph = new Graph(7, true);
        graph.addEdge(0, 2, 10);
        graph.addEdge(1, 2, 3);
        graph.addEdge(2, 3, -5);
        graph.addEdge(4, 5, 8);
        ShortestPathResult result = DagShortestPath.compute(graph);
        assertEquals(List.of(0L, 0L, 3L, -2L, 0L, 8L, 0L), result.distances());
        assertEquals(List.of(1, 2, 3), result.pathTo(3));
        assertEquals(List.of(4, 5), result.pathTo(5));
        assertEquals(List.of(6), result.pathTo(6));
        verifyPaths(graph, result);
    }

    @Test
    void handlesEmptyGraphAndIsolatedVertices() {
        ShortestPathResult empty = DagShortestPath.compute(new Graph(0, true));
        assertTrue(empty.distances().isEmpty());
        assertEquals(0, empty.sumDistances());
        assertEquals(0, empty.edgeScans());
        assertThrows(IllegalArgumentException.class, () -> empty.pathTo(0));
        ShortestPathResult isolated = DagShortestPath.compute(new Graph(3, true));
        assertEquals(List.of(0L, 0L, 0L), isolated.distances());
        assertEquals(List.of(-1, -1, -1), isolated.parents());
    }

    @Test
    void handlesParallelEdgesAndEqualDistances() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 1, -3);
        graph.addEdge(0, 2, -3);
        graph.addEdge(1, 3, 0);
        graph.addEdge(2, 3, 0);
        ShortestPathResult result = DagShortestPath.compute(graph);
        assertEquals(List.of(0L, -3L, -3L, -3L), result.distances());
        assertEquals(5, result.edgeScans());
        verifyPaths(graph, result);
    }

    @Test
    void usesLongForDistancesAndSum() {
        for (int weight : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            Graph graph = new Graph(3, true);
            graph.addEdge(0, 1, weight);
            graph.addEdge(1, 2, weight);
            ShortestPathResult result = DagShortestPath.compute(graph);
            assertEquals(2L * weight, result.distanceTo(2));
            assertEquals(3L * weight, result.sumDistances());
            verifyPaths(graph, result);
        }
    }

    @Test
    void rejectsCyclesSelfLoopsAndUndirectedGraphs() {
        Graph cycle = new Graph(4, true);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(2, 3, -1);
        cycle.addEdge(3, 2, 1);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.compute(cycle));
        Graph loop = new Graph(1, true);
        loop.addEdge(0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.compute(loop));
        assertThrows(IllegalArgumentException.class,
                () -> DagShortestPath.compute(new Graph(2, false)));
    }

    @Test
    void agreesWithBellmanFordOnRandomDagsIncludingNegativeWeights() {
        Random random = new Random(42);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(49);
            int[] permutation = new int[n];
            Arrays.setAll(permutation, i -> i);
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = permutation[i];
                permutation[i] = permutation[j];
                permutation[j] = temp;
            }
            Graph graph = new Graph(n, true);
            for (int i = 0; i < 4 * n; i++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a != b) {
                    graph.addEdge(permutation[Math.min(a, b)], permutation[Math.max(a, b)],
                            random.nextInt(201) - 100);
                }
            }
            long[] expected = bellmanFord(graph);
            ShortestPathResult result = DagShortestPath.compute(graph);
            for (int vertex = 0; vertex < n; vertex++) {
                assertEquals(expected[vertex], result.distanceTo(vertex));
            }
            assertEquals(Arrays.stream(expected).sum(), result.sumDistances());
            assertEquals(graph.edgeCount(), result.edgeScans());
            verifyPaths(graph, result);
        }
    }

    @Test
    void restoresLongPathWithoutRecursion() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int i = 1; i < n; i++) {
            graph.addEdge(i - 1, i, -1);
        }
        ShortestPathResult result = DagShortestPath.compute(graph);
        assertEquals(1L - n, result.distanceTo(n - 1));
        List<Integer> path = result.pathTo(n - 1);
        assertEquals(n, path.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i, path.get(i).intValue());
        }
        assertEquals(-((long) n * (n - 1) / 2), result.sumDistances());
    }

    @Test
    void preservesGraphAndReturnsImmutableResults() throws IOException {
        Graph graph = GraphReader.read(Path.of("data/dag.txt"));
        List<Edge> edges = List.copyOf(graph.edges());
        ShortestPathResult result = DagShortestPath.compute(graph);
        assertEquals(result, DagShortestPath.compute(graph));
        assertEquals(edges, graph.edges());
        assertThrows(UnsupportedOperationException.class, () -> result.distances().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.parents().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.pathTo(5).clear());
        assertThrows(IllegalArgumentException.class, () -> result.pathTo(-1));
        assertThrows(IllegalArgumentException.class, () -> result.distanceTo(6));
    }

    private long[] bellmanFord(Graph graph) {
        int n = graph.vertexCount();
        long[] distances = new long[n];
        Arrays.fill(distances, Long.MAX_VALUE);
        boolean[] hasIncoming = new boolean[n];
        for (Edge edge : graph.edges()) {
            hasIncoming[edge.to()] = true;
        }
        for (int vertex = 0; vertex < n; vertex++) {
            if (!hasIncoming[vertex]) {
                distances[vertex] = 0;
            }
        }
        for (int pass = 0; pass < n - 1; pass++) {
            boolean changed = false;
            for (Edge edge : graph.edges()) {
                if (distances[edge.from()] != Long.MAX_VALUE
                        && distances[edge.from()] + edge.weight() < distances[edge.to()]) {
                    distances[edge.to()] = distances[edge.from()] + edge.weight();
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return distances;
    }

    private void verifyPaths(Graph graph, ShortestPathResult result) {
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            List<Integer> path = result.pathTo(vertex);
            assertEquals(vertex, path.getLast().intValue());
            assertEquals(0, graph.inDegree(path.getFirst()));
            assertEquals(0, result.distanceTo(path.getFirst()));
            for (int i = 1; i < path.size(); i++) {
                int from = path.get(i - 1);
                int to = path.get(i);
                assertEquals(from, result.parents().get(to).intValue());
                assertTrue(graph.neighbors(from).stream().anyMatch(edge -> edge.to() == to
                        && result.distanceTo(from) + edge.weight() == result.distanceTo(to)));
            }
        }
    }
}
