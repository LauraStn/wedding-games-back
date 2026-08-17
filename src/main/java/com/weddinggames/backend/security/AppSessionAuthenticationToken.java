package com.weddinggames.backend.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AppSessionAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedActor actor;

    public AppSessionAuthenticationToken(AuthenticatedActor actor) {
        super(List.of(new SimpleGrantedAuthority(actor.role().authority())));
        this.actor = actor;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return actor;
    }

    public AuthenticatedActor getActor() {
        return actor;
    }
}
