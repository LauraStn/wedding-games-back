package com.weddinggames.backend.participant;

import com.weddinggames.backend.participant.dto.ParticipantImportConfirmRequest;
import com.weddinggames.backend.participant.dto.ParticipantImportConfirmResponse;
import com.weddinggames.backend.participant.dto.ParticipantImportPreviewResponse;
import com.weddinggames.backend.participant.dto.ParticipantResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/participants/import")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Import participants", description = "Import CSV/Excel des participants avec previsualisation")
public class ParticipantImportController {

    private final ParticipantImportService participantImportService;

    public ParticipantImportController(ParticipantImportService participantImportService) {
        this.participantImportService = participantImportService;
    }

    @PostMapping(value = "/preview", consumes = "multipart/form-data")
    public ParticipantImportPreviewResponse preview(@PathVariable UUID eventId, @RequestParam("file") MultipartFile file) {
        return ParticipantImportPreviewResponse.from(participantImportService.preview(eventId, file));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ParticipantImportConfirmResponse> confirm(
            @PathVariable UUID eventId, @Valid @RequestBody ParticipantImportConfirmRequest request) {
        var created = participantImportService.confirm(eventId, request.rows()).stream()
                .map(ParticipantResponse::from)
                .toList();
        return ResponseEntity.ok(ParticipantImportConfirmResponse.from(created));
    }
}
