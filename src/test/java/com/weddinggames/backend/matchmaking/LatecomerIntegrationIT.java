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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** Each test creates its own dedicated event: matchmaking/latecomer state must not leak across tests. */
class LatecomerIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("latecomer-test-" + UUID.randomUUID(), "Latecomer Test Event", "fr-FR"));
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

    private void markPresent(UUID participantId) throws Exception {
        Cookie participantCookie = loginAsParticipant(participantId);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(participantCookie)).andExpect(status().isOk());
    }

    private void markLate(Cookie adminCookie, UUID eventId, UUID participantId) throws Exception {
        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/lobby/participants/{participantId}/late", eventId, participantId)
                        .cookie(adminCookie))
                .andExpect(status().isOk());
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
    void joinsALatecomerIntoAnExistingBinomeMakingItATrio() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID a = createParticipant(adminCookie, eventId, "A");
        UUID b = createParticipant(adminCookie, eventId, "B");
        UUID lateArrival = createParticipant(adminCookie, eventId, "LateGuy");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char3-" + UUID.randomUUID());
        markPresent(a);
        markPresent(b);

        MvcResult launchResult = mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        String teamId = objectMapper
                .readTree(launchResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        markLate(adminCookie, eventId, lateArrival);

        mockMvc.perform(get(
                        "/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}/options",
                        eventId,
                        lateArrival)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatibleTeams", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.compatibleTeams[0].id").value(teamId));

        MvcResult joinResult = mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}/join-team/{teamId}",
                        eventId,
                        lateArrival,
                        teamId)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode team = objectMapper.readTree(joinResult.getResponse().getContentAsString());
        assertThat(team.get("members")).hasSize(3);
        assertThat(team.get("id").asText()).isEqualTo(teamId);
    }

    @Test
    void pairsTwoLatecomersIntoANewBinome() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID a = createParticipant(adminCookie, eventId, "A");
        UUID b = createParticipant(adminCookie, eventId, "B");
        UUID lateOne = createParticipant(adminCookie, eventId, "LateOne");
        UUID lateTwo = createParticipant(adminCookie, eventId, "LateTwo");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char3-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char4-" + UUID.randomUUID());
        markPresent(a);
        markPresent(b);
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());

        markLate(adminCookie, eventId, lateOne);
        markLate(adminCookie, eventId, lateTwo);

        mockMvc.perform(get(
                        "/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}/options",
                        eventId,
                        lateOne)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatibleLatecomers", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.compatibleLatecomers[0].participantId").value(lateTwo.toString()));

        MvcResult pairResult = mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}/pair-with/{otherId}",
                        eventId,
                        lateOne,
                        lateTwo)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode newTeam = objectMapper.readTree(pairResult.getResponse().getContentAsString());
        assertThat(newTeam.get("members")).hasSize(2);
    }

    @Test
    void rejectsJoiningATeamWhenAHardExclusionExistsWithAnExistingMember() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID a = createParticipant(adminCookie, eventId, "A");
        UUID b = createParticipant(adminCookie, eventId, "B");
        UUID lateArrival = createParticipant(adminCookie, eventId, "LateGuy");
        createHardExclusion(adminCookie, eventId, a, lateArrival);
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        markPresent(a);
        markPresent(b);

        MvcResult launchResult = mockMvc.perform(
                        post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        String teamId = objectMapper
                .readTree(launchResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();
        markLate(adminCookie, eventId, lateArrival);

        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}/join-team/{teamId}",
                        eventId,
                        lateArrival,
                        teamId)
                        .cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LATECOMER_HARD_EXCLUSION"));
    }

    @Test
    void nonStaffCannotAccessLatecomerEndpoints() throws Exception {
        WeddingEvent event = createEvent();
        Cookie juryCookie = loginAsNewStaff(StaffRole.JURY);

        mockMvc.perform(get(
                        "/api/v1/staff/events/{eventId}/matchmaking/latecomers/{participantId}/options",
                        event.getId(),
                        UUID.randomUUID())
                        .cookie(juryCookie))
                .andExpect(status().isForbidden());
    }
}
