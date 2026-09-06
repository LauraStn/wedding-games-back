package com.weddinggames.backend.luiouelle;

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
class LuiOuElleModerationIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("lui-ou-elle-mod-test-" + UUID.randomUUID(), "Lui ou Elle Moderation Test", "fr-FR"));
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

    private void openLobby(Cookie adminCookie, UUID eventId) throws Exception {
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/open", eventId).cookie(adminCookie))
                .andExpect(status().isOk());
    }

    private UUID proposeQuestion(Cookie participantCookie, String content) throws Exception {
        var result = mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(participantCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"%s"}
                                """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    @Test
    void staffCanListAcceptRejectAndCorrectAProposedQuestion() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        UUID questionId = proposeQuestion(aliceCookie, "Qui est le plus radin ?");

        mockMvc.perform(
                        get("/api/v1/staff/events/{eventId}/lui-ou-elle/questions", event.getId())
                                .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(post("/api/v1/staff/lui-ou-elle/questions/{id}/accept", questionId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(put("/api/v1/staff/lui-ou-elle/questions/{id}/content", questionId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Qui est le plus generreux ?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Qui est le plus generreux ?"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/v1/staff/lui-ou-elle/questions/{id}/reject", questionId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void editingAQuestionAfterAcceptanceSendsItBackToPendingForReview() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        UUID questionId = proposeQuestion(aliceCookie, "Qui est le plus radin ?");

        mockMvc.perform(post("/api/v1/staff/lui-ou-elle/questions/{id}/accept", questionId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/lui-ou-elle/questions/{id}", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Version modifiee par Alice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void participantCannotModerateQuestions() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        UUID questionId = proposeQuestion(aliceCookie, "Qui est le plus radin ?");

        mockMvc.perform(post("/api/v1/staff/lui-ou-elle/questions/{id}/accept", questionId).cookie(aliceCookie))
                .andExpect(status().isForbidden());
    }
}
