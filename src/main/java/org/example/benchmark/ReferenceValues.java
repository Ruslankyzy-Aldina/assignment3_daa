package org.example.benchmark;

import java.util.List;

public record ReferenceValues(int n, int m, long mstWeight, int sccCount, long distanceSum) {
    public static final List<ReferenceValues> ALL = List.of(
            new ReferenceValues(1_000, 4_000, 147_970, 27, 396_274),
            new ReferenceValues(1_000, 20_000, 30_363, 1, 252_411),
            new ReferenceValues(3_162, 12_648, 468_911, 137, 1_271_911),
            new ReferenceValues(3_162, 63_240, 97_589, 1, 723_157),
            new ReferenceValues(10_000, 40_000, 1_486_139, 373, 3_941_828),
            new ReferenceValues(10_000, 200_000, 309_550, 1, 2_381_342),
            new ReferenceValues(31_623, 126_492, 4_715_515, 1_188, 12_551_891),
            new ReferenceValues(31_623, 632_460, 968_517, 1, 7_376_612),
            new ReferenceValues(100_000, 400_000, 15_012_094, 3_923, 40_096_539),
            new ReferenceValues(100_000, 2_000_000, 3_056_681, 1, 23_512_087)
    );
}
