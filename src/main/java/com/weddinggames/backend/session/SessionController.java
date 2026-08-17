package com.weddinggames.backend.session;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.dto.ParticipantSessionResponse;
import com.weddinggames.backend.security.AuthenticatedActor;
import com.weddinggames.backend.security.SessionService;
import com.weddinggames.backend.session.dto.SessionMeResponse;
import com.weddinggames.backend.staff.StaffAccount;
import com.weddinggames.backend.staff.StaffAccountRepository;
import com.weddinggames.backend.staff.dto.StaffAccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/session")
@Tag(name = "Session", description = "Session courante (participant ou staff) portee par le cookie opaque")
public class SessionController {

    private final SessionService sessionService;
    private final ParticipantRepository participantRepository;
    private final StaffAccountRepository staffAccountRepository;

    public SessionController(
            SessionService sessionService,
            ParticipantRepository participantRepository,
            StaffAccountRepository staffAccountRepository) {
        this.sessionService = sessionService;
        this.participantRepository = participantRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    @GetMapping("/me")
    @Operation(summary = "Retourne l'identite, le role et - pour un participant - les points/victoires a jour")
    public SessionMeResponse me(@AuthenticationPrincipal AuthenticatedActor actor) {
        if (actor.isParticipant()) {
            Participant participant = participantRepository
                    .findById(actor.participantId())
                    .orElseThrow(() -> new NotFoundException("Participant introuvable."));
            return SessionMeResponse.ofParticipant(ParticipantSessionResponse.from(participant));
        }
        StaffAccount staff = staffAccountRepository
                .findById(actor.staffAccountId())
                .orElseThrow(() -> new NotFoundException("Compte introuvable."));
        return SessionMeResponse.ofStaff(StaffAccountResponse.from(staff), actor.role());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoque la session courante et efface le cookie")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.readCookie(request).ifPresent(sessionService::revoke);
        sessionService.clearCookie(response);
        return ResponseEntity.noContent().build();
    }
}
