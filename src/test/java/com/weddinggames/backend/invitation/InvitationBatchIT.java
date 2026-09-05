package com.weddinggames.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InvitationBatchIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private UUID eventId() {
        return weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
    }

    @Test
    void generatesAPrintablePdfForEveryParticipantWhenNoIdsAreGiven() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);

        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants/invitations/batch", eventId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("invitations-qr.pdf")))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void generatesForOnlyTheGivenParticipantIds() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId(), "Jessika", "Dijoux")
                .orElseThrow();

        String body = objectMapper.writeValueAsString(
                java.util.Map.of("participantIds", java.util.List.of(jessika.getId())));

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants/invitations/batch", eventId())
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void nonAdminStaffCannotAccessTheBatchEndpoint() throws Exception {
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants/invitations/batch", eventId())
                        .cookie(intervenantCookie)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
