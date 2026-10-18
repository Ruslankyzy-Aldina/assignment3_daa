package org.example.algorithms;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DSUTest {
    @Test
    void startsWithSeparateSets() {
        DSU dsu = new DSU(5);
        assertEquals(5, dsu.components());
        assertEquals(0, dsu.dsuOps());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, dsu.find(i));
        }
        assertEquals(5, dsu.findCalls());
        assertEquals(0, dsu.successfulUnions());
    }

    @Test
    void countsFindsAndOnlySuccessfulUnions() {
        DSU dsu = new DSU(3);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        assertFalse(dsu.union(2, 2));
        assertEquals(2, dsu.components());
        assertEquals(6, dsu.findCalls());
        assertEquals(1, dsu.successfulUnions());
        assertEquals(7, dsu.dsuOps());
        assertTrue(dsu.union(1, 2));
        assertEquals(1, dsu.components());
        assertEquals(10, dsu.dsuOps());
    }

    @Test
    void makeSetResetsSetsAndCounters() {
        DSU dsu = new DSU(3);
        dsu.union(0, 1);
        dsu.makeSet(2);
        assertEquals(2, dsu.components());
        assertEquals(0, dsu.findCalls());
        assertEquals(0, dsu.successfulUnions());
        assertNotEquals(dsu.find(0), dsu.find(1));
        assertThrows(IllegalArgumentException.class, () -> dsu.find(2));
    }

    @Test
    void handlesEmptyAndSingletonSets() {
        DSU empty = new DSU(0);
        assertEquals(0, empty.components());
        assertThrows(IllegalArgumentException.class, () -> empty.find(0));
        DSU singleton = new DSU(1);
        assertEquals(0, singleton.find(0));
        assertFalse(singleton.union(0, 0));
        assertEquals(1, singleton.components());
    }

    @Test
    void invalidArgumentsDoNotChangeStateOrCounters() {
        assertThrows(IllegalArgumentException.class, () -> new DSU(-1));
        DSU dsu = new DSU(3);
        assertThrows(IllegalArgumentException.class, () -> dsu.find(-1));
        assertThrows(IllegalArgumentException.class, () -> dsu.find(3));
        assertThrows(IllegalArgumentException.class, () -> dsu.union(0, 3));
        assertThrows(IllegalArgumentException.class, () -> dsu.union(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> dsu.makeSet(-1));
        assertEquals(3, dsu.components());
        assertEquals(0, dsu.dsuOps());
        assertEquals(0, dsu.find(0));
    }

    @Test
    void agreesWithReferenceAfterRandomUnions() {
        Random random = new Random(42);
        for (int trial = 0; trial < 20; trial++) {
            int n = 1 + random.nextInt(50);
            DSU dsu = new DSU(n);
            int[] labels = new int[n];
            Arrays.setAll(labels, i -> i);
            for (int step = 0; step < 200; step++) {
                int x = random.nextInt(n);
                int y = random.nextInt(n);
                int oldLabel = labels[y];
                int newLabel = labels[x];
                assertEquals(oldLabel != newLabel, dsu.union(x, y));
                for (int i = 0; i < n; i++) {
                    if (labels[i] == oldLabel) {
                        labels[i] = newLabel;
                    }
                }
                assertEquals(Arrays.stream(labels).distinct().count(), dsu.components());
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        assertEquals(labels[i] == labels[j], dsu.find(i) == dsu.find(j));
                    }
                }
            }
        }
    }

    @Test
    void handlesOneHundredThousandElements() {
        int n = 100_000;
        DSU dsu = new DSU(n);
        for (int i = 1; i < n; i++) {
            assertTrue(dsu.union(i - 1, i));
        }
        int root = dsu.find(0);
        for (int i = 1; i < n; i++) {
            assertEquals(root, dsu.find(i));
        }
        assertEquals(1, dsu.components());
        assertEquals(n - 1, dsu.successfulUnions());
        assertEquals(3L * n - 2, dsu.findCalls());
    }
}
