package com.weddinggames.backend.projection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Each test creates its own dedicated event: independent from every other IT class. */
class ProjectionIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("projection-test-" + UUID.randomUUID(), "Projection Test Event", "fr-FR"));
    }

    private void openLobby(Cookie adminCookie, UUID eventId) throws Exception {
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/open", eventId).cookie(adminCookie))
                .andExpect(status().isOk());
    }

    private UUID createGame(Cookie adminCookie, UUID eventId) throws Exception {
        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/games", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"type":"QUIZ","title":"Quiz absurde","sequence":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    @Test
    void reportsTheLobbyStateAndAnEmptyPodiumBeforeAnyGameStarts() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        openLobby(adminCookie, event.getId());

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/projection", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobby.status").value("OPEN"))
                .andExpect(jsonPath("$.activeGame").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.podium").isArray());
    }

    @Test
    void reportsTheActiveGameOnceStarted() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/start", gameId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/projection", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeGame.id").value(gameId.toString()))
                .andExpect(jsonPath("$.activeGame.status").value("ACTIVE"));
    }

    @Test
    void projectionRoleCanReadButNothingElseInThisEndpointCanMutate() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        Cookie projectionCookie = loginAsNewStaff(StaffRole.PROJECTION);

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/projection", event.getId()).cookie(projectionCookie))
                .andExpect(status().isOk());
    }

    @Test
    void participantCannotAccessTheProjectionEndpoint() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        String body =
                """
                {"firstName":"Alice","lastName":"Test","displayName":"Alice","tableLabel":null,"participantType":"GUEST"}
                """;
        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        UUID participantId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
        Cookie participantCookie = loginAsParticipant(participantId);

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/projection", event.getId()).cookie(participantCookie))
                .andExpect(status().isForbidden());
    }
}
