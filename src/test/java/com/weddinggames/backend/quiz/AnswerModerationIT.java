package com.weddinggames.backend.quiz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** Each test creates its own dedicated event: independent from every other IT class. */
class AnswerModerationIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("moderation-test-" + UUID.randomUUID(), "Moderation Test Event", "fr-FR"));
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

    /** Sets up: 2 participants matchmade into one team, a question activated, and a submitted answer. */
    private UUID setUpAnswer(Cookie adminCookie, UUID eventId, UUID gameId, UUID questionId, String content)
            throws Exception {
        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        createCharacter(adminCookie, eventId, "Char1-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Char2-" + UUID.randomUUID());
        Cookie aliceCookie = loginAsParticipant(alice);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(aliceCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(loginAsParticipant(bob))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/activate", questionId).cookie(adminCookie))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/quiz/questions/{questionId}/answer/take-control", questionId)
                        .cookie(aliceCookie))
                .andExpect(status().isOk());
        MvcResult result = mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"%s"}
                                """.formatted(content)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("teamId")
                .asText());
    }

    @Test
    void acceptsAnAnswer() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        setUpAnswer(adminCookie, eventId, gameId, questionId, "La tarte aux pommes");

        MvcResult listResult = mockMvc.perform(
                        get("/api/v1/staff/questions/{questionId}/answers", questionId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].moderationStatus").value("PENDING"))
                .andReturn();
        String answerId = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(post("/api/v1/staff/answers/{answerId}/accept", answerId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("ACCEPTED"));
    }

    @Test
    void hidesAnInappropriateAnswer() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        setUpAnswer(adminCookie, eventId, gameId, questionId, "Contenu inapproprie");

        MvcResult listResult = mockMvc.perform(
                        get("/api/v1/staff/questions/{questionId}/answers", questionId).cookie(adminCookie))
                .andReturn();
        String answerId = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(post("/api/v1/staff/answers/{answerId}/hide", answerId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("HIDDEN"));
    }

    @Test
    void correctsATypoWithoutChangingTheModerationStatus() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        setUpAnswer(adminCookie, eventId, gameId, questionId, "fote de frape");
        MvcResult listResult = mockMvc.perform(
                        get("/api/v1/staff/questions/{questionId}/answers", questionId).cookie(adminCookie))
                .andReturn();
        String answerId = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();
        mockMvc.perform(post("/api/v1/staff/answers/{answerId}/accept", answerId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/staff/answers/{answerId}/content", answerId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"faute de frappe"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("faute de frappe"))
                .andExpect(jsonPath("$.moderationStatus").value("ACCEPTED"));
    }

    @Test
    void relaunchesATeamClearingItsAnswer() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        UUID teamId = setUpAnswer(adminCookie, eventId, gameId, questionId, "Premiere tentative");

        mockMvc.perform(post(
                        "/api/v1/staff/questions/{questionId}/teams/{teamId}/relaunch", questionId, teamId)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(""))
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"))
                .andExpect(jsonPath("$.controllingParticipantId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void nonStaffCannotAccessModerationEndpoints() throws Exception {
        Cookie intervenantJuryCookie = loginAsNewStaff(StaffRole.JURY);

        mockMvc.perform(get(
                        "/api/v1/staff/questions/{questionId}/answers", UUID.randomUUID())
                        .cookie(intervenantJuryCookie))
                .andExpect(status().isForbidden());
    }
}
