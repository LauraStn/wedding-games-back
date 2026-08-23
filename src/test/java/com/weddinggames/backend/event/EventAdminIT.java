package com.weddinggames.backend.event;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventAdminIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Test
    void rejectsAConfigurationUpdateWithABlankTitleWithAConsistentErrorEnvelope() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();

        String invalidBody = """
                {"title":"","spouseOneName":null,"spouseTwoName":null,"eventDate":null,"venueName":null,"welcomeMessage":null,"visualConfig":null}
                """;

        mockMvc.perform(put("/api/v1/admin/events/{eventId}", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void adminCanConfigureTheEventThemeAndWeddingDetailsWithoutTouchingAnyCode() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        var eventId = weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();

        String validBody = """
                {
                  "title": "Le mariage de Jessika et Sandrine",
                  "spouseOneName": "Jessika",
                  "spouseTwoName": "Sandrine",
                  "eventDate": "2027-06-12",
                  "venueName": "Le Domaine des Ecrins",
                  "welcomeMessage": "Marie-es, prets, jouez !",
                  "visualConfig": {"primaryColor": "#2457ff", "logoUrl": "https://example.com/logo.png"}
                }
                """;

        mockMvc.perform(put("/api/v1/admin/events/{eventId}", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Le mariage de Jessika et Sandrine"))
                .andExpect(jsonPath("$.spouseOneName").value("Jessika"))
                .andExpect(jsonPath("$.spouseTwoName").value("Sandrine"))
                .andExpect(jsonPath("$.eventDate").value("2027-06-12"))
                .andExpect(jsonPath("$.venueName").value("Le Domaine des Ecrins"))
                .andExpect(jsonPath("$.visualConfig.primaryColor").value("#2457ff"));

        mockMvc.perform(get("/api/v1/admin/events/{eventId}", eventId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcomeMessage").value("Marie-es, prets, jouez !"));
    }

    @Test
    void adminCanReadAndUpdateTheCurrentEventWithoutKnowingItsId() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);

        String validBody = """
                {"title":"Le mariage de Jessika et Sandrine","spouseOneName":"Jessika","spouseTwoName":"Sandrine",
                 "eventDate":"2027-06-12","venueName":"Le Domaine des Ecrins","welcomeMessage":null,"visualConfig":null}
                """;

        mockMvc.perform(put("/api/v1/admin/event")
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("seed-wedding"))
                .andExpect(jsonPath("$.venueName").value("Le Domaine des Ecrins"));

        mockMvc.perform(get("/api/v1/admin/event").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spouseOneName").value("Jessika"));
    }

    @Test
    void anEventCreatedWithNoGamesConfiguredIsStillAcceptedByTheDatabase() throws Exception {
        var event = weddingEventRepository.save(new WeddingEvent("mariage-sans-jeux", "Mariage sans jeux configures", "fr-FR"));

        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        mockMvc.perform(get("/api/v1/admin/events/{eventId}", event.getId()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.visualConfig").isEmpty());
    }
}
