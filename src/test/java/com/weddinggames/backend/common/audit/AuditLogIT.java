package com.weddinggames.backend.common.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AuditLogIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("audit-log-test-" + UUID.randomUUID(), "Audit Log Test Event", "fr-FR"));
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

    @Test
    void deletingAParticipantIsAuditLogged() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID participantId = createParticipant(adminCookie, event.getId(), "Alice");

        mockMvc.perform(delete("/api/v1/admin/participants/{id}", participantId).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/audit-log", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("PARTICIPANT_DELETED"))
                .andExpect(jsonPath("$[0].entityId").value(participantId.toString()))
                .andExpect(jsonPath("$[0].staffDisplayName").value(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void creatingAHardExclusionIsAuditLoggedButAPreferenceOneIsNot() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        UUID bob = createParticipant(adminCookie, event.getId(), "Bob");
        UUID carol = createParticipant(adminCookie, event.getId(), "Carol");

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/exclusions", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"participantAId":"%s","participantBId":"%s","reason":"Famille","exclusionType":"HARD"}
                                """.formatted(alice, bob)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/exclusions", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"participantAId":"%s","participantBId":"%s","reason":"Prefere pas","exclusionType":"PREFERENCE"}
                                """
                                        .formatted(alice, carol)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/audit-log", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("HARD_EXCLUSION_CREATED"));
    }

    @Test
    void updatingTheReasonOfAHardExclusionIsAuditLogged() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        UUID bob = createParticipant(adminCookie, event.getId(), "Bob");
        var createResult = mockMvc.perform(post("/api/v1/admin/events/{eventId}/exclusions", event.getId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"participantAId":"%s","participantBId":"%s","reason":"Famille","exclusionType":"HARD"}
                                """.formatted(alice, bob)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID exclusionId = UUID.fromString(objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        mockMvc.perform(patch("/api/v1/admin/exclusions/{id}", exclusionId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"reason":"Nouveau motif"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/audit-log", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].action").value("HARD_EXCLUSION_REASON_UPDATED"));
    }

    @Test
    void batchInvitationGenerationIsAuditLogged() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        createParticipant(adminCookie, event.getId(), "Alice");

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants/invitations/batch", event.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/audit-log", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("INVITATION_BATCH_REGENERATED"));
    }

    @Test
    void nonAdminCannotAccessTheAuditLog() throws Exception {
        WeddingEvent event = createEvent();
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/audit-log", event.getId()).cookie(intervenantCookie))
                .andExpect(status().isForbidden());
    }
}
