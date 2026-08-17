package com.weddinggames.backend.security;

import com.weddinggames.backend.common.OpaqueTokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final AppSessionRepository appSessionRepository;
    private final SessionCookieProperties cookieProperties;
    private final Clock clock;

    public SessionService(
            AppSessionRepository appSessionRepository, SessionCookieProperties cookieProperties, Clock clock) {
        this.appSessionRepository = appSessionRepository;
        this.cookieProperties = cookieProperties;
        this.clock = clock;
    }

    @Transactional
    public String createParticipantSession(UUID participantId) {
        return createSession(ActorType.PARTICIPANT, participantId, null, Role.PARTICIPANT);
    }

    @Transactional
    public String createStaffSession(UUID staffAccountId, Role role) {
        return createSession(ActorType.STAFF, null, staffAccountId, role);
    }

    private String createSession(ActorType actorType, UUID participantId, UUID staffAccountId, Role role) {
        String rawToken = OpaqueTokenGenerator.generateRawToken();
        Instant expiresAt = Instant.now(clock).plus(Duration.ofSeconds(cookieProperties.getMaxAgeSeconds()));
        AppSession session = new AppSession(
                actorType, participantId, staffAccountId, role, OpaqueTokenGenerator.hash(rawToken), expiresAt);
        appSessionRepository.save(session);
        return rawToken;
    }

    @Transactional
    public Optional<AuthenticatedActor> resolveAndTouch(String rawToken) {
        String hash = OpaqueTokenGenerator.hash(rawToken);
        Optional<AppSession> found = appSessionRepository.findBySessionTokenHash(hash);
        Instant now = Instant.now(clock);
        return found.filter(session -> session.isValid(now)).map(session -> {
            session.touch(now);
            return new AuthenticatedActor(
                    session.getId(),
                    session.getActorType(),
                    session.getParticipantId(),
                    session.getStaffAccountId(),
                    session.getRole());
        });
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = OpaqueTokenGenerator.hash(rawToken);
        appSessionRepository.findBySessionTokenHash(hash).ifPresent(session -> session.revoke(Instant.now(clock)));
    }

    public void attachCookie(HttpServletResponse response, String rawToken) {
        response.addHeader(
                org.springframework.http.HttpHeaders.SET_COOKIE,
                buildCookie(rawToken, cookieProperties.getMaxAgeSeconds()).toString());
    }

    public void clearCookie(HttpServletResponse response) {
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
    }

    public Optional<String> readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieProperties.getName().equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.getName(), value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(maxAgeSeconds);
        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }
        return builder.build();
    }
}
