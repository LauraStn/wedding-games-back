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
@RequestMapping("/api/v1/invitations")
@Tag(name = "Invitations", description = "Resolution et confirmation d'une invitation par jeton opaque")
public class InvitationPublicController {

    private final InvitationService invitationService;
    private final SessionService sessionService;

    public InvitationPublicController(InvitationService invitationService, SessionService sessionService) {
        this.invitationService = invitationService;
        this.sessionService = sessionService;
    }

    @GetMapping("/{token}/resolve")
    @Operation(summary = "Resout un jeton d'invitation en identite a faire confirmer par l'invite")
    public InvitationResolveResponse resolve(@PathVariable String token) {
        return InvitationResolveResponse.from(invitationService.resolve(token));
    }

    @PostMapping("/{token}/confirm")
    @Operation(summary = "Confirme l'identite et ouvre une session participante opaque via cookie HttpOnly")
    public ParticipantSessionResponse confirm(@PathVariable String token, HttpServletResponse response) {
        Participant participant = invitationService.confirm(token);
        String rawSessionToken = sessionService.createParticipantSession(participant.getId());
        sessionService.attachCookie(response, rawSessionToken);
        return ParticipantSessionResponse.from(participant);
    }
}
