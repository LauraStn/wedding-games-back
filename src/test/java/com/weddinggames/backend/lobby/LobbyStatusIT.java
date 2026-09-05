package com.weddinggames.backend.lobby;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.ParticipantType;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Each test creates its own dedicated event: the lobby status machine is stateful and this class
 * shares a real Postgres instance (no per-test rollback) with every other IT, including
 * RolePermissionsIT, which also opens the shared "seed-wedding" lobby.
 */
class LobbyStatusIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("lobby-test-" + UUID.randomUUID(), "Lobby Test Event", "fr-FR"));
    }

    private Participant createParticipant(WeddingEvent event) {
        return participantRepository.save(
                new Participant(event, "Guest", "Test-" + UUID.randomUUID(), "Guest Test", null, ParticipantType.GUEST));
    }

    @Test
    void staffCanDriveTheLobbyThroughItsFullLifecycle() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/open", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/lock", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/start", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/pause", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/resume", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/finish", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void startingASessionWithoutLockingFirstIsRejected() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/open", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/start", event.getId()).cookie(adminCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_LOBBY_TRANSITION"));
    }

    @Test
    void participantSeesTheLobbyStatusPresentCountAndWelcomeMessageWithoutStaffData() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        event.setWelcomeMessage("Bienvenue au mariage !");
        weddingEventRepository.save(event);
        Participant participant = createParticipant(event);
        Cookie participantCookie = loginAsParticipant(participant.getId());

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/open", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(participantCookie)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/lobby").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.welcomeMessage").value("Bienvenue au mariage !"));
    }

    @Test
    void staffRolesCannotAccessTheParticipantLobbyStatusEndpoint() throws Exception {
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/lobby").cookie(intervenantCookie)).andExpect(status().isForbidden());
    }

    @Test
    void markingReadyIsDistinctFromConnectedAndSurvivesAHeartbeat() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        Participant participant = createParticipant(event);
        Cookie participantCookie = loginAsParticipant(participant.getId());

        mockMvc.perform(post("/api/v1/lobby/ready").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus").value("READY"));

        // A routine heartbeat afterwards must not downgrade the explicit "ready" declaration.
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(participantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus").value("READY"));

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/lobby/participants", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].connectionStatus").value("READY"));
    }

    @Test
    void flagsTwoParticipantsWithTheSameNameAsPossibleDuplicatesInTheStaffView() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        Participant first = participantRepository.save(
                new Participant(event, "Marie", "Curie", "Marie Curie", null, ParticipantType.GUEST));
        Participant second = participantRepository.save(
                new Participant(event, "marie", "curie", "Marie C.", null, ParticipantType.GUEST));
        Cookie firstCookie = loginAsParticipant(first.getId());
        Cookie secondCookie = loginAsParticipant(second.getId());

        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(firstCookie)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(secondCookie)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/lobby/participants", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].possibleDuplicate").value(true))
                .andExpect(jsonPath("$[1].possibleDuplicate").value(true));
    }

    @Test
    void flagsAParticipantLoggedInFromTwoDevicesAsPossibleQrReuse() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        Participant participant = createParticipant(event);
        // Two independent sessions for the same participant, as if two devices used the same QR/code.
        Cookie deviceOne = loginAsParticipant(participant.getId());
        loginAsParticipant(participant.getId());

        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(deviceOne)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/lobby/participants", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].possibleQrReuse").value(true));
    }

    @Test
    void doesNotFlagASingleSessionParticipantAsQrReuseOrDuplicate() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        Participant participant = createParticipant(event);
        Cookie participantCookie = loginAsParticipant(participant.getId());

        mockMvc.perform(post("/api/v1/lobby/heartbeat").cookie(participantCookie)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/staff/events/{eventId}/lobby/participants", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].possibleDuplicate").value(false))
                .andExpect(jsonPath("$[0].possibleQrReuse").value(false));
    }
}
