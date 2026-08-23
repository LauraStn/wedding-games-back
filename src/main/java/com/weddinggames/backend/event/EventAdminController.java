package com.weddinggames.backend.event;

import com.weddinggames.backend.event.dto.EventConfigUpdateRequest;
import com.weddinggames.backend.event.dto.EventPublicConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Evenement", description = "Configuration de l'evenement (theme, textes, informations des maries)")
public class EventAdminController {

    private final EventService eventService;

    public EventAdminController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/api/v1/admin/event")
    @Operation(summary = "Configuration de l'unique evenement de ce deploiement")
    public EventPublicConfigResponse getCurrent() {
        return EventPublicConfigResponse.from(eventService.getCurrent());
    }

    @PutMapping("/api/v1/admin/event")
    @Operation(summary = "Met a jour la configuration de l'unique evenement de ce deploiement")
    public EventPublicConfigResponse updateCurrent(@Valid @RequestBody EventConfigUpdateRequest request) {
        UUID currentId = eventService.getCurrent().getId();
        return EventPublicConfigResponse.from(eventService.updateConfiguration(currentId, request));
    }

    @GetMapping("/api/v1/admin/events/{eventId}")
    @Operation(summary = "Configuration d'un evenement donne (usage multi-evenements)")
    public EventPublicConfigResponse get(@PathVariable UUID eventId) {
        return EventPublicConfigResponse.from(eventService.get(eventId));
    }

    @PutMapping("/api/v1/admin/events/{eventId}")
    @Operation(summary = "Met a jour la configuration d'un evenement donne (usage multi-evenements)")
    public EventPublicConfigResponse update(
            @PathVariable UUID eventId, @Valid @RequestBody EventConfigUpdateRequest request) {
        return EventPublicConfigResponse.from(eventService.updateConfiguration(eventId, request));
    }
}
