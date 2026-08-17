package com.weddinggames.backend.participant;

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
}
