package com.weddinggames.backend.event.dto;

import com.weddinggames.backend.event.EventStatus;
import com.weddinggames.backend.event.WeddingEvent;
import java.util.Map;
import java.util.UUID;

public record EventPublicConfigResponse(
        UUID id, String slug, String title, String language, EventStatus status, Map<String, Object> visualConfig) {

    public static EventPublicConfigResponse from(WeddingEvent event) {
        return new EventPublicConfigResponse(
                event.getId(),
                event.getSlug(),
                event.getTitle(),
                event.getLanguage(),
                event.getStatus(),
                event.getVisualConfig());
    }
}
