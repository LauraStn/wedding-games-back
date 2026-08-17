package com.weddinggames.backend.event;

import com.weddinggames.backend.common.exception.NotFoundException;
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

    @Transactional
    public WeddingEvent ensureEventExists(String slug, String title, String language) {
        return weddingEventRepository
                .findBySlug(slug)
                .orElseGet(() -> weddingEventRepository.save(new WeddingEvent(slug, title, language)));
    }
}
