package com.weddinggames.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the opaque session cookie into a Spring Security {@link org.springframework.security.core.Authentication}.
 * Absence or invalidity of the cookie simply leaves the request unauthenticated;
 * authorization rules decide whether that is acceptable for the target endpoint.
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    public SessionAuthenticationFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        sessionService
                .readCookie(request)
                .flatMap(sessionService::resolveAndTouch)
                .ifPresent(actor -> SecurityContextHolder.getContext()
                        .setAuthentication(new AppSessionAuthenticationToken(actor)));
        filterChain.doFilter(request, response);
    }
}
