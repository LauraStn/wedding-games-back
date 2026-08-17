package com.weddinggames.backend.event;

import com.weddinggames.backend.event.dto.EventPublicConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Evenement", description = "Configuration publique de l'evenement")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{slug}/public")
    @Operation(summary = "Recupere la configuration publique (non sensible) de l'evenement")
    public EventPublicConfigResponse getPublicConfig(@PathVariable String slug) {
        return EventPublicConfigResponse.from(eventService.getBySlug(slug));
    }
}
