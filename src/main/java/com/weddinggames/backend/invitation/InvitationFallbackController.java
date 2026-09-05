package com.weddinggames.backend.invitation;

import com.weddinggames.backend.invitation.dto.InvitationResolveResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.dto.ParticipantSessionResponse;
import com.weddinggames.backend.security.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations/fallback")
@Tag(
        name = "Invitations",
        description = "Resolution et confirmation d'une invitation par code de secours (QR perdu ou illisible)")
public class InvitationFallbackController {

    private final InvitationService invitationService;
    private final SessionService sessionService;

    public InvitationFallbackController(InvitationService invitationService, SessionService sessionService) {
        this.invitationService = invitationService;
        this.sessionService = sessionService;
    }

    @GetMapping("/{code}/resolve")
    @Operation(summary = "Resout un code de secours en identite a faire confirmer par l'invite")
    public InvitationResolveResponse resolve(@PathVariable String code) {
        return InvitationResolveResponse.from(invitationService.resolveByFallbackCode(code));
    }

    @PostMapping("/{code}/confirm")
    @Operation(summary = "Confirme l'identite via le code de secours et ouvre une session participante")
    public ParticipantSessionResponse confirm(@PathVariable String code, HttpServletResponse response) {
        Participant participant = invitationService.confirmByFallbackCode(code);
        String rawSessionToken = sessionService.createParticipantSession(participant.getId());
        sessionService.attachCookie(response, rawSessionToken);
        return ParticipantSessionResponse.from(participant);
    }
}
