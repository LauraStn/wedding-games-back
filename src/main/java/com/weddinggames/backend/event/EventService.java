package com.weddinggames.backend.event;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.dto.EventConfigUpdateRequest;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final WeddingEventRepository weddingEventRepository;

    public EventService(WeddingEventRepository weddingEventRepository) {
        this.weddingEventRepository = weddingEventRepository;
    }

    @Transactional(readOnly = true)
    public WeddingEvent getBySlug(String slug) {
        return weddingEventRepository
                .findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Aucun evenement ne correspond a ce slug."));
    }

    @Transactional(readOnly = true)
    public WeddingEvent get(UUID eventId) {
        return weddingEventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Evenement introuvable."));
    }

    /** Each deployment hosts a single wedding: the oldest event is unambiguously "the" event. */
    @Transactional(readOnly = true)
    public WeddingEvent getCurrent() {
        List<WeddingEvent> events = weddingEventRepository.findAll();
        return events.stream()
                .min(Comparator.comparing(WeddingEvent::getCreatedAt))
                .orElseThrow(() -> new NotFoundException("Aucun evenement n'est configure sur cette instance."));
    }

    @Transactional
    public WeddingEvent ensureEventExists(String slug, String title, String language) {
        return weddingEventRepository
                .findBySlug(slug)
                .orElseGet(() -> weddingEventRepository.save(new WeddingEvent(slug, title, language)));
    }

    /** Updates the event's theme and wedding details. Never touches games or participants. */
    @Transactional
    public WeddingEvent updateConfiguration(UUID eventId, EventConfigUpdateRequest request) {
        WeddingEvent event = get(eventId);
        event.setTitle(request.title());
        event.setSpouseOneName(request.spouseOneName());
        event.setSpouseTwoName(request.spouseTwoName());
        event.setEventDate(request.eventDate());
        event.setVenueName(request.venueName());
        event.setWelcomeMessage(request.welcomeMessage());
        event.setVisualConfig(request.visualConfig() != null ? request.visualConfig() : new HashMap<>());
        return event;
    }
}
