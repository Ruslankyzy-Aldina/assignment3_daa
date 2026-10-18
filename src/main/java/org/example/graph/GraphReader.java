package org.example.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GraphReader {
    private GraphReader() {
    }

    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String[] header = fields(reader.readLine(), 1);
            int n = Integer.parseInt(header[0]);
            int m = Integer.parseInt(header[1]);
            if (n < 0 || m < 0) {
                throw new IllegalArgumentException("Counts must be non-negative");
            }
            boolean directed = switch (header[2]) {
                case "directed" -> true;
                case "undirected" -> false;
                default -> throw new IllegalArgumentException("Expected directed or undirected");
            };
            Graph graph = new Graph(n, directed);
            for (int i = 0; i < m; i++) {
                String[] edge = fields(reader.readLine(), i + 2);
                graph.addEdge(Integer.parseInt(edge[0]), Integer.parseInt(edge[1]),
                        Integer.parseInt(edge[2]));
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    throw new IllegalArgumentException("Unexpected content after " + m + " edges");
                }
            }
            return graph;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid graph file " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static String[] fields(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Missing data on line " + lineNumber);
        }
        String[] fields = line.trim().split("\\s+");
        if (fields.length != 3) {
            throw new IllegalArgumentException("Expected 3 fields on line " + lineNumber);
        }
        return fields;
    }
}
