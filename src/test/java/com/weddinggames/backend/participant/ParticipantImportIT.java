package com.weddinggames.backend.participant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.staff.StaffRole;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class ParticipantImportIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    private UUID eventId() {
        return weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
    }

    @Test
    void previewReportsValidDuplicateAndRejectedRowsWithoutPersistingAnything() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        UUID eventId = eventId();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "participants.csv",
                "text/csv",
                ("prenom,nom,table\n"
                                + "Alice,Wonderland,Table 5\n"
                                + "Jessika,Dijoux,Table 1\n"
                                + ",Sansnom,Table 1\n")
                        .getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/admin/events/{eventId}/participants/import/preview", eventId)
                        .file(file)
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.validCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.rejectedCount").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("VALID"))
                .andExpect(jsonPath("$.rows[1].status").value("DUPLICATE_EXISTING"))
                .andExpect(jsonPath("$.rows[2].status").value("REJECTED"));
    }

    @Test
    void confirmPersistsTheSelectedRows() throws Exception {
        Cookie adminCookie = loginAsNewStaff(StaffRole.ADMIN);
        UUID eventId = eventId();

        String body =
                """
                {"rows":[{"firstName":"Bob","lastName":"Builder","displayName":"Bob Builder","tableLabel":"Table 9","participantType":"GUEST"}]}
                """;

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/participants/import/confirm", eventId)
                        .cookie(adminCookie)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.created[0].displayName").value("Bob Builder"));
    }

    @Test
    void nonAdminStaffCannotAccessTheImportEndpoints() throws Exception {
        Cookie intervenantCookie = loginAsNewStaff(StaffRole.INTERVENANT);
        UUID eventId = eventId();

        MockMultipartFile file =
                new MockMultipartFile("file", "participants.csv", "text/csv", "prenom,nom\nA,B\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/admin/events/{eventId}/participants/import/preview", eventId)
                        .file(file)
                        .cookie(intervenantCookie))
                .andExpect(status().isForbidden());
    }
}
