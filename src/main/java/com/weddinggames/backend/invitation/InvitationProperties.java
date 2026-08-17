package com.weddinggames.backend.invitation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.invitation")
public class InvitationProperties {

    /** Frontend base URL the opaque token is appended to, e.g. https://mariage.example.com/invite */
    private String baseUrl = "http://localhost:5173/invite";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
