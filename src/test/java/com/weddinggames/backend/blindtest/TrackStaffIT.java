package com.weddinggames.backend.blindtest;

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
class TrackStaffIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("blind-test-staff-test-" + UUID.randomUUID(), "Blind Test Staff Test", "fr-FR"));
    }

    private UUID createGame(Cookie adminCookie, UUID eventId) throws Exception {
        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/games", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"type":"BLIND_TEST","title":"Blind test","sequence":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    private UUID createTrack(Cookie adminCookie, UUID gameId) throws Exception {
        var result = mockMvc.perform(post("/api/v1/admin/games/{gameId}/tracks", gameId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"title":"Freed from Desire","artist":"Gala","variant":"REVERSED","sequence":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    @Test
    void drivesATrackThroughActivateTimerAndClose() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        UUID trackId = createTrack(adminCookie, gameId);

        mockMvc.perform(post("/api/v1/staff/tracks/{trackId}/activate", trackId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.remainingSeconds").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/v1/staff/tracks/{trackId}/start-timer", trackId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingSeconds").value(30));

        mockMvc.perform(get("/api/v1/staff/games/{gameId}/tracks/active", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(trackId.toString()));

        mockMvc.perform(post("/api/v1/staff/tracks/{trackId}/close", trackId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/staff/games/{gameId}/tracks/active", gameId).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotActivateTheSameTrackTwice() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        UUID trackId = createTrack(adminCookie, gameId);

        mockMvc.perform(post("/api/v1/staff/tracks/{trackId}/activate", trackId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/staff/tracks/{trackId}/activate", trackId).cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRACK_STATUS_TRANSITION"));
    }

    @Test
    void projectionRoleCanReadTrackStateButCannotActivateIt() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        UUID trackId = createTrack(adminCookie, gameId);
        Cookie projectionCookie = loginAsNewStaff(StaffRole.PROJECTION);

        mockMvc.perform(get("/api/v1/staff/tracks/{trackId}/state", trackId).cookie(projectionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/v1/staff/tracks/{trackId}/activate", trackId).cookie(projectionCookie))
                .andExpect(status().isForbidden());
    }
}
