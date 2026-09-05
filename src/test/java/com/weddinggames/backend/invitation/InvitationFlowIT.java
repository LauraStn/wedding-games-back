package com.weddinggames.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class InvitationFlowIT extends AbstractIntegrationTest {

    @Autowired
    private ParticipantRepository participantRepository;

    private Participant findJessika() {
        return participantRepository.findAll().stream()
                .filter(p -> p.getFirstName().equals("Jessika") && p.getLastName().equals("Dijoux"))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Other test methods in this class generate invitations for Jessika, and this integration test
     * has no per-method rollback (real Postgres via Testcontainers), so a test that asserts "no active
     * invitation exists" must use a participant no other test ever touches, not the shared fixture.
     */
    private Participant createParticipantWithNoInvitationEver() {
        Participant jessika = findJessika();
        Participant participant = new Participant(
                jessika.getEvent(),
                "NeverInvited",
                "Test-" + java.util.UUID.randomUUID(),
                "Never Invited",
                null,
                com.weddinggames.backend.participant.ParticipantType.GUEST);
        return participantRepository.save(participant);
    }

    private String generateInvitationToken(Cookie adminCookie, java.util.UUID participantId) throws Exception {
        return generateInvitation(adminCookie, participantId).get("rawToken").asText();
    }

    private JsonNode generateInvitation(Cookie adminCookie, java.util.UUID participantId) throws Exception {
        MvcResult result = mockMvc.perform(post(
                                "/api/v1/admin/participants/{id}/invitation", participantId)
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void resolvesAValidInvitationToken() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        String token = generateInvitationToken(adminCookie, jessika.getId());

        mockMvc.perform(get("/api/v1/invitations/{token}/resolve", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jessika"))
                .andExpect(jsonPath("$.displayName").value("Jessika Dijoux"));
    }

    @Test
    void rejectsAnInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/{token}/resolve", "not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
    }

    @Test
    void rejectsARevokedTokenAfterRegeneration() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        String firstToken = generateInvitationToken(adminCookie, jessika.getId());
        // Regenerating must invalidate the previous token.
        generateInvitationToken(adminCookie, jessika.getId());

        mockMvc.perform(get("/api/v1/invitations/{token}/resolve", firstToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
    }

    @Test
    void confirmingCreatesAParticipantSessionRestorableViaSessionMe() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        String token = generateInvitationToken(adminCookie, jessika.getId());

        MvcResult confirmResult = mockMvc.perform(post("/api/v1/invitations/{token}/confirm", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Jessika Dijoux"))
                .andReturn();
        Cookie participantCookie = confirmResult.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertThat(participantCookie).isNotNull();

        mockMvc.perform(get("/api/v1/session/me").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorType").value("PARTICIPANT"))
                .andExpect(jsonPath("$.participant.displayName").value("Jessika Dijoux"));
    }

    @Test
    void reconnectingPreservesPointsAndWins() throws Exception {
        Participant jessika = findJessika();
        jessika.setTotalPoints(42);
        jessika.setTotalWins(3);
        participantRepository.save(jessika);

        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        String token = generateInvitationToken(adminCookie, jessika.getId());

        MvcResult confirmResult = mockMvc.perform(post("/api/v1/invitations/{token}/confirm", token))
                .andExpect(status().isOk())
                .andReturn();
        Cookie participantCookie = confirmResult.getResponse().getCookie(SESSION_COOKIE_NAME);

        // Guest closes the app and comes back later: same session cookie, same score.
        mockMvc.perform(get("/api/v1/session/me").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participant.totalPoints").value(42))
                .andExpect(jsonPath("$.participant.totalWins").value(3));
    }

    @Test
    void revokingAnInvitationInvalidatesItWithoutIssuingANewOne() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        String token = generateInvitationToken(adminCookie, jessika.getId());

        mockMvc.perform(post("/api/v1/admin/participants/{id}/invitation/revoke", jessika.getId()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/invitations/{token}/resolve", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));

        mockMvc.perform(get("/api/v1/admin/participants/{id}/invitation", jessika.getId()).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokingWithoutAnActiveInvitationReturnsNotFound() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant neverInvited = createParticipantWithNoInvitationEver();

        mockMvc.perform(post("/api/v1/admin/participants/{id}/invitation/revoke", neverInvited.getId())
                        .cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolvesAndConfirmsIdentityViaTheFallbackCode() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        String fallbackCode = generateInvitation(adminCookie, jessika.getId()).get("fallbackCode").asText();
        assertThat(fallbackCode).hasSize(6);

        mockMvc.perform(get("/api/v1/invitations/fallback/{code}/resolve", fallbackCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Jessika Dijoux"));

        MvcResult confirmResult = mockMvc.perform(post("/api/v1/invitations/fallback/{code}/confirm", fallbackCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Jessika Dijoux"))
                .andReturn();
        assertThat(confirmResult.getResponse().getCookie(SESSION_COOKIE_NAME)).isNotNull();
    }

    @Test
    void rejectsAnUnknownFallbackCode() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/fallback/{code}/resolve", "ZZZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
    }

    @Test
    void regeneratingAnInvitationInvalidatesThePreviousFallbackCodeToo() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        String firstFallbackCode = generateInvitation(adminCookie, jessika.getId()).get("fallbackCode").asText();
        generateInvitation(adminCookie, jessika.getId());

        mockMvc.perform(get("/api/v1/invitations/fallback/{code}/resolve", firstFallbackCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITATION"));
    }

    @Test
    void renewingTheFallbackCodeInvalidatesTheOldOneButKeepsTheQrTokenActive() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        JsonNode initial = generateInvitation(adminCookie, jessika.getId());
        String token = initial.get("rawToken").asText();
        String oldFallbackCode = initial.get("fallbackCode").asText();

        MvcResult renewResult = mockMvc.perform(post(
                                "/api/v1/admin/participants/{id}/invitation/fallback-code/renew", jessika.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn();
        String newFallbackCode =
                objectMapper.readTree(renewResult.getResponse().getContentAsString()).get("fallbackCode").asText();
        assertThat(newFallbackCode).isNotEqualTo(oldFallbackCode);

        mockMvc.perform(get("/api/v1/invitations/fallback/{code}/resolve", oldFallbackCode))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/invitations/fallback/{code}/resolve", newFallbackCode))
                .andExpect(status().isOk());
        // The QR token itself must still be untouched by a fallback-code-only renewal.
        mockMvc.perform(get("/api/v1/invitations/{token}/resolve", token)).andExpect(status().isOk());
    }

    @Test
    void loggingOutRevokesTheSession() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        Participant jessika = findJessika();
        String token = generateInvitationToken(adminCookie, jessika.getId());

        MvcResult confirmResult = mockMvc.perform(post("/api/v1/invitations/{token}/confirm", token))
                .andReturn();
        Cookie participantCookie = confirmResult.getResponse().getCookie(SESSION_COOKIE_NAME);

        mockMvc.perform(post("/api/v1/session/logout").cookie(participantCookie)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/session/me").cookie(participantCookie)).andExpect(status().isUnauthorized());
    }
}
