package com.weddinggames.backend.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;

import com.weddinggames.backend.common.Gender;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test (no Spring context, no DB) for the pairing algorithm. Uses random UUIDs and
 * asserts on outcome properties rather than exact pairings, since the algorithm is randomized.
 */
class MatchmakingAlgorithmTest {

    private final MatchmakingAlgorithm algorithm = new MatchmakingAlgorithm(new Random());

    private List<UUID> randomIds(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(UUID.randomUUID());
        }
        return ids;
    }

    private MatchmakingAlgorithm.Input inputWithoutExclusions(List<UUID> ids) {
        return new MatchmakingAlgorithm.Input(ids, Set.of(), Set.of(), Set.of());
    }

    @RepeatedTest(20)
    void everyParticipantAppearsExactlyOnceRegardlessOfCount() {
        for (int count : List.of(2, 3, 4, 5, 7, 12, 13)) {
            List<UUID> ids = randomIds(count);
            Optional<List<MatchmakingAlgorithm.Group>> result = algorithm.generate(inputWithoutExclusions(ids));

            assertThat(result).isPresent();
            List<UUID> flattened = result.get().stream()
                    .flatMap(group -> group.participantIds().stream())
                    .toList();
            assertThat(flattened).hasSize(count).containsExactlyInAnyOrderElementsOf(ids);
        }
    }

    @Test
    void anEvenCountProducesOnlyPairsNeverATrio() {
        List<UUID> ids = randomIds(8);
        List<MatchmakingAlgorithm.Group> groups =
                algorithm.generate(inputWithoutExclusions(ids)).orElseThrow();

        assertThat(groups).hasSize(4);
        assertThat(groups).allSatisfy(group -> assertThat(group.participantIds()).hasSize(2));
    }

    @Test
    void anOddCountProducesExactlyOneTrioAndTheRestArePairs() {
        List<UUID> ids = randomIds(9);
        List<MatchmakingAlgorithm.Group> groups =
                algorithm.generate(inputWithoutExclusions(ids)).orElseThrow();

        assertThat(groups).hasSize(4);
        assertThat(groups).filteredOn(group -> group.participantIds().size() == 3).hasSize(1);
        assertThat(groups).filteredOn(group -> group.participantIds().size() == 2).hasSize(3);
    }

    /** The scenario the acceptance criteria names explicitly, at a realistic guest-list scale. */
    @RepeatedTest(20)
    void jessikaIsNeverGroupedWithSandrineOrPatrickAmongARealisticGuestList() {
        UUID jessika = UUID.randomUUID();
        UUID sandrine = UUID.randomUUID();
        UUID patrick = UUID.randomUUID();
        List<UUID> others = randomIds(17);
        List<UUID> everyone = new ArrayList<>(others);
        everyone.add(jessika);
        everyone.add(sandrine);
        everyone.add(patrick);

        Set<UnorderedPair> hardExclusions =
                Set.of(new UnorderedPair(jessika, sandrine), new UnorderedPair(jessika, patrick));
        MatchmakingAlgorithm.Input input = new MatchmakingAlgorithm.Input(everyone, hardExclusions, Set.of(), Set.of());

        List<MatchmakingAlgorithm.Group> groups = algorithm.generate(input).orElseThrow();

        MatchmakingAlgorithm.Group jessikaGroup = groups.stream()
                .filter(g -> g.participantIds().contains(jessika))
                .findFirst()
                .orElseThrow();
        assertThat(jessikaGroup.participantIds()).doesNotContain(sandrine, patrick);
    }

    @Test
    void reportsInfeasibleWhenTheOnlyPossibleTrioContainsAHardExclusion() {
        // Exactly 3 people, and the one trio they must form contains an excluded pair: no
        // arrangement can ever satisfy the constraint, so this must fail loudly rather than
        // silently ignore the exclusion.
        UUID jessika = UUID.randomUUID();
        UUID sandrine = UUID.randomUUID();
        UUID patrick = UUID.randomUUID();
        List<UUID> everyone = List.of(jessika, sandrine, patrick);
        Set<UnorderedPair> hardExclusions =
                Set.of(new UnorderedPair(jessika, sandrine), new UnorderedPair(jessika, patrick));
        MatchmakingAlgorithm.Input input = new MatchmakingAlgorithm.Input(everyone, hardExclusions, Set.of(), Set.of());

        Optional<List<MatchmakingAlgorithm.Group>> result = algorithm.generate(input);

        assertThat(result).isEmpty();
        assertThat(algorithm.relevantHardExclusions(input)).hasSize(2);
    }

    @Test
    void aSingleLeftoverParticipantIsNeverSilentlyDropped() {
        Optional<List<MatchmakingAlgorithm.Group>> result =
                algorithm.generate(inputWithoutExclusions(randomIds(1)));

        assertThat(result).isEmpty();
    }

    @Test
    void anEmptyGuestListProducesNoGroups() {
        Optional<List<MatchmakingAlgorithm.Group>> result =
                algorithm.generate(inputWithoutExclusions(List.of()));

        assertThat(result).contains(List.of());
    }

    @RepeatedTest(10)
    void minimizesPreferenceExclusionsWhenAConflictFreeArrangementExists() {
        UUID couple1 = UUID.randomUUID();
        UUID couple2 = UUID.randomUUID();
        List<UUID> others = randomIds(6);
        List<UUID> everyone = new ArrayList<>(others);
        everyone.add(couple1);
        everyone.add(couple2);

        Set<UnorderedPair> softExclusions = Set.of(new UnorderedPair(couple1, couple2));
        MatchmakingAlgorithm.Input input =
                new MatchmakingAlgorithm.Input(everyone, Set.of(), softExclusions, Set.of());

        List<MatchmakingAlgorithm.Group> groups = algorithm.generate(input).orElseThrow();

        boolean coupleKeptTogether = groups.stream()
                .anyMatch(g -> g.participantIds().contains(couple1) && g.participantIds().contains(couple2));
        assertThat(coupleKeptTogether).isFalse();
    }

    @Test
    void resultIsDeterministicForAGivenRandomSeed() {
        List<UUID> ids = randomIds(10);
        MatchmakingAlgorithm.Input input = inputWithoutExclusions(ids);

        List<MatchmakingAlgorithm.Group> first =
                new MatchmakingAlgorithm(new Random(42)).generate(input).orElseThrow();
        List<MatchmakingAlgorithm.Group> second =
                new MatchmakingAlgorithm(new Random(42)).generate(input).orElseThrow();

        Set<Set<UUID>> firstAsSets = new HashSet<>();
        first.forEach(g -> firstAsSets.add(new HashSet<>(g.participantIds())));
        Set<Set<UUID>> secondAsSets = new HashSet<>();
        second.forEach(g -> secondAsSets.add(new HashSet<>(g.participantIds())));

        assertThat(firstAsSets).isEqualTo(secondAsSets);
    }

    @RepeatedTest(20)
    void assignCharactersGivesEveryParticipantADistinctCharacter() {
        List<UUID> participants = randomIds(6);
        List<UUID> characters = randomIds(6);

        Map<UUID, UUID> assignment = algorithm.assignCharacters(participants, Map.of(), characters, Map.of());

        assertThat(assignment.keySet()).containsExactlyInAnyOrderElementsOf(participants);
        assertThat(new HashSet<>(assignment.values())).hasSize(6).containsExactlyInAnyOrderElementsOf(characters);
    }

    @RepeatedTest(20)
    void prefersASameGenderCharacterWhenOneIsAvailable() {
        UUID alice = UUID.randomUUID();
        UUID femaleCharacter = UUID.randomUUID();
        UUID maleCharacter = UUID.randomUUID();
        List<UUID> participants = List.of(alice);
        List<UUID> characters = List.of(femaleCharacter, maleCharacter);
        Map<UUID, Gender> participantGenders = Map.of(alice, Gender.FEMALE);
        Map<UUID, Gender> characterGenders = Map.of(femaleCharacter, Gender.FEMALE, maleCharacter, Gender.MALE);

        Map<UUID, UUID> assignment =
                algorithm.assignCharacters(participants, participantGenders, characters, characterGenders);

        assertThat(assignment.get(alice)).isEqualTo(femaleCharacter);
    }

    @RepeatedTest(20)
    void fallsBackToANoPreferenceCharacterWhenNoSameGenderOneIsLeft() {
        UUID alice = UUID.randomUUID();
        UUID neutralCharacter = UUID.randomUUID();
        UUID maleCharacter = UUID.randomUUID();
        List<UUID> participants = List.of(alice);
        List<UUID> characters = List.of(neutralCharacter, maleCharacter);
        Map<UUID, Gender> participantGenders = Map.of(alice, Gender.FEMALE);
        Map<UUID, Gender> characterGenders = Map.of(maleCharacter, Gender.MALE);

        Map<UUID, UUID> assignment =
                algorithm.assignCharacters(participants, participantGenders, characters, characterGenders);

        assertThat(assignment.get(alice)).isEqualTo(neutralCharacter);
    }

    @Test
    void stillAssignsAnOppositeGenderCharacterRatherThanLeavingSomeoneWithNone() {
        UUID alice = UUID.randomUUID();
        UUID onlyMaleCharacter = UUID.randomUUID();
        Map<UUID, Gender> participantGenders = Map.of(alice, Gender.FEMALE);
        Map<UUID, Gender> characterGenders = Map.of(onlyMaleCharacter, Gender.MALE);

        Map<UUID, UUID> assignment = algorithm.assignCharacters(
                List.of(alice), participantGenders, List.of(onlyMaleCharacter), characterGenders);

        assertThat(assignment.get(alice)).isEqualTo(onlyMaleCharacter);
    }

    @RepeatedTest(20)
    void genderedParticipantsGetPriorityOverUngenderedOnesForMatchingCharacters() {
        UUID gendered = UUID.randomUUID();
        UUID ungendered = UUID.randomUUID();
        UUID femaleCharacter = UUID.randomUUID();
        UUID neutralCharacter = UUID.randomUUID();
        Map<UUID, Gender> participantGenders = Map.of(gendered, Gender.FEMALE);
        Map<UUID, Gender> characterGenders = Map.of(femaleCharacter, Gender.FEMALE);

        Map<UUID, UUID> assignment = algorithm.assignCharacters(
                List.of(ungendered, gendered),
                participantGenders,
                List.of(femaleCharacter, neutralCharacter),
                characterGenders);

        // The gendered participant must get the matching character even though the ungendered
        // one was listed first in the input order.
        assertThat(assignment.get(gendered)).isEqualTo(femaleCharacter);
        assertThat(assignment.get(ungendered)).isEqualTo(neutralCharacter);
    }

    @Test
    void ungenderedParticipantsGetWhicheverCharacterIsLeftRegardlessOfItsGenderTag() {
        UUID bob = UUID.randomUUID();
        UUID femaleCharacter = UUID.randomUUID();
        Map<UUID, Gender> characterGenders = Map.of(femaleCharacter, Gender.FEMALE);

        Map<UUID, UUID> assignment =
                algorithm.assignCharacters(List.of(bob), new HashMap<>(), List.of(femaleCharacter), characterGenders);

        assertThat(assignment.get(bob)).isEqualTo(femaleCharacter);
    }
}
