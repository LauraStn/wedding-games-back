package com.weddinggames.backend.control;

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
class GameControlLockIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("control-lock-test-" + UUID.randomUUID(), "Control Lock Test", "fr-FR"));
    }

    private UUID createGame(Cookie adminCookie, UUID eventId) throws Exception {
        var result = mockMvc.perform(post("/api/v1/admin/events/{eventId}/games", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"type":"QUIZ","title":"Quiz absurde","sequence":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    @Test
    void isUnclaimedInitiallyThenClaimableByOneIntervenant() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie aliceCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/staff/games/{gameId}/control-lock", gameId).cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderStaffAccountId").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/claim", gameId).cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderDisplayName").value(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void asecondIntervenantCannotClaimAnAlreadyHeldLock() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie aliceCookie = loginAsNewStaff(StaffRole.INTERVENANT);
        Cookie bobCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/claim", gameId).cookie(aliceCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/claim", gameId).cookie(bobCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_CONTROL_LOCKED"));
    }

    @Test
    void theHolderCanReleaseAndSomeoneElseCanThenClaim() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie aliceCookie = loginAsNewStaff(StaffRole.INTERVENANT);
        Cookie bobCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/claim", gameId).cookie(aliceCookie))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/release", gameId).cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderStaffAccountId").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/claim", gameId).cookie(bobCookie))
                .andExpect(status().isOk());
    }

    @Test
    void aNonHolderCannotReleaseButAnAdminCanForceIt() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie aliceCookie = loginAsNewStaff(StaffRole.INTERVENANT);
        Cookie bobCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/claim", gameId).cookie(aliceCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/release", gameId).cookie(bobCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_CONTROL_NOT_HELD_BY_YOU"));

        mockMvc.perform(post("/api/v1/staff/games/{gameId}/control-lock/release", gameId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderStaffAccountId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void juryRoleCannotAccessTheControlLock() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID gameId = createGame(adminCookie, event.getId());
        Cookie juryCookie = loginAsNewStaff(StaffRole.JURY);

        mockMvc.perform(get("/api/v1/staff/games/{gameId}/control-lock", gameId).cookie(juryCookie))
                .andExpect(status().isForbidden());
    }
}
