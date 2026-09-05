package com.weddinggames.backend.invitation;

import com.weddinggames.backend.invitation.dto.InvitationAdminResponse;
import com.weddinggames.backend.invitation.dto.InvitationFallbackCodeResponse;
import com.weddinggames.backend.invitation.dto.InvitationStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/participants/{participantId}/invitation")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Invitations", description = "Generation et regeneration des invitations (reserve a l'administrateur)")
public class InvitationAdminController {

    private final InvitationService invitationService;

    public InvitationAdminController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    @Operation(
            summary = "Genere une nouvelle invitation, invalidant l'ancien jeton actif s'il existait",
            description = "Le jeton brut n'est retourne qu'une seule fois, dans cette reponse.")
    public ResponseEntity<InvitationAdminResponse> generateOrRegenerate(@PathVariable UUID participantId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.generateOrRegenerate(participantId));
    }

    @GetMapping
    @Operation(summary = "Consulte le statut de l'invitation active d'un participant (sans exposer le jeton)")
    public InvitationStatusResponse getStatus(@PathVariable UUID participantId) {
        return InvitationStatusResponse.from(invitationService.getCurrentInvitation(participantId));
    }

    @PostMapping("/revoke")
    @Operation(
            summary = "Revoque le jeton actif d'un participant sans en emettre un nouveau",
            description = "Utile pour un QR perdu ou compromis: coupe l'acces immediatement, sans reemission "
                    + "instantanee. L'administrateur regenere plus tard via POST .../invitation s'il le souhaite.")
    public ResponseEntity<Void> revoke(@PathVariable UUID participantId) {
        invitationService.revoke(participantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fallback-code/renew")
    @Operation(
            summary = "Renouvelle uniquement le code de secours, sans toucher au jeton QR actif",
            description = "Utile si le participant craint que son code ait ete vu par quelqu'un d'autre, "
                    + "sans avoir a rescanner un nouveau QR.")
    public InvitationFallbackCodeResponse renewFallbackCode(@PathVariable UUID participantId) {
        return new InvitationFallbackCodeResponse(invitationService.renewFallbackCode(participantId));
    }
}
