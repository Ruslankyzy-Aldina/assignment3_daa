package org.example;

import org.example.graph.Graph;
import org.example.graph.GraphReader;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        Path path = Path.of(args.length == 0 ? "data/connected.txt" : args[0]);
        Graph graph = GraphReader.read(path);
        System.out.printf("Vertices: %d, edges: %d, directed: %b%n",
                graph.vertexCount(), graph.edgeCount(), graph.isDirected());
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            System.out.println(vertex + " -> " + graph.neighbors(vertex));
        }
    }
}
