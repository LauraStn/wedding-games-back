package com.weddinggames.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.PairingExclusion;
import com.weddinggames.backend.exclusion.PairingExclusionRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that each role only reaches the endpoints the specification grants it, using the
 * real security filter chain and method-security annotations end to end (no mocking).
 */
class RolePermissionsIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private PairingExclusionRepository pairingExclusionRepository;

    private WeddingEvent seedEvent() {
        return weddingEventRepository.findBySlug("seed-wedding").orElseThrow();
    }

    private Participant seedParticipant(String firstName, String lastName) {
        return participantRepository.findAll().stream()
                .filter(p -> p.getFirstName().equals(firstName) && p.getLastName().equals(lastName))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void anonymousRequestToAdminEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/staff")).andExpect(status().isUnauthorized());
    }

    @Test
    void participantCannotAccessAdminEndpoints() throws Exception {
        Participant jessika = seedParticipant("Jessika", "Dijoux");
        Cookie participantCookie = loginAsParticipant(jessika.getId());

        mockMvc.perform(get("/api/v1/admin/staff").cookie(participantCookie)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanListStaffAccounts() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        mockMvc.perform(get("/api/v1/admin/staff").cookie(adminCookie)).andExpect(status().isOk());
    }

    @Test
    void intervenantCanOpenTheLobbyButNotAccessAdminExclusions() throws Exception {
        UUID eventId = seedEvent().getId();
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/open", eventId).cookie(intervenantCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/exclusions", eventId).cookie(intervenantCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void intervenantCannotConfigureTheEventTheme() throws Exception {
        UUID eventId = seedEvent().getId();
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/admin/events/{eventId}", eventId).cookie(intervenantCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void juryAndProjectionCannotAccessAdminEndpoints() throws Exception {
        Cookie juryCookie = loginAsNewStaff(StaffRole.JURY);
        Cookie projectionCookie = loginAsNewStaff(StaffRole.PROJECTION);

        mockMvc.perform(get("/api/v1/admin/staff").cookie(juryCookie)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/staff").cookie(projectionCookie)).andExpect(status().isForbidden());
    }

    @Test
    void intervenantCanNeverDeleteAnAbsoluteExclusion() throws Exception {
        WeddingEvent event = seedEvent();
        PairingExclusion hardExclusion = pairingExclusionRepository
                .findByEventIdAndParticipantAIdAndParticipantBId(
                        event.getId(),
                        minId(seedParticipant("Jessika", "Dijoux").getId(), seedParticipant("Sandrine", "Santin").getId()),
                        maxId(
                                seedParticipant("Jessika", "Dijoux").getId(),
                                seedParticipant("Sandrine", "Santin").getId()))
                .orElseThrow();

        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        // The intervenant has no access to the exclusion endpoints at all: 403, regardless of exclusion type.
        mockMvc.perform(delete("/api/v1/admin/exclusions/{id}", hardExclusion.getId()).cookie(intervenantCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void evenAnAdminCanNeverDeleteAnAbsoluteExclusionThroughTheApi() throws Exception {
        WeddingEvent event = seedEvent();
        UUID jessikaId = seedParticipant("Jessika", "Dijoux").getId();
        UUID patrickId = seedParticipant("Patrick", "Santin").getId();
        PairingExclusion hardExclusion = pairingExclusionRepository
                .findByEventIdAndParticipantAIdAndParticipantBId(event.getId(), minId(jessikaId, patrickId), maxId(jessikaId, patrickId))
                .orElseThrow();

        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);

        mockMvc.perform(delete("/api/v1/admin/exclusions/{id}", hardExclusion.getId()).cookie(adminCookie))
                .andExpect(status().isConflict());
    }

    private static UUID minId(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static UUID maxId(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? b : a;
    }
}
