package com.weddinggames.backend.team;

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

class TeamParticipantIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("team-reveal-test-" + UUID.randomUUID(), "Team Reveal Test Event", "fr-FR"));
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

    @Test
    void participantSeesTheirOwnCharacterAndTheirPartnersAfterMatchmaking() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID eventId = event.getId();
        UUID alice = createParticipant(adminCookie, eventId, "Alice");
        UUID bob = createParticipant(adminCookie, eventId, "Bob");
        createCharacter(adminCookie, eventId, "Character-A-" + UUID.randomUUID());
        createCharacter(adminCookie, eventId, "Character-B-" + UUID.randomUUID());

        Cookie aliceCookie = loginAsParticipant(alice);
        Cookie bobCookie = loginAsParticipant(bob);
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(aliceCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(bobCookie)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/matchmaking/launch", eventId).cookie(adminCookie))
                .andExpect(status().isOk());

        var aliceResult = mockMvc.perform(get("/api/v1/team/me").cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myCharacterName").isNotEmpty())
                .andExpect(jsonPath("$.partners", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.partners[0].participantId").value(bob.toString()))
                .andReturn();
        String aliceCharacterName = objectMapper
                .readTree(aliceResult.getResponse().getContentAsString())
                .get("myCharacterName")
                .asText();

        mockMvc.perform(get("/api/v1/team/me").cookie(bobCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partners[0].participantId").value(alice.toString()))
                .andExpect(jsonPath("$.partners[0].characterName").value(aliceCharacterName));
    }

    @Test
    void failsBeforeMatchmakingHasEverBeenLaunched() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        Cookie aliceCookie = loginAsParticipant(alice);

        mockMvc.perform(get("/api/v1/team/me").cookie(aliceCookie)).andExpect(status().isNotFound());
    }

    @Test
    void staffRolesCannotAccessTheParticipantOnlyRevealEndpoint() throws Exception {
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/team/me").cookie(intervenantCookie)).andExpect(status().isForbidden());
    }
}
