package com.weddinggames.backend.participant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParticipantAdminIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Test
    void rejectsAnInvalidParticipantCreationRequestWithAConsistentErrorEnvelope() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();

        String invalidBody =
                """
                {"firstName":"","lastName":"","displayName":"","tableLabel":null,"participantType":null}
                """;

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void createsAParticipantWithAValidRequest() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();

        String validBody =
                """
                {"firstName":"Marie","lastName":"Curie","displayName":"Marie Curie","tableLabel":"Table 2","participantType":"GUEST"}
                """;

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Marie Curie"))
                .andExpect(jsonPath("$.status").value("INVITED"));
    }

    @Test
    void filtersParticipantsByTableLabelAndFreeTextSearch() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"firstName":"Alice","lastName":"Wonderland","displayName":"Alice Wonderland","tableLabel":"Table 5","participantType":"GUEST"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .param("tableLabel", "Table 5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName", org.hamcrest.Matchers.hasItem("Alice Wonderland")));

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .param("query", "wonderland"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName", org.hamcrest.Matchers.hasItem("Alice Wonderland")));

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/participants", eventId)
                        .cookie(adminCookie)
                        .param("tableLabel", "Table 99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Alice Wonderland"))));
    }

    @Test
    void disablingAParticipantSetsItsStatus() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        var participantId = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Jessika", "Dijoux")
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/participants/{id}/disable", participantId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void updatesAParticipantsPresenceStatus() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        var participantId = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Jessika", "Dijoux")
                .orElseThrow()
                .getId();

        mockMvc.perform(patch("/api/v1/admin/participants/{id}/status", participantId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updatesAParticipantsTable() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        var participantId = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Jessika", "Dijoux")
                .orElseThrow()
                .getId();

        mockMvc.perform(patch("/api/v1/admin/participants/{id}/table", participantId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"tableLabel":"Table 12"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableLabel").value("Table 12"));
    }

    @Test
    void deletingAParticipantInvolvedInAHardExclusionIsRefused() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        var jessikaId = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Jessika", "Dijoux")
                .orElseThrow()
                .getId();

        mockMvc.perform(delete("/api/v1/admin/participants/{id}", jessikaId).cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_HAS_HARD_EXCLUSION"));
    }
}
