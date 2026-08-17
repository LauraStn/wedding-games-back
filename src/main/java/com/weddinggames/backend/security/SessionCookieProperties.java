package com.weddinggames.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.session-cookie")
public class SessionCookieProperties {

    /** Cookie name carrying the opaque session token. */
    private String name = "wg_session";

    /** Session lifetime in seconds; also used as the cookie max-age. */
    private long maxAgeSeconds = 60L * 60 * 24 * 30;

    /** Must be true in production (HTTPS-only cookie). Disabled by default in dev over plain HTTP. */
    private boolean secure = true;

    /** Lax is enough since the frontend calls the API with credentials from top-level navigation/fetch. */
    private String sameSite = "Lax";

    /** Optional cookie domain, left unset (host-only cookie) unless the frontend is on a sibling subdomain. */
    private String domain;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
