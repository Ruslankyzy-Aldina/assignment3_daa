package org.example;

import org.example.graph.Graph;
import org.example.graph.GraphReader;
import org.example.algorithms.Kruskal;
import org.example.algorithms.Prim;
import org.example.algorithms.MstResult;
import org.example.algorithms.Kosaraju;
import org.example.algorithms.Kahn;
import org.example.algorithms.SccResult;
import org.example.algorithms.TopologicalResult;
import org.example.algorithms.DagShortestPath;
import org.example.algorithms.ShortestPathResult;

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
        if (!graph.isDirected()) {
            MstResult kruskal = Kruskal.compute(graph);
            MstResult prim = Prim.compute(graph);
            if (kruskal.totalWeight() != prim.totalWeight()
                    || kruskal.components() != prim.components()) {
                throw new IllegalStateException("Kruskal and Prim results disagree");
            }
            printResult("Kruskal", kruskal);
            printResult("Prim", prim);
        } else {
            SccResult scc = Kosaraju.compute(graph);
            System.out.printf("Kosaraju: components=%d, edgeScans=%d, maxDepth=%d%n",
                    scc.components(), scc.edgeScans(), scc.maxDepth());
            System.out.println("Component IDs: " + scc.componentOf());
            TopologicalResult topological = Kahn.compute(graph);
            if (topological.hasCycle()) {
                System.out.println("Kahn: cycle detected");
            } else {
                if (!Kahn.isValidOrder(graph, topological.order())) {
                    throw new IllegalStateException("Invalid topological order");
                }
                System.out.println("Kahn: " + topological.order());
                ShortestPathResult paths = DagShortestPath.compute(graph);
                System.out.printf("DagShortestPath: sum=%d, edgeScans=%d%n",
                        paths.sumDistances(), paths.edgeScans());
                for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
                    System.out.printf("Vertex %d: distance=%d, path=%s%n",
                            vertex, paths.distanceTo(vertex), paths.pathTo(vertex));
                }
            }
            System.out.println("edgeScans=" + topological.edgeScans());
        }
    }

    private static void printResult(String algorithm, MstResult result) {
        System.out.printf("%s: weight=%d, components=%d, edges=%d%n",
                algorithm, result.totalWeight(), result.components(), result.edges().size());
        System.out.printf("dsuOps=%d, edgeScans=%d, comparisons=%d%n",
                result.dsuOps(), result.edgeScans(), result.comparisons());
        System.out.println(result.edges());
    }
}
