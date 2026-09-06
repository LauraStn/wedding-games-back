package com.weddinggames.backend.blindtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class TrackAdminIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("blind-test-track-test-" + UUID.randomUUID(), "Blind Test Track Test", "fr-FR"));
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

    @Test
    void createsListsUpdatesAndDeletesATrack() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());

        var createResult = mockMvc.perform(post("/api/v1/admin/games/{gameId}/tracks", gameId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"title":"Freed from Desire","artist":"Gala","variant":"REVERSED","sequence":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Freed from Desire"))
                .andReturn();
        UUID trackId = UUID.fromString(objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        mockMvc.perform(get("/api/v1/admin/games/{gameId}/tracks", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(put("/api/v1/admin/tracks/{id}", trackId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"title":"Freed from Desire","artist":"Gala","variant":"SLOWED_DOWN","sequence":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variant").value("SLOWED_DOWN"))
                .andExpect(jsonPath("$.sequence").value(1));

        mockMvc.perform(delete("/api/v1/admin/tracks/{id}", trackId).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/games/{gameId}/tracks", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void rejectsCreatingATrackForAnUnknownGame() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/games/{gameId}/tracks", UUID.randomUUID())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"title":"Titre","artist":"Artiste","variant":"REVERSED","sequence":0}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotManageTracks() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(post("/api/v1/admin/games/{gameId}/tracks", gameId)
                        .cookie(intervenantCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"title":"Titre","artist":"Artiste","variant":"REVERSED","sequence":0}
                                """))
                .andExpect(status().isForbidden());
    }
}
