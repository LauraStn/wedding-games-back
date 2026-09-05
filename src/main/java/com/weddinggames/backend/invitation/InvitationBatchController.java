package com.weddinggames.backend.invitation;

import com.weddinggames.backend.invitation.dto.InvitationBatchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/participants/invitations/batch")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Invitations", description = "Generation et regeneration des invitations (reserve a l'administrateur)")
public class InvitationBatchController {

    private final InvitationService invitationService;
    private final InvitationPrintSheetService invitationPrintSheetService;

    public InvitationBatchController(
            InvitationService invitationService, InvitationPrintSheetService invitationPrintSheetService) {
        this.invitationService = invitationService;
        this.invitationPrintSheetService = invitationPrintSheetService;
    }

    @PostMapping(produces = "application/pdf")
    @Operation(
            summary = "Genere (ou regenere) une invitation pour tout ou partie des participants de l'evenement, "
                    + "et retourne directement la planche d'impression PDF des QR",
            description = "Les jetons bruts ne sont jamais retournes en JSON: ils n'existent que le temps de "
                    + "produire les QR de cette planche, puis disparaissent definitivement.")
    public ResponseEntity<byte[]> generateBatchAndPrintSheet(
            @PathVariable UUID eventId, @RequestBody(required = false) InvitationBatchRequest request) {
        var participantIds = request == null ? null : request.participantIds();
        var cards = invitationService.generateBatch(eventId, participantIds);
        byte[] pdf = invitationPrintSheetService.buildPrintSheet(cards);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("invitations-qr.pdf").build().toString())
                .body(pdf);
    }
}
