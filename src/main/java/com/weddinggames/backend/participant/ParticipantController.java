package com.weddinggames.backend.participant;

import com.weddinggames.backend.participant.dto.ParticipantCreateRequest;
import com.weddinggames.backend.participant.dto.ParticipantResponse;
import com.weddinggames.backend.participant.dto.ParticipantStatusUpdateRequest;
import com.weddinggames.backend.participant.dto.ParticipantTableUpdateRequest;
import com.weddinggames.backend.participant.dto.ParticipantUpdateRequest;
import com.weddinggames.backend.security.AuthenticatedActor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Participants", description = "Gestion des participants (reserve a l'administrateur)")
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantExportService participantExportService;

    public ParticipantController(
            ParticipantService participantService, ParticipantExportService participantExportService) {
        this.participantService = participantService;
        this.participantExportService = participantExportService;
    }

    @GetMapping("/events/{eventId}/participants")
    public List<ParticipantResponse> list(
            @PathVariable UUID eventId,
            @RequestParam(required = false) ParticipantStatus status,
            @RequestParam(required = false) String tableLabel,
            @RequestParam(required = false) ParticipantType participantType,
            @RequestParam(required = false) String query) {
        return participantService.search(eventId, status, tableLabel, participantType, query).stream()
                .map(ParticipantResponse::from)
                .toList();
    }

    @GetMapping("/events/{eventId}/participants/export")
    public ResponseEntity<byte[]> export(
            @PathVariable UUID eventId,
            @RequestParam(required = false) ParticipantStatus status,
            @RequestParam(required = false) String tableLabel,
            @RequestParam(required = false) ParticipantType participantType,
            @RequestParam(required = false) String query) {
        byte[] csv = participantExportService.exportCsv(eventId, status, tableLabel, participantType, query);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("participants.csv").build().toString())
                .body(csv);
    }

    @PostMapping("/events/{eventId}/participants")
    public ResponseEntity<ParticipantResponse> create(
            @PathVariable UUID eventId, @Valid @RequestBody ParticipantCreateRequest request) {
        Participant created = participantService.create(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ParticipantResponse.from(created));
    }

    @GetMapping("/participants/{id}")
    public ParticipantResponse get(@PathVariable UUID id) {
        return ParticipantResponse.from(participantService.get(id));
    }

    @PutMapping("/participants/{id}")
    public ParticipantResponse update(@PathVariable UUID id, @Valid @RequestBody ParticipantUpdateRequest request) {
        return ParticipantResponse.from(participantService.update(id, request));
    }

    @DeleteMapping("/participants/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedActor actor) {
        participantService.delete(id, actor.staffAccountId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/participants/{id}/disable")
    public ParticipantResponse disable(@PathVariable UUID id) {
        return ParticipantResponse.from(participantService.disable(id));
    }

    @PatchMapping("/participants/{id}/status")
    public ParticipantResponse updateStatus(
            @PathVariable UUID id, @Valid @RequestBody ParticipantStatusUpdateRequest request) {
        return ParticipantResponse.from(participantService.updateStatus(id, request.status()));
    }

    @PatchMapping("/participants/{id}/table")
    public ParticipantResponse updateTable(
            @PathVariable UUID id, @Valid @RequestBody ParticipantTableUpdateRequest request) {
        return ParticipantResponse.from(participantService.updateTable(id, request.tableLabel()));
    }
}
