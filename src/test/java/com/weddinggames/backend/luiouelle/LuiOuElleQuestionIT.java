package com.weddinggames.backend.luiouelle;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class LuiOuElleQuestionIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private WeddingEvent createEvent() {
        return weddingEventRepository.save(
                new WeddingEvent("lui-ou-elle-test-" + UUID.randomUUID(), "Lui ou Elle Test Event", "fr-FR"));
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

    private void openLobby(Cookie adminCookie, UUID eventId) throws Exception {
        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/open", eventId).cookie(adminCookie))
                .andExpect(status().isOk());
    }

    @Test
    void proposesAQuestionWhileTheLobbyIsOpen() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);

        mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Qui est le plus bordelique ?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Qui est le plus bordelique ?"))
                .andExpect(jsonPath("$.authorId").value(alice.toString()));

        mockMvc.perform(get("/api/v1/lui-ou-elle/questions/me").cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void rejectsAThirdQuestionBeyondTheDefaultLimitOfTwo() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                            .cookie(aliceCookie)
                            .contentType("application/json")
                            .content("""
                                    {"content":"Question %d ?"}
                                    """.formatted(i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Question de trop ?"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LUI_OU_ELLE_QUESTION_LIMIT_REACHED"));
    }

    @Test
    void rejectsProposingBeforeTheLobbyIsOpen() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        Cookie aliceCookie = loginAsParticipant(alice);

        mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Trop tot ?"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOBBY_NOT_OPEN"));
    }

    @Test
    void editsMyOwnQuestionWhileTheLobbyIsStillOpen() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);

        var created = mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Premiere version"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID questionId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText());

        mockMvc.perform(put("/api/v1/lui-ou-elle/questions/{id}", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Version corrigee"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Version corrigee"));
    }

    @Test
    void cannotEditAnymoreOnceTheLobbyIsLocked() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);

        var created = mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Premiere version"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID questionId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText());

        mockMvc.perform(post("/api/v1/staff/events/{eventId}/lobby/lock", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/lui-ou-elle/questions/{id}", questionId)
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Trop tard"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOBBY_NOT_OPEN"));
    }

    @Test
    void cannotEditAnotherParticipantsQuestion() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        UUID alice = createParticipant(adminCookie, event.getId(), "Alice");
        UUID bob = createParticipant(adminCookie, event.getId(), "Bob");
        openLobby(adminCookie, event.getId());
        Cookie aliceCookie = loginAsParticipant(alice);
        Cookie bobCookie = loginAsParticipant(bob);

        var created = mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(aliceCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Question d'Alice"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID questionId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText());

        mockMvc.perform(put("/api/v1/lui-ou-elle/questions/{id}", questionId)
                        .cookie(bobCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Vole par Bob"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCannotProposeQuestions() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        WeddingEvent event = createEvent();
        openLobby(adminCookie, event.getId());

        mockMvc.perform(post("/api/v1/lui-ou-elle/questions")
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"content":"Une question staff ?"}
                                """))
                .andExpect(status().isForbidden());
    }
}
