package com.weddinggames.backend.game;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.game.dto.GameCreateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final WeddingEventRepository weddingEventRepository;

    public GameService(GameRepository gameRepository, WeddingEventRepository weddingEventRepository) {
        this.gameRepository = gameRepository;
        this.weddingEventRepository = weddingEventRepository;
    }

    @Transactional(readOnly = true)
    public List<Game> listByEvent(UUID eventId) {
        return gameRepository.findByEventIdOrderBySequence(eventId);
    }

    @Transactional(readOnly = true)
    public Game get(UUID id) {
        return gameRepository.findById(id).orElseThrow(() -> new NotFoundException("Partie introuvable."));
    }

    @Transactional
    public Game create(UUID eventId, GameCreateRequest request) {
        WeddingEvent event = weddingEventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evenement introuvable."));
        Game game = new Game(event, request.type(), request.title(), request.sequence());
        return gameRepository.save(game);
    }

    @Transactional
    public Game start(UUID id) {
        Game game = get(id);
        game.start();
        return game;
    }

    @Transactional
    public Game pause(UUID id) {
        Game game = get(id);
        game.pause();
        return game;
    }

    @Transactional
    public Game resume(UUID id) {
        Game game = get(id);
        game.resume();
        return game;
    }

    @Transactional
    public Game nextQuestion(UUID id) {
        Game game = get(id);
        game.nextQuestion();
        return game;
    }
}
