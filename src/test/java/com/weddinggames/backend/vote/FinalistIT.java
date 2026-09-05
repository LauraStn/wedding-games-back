package com.weddinggames.backend.vote;

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
class FinalistIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("finalist-test-" + UUID.randomUUID(), "Finalist Test Event", "fr-FR"));
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

    private UUID submitAndAcceptAnswer(Cookie adminCookie, UUID questionId, Cookie writerCookie, String content)
            throws Exception {
        mockMvc.perform(post("/api/v1/quiz/questions/{questionId}/answer/take-control", questionId)
                        .cookie(writerCookie))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/quiz/questions/{questionId}/answer", questionId)
                        .cookie(writerCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"%s"}
                                """.formatted(content)))
                .andExpect(status().isOk());
        MvcResult listResult = mockMvc.perform(
                        get("/api/v1/staff/questions/{questionId}/answers", questionId).cookie(adminCookie))
                .andReturn();
        JsonNode answers = objectMapper.readTree(listResult.getResponse().getContentAsString());
        for (JsonNode answer : answers) {
            if (answer.get("content").asText().equals(content)) {
                UUID answerId = UUID.fromString(answer.get("id").asText());
                mockMvc.perform(post("/api/v1/staff/answers/{answerId}/accept", answerId).cookie(adminCookie))
                        .andExpect(status().isOk());
                return answerId;
            }
        }
        throw new IllegalStateException("Answer not found: " + content);
    }

    @Test
    void computesFinalistsWithVoteCountMaskedByDefaultAndRevealableOnDemand() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        UUID carol = createParticipant(adminCookie, eventId, "Carol");
        UUID dave = createParticipant(adminCookie, eventId, "Dave");
        createHardExclusion(adminCookie, eventId, alice, carol);
        for (int i = 0; i < 4; i++) {
            createCharacter(adminCookie, eventId, "Char" + i + "-" + UUID.randomUUID());
        }
        Cookie aliceCookie = loginAsParticipant(alice);
        Cookie carolCookie = loginAsParticipant(carol);
        for (UUID id : new UUID[] {alice, bob, carol, dave}) {
            mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(loginAsParticipant(id)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());

        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/activate", questionId).cookie(adminCookie))
                .andExpect(status().isOk());

        UUID aliceTeamAnswerId = submitAndAcceptAnswer(adminCookie, questionId, aliceCookie, "Reponse Alice");
        UUID carolTeamAnswerId = submitAndAcceptAnswer(adminCookie, questionId, carolCookie, "Reponse Carol");

        mockMvc.perform(post("/api/v1/staff/questions/{questionId}/close", questionId).cookie(adminCookie))
                .andExpect(status().isOk());

        // Alice votes for Carol's team (the only option, since she can't vote for her own).
        mockMvc.perform(post("/api/v1/vote/questions/{questionId}", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"answerId":"%s"}
                                """.formatted(carolTeamAnswerId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/vote/questions/{questionId}", questionId)
                        .cookie(carolCookie)
                        .contentType("application/json")
                        .content("""
                                {"answerId":"%s"}
                                """.formatted(aliceTeamAnswerId)))
                .andExpect(status().isCreated());

        // Vote count masked by default.
        mockMvc.perform(get("/api/v1/staff/questions/{questionId}/finalists", questionId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].voteCount").value(org.hamcrest.Matchers.nullValue()));

        // Revealed on demand.
        mockMvc.perform(get("/api/v1/staff/questions/{questionId}/finalists", questionId)
                        .param("revealVoteCount", "true")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].voteCount").value(1))
                .andExpect(jsonPath("$[1].voteCount").value(1));
    }

    @Test
    void juryRoleCanAccessFinalists() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        Cookie juryCookie = loginAsNewStaff(StaffRole.JURY);

        mockMvc.perform(get("/api/v1/staff/questions/{questionId}/finalists", questionId).cookie(juryCookie))
                .andExpect(status().isOk());
    }

    @Test
    void participantRoleCannotAccessFinalists() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID gameId = createGame(adminCookie, eventId);
        UUID questionId = createQuestion(adminCookie, gameId);
        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        Cookie aliceCookie = loginAsParticipant(alice);

        mockMvc.perform(get("/api/v1/staff/questions/{questionId}/finalists", questionId).cookie(aliceCookie))
                .andExpect(status().isForbidden());
    }
}
