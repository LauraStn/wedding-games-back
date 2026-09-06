package com.weddinggames.backend.blindtest;

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
class BlindTestFormatIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("blind-test-format-test-" + UUID.randomUUID(), "Blind Test Format Test", "fr-FR"));
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
    void getsDefaultFormatThenUpdatesIt() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());

        mockMvc.perform(get("/api/v1/admin/games/{gameId}/blind-test-format", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundDurationSeconds").value(30))
                .andExpect(jsonPath("$.pointsPerCorrectGuess").value(10));

        mockMvc.perform(put("/api/v1/admin/games/{gameId}/blind-test-format", gameId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"roundDurationSeconds":45,"pointsPerCorrectGuess":20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundDurationSeconds").value(45))
                .andExpect(jsonPath("$.pointsPerCorrectGuess").value(20));

        mockMvc.perform(get("/api/v1/admin/games/{gameId}/blind-test-format", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundDurationSeconds").value(45));
    }

    @Test
    void rejectsAnInvalidRoundDuration() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());

        mockMvc.perform(put("/api/v1/admin/games/{gameId}/blind-test-format", gameId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"roundDurationSeconds":0,"pointsPerCorrectGuess":10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotAccessTheFormat() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/admin/games/{gameId}/blind-test-format", gameId).cookie(intervenantCookie))
                .andExpect(status().isForbidden());
    }
}
