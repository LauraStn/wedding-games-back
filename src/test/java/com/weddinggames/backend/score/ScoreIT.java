package com.weddinggames.backend.score;

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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Each test creates its own dedicated event: independent from every other IT class. */
class ScoreIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("score-test-" + UUID.randomUUID(), "Score Test Event", "fr-FR"));
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

    /**
     * Runs matchmaking once on 4 fresh participants, with a hard exclusion forcing 2 of them onto
     * different teams, guaranteeing exactly 2 teams are formed. Returns their ids. A single launch
     * is required because relaunching matchmaking on the same event replaces all existing teams
     * (and, per the score table's ON DELETE CASCADE, any score already awarded to them).
     */
    private List<UUID> createTwoTeamsViaMatchmaking(Cookie adminCookie, UUID eventId) throws Exception {
        UUID alice = createParticipant(adminCookie, eventId, "Alice-" + UUID.randomUUID());
        UUID bob = createParticipant(adminCookie, eventId, "Bob-" + UUID.randomUUID());
        UUID carol = createParticipant(adminCookie, eventId, "Carol-" + UUID.randomUUID());
        UUID dave = createParticipant(adminCookie, eventId, "Dave-" + UUID.randomUUID());
        createHardExclusion(adminCookie, eventId, alice, carol);
        for (int i = 0; i < 4; i++) {
            createCharacter(adminCookie, eventId, "Char" + i + "-" + UUID.randomUUID());
        }
        for (UUID id : new UUID[] {alice, bob, carol, dave}) {
            mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(loginAsParticipant(id)))
                    .andExpect(status().isOk());
        }
        var result = mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode teams = objectMapper.readTree(result.getResponse().getContentAsString());
        return List.of(
                UUID.fromString(teams.get(0).get("id").asText()), UUID.fromString(teams.get(1).get("id").asText()));
    }

    @Test
    void awardsPointsToATeamAndPersistsTheLedgerEntry() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID teamId = createTwoTeamsViaMatchmaking(adminCookie, event.getId()).get(0);

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/scores", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"gameId":null,"teamId":"%s","points":10,"reason":"Manche 1"}
                                """.formatted(teamId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teamId").value(teamId.toString()))
                .andExpect(jsonPath("$.points").value(10))
                .andExpect(jsonPath("$.reason").value("Manche 1"));

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/scores", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void rejectsAwardingPointsToAnUnknownTeam() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID unknownTeamId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/scores", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"gameId":null,"teamId":"%s","points":10,"reason":null}
                                """.formatted(unknownTeamId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void computesThePodiumRankedByTotalPointsAcrossSeveralAwards() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        List<UUID> teams = createTwoTeamsViaMatchmaking(adminCookie, event.getId());
        UUID teamAId = teams.get(0);
        UUID teamBId = teams.get(1);

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/scores", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"gameId":null,"teamId":"%s","points":10,"reason":"Manche 1"}
                                """.formatted(teamAId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/scores", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"gameId":null,"teamId":"%s","points":5,"reason":"Manche 2"}
                                """.formatted(teamAId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/scores", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"gameId":null,"teamId":"%s","points":8,"reason":"Manche 1"}
                                """.formatted(teamBId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/podium", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamId").value(teamAId.toString()))
                .andExpect(jsonPath("$[0].totalPoints").value(15))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[1].teamId").value(teamBId.toString()))
                .andExpect(jsonPath("$[1].totalPoints").value(8))
                .andExpect(jsonPath("$[1].rank").value(2));
    }

    @Test
    void juryRoleCanViewThePodiumButCannotAwardPoints() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        Cookie juryCookie = loginAsNewStaff(StaffRole.JURY);

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/podium", event.getId()).cookie(juryCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/scores", event.getId())
                        .cookie(juryCookie)
                        .contentType("application/json")
                        .content("""
                                {"gameId":null,"teamId":"%s","points":10,"reason":null}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void participantCannotViewThePodium() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        Cookie aliceCookie = loginAsParticipant(alice);

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/podium", event.getId()).cookie(aliceCookie))
                .andExpect(status().isForbidden());
    }
}
