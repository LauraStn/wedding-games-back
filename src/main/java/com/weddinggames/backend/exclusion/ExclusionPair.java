package com.weddinggames.backend.exclusion;

import java.util.UUID;

/** Normalizes an unordered participant pair into a stable (lower, higher) order by UUID comparison. */
final class ExclusionPair {

    private ExclusionPair() {}

    static UUID lower(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    static UUID higher(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? b : a;
    }
}
