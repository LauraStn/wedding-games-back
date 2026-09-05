package com.weddinggames.backend.matchmaking;

import com.weddinggames.backend.common.Gender;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Pure, DB-free pairing algorithm: groups participants into binômes (pairs), plus exactly one
 * trio when the count is odd. HARD exclusions are never violated; PREFERENCE exclusions and
 * repeats of the previous matchmaking's pairs are minimized but never block a solution.
 *
 * <p>Approach: randomized greedy with restarts. At realistic wedding-guest-list scale (dozens of
 * people, a handful of exclusions), a full constraint solver would be overkill - a few hundred
 * random shuffles reliably find a HARD-valid, low-soft-cost arrangement when one exists.
 */
@Component
public class MatchmakingAlgorithm {

    private static final int MAX_ATTEMPTS = 500;
    private static final int PREFERENCE_EXCLUSION_COST = 100;
    private static final int REPEATED_PAIR_COST = 10;

    private final Random random;

    public MatchmakingAlgorithm() {
        this(new Random());
    }

    /** Accepts an injected {@link Random} so tests can assert on outcome properties deterministically. */
    public MatchmakingAlgorithm(Random random) {
        this.random = random;
    }

    public record Input(
            List<UUID> participantIds,
            Set<UnorderedPair> hardExclusions,
            Set<UnorderedPair> softExclusions,
            Set<UnorderedPair> previousPairs) {}

    public record Group(List<UUID> participantIds) {}

    /** Empty when no arrangement respects every HARD exclusion. */
    public Optional<List<Group>> generate(Input input) {
        List<UUID> participants = input.participantIds();
        List<Group> best = null;
        int bestCost = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < MAX_ATTEMPTS && bestCost > 0; attempt++) {
            List<UUID> shuffled = new ArrayList<>(participants);
            Collections.shuffle(shuffled, random);
            List<Group> candidate = tryGenerate(shuffled, input);
            if (candidate != null) {
                int cost = computeCost(candidate, input);
                if (cost < bestCost) {
                    bestCost = cost;
                    best = candidate;
                }
            }
        }

        return Optional.ofNullable(best);
    }

    /**
     * Assigns one character per participant, no repeats. When both a participant and a character
     * carry a {@link Gender} tag, a same-gender match is preferred; an untagged (no-preference)
     * character is the next best fallback; only as a last resort (not enough same-gender or
     * untagged characters left) does an opposite-gender character get used. Never blocks: an
     * untagged participant, or one whose preference can't be honored, still gets a character.
     */
    public Map<UUID, UUID> assignCharacters(
            List<UUID> participantIds,
            Map<UUID, Gender> participantGenders,
            List<UUID> characterIds,
            Map<UUID, Gender> characterGenders) {
        List<UUID> pool = new ArrayList<>(characterIds);
        Collections.shuffle(pool, random);

        // Gendered participants are served first so they get first pick of a matching character;
        // participants with no preference take whatever the process leaves behind.
        List<UUID> processingOrder = new ArrayList<>(participantIds);
        processingOrder.sort(Comparator.comparing(id -> participantGenders.get(id) == null ? 1 : 0));

        Map<UUID, UUID> assignment = new LinkedHashMap<>();
        for (UUID participantId : processingOrder) {
            UUID character = pickCharacter(pool, participantGenders.get(participantId), characterGenders);
            pool.remove(character);
            assignment.put(participantId, character);
        }
        return assignment;
    }

    private UUID pickCharacter(List<UUID> pool, Gender wanted, Map<UUID, Gender> characterGenders) {
        if (wanted != null) {
            Optional<UUID> sameGender =
                    pool.stream().filter(c -> characterGenders.get(c) == wanted).findFirst();
            if (sameGender.isPresent()) {
                return sameGender.get();
            }
            Optional<UUID> noPreference =
                    pool.stream().filter(c -> characterGenders.get(c) == null).findFirst();
            if (noPreference.isPresent()) {
                return noPreference.get();
            }
        }
        return pool.get(0);
    }

    /** All HARD exclusion pairs where both participants are in this input, for a clear error message. */
    public List<UnorderedPair> relevantHardExclusions(Input input) {
        Set<UUID> present = Set.copyOf(input.participantIds());
        return input.hardExclusions().stream()
                .filter(pair -> present.contains(pair.first()) && present.contains(pair.second()))
                .toList();
    }

    private List<Group> tryGenerate(List<UUID> shuffled, Input input) {
        Deque<UUID> remaining = new ArrayDeque<>(shuffled);
        List<Group> groups = new ArrayList<>();

        while (remaining.size() > 3) {
            UUID a = remaining.pollFirst();
            UUID bestPartner = null;
            int bestCost = Integer.MAX_VALUE;
            for (UUID candidate : remaining) {
                if (isHardExcluded(a, candidate, input)) {
                    continue;
                }
                int cost = pairCost(a, candidate, input);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestPartner = candidate;
                }
            }
            if (bestPartner == null) {
                return null;
            }
            remaining.remove(bestPartner);
            groups.add(new Group(List.of(a, bestPartner)));
        }

        List<UUID> rest = new ArrayList<>(remaining);
        if (rest.isEmpty()) {
            // Nothing left over: the loop consumed everyone in pairs (even total).
            return groups;
        } else if (rest.size() == 2) {
            if (isHardExcluded(rest.get(0), rest.get(1), input)) {
                return null;
            }
            groups.add(new Group(rest));
        } else if (rest.size() == 3) {
            if (isHardExcluded(rest.get(0), rest.get(1), input)
                    || isHardExcluded(rest.get(0), rest.get(2), input)
                    || isHardExcluded(rest.get(1), rest.get(2), input)) {
                return null;
            }
            groups.add(new Group(rest));
        } else {
            // A single leftover participant (or any other size) can never form a valid binôme/trio.
            return null;
        }
        return groups;
    }

    private int computeCost(List<Group> groups, Input input) {
        int total = 0;
        for (Group group : groups) {
            List<UUID> members = group.participantIds();
            for (int i = 0; i < members.size(); i++) {
                for (int j = i + 1; j < members.size(); j++) {
                    total += pairCost(members.get(i), members.get(j), input);
                }
            }
        }
        return total;
    }

    private boolean isHardExcluded(UUID a, UUID b, Input input) {
        return input.hardExclusions().contains(new UnorderedPair(a, b));
    }

    private int pairCost(UUID a, UUID b, Input input) {
        UnorderedPair pair = new UnorderedPair(a, b);
        int cost = 0;
        if (input.softExclusions().contains(pair)) {
            cost += PREFERENCE_EXCLUSION_COST;
        }
        if (input.previousPairs().contains(pair)) {
            cost += REPEATED_PAIR_COST;
        }
        return cost;
    }
}
