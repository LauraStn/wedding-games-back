package com.weddinggames.backend.staff;

import com.weddinggames.backend.security.SessionService;
import com.weddinggames.backend.staff.dto.StaffAccountResponse;
import com.weddinggames.backend.staff.dto.StaffLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/staff")
@Tag(name = "Authentification staff", description = "Connexion des membres de l'organisation (mot de passe BCrypt)")
public class StaffAuthController {

    private final StaffAccountService staffAccountService;
    private final SessionService sessionService;

    public StaffAuthController(StaffAccountService staffAccountService, SessionService sessionService) {
        this.staffAccountService = staffAccountService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifie un membre de l'organisation et ouvre une session opaque via cookie HttpOnly")
    public StaffAccountResponse login(@Valid @RequestBody StaffLoginRequest request, HttpServletResponse response) {
        StaffAccount account = staffAccountService.authenticate(request.username(), request.password());
        String rawSessionToken =
                sessionService.createStaffSession(account.getId(), account.getRole().toSecurityRole());
        sessionService.attachCookie(response, rawSessionToken);
        return StaffAccountResponse.from(account);
    }
}
