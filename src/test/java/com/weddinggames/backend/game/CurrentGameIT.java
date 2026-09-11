package com.weddinggames.backend.game;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.ParticipantType;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** Each test creates its own dedicated event: independent from every other IT class. */
class CurrentGameIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("current-game-test-" + UUID.randomUUID(), "Current Game Test", "fr-FR"));
    }

    private Participant createParticipant(WeddingEvent event) {
        return participantRepository.save(
                new Participant(event, "Guest", "Test-" + UUID.randomUUID(), "Guest Test", null, ParticipantType.GUEST));
    }

    private UUID createGame(Cookie adminCookie, UUID eventId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/games", eventId)
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

    private UUID createQuestion(Cookie adminCookie, UUID gameId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/games/{gameId}/questions", gameId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"prompt":"Quel est le comble pour un electricien ?","sequence":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    @Test
    void reportsNothingLiveWhenNoGameHasStarted() throws Exception {
        WeddingEvent event = createEvent();
        Cookie participantCookie = loginAsParticipant(createParticipant(event).getId());

        mockMvc.perform(get("/api/v1/games/current").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.question").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void exposesTheActiveGameAndCurrentQuestionIdToTheGuest() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        Cookie participantCookie = loginAsParticipant(createParticipant(event).getId());

        UUID gameId = createGame(adminCookie, event.getId());
        UUID questionId = createQuestion(adminCookie, gameId);

        // A configured-but-not-started game is still nothing to the guest.
        mockMvc.perform(get("/api/v1/games/current").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/start", gameId).cookie(adminCookie))
                .andExpect(status().isOk());

        // Started, but the question has not been activated yet: game visible, question still null.
        mockMvc.perform(get("/api/v1/games/current").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game.id").value(gameId.toString()))
                .andExpect(jsonPath("$.game.status").value("ACTIVE"))
                .andExpect(jsonPath("$.question").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/activate", questionId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/games/current").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game.id").value(gameId.toString()))
                .andExpect(jsonPath("$.question.id").value(questionId.toString()))
                .andExpect(jsonPath("$.question.status").value("ACTIVE"))
                .andExpect(jsonPath("$.question.prompt").value("Quel est le comble pour un electricien ?"));
    }

    @Test
    void isForbiddenForStaffRoles() throws Exception {
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/games/current").cookie(intervenantCookie)).andExpect(status().isForbidden());
    }
}
