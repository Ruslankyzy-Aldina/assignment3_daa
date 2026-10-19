package org.example.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    @TempDir
    Path temp;

    @Test
    void maintainsInDegreesForBothGraphTypes() {
        Graph directed = new Graph(3, true);
        directed.addEdge(0, 1, 1);
        directed.addEdge(0, 1, 2);
        directed.addEdge(1, 1, 3);
        assertEquals(0, directed.inDegree(0));
        assertEquals(3, directed.inDegree(1));
        assertEquals(0, directed.inDegree(2));
        assertThrows(IllegalArgumentException.class, () -> directed.inDegree(3));
        Graph undirected = new Graph(2, false);
        undirected.addEdge(0, 1, 1);
        undirected.addEdge(0, 0, 2);
        assertEquals(3, undirected.inDegree(0));
        assertEquals(1, undirected.inDegree(1));
    }

    @Test
    void directedEdgeHasNoReverseEntry() {
        Graph graph = new Graph(2, true);
        graph.addEdge(0, 1, -5);
        assertEquals(List.of(new Edge(0, 1, -5)), graph.neighbors(0));
        assertTrue(graph.neighbors(1).isEmpty());
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void undirectedGraphPreservesParallelEdgesLoopsAndIsolatedVertices() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 1, 7);
        graph.addEdge(0, 0, -2);
        assertEquals(3, graph.edgeCount());
        assertEquals(4, graph.neighbors(0).size());
        assertEquals(List.of(new Edge(1, 0, 4), new Edge(1, 0, 7)), graph.neighbors(1));
        assertTrue(graph.neighbors(2).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> graph.edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.neighbors(0).clear());
    }

    @Test
    void rejectsInvalidVerticesWithoutChangingGraph() {
        Graph graph = new Graph(2, false);
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, true));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.neighbors(-1));
        assertEquals(0, graph.edgeCount());
        assertTrue(graph.neighbors(0).isEmpty());
        assertEquals(0, new Graph(0, true).vertexCount());
    }

    @Test
    void readsAllThreeExampleFiles() throws IOException {
        Graph connected = GraphReader.read(Path.of("data/connected.txt"));
        assertEquals(4, connected.vertexCount());
        assertEquals(4, connected.edgeCount());
        assertFalse(connected.isDirected());
        Graph disconnected = GraphReader.read(Path.of("data/disconnected.txt"));
        assertEquals(6, disconnected.vertexCount());
        assertEquals(3, disconnected.edgeCount());
        assertTrue(disconnected.neighbors(5).isEmpty());
        Graph cyclic = GraphReader.read(Path.of("data/cyclic.txt"));
        assertTrue(cyclic.isDirected());
        assertEquals(4, cyclic.edgeCount());
        assertTrue(cyclic.neighbors(2).contains(new Edge(2, 0, 1)));
    }

    @Test
    void rejectsMalformedFiles() throws IOException {
        List<String> invalid = List.of("", "2 0 unknown", "-1 0 directed", "2 -1 directed",
                "2 1 directed\n0 2 4", "2 1 directed", "2 0 directed\n0 1 3",
                "2 1 directed\n0 1", "two 0 directed", "2 1 directed\n0 1 2147483648");
        Path file = temp.resolve("invalid.txt");
        for (String content : invalid) {
            Files.writeString(file, content);
            assertThrows(IOException.class, () -> GraphReader.read(file), content);
        }
    }

    @Test
    void readsEmptyGraphAndWhitespace() throws IOException {
        Path file = temp.resolve("empty.txt");
        Files.writeString(file, " 0\t0 directed \n\n");
        Graph graph = GraphReader.read(file);
        assertEquals(0, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
    }
}
