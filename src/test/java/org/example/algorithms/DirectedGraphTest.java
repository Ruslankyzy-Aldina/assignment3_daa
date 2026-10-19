package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.graph.GraphReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DirectedGraphTest {
    @Test
    void findsComponentsAndCycleInExample() throws IOException {
        Graph graph = GraphReader.read(Path.of("data/cyclic.txt"));
        SccResult result = Kosaraju.compute(graph);
        assertEquals(2, result.components());
        assertEquals(result.componentOf().get(0), result.componentOf().get(1));
        assertEquals(result.componentOf().get(1), result.componentOf().get(2));
        assertNotEquals(result.componentOf().get(2), result.componentOf().get(3));
        assertEquals(2L * graph.edgeCount(), result.edgeScans());
        assertTrue(Kahn.compute(graph).hasCycle());
        assertTrue(Kahn.compute(graph).order().isEmpty());
    }

    @Test
    void ordersDagExampleAndCountsEveryEdgeOnce() throws IOException {
        Graph graph = GraphReader.read(Path.of("data/dag.txt"));
        TopologicalResult result = Kahn.compute(graph);
        assertFalse(result.hasCycle());
        assertTrue(Kahn.isValidOrder(graph, result.order()));
        assertEquals(graph.edgeCount(), result.edgeScans());
        assertEquals(graph.vertexCount(), Kosaraju.compute(graph).components());
        assertEquals(result, Kahn.compute(graph));
        assertEquals(2, graph.inDegree(2));
    }

    @Test
    void handlesEmptyGraphsAndIsolatedVertices() {
        Graph empty = new Graph(0, true);
        assertEquals(new SccResult(List.of(), 0, 0, 0), Kosaraju.compute(empty));
        assertFalse(Kahn.compute(empty).hasCycle());
        assertTrue(Kahn.isValidOrder(empty, Kahn.compute(empty).order()));
        Graph isolated = new Graph(5, true);
        assertEquals(5, Kosaraju.compute(isolated).components());
        assertEquals(1, Kosaraju.compute(isolated).maxDepth());
        assertTrue(Kahn.isValidOrder(isolated, Kahn.compute(isolated).order()));
    }

    @Test
    void handlesParallelEdgesAndSelfLoops() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        assertEquals(2, graph.inDegree(1));
        assertEquals(3, Kahn.compute(graph).edgeScans());
        assertTrue(Kahn.isValidOrder(graph, Kahn.compute(graph).order()));
        graph.addEdge(2, 2, -1);
        assertEquals(4, Kosaraju.compute(graph).components());
        assertEquals(8, Kosaraju.compute(graph).edgeScans());
        TopologicalResult result = Kahn.compute(graph);
        assertTrue(result.hasCycle());
        assertTrue(result.order().isEmpty());
        assertEquals(3, result.edgeScans());
    }

    @Test
    void detectsCycleInDisconnectedComponent() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 0);
        graph.addEdge(2, 3, 0);
        graph.addEdge(3, 2, 0);
        assertTrue(Kahn.compute(graph).hasCycle());
        assertTrue(Kahn.compute(graph).order().isEmpty());
        assertEquals(4, Kosaraju.compute(graph).components());
    }

    @Test
    void rejectsUndirectedGraphsAndInvalidOrders() {
        Graph undirected = new Graph(2, false);
        assertThrows(IllegalArgumentException.class, () -> Kosaraju.compute(undirected));
        assertThrows(IllegalArgumentException.class, () -> Kahn.compute(undirected));
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 0);
        assertFalse(Kahn.isValidOrder(graph, List.of(0, 1)));
        assertFalse(Kahn.isValidOrder(graph, List.of(0, 1, 1)));
        assertFalse(Kahn.isValidOrder(graph, List.of(1, 0, 2)));
        assertFalse(Kahn.isValidOrder(graph, List.of(0, 1, 3)));
        assertFalse(Kahn.isValidOrder(graph, List.of(-1, 0, 1)));
        assertTrue(Kahn.isValidOrder(graph, List.of(2, 0, 1)));
    }

    @Test
    void agreesWithBfsReachabilityOnRandomGraphs() {
        Random random = new Random(42);
        for (int trial = 0; trial < 100; trial++) {
            int n = 1 + random.nextInt(200);
            Graph graph = new Graph(n, true);
            int m = random.nextInt(5 * n + 1);
            for (int i = 0; i < m; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(21) - 10);
            }
            boolean[][] reachable = new boolean[n][n];
            for (int start = 0; start < n; start++) {
                ArrayDeque<Integer> queue = new ArrayDeque<>();
                reachable[start][start] = true;
                queue.add(start);
                while (!queue.isEmpty()) {
                    int vertex = queue.remove();
                    for (Edge edge : graph.neighbors(vertex)) {
                        if (!reachable[start][edge.to()]) {
                            reachable[start][edge.to()] = true;
                            queue.add(edge.to());
                        }
                    }
                }
            }
            SccResult result = Kosaraju.compute(graph);
            int expectedCount = 0;
            for (int i = 0; i < n; i++) {
                boolean first = true;
                for (int j = 0; j < n; j++) {
                    boolean same = reachable[i][j] && reachable[j][i];
                    assertEquals(same, result.componentOf().get(i).equals(result.componentOf().get(j)));
                    if (j < i && same) {
                        first = false;
                    }
                }
                if (first) {
                    expectedCount++;
                }
            }
            assertEquals(expectedCount, result.components());
            assertEquals(expectedCount, new HashSet<>(result.componentOf()).size());
            assertEquals(2L * m, result.edgeScans());
        }
    }

    @Test
    void ordersRandomDagsWithShuffledVertexNumbers() {
        Random random = new Random(17);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(99);
            int[] rank = new int[n];
            for (int i = 0; i < n; i++) {
                rank[i] = i;
            }
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = rank[i];
                rank[i] = rank[j];
                rank[j] = temp;
            }
            Graph graph = new Graph(n, true);
            for (int i = 0; i < 4 * n; i++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a != b) {
                    graph.addEdge(rank[a] < rank[b] ? a : b, rank[a] < rank[b] ? b : a, 1);
                }
            }
            TopologicalResult result = Kahn.compute(graph);
            assertFalse(result.hasCycle());
            assertTrue(Kahn.isValidOrder(graph, result.order()));
            assertEquals(graph.edgeCount(), result.edgeScans());
        }
    }

    @Test
    void handlesPathOfOneHundredThousandVerticesWithoutRecursion() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int i = 1; i < n; i++) {
            graph.addEdge(i - 1, i, 1);
        }
        SccResult result = Kosaraju.compute(graph);
        assertEquals(n, result.components());
        assertEquals(n, result.maxDepth());
        assertEquals(2L * (n - 1), result.edgeScans());
        TopologicalResult order = Kahn.compute(graph);
        assertFalse(order.hasCycle());
        assertTrue(Kahn.isValidOrder(graph, order.order()));
        graph.addEdge(n - 1, 0, 1);
        SccResult cycle = Kosaraju.compute(graph);
        assertEquals(1, cycle.components());
        assertEquals(n, cycle.maxDepth());
        assertEquals(2L * n, cycle.edgeScans());
    }

    @Test
    void resultsAreImmutableAndRunsIndependent() throws IOException {
        Graph graph = GraphReader.read(Path.of("data/dag.txt"));
        List<Edge> edges = List.copyOf(graph.edges());
        SccResult scc = Kosaraju.compute(graph);
        TopologicalResult order = Kahn.compute(graph);
        assertEquals(scc, Kosaraju.compute(graph));
        assertEquals(edges, graph.edges());
        assertThrows(UnsupportedOperationException.class, () -> scc.componentOf().clear());
        assertThrows(UnsupportedOperationException.class, () -> order.order().clear());
    }
}
