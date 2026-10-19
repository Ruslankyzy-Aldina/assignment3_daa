# DAA Assignment 3 - Graphs

Java implementations of DSU, Kruskal, Prim, Kosaraju, Kahn and DAG shortest paths, with operation counters and JUnit 5 tests.

## Build and Run

Requires JDK 25 and Maven 3.9+. Run from the project root:

```shell
mvn test
mvn package
java -cp target/classes org.example.Main data/connected.txt
java -cp target/classes org.example.Main data/dag.txt
```

## Input

```text
n m directed|undirected
u v w
```

Provide exactly `m` edges. Vertices are numbered `0..n-1`; weights are integers. Examples in `data/` include connected, disconnected, cyclic and DAG inputs.

MST algorithms return a forest for disconnected graphs. DAG shortest paths start from all zero-in-degree vertices and support negative weights and path restoration.

Benchmark, plots and report are pending.

