package com.weddinggames.backend.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** Each test creates its own dedicated event, characters and participants: matchmaking mutates
 * shared per-event state (teams) and must not interfere with other IT classes. */
class MatchmakingIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("matchmaking-test-" + UUID.randomUUID(), "Matchmaking Test Event", "fr-FR"));
    }

    private UUID createParticipant(Cookie adminCookie, UUID eventId, String displayName) throws Exception {
        String body =
                """
                {"firstName":"%s","lastName":"Test","displayName":"%s","tableLabel":null,"participantType":"GUEST"}
                """
                        .formatted(displayName, displayName);
        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    private void createCharacter(Cookie adminCookie, UUID eventId, String name) throws Exception {
        mockMvc.perform(post("/api/v1/admin/events/{eventId}/characters", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"name":"%s","description":null,"avatarUrl":null}
                                """.formatted(name)))
                .andExpect(status().isCreated());
    }

    private UUID createParticipantWithGender(Cookie adminCookie, UUID eventId, String displayName, String gender)
            throws Exception {
        String body =
                """
                {"firstName":"%s","lastName":"Test","displayName":"%s","tableLabel":null,"participantType":"GUEST","gender":"%s"}
                """
                        .formatted(displayName, displayName, gender);
        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    private UUID createCharacterWithGender(Cookie adminCookie, UUID eventId, String name, String gender)
            throws Exception {
        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/characters", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"name":"%s","description":null,"avatarUrl":null,"gender":"%s"}
                                """.formatted(name, gender)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    private void markPresent(UUID participantId) throws Exception {
        Cookie participantCookie = loginAsParticipant(participantId);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(participantCookie)).andExpect(status().isOk());
    }

    private void createHardExclusion(Cookie adminCookie, UUID eventId, UUID participantAId, UUID participantBId)
            throws Exception {
        String body = """
                {"participantAId":"%s","participantBId":"%s","reason":"test","exclusionType":"HARD"}
                """
                .formatted(participantAId, participantBId);
        mockMvc.perform(post("/api/v1/admin/events/{eventId}/exclusions", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void launchGeneratesTeamsRespectingHardExclusionsWithDistinctCharacters() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID jessika = createParticipant(adminCookie, eventId, "Jessika");
        UUID sandrine = createParticipant(adminCookie, eventId, "Sandrine");
        UUID patrick = createParticipant(adminCookie, eventId, "Patrick");
        UUID guest1 = createParticipant(adminCookie, eventId, "Guest1");
        UUID guest2 = createParticipant(adminCookie, eventId, "Guest2");
        UUID guest3 = createParticipant(adminCookie, eventId, "Guest3");
        List<UUID> allParticipants = List.of(jessika, sandrine, patrick, guest1, guest2, guest3);

        createHardExclusion(adminCookie, eventId, jessika, sandrine);
        createHardExclusion(adminCookie, eventId, jessika, patrick);

        for (int i = 0; i < 6; i++) {
            createCharacter(adminCookie, eventId, "Character-" + i + "-" + UUID.randomUUID());
        }
        for (UUID participantId : allParticipants) {
            markPresent(participantId);
        }

        MvcResult launchResult = mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode teams = objectMapper.readTree(launchResult.getResponse().getContentAsString());

        assertThat(teams).hasSize(3);
        List<String> allMemberIds = new ArrayList<>();
        List<String> allCharacterIds = new ArrayList<>();
        String jessikaTeamMembers = null;
        for (JsonNode team : teams) {
            List<String> memberIdsInTeam = new ArrayList<>();
            for (JsonNode member : team.get("members")) {
                allMemberIds.add(member.get("participantId").asText());
                allCharacterIds.add(member.get("characterId").asText());
                memberIdsInTeam.add(member.get("participantId").asText());
            }
            if (memberIdsInTeam.contains(jessika.toString())) {
                jessikaTeamMembers = String.join(",", memberIdsInTeam);
            }
        }
        assertThat(allMemberIds)
                .hasSize(6)
                .containsExactlyInAnyOrderElementsOf(allParticipants.stream().map(UUID::toString).toList());
        assertThat(new java.util.HashSet<>(allCharacterIds)).hasSize(6);
        assertThat(jessikaTeamMembers).isNotNull();
        assertThat(jessikaTeamMembers).doesNotContain(sandrine.toString()).doesNotContain(patrick.toString());
    }

    @Test
    void launchFailsExplicitlyWhenNoArrangementCanRespectHardExclusions() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID jessika = createParticipant(adminCookie, eventId, "Jessika");
        UUID sandrine = createParticipant(adminCookie, eventId, "Sandrine");
        UUID patrick = createParticipant(adminCookie, eventId, "Patrick");
        createHardExclusion(adminCookie, eventId, jessika, sandrine);
        createHardExclusion(adminCookie, eventId, jessika, patrick);
        for (UUID id : List.of(jessika, sandrine, patrick)) {
            markPresent(id);
        }

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCHMAKING_INFEASIBLE"));
    }

    @Test
    void launchFailsWhenFewerThanTwoParticipantsArePresent() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID solo = createParticipant(adminCookie, eventId, "Solo");
        markPresent(solo);

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MATCHMAKING_NOT_ENOUGH_PARTICIPANTS"));
    }

    @Test
    void relaunchingReplacesThePreviousTeams() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID a = createParticipant(adminCookie, eventId, "A");
        UUID b = createParticipant(adminCookie, eventId, "B");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        markPresent(a);
        markPresent(b);

        MvcResult firstLaunch = mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        String firstTeamId = objectMapper
                .readTree(firstLaunch.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        MvcResult secondLaunch = mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        String secondTeamId = objectMapper
                .readTree(secondLaunch.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        assertThat(secondTeamId).isNotEqualTo(firstTeamId);

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/matchmaking/teams", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(secondTeamId));
    }

    @Test
    void nonStaffCannotLaunchMatchmaking() throws Exception {
        WeddingEvent event = createEvent();
        Cookie juryCookie = loginAsNewStaff(StaffRole.JURY);

        mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", event.getId()).cookie(juryCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void prefersASameGenderCharacterWhenBothAreTagged() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID alice = createParticipantWithGender(adminCookie, eventId, "Alice", "FEMALE");
        UUID bob = createParticipantWithGender(adminCookie, eventId, "Bob", "MALE");
        UUID femaleCharacter = createCharacterWithGender(adminCookie, eventId, "SailorMoon-" + UUID.randomUUID(), "FEMALE");
        UUID maleCharacter = createCharacterWithGender(adminCookie, eventId, "Sangoku-" + UUID.randomUUID(), "MALE");
        markPresent(alice);
        markPresent(bob);

        MvcResult launchResult = mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode teams = objectMapper.readTree(launchResult.getResponse().getContentAsString());

        String aliceCharacterId = null;
        String bobCharacterId = null;
        for (JsonNode member : teams.get(0).get("members")) {
            if (member.get("participantId").asText().equals(alice.toString())) {
                aliceCharacterId = member.get("characterId").asText();
            }
            if (member.get("participantId").asText().equals(bob.toString())) {
                bobCharacterId = member.get("characterId").asText();
            }
        }
        assertThat(aliceCharacterId).isEqualTo(femaleCharacter.toString());
        assertThat(bobCharacterId).isEqualTo(maleCharacter.toString());
    }
}
