package com.weddinggames.backend.exclusion;

import com.weddinggames.backend.exclusion.dto.PairingCheckResponse;
import com.weddinggames.backend.exclusion.dto.PairingExclusionCreateRequest;
import com.weddinggames.backend.exclusion.dto.PairingExclusionReasonUpdateRequest;
import com.weddinggames.backend.exclusion.dto.PairingExclusionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Exclusions", description = "Gestion des exclusions de matchmaking (reserve a l'administrateur)")
public class PairingExclusionController {

    private final PairingExclusionService pairingExclusionService;
    private final PairingConstraintService pairingConstraintService;

    public PairingExclusionController(
            PairingExclusionService pairingExclusionService, PairingConstraintService pairingConstraintService) {
        this.pairingExclusionService = pairingExclusionService;
        this.pairingConstraintService = pairingConstraintService;
    }

    @GetMapping("/api/v1/admin/events/{eventId}/exclusions")
    public List<PairingExclusionResponse> list(@PathVariable UUID eventId) {
        return pairingExclusionService.listByEvent(eventId).stream()
                .map(PairingExclusionResponse::from)
                .toList();
    }

    @PostMapping("/api/v1/admin/events/{eventId}/exclusions")
    public ResponseEntity<PairingExclusionResponse> create(
            @PathVariable UUID eventId, @Valid @RequestBody PairingExclusionCreateRequest request) {
        PairingExclusion created = pairingExclusionService.create(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PairingExclusionResponse.from(created));
    }

    @GetMapping("/api/v1/admin/exclusions/{id}")
    public PairingExclusionResponse get(@PathVariable UUID id) {
        return PairingExclusionResponse.from(pairingExclusionService.get(id));
    }

    @PatchMapping("/api/v1/admin/exclusions/{id}")
    public PairingExclusionResponse updateReason(
            @PathVariable UUID id, @Valid @RequestBody PairingExclusionReasonUpdateRequest request) {
        return PairingExclusionResponse.from(pairingExclusionService.updateReason(id, request.reason()));
    }

    @DeleteMapping("/api/v1/admin/exclusions/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pairingExclusionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/events/{eventId}/exclusions/check")
    public PairingCheckResponse check(
            @PathVariable UUID eventId,
            @RequestParam UUID participantAId,
            @RequestParam UUID participantBId) {
        return new PairingCheckResponse(
                pairingConstraintService.canPair(eventId, participantAId, participantBId),
                pairingConstraintService.hasHardExclusion(eventId, participantAId, participantBId));
    }
}
