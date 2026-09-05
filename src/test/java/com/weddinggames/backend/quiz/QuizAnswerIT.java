package com.weddinggames.backend.quiz;

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
import org.springframework.test.web.servlet.MvcResult;

/** Each test creates its own dedicated event: independent from every other IT class. */
class QuizAnswerIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("quiz-test-" + UUID.randomUUID(), "Quiz Test Event", "fr-FR"));
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

    private UUID createQuestion(Cookie adminCookie, UUID gameId) throws Exception {
        var result = mockMvc.perform(post("/api/v1/admin/games/{gameId}/questions", gameId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"prompt":"Quel est le comble ?","sequence":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    @Test
    void takesControlEditsAndTransfersControlToATeammate() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        Cookie aliceCookie = loginAsParticipant(alice);
        Cookie bobCookie = loginAsParticipant(bob);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(aliceCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(bobCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());

        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/activate", questionId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/quiz/questions/{questionId}/answer/take-control", questionId)
                        .cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllingParticipantId").value(alice.toString()));

        mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"La tarte aux pommes"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("La tarte aux pommes"));

        // Bob, read-only until now, can poll and see Alice's live draft.
        mockMvc.perform(get("/api/v1/quiz/questions/{questionId}/answer", questionId).cookie(bobCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("La tarte aux pommes"))
                .andExpect(jsonPath("$.controllingParticipantId").value(alice.toString()));

        // Bob cannot edit while Alice holds the pen.
        mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(bobCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Vole la main"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_NOT_IN_CONTROL"));

        // Bob takes control: the pen transfers, previous content is kept.
        mockMvc.perform(post("/api/v1/quiz/questions/{questionId}/answer/take-control", questionId)
                        .cookie(bobCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllingParticipantId").value(bob.toString()))
                .andExpect(jsonPath("$.content").value("La tarte aux pommes"));

        mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(bobCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Le gateau au chocolat"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Le gateau au chocolat"));

        // Alice, no longer in control, cannot edit anymore.
        mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Trop tard"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_NOT_IN_CONTROL"));
    }

    @Test
    void rejectsAnEmptyAnswer() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        Cookie aliceCookie = loginAsParticipant(alice);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(aliceCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(loginAsParticipant(bob))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/activate", questionId).cookie(adminCookie))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/quiz/questions/{questionId}/answer/take-control", questionId)
                        .cookie(aliceCookie))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsTakingControlWhenTheQuestionIsNotActive() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        Cookie aliceCookie = loginAsParticipant(alice);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(aliceCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(loginAsParticipant(bob))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        // Never activated.

        mockMvc.perform(post("/api/v1/quiz/questions/{questionId}/answer/take-control", questionId)
                        .cookie(aliceCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_ACTIVE"));
    }

    @Test
    void cannotEditAnymoreOnceTheQuestionIsClosed() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        Cookie aliceCookie = loginAsParticipant(alice);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(aliceCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(loginAsParticipant(bob))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/activate", questionId).cookie(adminCookie))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/quiz/questions/{questionId}/answer/take-control", questionId)
                        .cookie(aliceCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/close", questionId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Trop tard"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_ACTIVE"));
    }
}
