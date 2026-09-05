package com.weddinggames.backend.character;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GameCharacterAdminIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private UUID eventId() {
        return weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
    }

    @Test
    void createsListsUpdatesAndDeactivatesACharacter() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        UUID eventId = eventId();

        var createResult = mockMvc.perform(post("/api/v1/admin/events/{eventId}/characters", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"name":"Sangoku","description":"Super Saiyan","avatarUrl":null}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String id = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/characters", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItem("Sangoku")));

        mockMvc.perform(put("/api/v1/admin/characters/{id}", id)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(
                                """
                                {"name":"Sangoku","description":"Le plus fort de l'univers","avatarUrl":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Le plus fort de l'univers"));

        mockMvc.perform(post("/api/v1/admin/characters/{id}/deactivate", id).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/v1/admin/characters/{id}/activate", id).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(delete("/api/v1/admin/characters/{id}", id).cookie(adminCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsCreatingTwoCharactersWithTheSameNameForTheSameEvent() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        UUID eventId = eventId();
        String body =
                """
                {"name":"Sailor Moon","description":null,"avatarUrl":null}
                """;

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/characters", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/characters", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHARACTER_NAME_TAKEN"));
    }

    @Test
    void rejectsAnInvalidCharacterCreationRequestWithAConsistentErrorEnvelope() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        UUID eventId = eventId();

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/characters", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content("""
                                {"name":"","description":null,"avatarUrl":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void nonAdminStaffCannotManageTheCharacterCatalog() throws Exception {
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/characters", eventId()).cookie(intervenantCookie))
                .andExpect(status().isForbidden());
    }
}
