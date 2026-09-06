package com.weddinggames.backend.luiouelle;

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
class LuiOuElleSelectionIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(new WeddingEvent(
                "lui-ou-elle-selection-test-" + UUID.randomUUID(), "Lui ou Elle Selection Test", "fr-FR"));
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

    private UUID proposeAndAccept(Cookie adminCookie, Cookie participantCookie, String content) throws Exception {
        var result = mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(participantCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"%s"}
                                """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID questionId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
        mockMvc.perform(post("/api/v1/staff/lui-ou-elle/questions/{id}/accept", questionId).cookie(adminCookie))
                .andExpect(status().isOk());
        return questionId;
    }

    @Test
    void selectsAnAcceptedQuestionAndMarksItPlayed() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        UUID questionId = proposeAndAccept(adminCookie, aliceCookie, "Qui est le plus radin ?");

        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/lui-ou-elle/questions/select-random", event.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.status").value("PLAYED"));
    }

    @Test
    void neverSelectsTheSameQuestionTwiceAcrossRepeatedDraws() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        UUID first = proposeAndAccept(adminCookie, aliceCookie, "Question un ?");
        UUID second = proposeAndAccept(adminCookie, aliceCookie, "Question deux ?");

        var firstDraw = mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/lui-ou-elle/questions/select-random", event.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        UUID firstDrawnId = UUID.fromString(objectMapper
                .readTree(firstDraw.getResponse().getContentAsString())
                .get("id")
                .asText());
        UUID expectedRemaining = firstDrawnId.equals(first) ? second : first;

        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/lui-ou-elle/questions/select-random", event.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expectedRemaining.toString()));
    }

    @Test
    void rejectsSelectionWhenNoQuestionIsAccepted() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();

        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/lui-ou-elle/questions/select-random", event.getId())
                        .cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_ACCEPTED_LUI_OU_ELLE_QUESTION"));
    }

    @Test
    void participantCannotTriggerSelection() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        Cookie aliceCookie = loginAsParticipant(alice);

        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/lui-ou-elle/questions/select-random", event.getId())
                        .cookie(aliceCookie))
                .andExpect(status().isForbidden());
    }
}
