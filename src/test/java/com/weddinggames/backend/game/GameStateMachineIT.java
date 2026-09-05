package com.weddinggames.backend.game;

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
import org.springframework.test.web.servlet.MvcResult;

class GameStateMachineIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("game-fsm-test-" + UUID.randomUUID(), "Game FSM Test Event", "fr-FR"));
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
    void drivesAGameThroughStartNextQuestionPauseAndResume() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/start", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.phase").value("PREPARATION"));

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/next-question", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("QUESTION"));

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/pause", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.phase").value("QUESTION"));

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/resume", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.phase").value("QUESTION"));
    }

    @Test
    void cannotAdvanceToTheNextQuestionBeforeStarting() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/next-question", gameId).cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_NOT_ACTIVE"));
    }

    @Test
    void listsGamesForAnEventOrderedBySequence() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        createGame(adminCookie, event.getId());
        createGame(adminCookie, event.getId());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/games", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void nonStaffCannotDriveTheGameStateMachine() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie juryCookie = loginAsNewStaff(StaffRole.JURY);

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/start", gameId).cookie(juryCookie))
                .andExpect(status().isForbidden());
    }
}
