package com.weddinggames.backend.matchmaking;

import java.util.UUID;

/** A pair of participant ids, normalized so (a, b) and (b, a) are always equal. */
public record UnorderedPair(UUID first, UUID second) {

    public UnorderedPair {
        if (first.compareTo(second) > 0) {
            UUID swap = first;
            first = second;
            second = swap;
        }
    }
}
