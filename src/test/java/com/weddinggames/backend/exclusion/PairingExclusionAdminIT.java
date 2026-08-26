package com.weddinggames.backend.exclusion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PairingExclusionAdminIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private PairingExclusionRepository pairingExclusionRepository;

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
    void createsListsAndUpdatesAPreferenceExclusion() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        UUID participantAId = createParticipant(adminCookie, eventId, "ExclusionTestGuestA");
        UUID participantBId = createParticipant(adminCookie, eventId, "ExclusionTestGuestB");

        String createBody =
                """
                {"participantAId":"%s","participantBId":"%s","reason":"Ex conjoints","exclusionType":"PREFERENCE"}
                """
                        .formatted(participantAId, participantBId);

        var createResult = mockMvc.perform(post("/api/v1/admin/events/{eventId}/exclusions", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exclusionType").value("PREFERENCE"))
                .andExpect(jsonPath("$.locked").value(false))
                .andReturn();
        UUID exclusionId = UUID.fromString(objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/exclusions", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(exclusionId.toString())));

        mockMvc.perform(patch("/api/v1/admin/exclusions/{id}", exclusionId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"reason":"Nouveau motif"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Nouveau motif"));

        mockMvc.perform(delete("/api/v1/admin/exclusions/{id}", exclusionId).cookie(adminCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void creatingADuplicateExclusionIsRefused() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        UUID participantAId = createParticipant(adminCookie, eventId, "ExclusionTestGuestC");
        UUID participantBId = createParticipant(adminCookie, eventId, "ExclusionTestGuestD");

        String createBody =
                """
                {"participantAId":"%s","participantBId":"%s","reason":null,"exclusionType":"PREFERENCE"}
                """
                        .formatted(participantAId, participantBId);

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/exclusions", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/exclusions", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXCLUSION_ALREADY_EXISTS"));
    }

    @Test
    void deletingTheHardJessikaSandrineExclusionIsRefused() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        Participant jessika = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Jessika", "Dijoux")
                .orElseThrow();
        Participant sandrine = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Sandrine", "Santin")
                .orElseThrow();
        UUID lowId = ExclusionPair.lower(jessika.getId(), sandrine.getId());
        UUID highId = ExclusionPair.higher(jessika.getId(), sandrine.getId());
        UUID exclusionId = pairingExclusionRepository
                .findByEventIdAndParticipantAIdAndParticipantBId(eventId, lowId, highId)
                .orElseThrow()
                .getId();

        mockMvc.perform(delete("/api/v1/admin/exclusions/{id}", exclusionId).cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HARD_EXCLUSION_IMMUTABLE"));
    }

    @Test
    void checkEndpointReportsWhetherTwoParticipantsCanBePaired() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
        Participant jessika = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Jessika", "Dijoux")
                .orElseThrow();
        Participant patrick = participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, "Patrick", "Santin")
                .orElseThrow();

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/exclusions/check", eventId)
                        .cookie(adminCookie)
                        .param("participantAId", jessika.getId().toString())
                        .param("participantBId", patrick.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canPair").value(false))
                .andExpect(jsonPath("$.hasHardExclusion").value(true));

        UUID freeParticipantId = createParticipant(adminCookie, eventId, "ExclusionTestGuestE");

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/exclusions/check", eventId)
                        .cookie(adminCookie)
                        .param("participantAId", jessika.getId().toString())
                        .param("participantBId", freeParticipantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canPair").value(true))
                .andExpect(jsonPath("$.hasHardExclusion").value(false));
    }
}
