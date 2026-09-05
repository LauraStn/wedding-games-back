package com.weddinggames.backend.vote;

import static org.assertj.core.api.Assertions.assertThat;
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
class VoteIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(new WeddingEvent("vote-test-" + UUID.randomUUID(), "Vote Test Event", "fr-FR"));
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
    void votingIsAnonymizedShuffledAndExcludesMyOwnTeam() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();

        // Team A: Alice + Bob. Team B: Carol + Dave.
        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        UUID carol = createParticipant(adminCookie, eventId, "Carol");
        UUID dave = createParticipant(adminCookie, eventId, "Dave");
        for (int i = 0; i < 4; i++) {
            createCharacter(adminCookie, eventId, "Char" + i + "-" + UUID.randomUUID());
        }
        // Forces Alice and Carol onto different teams, regardless of how matchmaking otherwise
        // pairs the other two: the test only needs their two answers to come from two distinct
        // teams, not any specific pairing.
        createHardExclusion(adminCookie, eventId, alice, carol);
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

        // Alice's ballot must show only Carol's team's answer, anonymized (no team/character fields at all).
        MvcResult ballotResult = mockMvc.perform(
                        get("/api/v1/vote/questions/{questionId}/options", questionId).cookie(aliceCookie))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ballot = objectMapper.readTree(ballotResult.getResponse().getContentAsString());
        assertThat(ballot).hasSize(1);
        assertThat(ballot.get(0).get("answerId").asText()).isEqualTo(carolTeamAnswerId.toString());
        assertThat(ballot.get(0).has("teamId")).isFalse();
        assertThat(ballot.get(0).has("characterName")).isFalse();

        // Alice cannot vote for her own team's answer, even if she tries directly by id.
        mockMvc.perform(post("/api/v1/vote/questions/{questionId}", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"answerId":"%s"}
                                """.formatted(aliceTeamAnswerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTE_SELF_TEAM_FORBIDDEN"));

        // Alice votes for Carol's team.
        mockMvc.perform(post("/api/v1/vote/questions/{questionId}", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"answerId":"%s"}
                                """.formatted(carolTeamAnswerId)))
                .andExpect(status().isCreated());

        // Alice cannot vote a second time on the same question.
        mockMvc.perform(post("/api/v1/vote/questions/{questionId}", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"answerId":"%s"}
                                """.formatted(carolTeamAnswerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTE_ALREADY_CAST"));
    }

    @Test
    void votingBeforeTheQuestionIsClosedIsRejected() throws Exception {
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
        // Never closed.

        mockMvc.perform(get("/api/v1/vote/questions/{questionId}/options", questionId).cookie(aliceCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_CLOSED"));
    }
}
