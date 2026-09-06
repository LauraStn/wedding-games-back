package com.weddinggames.backend.whosaidit;

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

/** Each test creates its own dedicated event: independent from every other IT class. */
class WhoSaidItRevealConsentIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(new WeddingEvent(
                "who-said-it-consent-test-" + UUID.randomUUID(), "Who Said It Consent Test", "fr-FR"));
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

    private UUID proposeAndAccept(Cookie adminCookie, Cookie participantCookie, String content, boolean consent)
            throws Exception {
        var result = mockMvc.perform(post("/api/v1/who-said-it/questions")
                        .cookie(participantCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"%s","revealAuthorConsent":%s}
                                """.formatted(content, consent)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID questionId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
        mockMvc.perform(post("/api/v1/staff/who-said-it/questions/{id}/accept", questionId).cookie(adminCookie))
                .andExpect(status().isOk());
        return questionId;
    }

    @Test
    void hidesTheAuthorNameAtSelectionTimeWhenConsentWasNotGiven() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        proposeAndAccept(adminCookie, aliceCookie, "Qui est le plus radin ?", false);

        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/who-said-it/questions/select-random", event.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorDisplayName").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.revealAuthorConsent").value(false));
    }

    @Test
    void revealsTheAuthorNameAtSelectionTimeWhenConsentWasGiven() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        proposeAndAccept(adminCookie, aliceCookie, "Qui est le plus radin ?", true);

        mockMvc.perform(post(
                        "/api/v1/staff/events/{eventId}/who-said-it/questions/select-random", event.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorDisplayName").value("Alice"))
                .andExpect(jsonPath("$.revealAuthorConsent").value(true));
    }

    @Test
    void staffModerationListAlwaysShowsTheAuthorNameRegardlessOfConsent() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        proposeAndAccept(adminCookie, aliceCookie, "Qui est le plus radin ?", false);

        mockMvc.perform(
                        get("/api/v1/staff/events/{eventId}/who-said-it/questions", event.getId())
                                .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorDisplayName").value("Alice"))
                .andExpect(jsonPath("$[0].revealAuthorConsent").value(false));
    }
}
